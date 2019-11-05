package org.tactical.minimap.scheduler;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
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
import org.tactical.minimap.repository.marker.BlockadeMarker;
import org.tactical.minimap.repository.marker.DangerMarker;
import org.tactical.minimap.repository.marker.FlagBlackMarker;
import org.tactical.minimap.repository.marker.FlagBlueMarker;
import org.tactical.minimap.repository.marker.FlagOrangeMarker;
import org.tactical.minimap.repository.marker.GroupMarker;
import org.tactical.minimap.repository.marker.InfoMarker;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.repository.marker.PoliceMarker;
import org.tactical.minimap.repository.marker.RiotPoliceMarker;
import org.tactical.minimap.repository.marker.TearGasMarker;
import org.tactical.minimap.repository.marker.WaterTruckMarker;
import org.tactical.minimap.repository.marker.livestream.ImageMarker;
import org.tactical.minimap.service.ImageService;
import org.tactical.minimap.service.LayerService;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.service.TelegramMessageService;
import org.tactical.minimap.util.MarkerGeoCoding;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.google.gson.Gson;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

@Service
public class TelegramParserScheduler {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	TelegramMessageService telegramMessageService;

	@Autowired
	MarkerService markerService;

	@Autowired
	LayerService layerService;

	@Autowired
	RedisService redisService;

	@Autowired
	ImageService imageService;

	@Value("${API_KEY}")
	String apiKey;

	@Value("${PATTERN_FOLDER}")
	String patternFolder;

	@Value("${MAP_FOLDER}")
	String mapFolder;

	static Map<String, List<String>> patternMap;

	// time pattern
	Pattern timePattern = Pattern.compile("((?:[2][0-3]|[0-5][0-9])\\:?[0-5][0-9])");
	// marker pattern
	Pattern policeMarkerPattern = Pattern.compile("([0-9][0-9])*?(?:隻|名|個|綠|白)*?(?:閃燈)*?(?:藍|白)*?(?:大|小)*?\\s*?(EU|eu|衝|警車|警|籠|豬籠|軍裝|豬龍|豬)");
	Pattern blackFlagPattern = Pattern.compile("(黑旗)");
	Pattern orangeFlagPattern = Pattern.compile("(橙旗)");
	Pattern blueFlagPattern = Pattern.compile("(藍旗)");
	Pattern tearGasPattern = Pattern.compile("(催淚)");
	Pattern riotPolicePattern = Pattern.compile("([0-9][0-9])*?(?:隻|名|個|綠|白)*?\\s*?(防暴|速龍)");
	Pattern waterCarPattern = Pattern.compile("(水炮)");
	Pattern groupPattern = Pattern.compile("(安全|safe|Safe|clear)");
	Pattern dangerPattern = Pattern.compile("(制服|拉左|被捕)");

	Pattern blockPattern = Pattern.compile("(關閉|落閘|全封|封站|封路)");

	public void initialConfig() {
		try {
			patternMap = new HashMap<String, List<String>>();

			prepareData("region", mapFolder + patternFolder + "/v2/region");

			prepareData("district", mapFolder + patternFolder + "/v2/recreation");

			prepareData("district", mapFolder + patternFolder + "/v2/district");

			prepareData("building", mapFolder + patternFolder + "/v2/estate.building");

			prepareData("wildcard", mapFolder + patternFolder + "/v2/estate.building_wildcard");

			prepareData("building", mapFolder + patternFolder + "/v2/28hse.building");

			prepareData("building", mapFolder + patternFolder + "/v2/building");

			prepareData("plaza", mapFolder + patternFolder + "/v2/plaza");

			prepareData("wildcard", mapFolder + patternFolder + "/v2/plaza_wildcard");

			prepareData("street", mapFolder + patternFolder + "/v2/street");

			prepareData("wildcard", mapFolder + patternFolder + "/v2/street_wildcard");

			prepareData("mtr", mapFolder + patternFolder + "/v2/mtr");

			prepareData("additional", mapFolder + patternFolder + "/v2/additional");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void prepareData(String category, String filePath) throws IOException {
		List<String> patternList = new LinkedList<String>();

		File file = new File(filePath);
		Scanner sc = new Scanner(file);

		while (sc.hasNextLine()) {
			patternList.add(sc.nextLine());
		}

		sc.close();

		patternList.sort((s1, s2) -> s2.length() - s1.length());

		if (patternMap.get(category) != null) {
			List<String> currentStringList = patternMap.get(category);
			currentStringList.addAll(patternList);
		} else {
			patternMap.put(category, patternList);
		}

	}

	@Async
	@Scheduled(fixedRate = 10000)
	public void doParse() throws IOException, ParseException {
		initialConfig();

		// get message
		List<TelegramMessage> telegramMessageList = telegramMessageService.getPendingTelegramMessage();
		if (telegramMessageList.size() > 0)
			logger.info("fetcing telegram Message List from DB [{}]", telegramMessageList.size());

		List<Long> okIdList = new ArrayList<Long>();
		List<Long> notOkIdList = new ArrayList<Long>();

		for (TelegramMessage telegramMessage : telegramMessageList) {
			String message = telegramMessage.getMessage();

			if (message.length() > 150) {
				logger.info("message characters length > 150. mark to fail " + telegramMessage.getTelegramMessageId());
				notOkIdList.add(telegramMessage.getTelegramMessageId());

			} else {
				Matcher matcher = timePattern.matcher(message);

				if (matcher.find()) {

					try {

						HashMap<String, Integer> keyMap = new HashMap<String, Integer>();

						// convert message
						message = message.replaceAll("\n", "");

						HashMap<String, String> messageRules = getRules();

						for (String mrKey : messageRules.keySet()) {
							message = message.replaceAll(mrKey, messageRules.get(mrKey));
						}

						logger.info("processing message {}", message);

						processData(message, "region", keyMap, 40);

						processData(message, "street", keyMap, 30);

						processData(message, "district", keyMap, 25);

						processData(message, "building", keyMap, 15);

						processData(message, "plaza", keyMap, 15);

						processData(message, "mtr", keyMap, 15);

						processData(message, "wildcard", keyMap, 5);

						processData(message, "additional", keyMap, 10);

						if (keyMap.keySet().size() == 0) {
							logger.info("cannot hit any street pattern. mark to fail " + telegramMessage.getTelegramMessageId());
							notOkIdList.add(telegramMessage.getTelegramMessageId());

						} else {

							// save parse result
							Gson gson = new Gson();
							telegramMessage.setResult(gson.toJson(keyMap));
							telegramMessageService.saveTelegramMessage(telegramMessage);

							MarkerGeoCoding latlng = doGoogle(keyMap);
							// arkerGeoCoding latlng = doGeoDataHK(keyMap);

							if (latlng == null) {

								logger.info("MarkerGeoCoding not return ok. mark to fail " + telegramMessage.getTelegramMessageId());
								notOkIdList.add(telegramMessage.getTelegramMessageId());

							} else {
								Layer layer = layerService.getLayerByKey("scout");

								MarkerDTO markerDTO = new MarkerDTO();
								markerDTO.setLat(Math.floor(latlng.getLat() * 10000000) / 10000000);
								markerDTO.setLng(Math.floor(latlng.getLng() * 10000000) / 10000000);
								markerDTO.setLayer("scout");
								markerDTO.setMessage(telegramMessage.getMessage() + "\n#" + telegramMessage.getGroupKey());
								markerDTO.setUuid("TELEGRAM_BOT");

								try {
									// analyst target icon
									int level = 1;
									Marker marker = null;

									Matcher isPoliceMatcher = policeMarkerPattern.matcher(message);
									Matcher blackFlagMatcher = blackFlagPattern.matcher(message);
									Matcher blueFlagMatcher = blueFlagPattern.matcher(message);
									Matcher orangeFlagMatcher = orangeFlagPattern.matcher(message);
									Matcher tearGasMatcher = tearGasPattern.matcher(message);
									Matcher riotPoliceMatcher = riotPolicePattern.matcher(message);
									Matcher waterCarMatcher = waterCarPattern.matcher(message);
									Matcher blockMatcher = blockPattern.matcher(message);
									Matcher groupMatcher = groupPattern.matcher(message);
									Matcher dangerMatcher = dangerPattern.matcher(message);

									if (telegramMessage.getMedia() != null) {
										marker = ImageMarker.class.newInstance();

										String fileName = telegramMessage.getMedia().replaceAll(".*\\/(.*)$", "$1");
										File file = new File(imageService.getServerFullPath(fileName));
										String ext = FilenameUtils.getExtension(fileName);

										markerDTO.setImagePath(fileName);

										imageService.resizeImage(file, file, ext, 400);

									} else if (dangerMatcher.find()) {
										marker = DangerMarker.class.newInstance();
									} else if (groupMatcher.find()) {
										marker = GroupMarker.class.newInstance();
									} else if (waterCarMatcher.find()) {
										marker = WaterTruckMarker.class.newInstance();
									} else if (blackFlagMatcher.find()) {
										marker = FlagBlackMarker.class.newInstance();
									} else if (blueFlagMatcher.find()) {
										marker = FlagBlueMarker.class.newInstance();
									} else if (orangeFlagMatcher.find()) {
										marker = FlagOrangeMarker.class.newInstance();
									} else if (blockMatcher.find()) {
										marker = BlockadeMarker.class.newInstance();
									} else if (tearGasMatcher.find()) {
										marker = TearGasMarker.class.newInstance();
									} else if (riotPoliceMatcher.find()) {
										if (riotPoliceMatcher.groupCount() > 1) {
											try {
												level = Integer.parseInt(riotPoliceMatcher.group(1));
												level = level < 10 ? level : 10;
												logger.info("message level : {} ", level);
											} catch (NumberFormatException nfe) {
												logger.info("process level error, group count {}", riotPoliceMatcher.groupCount());
											}
										}
										marker = RiotPoliceMarker.class.newInstance();
										marker.setLevel(level);
									} else if (isPoliceMatcher.find()) {
										if (isPoliceMatcher.groupCount() > 1) {
											try {
												level = Integer.parseInt(isPoliceMatcher.group(1));
												level = level < 10 ? level : 10;
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
									logger.info("exception occur. mark to fail " + telegramMessage.getTelegramMessageId());
								}
							}

						}

					} catch (Exception e) {
						e.printStackTrace();
						logger.info("exception occur . mark to fail " + telegramMessage.getTelegramMessageId());
						notOkIdList.add(telegramMessage.getTelegramMessageId());
					}

				} else {
					logger.info("time pattern not found. mark to fail " + telegramMessage.getTelegramMessageId());
					notOkIdList.add(telegramMessage.getTelegramMessageId());
				}
			}
		}

		if (okIdList.size() > 0) {
			telegramMessageService.updateTelegramMessageOK(okIdList);
			markerService.broadcastUpdateToAllLoggedUser();
		}

		if (notOkIdList.size() > 0)
			telegramMessageService.updateTelegramMessageNotOK(notOkIdList);

	}

	private MarkerGeoCoding doGoogle(final HashMap<String, Integer> keyMap) {

		final Map<String, Integer> sortedMap = keyMap.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(4).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		logger.info("keyMap {} ", sortedMap);
		// get geo location
		HttpResponse<JsonNode> response = Unirest.get("https://maps.googleapis.com/maps/api/geocode/json").queryString("key", apiKey).queryString("address", String.join(" ", sortedMap.keySet())).asJson();

		JSONObject body = response.getBody().getObject();

		logger.info("google return {} ", response.getBody().toPrettyString());

		if (body.get("status").equals("OK")) {
			JSONObject jsonObjLatLng = body.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location");

			// add marker
			MarkerGeoCoding latlng = new MarkerGeoCoding();

			double randLat = (ThreadLocalRandom.current().nextInt(0, 60 + 1) - 30) / 100000.0;
			double randLng = (ThreadLocalRandom.current().nextInt(0, 60 + 1) - 30) / 100000.0;

			latlng.setLat(jsonObjLatLng.getDouble("lat") + randLat);
			latlng.setLng(jsonObjLatLng.getDouble("lng") + randLng);

			return latlng;
		} else {
			return null;
		}
	}

	private MarkerGeoCoding doGeoDataHK(final HashMap<String, Integer> keyMap) {

		final Map<String, Integer> sortedMap = keyMap.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		logger.info("keyMap {} ", sortedMap);
		// get geo location
		HttpResponse<JsonNode> response = Unirest.get("http://www.als.ogcio.gov.hk/lookup").header("Accept", "application/json").header("Accept-Language", "en,zh-Hant").header("Accept-Encoding", "gzip").queryString("q", String.join(" ", sortedMap.keySet())).queryString("n", "1").asJson();

		JSONObject body = response.getBody().getObject();

		logger.info("google return {} ", response.getBody().toPrettyString());

		if (body.getJSONArray("SuggestedAddress") != null) {
			JSONObject jsonObjLatLng = body.getJSONArray("SuggestedAddress").getJSONObject(0).getJSONObject("Address").getJSONObject("PremisesAddress").getJSONArray("GeospatialInformation").getJSONObject(0);

			// add marker
			MarkerGeoCoding latlng = new MarkerGeoCoding();

			double randLat = (ThreadLocalRandom.current().nextInt(0, 8 + 1) - 4) / 10000.0;
			double randLng = (ThreadLocalRandom.current().nextInt(0, 8 + 1) - 4) / 10000.0;

			latlng.setLat(jsonObjLatLng.getDouble("Latitude") + randLat);
			latlng.setLng(jsonObjLatLng.getDouble("Longitude") + randLng);

			return latlng;
		} else {
			return null;
		}
	}

	private HashMap<String, String> getRules() {
		HashMap<String, String> ruleMap = new HashMap<String, String>();

		List<TelegramMessageRule> telegramMessageRuleList = telegramMessageService.getActiveTelegramMessageRules();

		for (TelegramMessageRule tmr : telegramMessageRuleList) {
			ruleMap.put(tmr.getRule(), tmr.getGoal());
		}

		return ruleMap;
	}

	private void processData(String message, String category, HashMap<String, Integer> keyMap, final int categoryWeight) throws IOException {
		List<String> patternList = patternMap.get(category);
		for (String pattern : patternList) {
			int weight = categoryWeight;

			String processingPattern = pattern;

			String replaceToPattern = null;
			if (processingPattern.indexOf("|") > 0) {
				replaceToPattern = processingPattern.substring(processingPattern.indexOf("|") + 1, processingPattern.length());
				processingPattern = processingPattern.substring(0, processingPattern.indexOf("|"));
			}

			Pattern addressPattern = Pattern.compile(processingPattern);
			Matcher addressMatcher = addressPattern.matcher(message);

			if (addressMatcher.find()) {
				double weightMultipler = (((message.length() * 1.0) - addressMatcher.start()) / message.length()) * 30;
				weight += weightMultipler;
				
				if (keyMap.keySet().size() == 0) {
					addPattern(processingPattern, weight, keyMap, addressMatcher.group(0), replaceToPattern);
				} else {
					boolean isExist = false;
					for (String key : keyMap.keySet()) {
						if (key.matches(".*" + pattern + ".*")) {
							logger.info("matched pattern {} {}", category, pattern);
							isExist = true;
						}
					}
					if (!isExist)
						addPattern(processingPattern, weight, keyMap, addressMatcher.group(0), replaceToPattern);
					logger.info("{}", keyMap);
				}
			}
		}

	}

	private void addPattern(String pattern, Integer weight, HashMap<String, Integer> keyMap, String patternMessage, String replaceToPattern) {
		if (replaceToPattern != null) {
			String newPattern = patternMessage.replaceAll(pattern, replaceToPattern);
			keyMap.put(newPattern, weight);
		} else {
			keyMap.put(pattern, weight);
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
