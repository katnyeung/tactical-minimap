package org.tactical.minimap.scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.repository.TelegramMessage;
import org.tactical.minimap.repository.TelegramMessageRule;
import org.tactical.minimap.repository.marker.InfoMarker;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.repository.marker.PoliceMarker;
import org.tactical.minimap.service.LayerService;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.service.TelegramMessageService;
import org.tactical.minimap.web.DTO.MarkerDTO;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

@Service
public class TelegramParserScheduler {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	TelegramMessageService telegrameMessageService;

	@Autowired
	MarkerService markerService;

	@Autowired
	LayerService layerService;

	@Autowired
	RedisService redisService;

	@Value("${API_KEY}")
	String apiKey;

	@Async
	@Scheduled(fixedRate = 3000)
	public void doParse() throws IOException, ParseException {

		// time pattern
		Pattern timePattern = Pattern.compile("([0-9][0-9]\\:?[0-9][0-9])");
		// marker pattern
		Pattern policeMarkerPattern = Pattern.compile("([0-9])*?名*?(?:閃燈)*?(?:藍|白)*?(?:大|小)*?(EU|eu|衝|警車|警|籠|豬籠|防暴|軍裝)");

		// get message
		List<TelegramMessage> telegramMessageList = telegrameMessageService.getPendingTelegramMessage();
		logger.info("fetcing telegram Message List from DB [{}]", telegramMessageList.size());

		List<Long> okIdList = new ArrayList<Long>();
		List<Long> notOkIdList = new ArrayList<Long>();

		for (TelegramMessage telegramMessage : telegramMessageList) {
			String message = telegramMessage.getMessage();

			Matcher matcher = timePattern.matcher(message);

			if (matcher.find()) {
				HashMap<String, Integer> keyMap = new HashMap<String, Integer>();
				// String time = matcher.group(1).replaceAll(":", "");

				// convert message
				HashMap<String, String> messageRules = getRules();

				for (String mrKey : messageRules.keySet()) {
					message = message.replaceAll(mrKey, messageRules.get(mrKey));
				}

				logger.info("processing message {}", message);

				processData(message, "data/region.chi", keyMap, 30);

				processData(message, "data/subDistrict.chi", keyMap, 20);

				processData(message, "data/building.chi", keyMap, 15);

				processData(message, "data/estate.chi", keyMap, 10);

				processData(message, "data/street.chi", keyMap, 10);

				processData(message, "data/village.chi", keyMap, 10);


				final Map<String, Integer> sortedMap = keyMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

				logger.info("keyMap {} ", sortedMap);
				// get geo location
				HttpResponse<JsonNode> response = Unirest.get("https://maps.googleapis.com/maps/api/geocode/json")
						.queryString("key", apiKey).queryString("address", String.join(" ", sortedMap.keySet())).asJson();

				JSONObject body = response.getBody().getObject();

				logger.info("google return {} ", response.getBody().toPrettyString());

				if (body.get("status").equals("OK")) {
					JSONObject latlng = body.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location");

					// add marker
					Layer layer = layerService.getLayerByKey("scout");

					MarkerDTO markerDTO = new MarkerDTO();
					markerDTO.setLat(latlng.getDouble("lat"));
					markerDTO.setLng(latlng.getDouble("lng"));
					markerDTO.setLayer("scout");
					markerDTO.setMessage(message);
					markerDTO.setUuid("TELEGRAM_BOT");

					try {
						// analyst target icon
						int level = 1;
						Marker marker = null;

						Matcher isPoliceMatcher = policeMarkerPattern.matcher(message);

						if (isPoliceMatcher.find()) {
							if (isPoliceMatcher.groupCount() > 1) {
								try {
									level = Integer.parseInt(isPoliceMatcher.group(1));
									logger.info("message level : {} ", level);
								} catch (NumberFormatException nfe) {
									logger.info("process level error, group count {}", isPoliceMatcher.groupCount());
								}
							}
							marker = PoliceMarker.class.newInstance();
							marker.setLevel(level);
						} else {
							marker = InfoMarker.class.newInstance();
						}

						logger.info("adding marker " + marker.getType());

						markerService.addMarker(layer, markerDTO, marker);

						okIdList.add(telegramMessage.getTelegramMessageId());
					} catch (InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
						notOkIdList.add(telegramMessage.getTelegramMessageId());
					}
				} else {
					notOkIdList.add(telegramMessage.getTelegramMessageId());
				}
			}
		}

		if (okIdList.size() > 0)
			telegrameMessageService.updateTelegramMessageOK(okIdList);

		if (notOkIdList.size() > 0)
			telegrameMessageService.updateTelegramMessageNotOK(notOkIdList);

	}

	private HashMap<String, String> getRules() {
		HashMap<String, String> ruleMap = new HashMap<String, String>();

		List<TelegramMessageRule> telegramMessageRuleList = telegrameMessageService.getActiveTelegramMessageRules();

		for (TelegramMessageRule tmr : telegramMessageRuleList) {
			ruleMap.put(tmr.getRule(), tmr.getGoal());
		}

		return ruleMap;
	}

	private void processData(String message, String filePath, HashMap<String, Integer> keyMap, int i) throws IOException {
		InputStream in = this.getClass().getClassLoader().getResourceAsStream(filePath);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		String line = reader.readLine();
		while (line != null) {
			i = 0;
			// matcherLine =
			// line.replaceAll("(.{2,})(?:新邨|邨|新城|城|花園|園|豪園|樓|閣|工業大廈|大樓|苑|大廈|工業中心|中心|官邸|閣|居|灣|臺|山莊|小築|台|別墅)$",
			// "$1");
			String matcherLine = line.replaceAll("(\\(|\\)|\\[|\\]|\\.)", "\\$1");

			if (!message.matches(".*" + matcherLine + ".*")) {

				for (String key : keyMap.keySet()) {
					if (matcherLine.matches(".*" + key + ".*")) {
						matcherLine = matcherLine.replaceAll(key, "");
						i -= 20;
					}
				}

				if (!message.matches(".*" + matcherLine + ".*")) {
					matcherLine = matcherLine.replaceAll("(邨|新城|花園)", "");
					i -= 10;
				}

			}

			if (message.matches(".*" + matcherLine + ".*")) {
				keyMap.put(line, i);
			}

			line = reader.readLine();
		}

		reader.close();
	}

	public static int BoyerMooreHorspoolSimpleSearch(char[] pattern, char[] text) {
		int patternSize = pattern.length;
		int textSize = text.length;

		int i = 0, j = 0;

		while ((i + patternSize) <= textSize) {
			j = patternSize - 1;
			while (text[i + j] == pattern[j]) {
				j--;
				if (j < 0)
					return i;
			}
			i++;
		}
		return -1;
	}

}
