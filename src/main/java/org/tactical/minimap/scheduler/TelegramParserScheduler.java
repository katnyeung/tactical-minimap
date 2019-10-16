package org.tactical.minimap.scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.repository.TelegramMessage;
import org.tactical.minimap.repository.TelegramMessageRule;
import org.tactical.minimap.repository.marker.InfoMarker;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.service.LayerService;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.service.TelegramMessageService;
import org.tactical.minimap.util.ConstantsUtil;
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

	@Async
	@Scheduled(fixedRate = 300000)
	public void doParse() throws IOException {

		// get message
		List<TelegramMessage> telegramMessageList = telegrameMessageService.getPendingTelegramMessage();
		
		for(TelegramMessage telegramMessage : telegramMessageList) {
			String message = telegramMessage.getMessage();
			
			telegramMessage.setStatus(ConstantsUtil.TELEGRAM_MESSAGE_PROCESSED_OK);
			
			
		}
		
		
		String message = "#元朗 2219 大馬路50防暴西行行過康樂路";

		// find time pattern
		Pattern timePattern = Pattern.compile("([0-9][0-9]\\:?[0-9][0-9])");
		Matcher matcher = timePattern.matcher(message);
		
		if (matcher.find()) {
			String time = matcher.group(1).replaceAll(":", "");
			logger.info("{}", time);
			
			// convert message
			HashMap<String, String> messageRules = getRules();
			messageRules.put("(黨鐵站|黨鐵|狗鐵|地鐵)", "港鐵站");
			messageRules.put("(西鐵站)", "西港鐵站");
			messageRules.put("(出口)", "進出口");
			messageRules.put("(狗署)", "警署");
			messageRules.put("(狗)", "警");
			messageRules.put("(村)", "邨");
			
			for (String mrKey : messageRules.keySet()) {
				message = message.replaceAll(mrKey, messageRules.get(mrKey));
			}

			logger.info("{}", message);

			// process message
			HashMap<String, Integer> keyMap = new HashMap<String, Integer>();

			processData(message, "data/region.chi", keyMap, 10);

			processData(message, "data/subDistrict.chi", keyMap, 10);
			
			processData(message, "data/building.chi", keyMap, 30);

			processData(message, "data/estate.chi", keyMap, 20);

			processData(message, "data/street.chi", keyMap, 15);

			processData(message, "data/village.chi", keyMap, 20);

			logger.info("{}", keyMap);
			
			//get geo location
			HttpResponse<JsonNode> response = Unirest.get("https://maps.googleapis.com/maps/api/geocode/json")
				.queryString("key", "")
				.queryString("address", String.join(" ", keyMap.keySet()))
				.asJson();

			JSONObject body = response.getBody().getObject();

			if (body.get("status").equals("OK")) {
				JSONObject latlng = body
									.getJSONArray("results")
									.getJSONObject(0)
									.getJSONObject("geometry")
									.getJSONObject("location");

				logger.info("{}", latlng.getDouble("lat"));
				logger.info("{}", latlng.getDouble("lng"));

				// analyst target icon
				
				// add marker
				Layer layer = layerService.getLayerByKey("scout");
				
				MarkerDTO markerDTO = new MarkerDTO();
				markerDTO.setLat(latlng.getDouble("lat"));
				markerDTO.setLng(latlng.getDouble("lng"));
				markerDTO.setLayer("scout");
				markerDTO.setMessage(message);
				markerDTO.setType("info");
				markerDTO.setUuid("TELEGRAM_BOT");
				try {
					Marker marker = InfoMarker.class.newInstance();
					
					markerService.addMarker(layer, markerDTO, marker);
					
				} catch (InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}

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
			line = reader.readLine();
			if (line != null) {
				
				String matcherLine = line.replaceAll("(新邨|邨)", "");
				
				for(String key : keyMap.keySet()) {
					matcherLine = matcherLine.replaceAll(key, "");
				}
				
				if (BoyerMooreHorspoolSimpleSearch(matcherLine.toCharArray(), message.toCharArray()) > 0) {
					keyMap.put(line, i);
				}
			}
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
