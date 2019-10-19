package org.tactical.minimap.scheduler;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

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

	@Value("${PATTERN_FOLDER}")
	String patternFolder;

	@Value("${MAP_FOLDER}")
	String mapFolder;

	static Map<String, List<String>> patternMap;

	@PostConstruct
	public void initialConfig() {
		try {
			patternMap = new HashMap<String, List<String>>();

			prepareData("region", mapFolder + patternFolder + "/region.chi");

			prepareData("subDistrict", mapFolder + patternFolder + "/subDistrict.chi");

			prepareData("building", mapFolder + patternFolder + "/building.chi");

			prepareData("estate", mapFolder + patternFolder + "/estate.chi");

			prepareData("street", mapFolder + patternFolder + "/street.chi");

			prepareData("village", mapFolder + patternFolder + "/village.chi");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void prepareData(String category, String filePath) throws IOException {
		List<String> patternList = new ArrayList<String>();

		File file = new File(filePath);
		Scanner sc = new Scanner(file);

		while (sc.hasNextLine()) {
			patternList.add(sc.nextLine());
		}

		sc.close();

		patternMap.put(category, patternList);
	}

	@Async
	@Scheduled(fixedRate = 10000)
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

				processData(message, "subDistrict", keyMap, 20);

				processData(message, "building", keyMap, 15);

				processData(message, "estate", keyMap, 10);

				processData(message, "street", keyMap, 10);

				processData(message, "village", keyMap, 10);

				processData(message, "region", keyMap, 10);

				if (keyMap.keySet().size() == 0) {
					
					notOkIdList.add(telegramMessage.getTelegramMessageId());
					
				} else {

					final Map<String, Integer> sortedMap = keyMap.entrySet().stream()
							.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
							.limit(3)
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

					logger.info("keyMap {} ", sortedMap);
					// get geo location
					HttpResponse<JsonNode> response = Unirest
							.get("https://maps.googleapis.com/maps/api/geocode/json")
							.queryString("key", apiKey)
							.queryString("address", String.join(" ", sortedMap.keySet())).asJson();

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
			}else {
				notOkIdList.add(telegramMessage.getTelegramMessageId());
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

	private void processData(String message, String category, HashMap<String, Integer> keyMap, final int categoryWeight) throws IOException {
		List<String> patternList = patternMap.get(category);
		for (String pattern : patternList) {
			int weight = categoryWeight;

			String processingPattern = pattern.replaceAll("(\\(|\\)|\\[|\\]|\\.)", "\\$1");

			if (!message.matches(".*" + processingPattern + ".*")) {
				for (String key : keyMap.keySet()) {
					if (processingPattern.matches(".*" + key + ".*")) {
						processingPattern = processingPattern.replaceAll(key, "");
						weight -= 20;
					}
				}
				if (!message.matches(".*" + processingPattern + ".*")) {
					processingPattern = processingPattern.replaceAll("(邨|新城|花園)", "");
					weight -= 10;
				}
			}

			if (message.matches(".*" + processingPattern + ".*")) {
				keyMap.put(pattern, weight);
			}

		}

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
