package org.tactical.minimap.scheduler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.osgeo.proj4j.ProjCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.repository.StreetData;
import org.tactical.minimap.repository.StreetDataDetail;
import org.tactical.minimap.repository.TelegramChannel;
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
import org.tactical.minimap.repository.marker.WarningMarker;
import org.tactical.minimap.repository.marker.WaterTruckMarker;
import org.tactical.minimap.repository.marker.livestream.ImageMarker;
import org.tactical.minimap.repository.marker.shape.ShapeMarker;
import org.tactical.minimap.service.ImageService;
import org.tactical.minimap.service.LayerService;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.service.StreetDataService;
import org.tactical.minimap.service.TelegramMessageService;
import org.tactical.minimap.util.MarkerGeoCoding;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

@Service
public class TelegramParserScheduler {
	//	([a-zA-Z])出口|$1出口
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

	@Autowired
	StreetDataService streetDataService;

	@Value("${API_KEY}")
	String apiKey;

	// time pattern
	Pattern timePattern = Pattern.compile("((?:[2][0-3]|[0-5][0-9])\\:?[0-5][0-9])");
	// marker pattern
	Pattern policeMarkerPattern = Pattern.compile("([0-9]*?)(?:隻|名|個|綠|白|架)*?(?:閃燈|閃光|蒙面)*?(?:藍|白)*?(?:大|小)*?\\s*?(suv|SUV|EU|eu|Eu|衝|警車|警|綠衫|籠|豬籠|軍裝|豬龍|豬|曱|green object|blue object|狗|私家車)");
	Pattern blackFlagPattern = Pattern.compile("(黑旗)");
	Pattern orangeFlagPattern = Pattern.compile("(橙旗)");
	Pattern blueFlagPattern = Pattern.compile("(藍旗)");
	Pattern tearGasPattern = Pattern.compile("(催淚|催淚彈|tg|TG)(?!槍|彈槍)");
	Pattern riotPolicePattern = Pattern.compile("([0-9]*?)(?:隻|名|個|綠|白|架)*?\\s*?(防暴|速龍|鋭武)");
	Pattern waterCarPattern = Pattern.compile("(水炮)");
	Pattern groupPattern = Pattern.compile("((?<!小心|不|公眾)安全|safe|Safe|clear|冇狗|(?<!仍未)清理)");
	Pattern dangerPattern = Pattern.compile("(制服|拉左|被捕)");
	Pattern warningPattern = Pattern.compile("(交通意外|意外|壞車)");
	Pattern blockPattern = Pattern.compile("(關閉|落閘|全封|封站|封路|受阻|封閉|慢車)");

	@Async
	@Scheduled(fixedRate = 10000)
	public void doParse() throws IOException, ParseException {
		telegramMessageService.initialConfig();

		Unirest.config().verifySsl(false);

		// get message S for street C for chat
		List<String> messageTypeList = new ArrayList<String>();
		messageTypeList.add("S");
		messageTypeList.add("C");

		List<TelegramMessage> telegramMessageList = telegramMessageService.getPendingTelegramMessage(messageTypeList);
		if (telegramMessageList.size() > 0)
			logger.info("fetcing telegram Message List from DB [{}]", telegramMessageList.size());

		List<Long> okIdList = new ArrayList<Long>();
		List<Long> notOkIdList = new ArrayList<Long>();

		for (TelegramMessage telegramMessage : telegramMessageList) {
			MarkerGeoCoding latlng = null;

			if (telegramMessage.getMessageType().equals("S")) {
				latlng = processLocationMessage(telegramMessage, okIdList, notOkIdList);
			}

			if (telegramMessage.getMessageType().equals("S") || telegramMessage.getMessageType().equals("C")) {
				processChatMessage(telegramMessage, latlng);
			}
		}

		if (okIdList.size() > 0) {
			telegramMessageService.updateTelegramMessageOK(okIdList);
			markerService.broadcastUpdateToAllLoggedUser();
		}

		if (notOkIdList.size() > 0)
			telegramMessageService.updateTelegramMessageNotOK(notOkIdList);

	}

	private void processChatMessage(TelegramMessage telegramMessage, MarkerGeoCoding latlng) throws FileNotFoundException {
		String region = streetDataService.getRegionByLatlng(latlng);

		// store the group key to DB when the time after an other
		telegramMessageService.processGroupKey();

		telegramMessageService.processChatMessage(telegramMessage.getMessage(), region);

		telegramMessage.setStatus("O");
		telegramMessageService.saveTelegramMessage(telegramMessage);
	}

	private MarkerGeoCoding processLocationMessage(TelegramMessage telegramMessage, List<Long> okIdList, List<Long> notOkIdList) {
		String message = telegramMessage.getMessage();

		Map<String, Integer> keyMap = new HashMap<String, Integer>();

		MarkerGeoCoding latlng = null;

		if (message.length() > 150) {
			logger.info("message characters length > 150. mark to fail " + telegramMessage.getTelegramMessageId());
			notOkIdList.add(telegramMessage.getTelegramMessageId());

		} else {
			
			Matcher matcher = timePattern.matcher(message);

			if (matcher.find()) {

				try {

					// convert message
					message = message.replaceAll("\n|\r\n", "");

					HashMap<String, String> messageRules = getRules();

					for (String mrKey : messageRules.keySet()) {
						message = message.replaceAll(mrKey, messageRules.get(mrKey));
					}

					logger.info("processing message {}", message);

					message = telegramMessageService.processData(message, "region", keyMap, 40);

					message = telegramMessageService.processData(message, "street", keyMap, 50);

					message = telegramMessageService.processData(message, "plaza", keyMap, 15);

					message = telegramMessageService.processData(message, "building", keyMap, 15);

					message = telegramMessageService.processData(message, "district", keyMap, 25);

					message = telegramMessageService.processData(message, "mtr", keyMap, 50);

					message = telegramMessageService.processData(message, "wildcard", keyMap, 5);

					message = telegramMessageService.processData(message, "additional", keyMap, 15);

					if (keyMap.keySet().size() == 0) {
						logger.info("cannot hit any street pattern. mark to fail " + telegramMessage.getTelegramMessageId());
						notOkIdList.add(telegramMessage.getTelegramMessageId());

					} else {

						// get telegram channel setting
						TelegramChannel tc = telegramMessageService.getChannelByGroupName(telegramMessage.getGroupKey());

						if (tc.getSearchPrefix() != null && !tc.getSearchPrefix().equals("")) {
							keyMap.put(tc.getSearchPrefix(), 100);
						}

						// by pass internet search with only 1 result
						if (keyMap.size() == 1) {
							for (String key : keyMap.keySet()) {
								StreetData sd = streetDataService.findStreetData(key);
								if (sd != null) {
									latlng = MarkerGeoCoding.latlng(sd.getLat(), sd.getLng(), "local");
								}
							}
						}

						// step 2 if default setting not found or keyMap size > 1 , go internet search
						if (latlng == null) {
							int streetCount = 0;

							for (String key : keyMap.keySet()) {
								if (key.matches(".*(道|道西|道東|道南|道北|路|街|橋|隧道)$")) {
									streetCount++;
									keyMap.put(key, keyMap.get(key) - 50);
								}
							}

							// filter out the key weight < 0
							keyMap.entrySet().removeIf(e -> e.getValue() < 0);

							if ((tc.getGeoCodeMethod() != null && tc.getGeoCodeMethod().equals("google")) || keyMap.containsKey("交界") || keyMap.containsKey("太和路") || streetCount > 1) {

								keyMap.entrySet().removeIf(e -> e.getKey().matches("(交界)"));

								latlng = doGoogle(keyMap, tc);

							} else {

								if (streetCount > 0 && !keyMap.containsKey("朗豪坊")) {
									Iterator<String> iter = keyMap.keySet().iterator();
									HashMap<String, Integer> tempKeyMap = new HashMap<String, Integer>();

									while (iter.hasNext()) {
										String key = iter.next();
										if (key.matches(".*(東|南|西|北)$")) {
											logger.info("{}", key);
											Integer value = keyMap.get(key);
											iter.remove();
											key = key.replaceAll("東", " E").replaceAll("南", "S").replaceAll("西", " W").replaceAll("北", " N");

											tempKeyMap.put(key, value);
										}
									}

									keyMap.putAll(tempKeyMap);
									keyMap.put("香港", 200);

									latlng = doArcgis(keyMap, tc);

								} else {

									latlng = doGeoDataHK(keyMap, tc);

								}
							}
						}

						if (latlng == null) {

							logger.info("MarkerGeoCoding not return ok. mark to fail " + telegramMessage.getTelegramMessageId());
							notOkIdList.add(telegramMessage.getTelegramMessageId());

						} else {
							// save parse result
							Gson gson = new Gson();
							telegramMessage.setResult("{\"label\":\"" + latlng.getLabel() + "\",\"data\":" + gson.toJson(keyMap) + "}");
							telegramMessage.setRegion(streetDataService.getRegionByLatlng(latlng));
							
							telegramMessageService.saveTelegramMessage(telegramMessage);

							try {

								convertGeoLocateToMarker(latlng, tc, telegramMessage, keyMap);

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

		return latlng;
	}

	private String processChineseNumber(String message) {
		message = message.replaceAll("一", "1");
		message = message.replaceAll("二", "2");
		message = message.replaceAll("三", "3");
		message = message.replaceAll("四", "4");
		message = message.replaceAll("五", "5");
		message = message.replaceAll("六", "6");
		message = message.replaceAll("七", "7");
		message = message.replaceAll("八", "8");
		message = message.replaceAll("九", "9");
		message = message.replaceAll("十", "10");
		message = message.replaceAll("零", "0");
		return message;
	}

	private void convertGeoLocateToMarker(MarkerGeoCoding latlng, TelegramChannel tc, TelegramMessage tm, Map<String, Integer> keyMap) throws InstantiationException, IllegalAccessException, IOException {
		String originalMessage = tm.getMessage();

		String message = processChineseNumber(originalMessage);
		
		String groupKey = tm.getGroupKey();

		String layerString = "scout";
		if (tc.getLayer() != null) {
			layerString = tc.getLayer();
		}

		Layer layer = layerService.getLayerByKey(layerString);

		MarkerDTO markerDTO = new MarkerDTO();
		markerDTO.setLat(Math.floor(latlng.getLat() * 10000000) / 10000000);
		markerDTO.setLng(Math.floor(latlng.getLng() * 10000000) / 10000000);
		markerDTO.setLayer(layer.getLayerKey());
		markerDTO.setMessage(originalMessage + "\n#" + groupKey);
		markerDTO.setUuid("TELEGRAM_BOT");
		markerDTO.setTelegramMessageId(tm.getTelegramMessageId());

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
		Matcher warningMatcher = warningPattern.matcher(message);

		String lineColor = "red";

		if (tm.getMedia() != null) {
			marker = ImageMarker.class.newInstance();

			String fileName = tm.getMedia().replaceAll(".*\\/(.*)$", "$1");
			File file = new File(imageService.getServerFullPath(fileName));
			String ext = FilenameUtils.getExtension(fileName);

			markerDTO.setImagePath(fileName);

			imageService.resizeImage(file, file, ext, 400);

			lineColor = "red";
		} else if (dangerMatcher.find()) {
			marker = DangerMarker.class.newInstance();
		} else if (groupMatcher.find()) {
			marker = GroupMarker.class.newInstance();
			lineColor = "#16aa6d";
		} else if (warningMatcher.find()) {
			marker = WarningMarker.class.newInstance();
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
				riotPoliceMatcher.reset();
				int totalPolice = 0;

				while (riotPoliceMatcher.find()) {
					if (riotPoliceMatcher.group(1) != null && !riotPoliceMatcher.group(1).equals("")) {
						totalPolice += Integer.parseInt(riotPoliceMatcher.group(1));
					} else {
						totalPolice++;
					}
				}

				level = totalPolice < 20 ? totalPolice : 20;
			}
			marker = RiotPoliceMarker.class.newInstance();
			marker.setLevel(level);

		} else if (isPoliceMatcher.find()) {

			if (isPoliceMatcher.groupCount() > 1) {
				isPoliceMatcher.reset();
				int totalPolice = 0;

				while (isPoliceMatcher.find()) {

					logger.info("Police : {} ", isPoliceMatcher.group(1));
					if (isPoliceMatcher.group(1) != null && !isPoliceMatcher.group(1).equals("")) {
						totalPolice += Integer.parseInt(isPoliceMatcher.group(1));
					} else {
						totalPolice++;
					}
				}

				level = totalPolice < 20 ? totalPolice : 20;
			}

			marker = PoliceMarker.class.newInstance();
			marker.setLevel(level);
			lineColor = "#ed6312";

		} else {

			marker = InfoMarker.class.newInstance();
			lineColor = "#395aa3";

		}

		// rephase as shape
		ShapeMarker shapeMarker = processShapeMarker(keyMap, markerDTO, latlng, level);

		if (shapeMarker != null) {
			shapeMarker.setIcon(marker.getIcon());
			shapeMarker.setIconSize(marker.getIconSize());

			markerDTO.setColor(lineColor);
			logger.info("shape color : {}", lineColor);

			marker = shapeMarker;
		}

		logger.info("adding marker " + marker.getType());

		double randLat = (ThreadLocalRandom.current().nextInt(0, 60 + 1) - 30) / 100000.0;
		double randLng = (ThreadLocalRandom.current().nextInt(0, 60 + 1) - 30) / 100000.0;

		markerDTO.setLat(markerDTO.getLat() + randLat);
		markerDTO.setLng(markerDTO.getLng() + randLng);
		
		markerService.addMarker(layer, markerDTO, marker);
	}

	private ShapeMarker processShapeMarker(Map<String, Integer> keyMap, MarkerDTO markerDTO, MarkerGeoCoding latlng, int level) throws InstantiationException, IllegalAccessException, JsonProcessingException {
		ShapeMarker shapeMarker = null;

		ObjectMapper om = new ObjectMapper();

		List<LinkedHashMap<String, Double>> shapeList = new LinkedList<LinkedHashMap<String, Double>>();

		boolean anyShapeHit = false;

		for (String key : keyMap.keySet()) {

			List<StreetData> sdList = streetDataService.findStreetDataList("ST", key);

			for (StreetData sd : sdList) {

				List<LinkedHashMap<String, Double>> subShapeList = new LinkedList<LinkedHashMap<String, Double>>();

				List<StreetDataDetail> sddList = sd.getStreetDataDetailList();

				for (StreetDataDetail sdd : sddList) {

					double lat = sdd.getLat();
					double lng = sdd.getLng();

					LinkedHashMap<String, Double> smd = new LinkedHashMap<String, Double>();

					double fromLat = latlng.getLat() - 0.0025;
					double fromLng = latlng.getLng() - 0.0025;
					double toLat = latlng.getLat() + 0.0025;
					double toLng = latlng.getLng() + 0.0025;

					if ((fromLat < lat && lat <= toLat) && (fromLng < lng && lng <= toLng)) {
						logger.debug("{} < {} < {} , {} < {} < {}", fromLat, lat, toLat, fromLng, lng, toLng);

						smd.put("lat", lat);
						smd.put("lng", lng);
						smd.put("group", Double.parseDouble(sdd.getGroupId()));

						subShapeList.add(smd);
					}
				}

				if (subShapeList.size() > 0) {
					shapeList.addAll(subShapeList);
					anyShapeHit = true;
				}
			}
		}

		markerDTO.setShapeType("polyline_group");
		markerDTO.setShapeList(om.writeValueAsString(shapeList));

		if (anyShapeHit) {
			shapeMarker = ShapeMarker.class.newInstance();
			shapeMarker.setLevel(level);
		}

		return shapeMarker;
	}

	private MarkerGeoCoding doGoogle(final Map<String, Integer> keyMap, TelegramChannel tc) {

		final Map<String, Integer> sortedMap = keyMap.entrySet()
				.stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		logger.info("keyMap {} ", sortedMap);
		// get geo location
		HttpResponse<JsonNode> response = Unirest.get("https://maps.googleapis.com/maps/api/geocode/json").queryString("key", apiKey).queryString("address", String.join(" ", sortedMap.keySet())).asJson();

		JSONObject body = response.getBody().getObject();

		if (body.get("status").equals("OK")) {
			JSONObject jsonObjLatLng = body.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location");

			// add marker
			MarkerGeoCoding latlng = MarkerGeoCoding.latlng(jsonObjLatLng.getDouble("lat"), jsonObjLatLng.getDouble("lng"), "google");

			return latlng;
		} else {
			return null;
		}
	}

	private MarkerGeoCoding doGeoDataHK(final Map<String, Integer> keyMap, TelegramChannel tc) {

		final Map<String, Integer> sortedMap = keyMap.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(4).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		logger.info("keyMap {} ", sortedMap);
		// get geo location
		HttpResponse<JsonNode> response = Unirest.get("https://geodata.gov.hk/gs/api/v1.0.0/locationSearch").queryString("q", String.join(" ", sortedMap.keySet())).asJson();

		JSONArray bodyArray = response.getBody().getArray();

		for (Object obj : bodyArray) {
			JSONObject jsonObjectXY = (JSONObject) obj;

			// Note these are x, y so lng, lat
			double x = jsonObjectXY.getDouble("x");
			double y = jsonObjectXY.getDouble("y");
			ProjCoordinate dstCoord = markerService.toWGS84(x, y);

			if (tc.getFromLat() > 0 && tc.getFromLng() > 0 && tc.getToLat() > 0 && tc.getToLng() > 0) {
				if (tc.getFromLat() < dstCoord.y && dstCoord.y < tc.getToLat() && tc.getFromLng() < dstCoord.x && dstCoord.x < tc.getToLng()) {

					MarkerGeoCoding latlng = MarkerGeoCoding.latlng(dstCoord.y, dstCoord.x, "geodatahk");

					return latlng;
				}
			} else {
				MarkerGeoCoding latlng = MarkerGeoCoding.latlng(dstCoord.y, dstCoord.x, "geodatahk");

				return latlng;
			}

		}
		return null;
	}

	private MarkerGeoCoding doArcgis(final Map<String, Integer> keyMap, TelegramChannel tc) {

		final Map<String, Integer> sortedMap = keyMap.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(4).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		logger.info("keyMap {} ", sortedMap);

		// get geo location
		HttpResponse<JsonNode> response = Unirest.get("https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/findAddressCandidates").queryString("f", "json").queryString("outFields", "matchAddr").queryString("singleLine", String.join(" ", sortedMap.keySet())).asJson();

		JSONObject bodyObject = response.getBody().getObject();

		JSONArray bodyArray = bodyObject.getJSONArray("candidates");

		for (Object obj : bodyArray) {
			JSONObject jsonObjectLatLng = (JSONObject) obj;

			// Note these are x, y so lng, lat
			double lat = jsonObjectLatLng.getJSONObject("location").getDouble("y");
			double lng = jsonObjectLatLng.getJSONObject("location").getDouble("x");

			if (tc.getFromLat() > 0 && tc.getFromLng() > 0 && tc.getToLat() > 0 && tc.getToLng() > 0) {
				if (tc.getFromLat() < lat && lat < tc.getToLat() && tc.getFromLng() < lng && lng < tc.getToLng()) {

					MarkerGeoCoding latlng = MarkerGeoCoding.latlng(lat, lng, "arcgis");

					return latlng;
				}
			} else {
				MarkerGeoCoding latlng = MarkerGeoCoding.latlng(lat, lng, "arcgis");

				return latlng;
			}

		}
		return null;
	}

	private MarkerGeoCoding doOGCIO(final HashMap<String, Integer> keyMap, TelegramChannel tc) {

		final Map<String, Integer> sortedMap = keyMap.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		logger.info("keyMap {} ", sortedMap);
		// get geo location
		HttpResponse<JsonNode> response = Unirest.get("http://www.als.ogcio.gov.hk/lookup").header("Accept", "application/json").header("Accept-Language", "en,zh-Hant").header("Accept-Encoding", "gzip").queryString("q", String.join(" ", sortedMap.keySet())).queryString("n", "1").asJson();

		JSONObject body = response.getBody().getObject();

		if (body.getJSONArray("SuggestedAddress") != null) {
			JSONObject jsonObjLatLng = body.getJSONArray("SuggestedAddress").getJSONObject(0).getJSONObject("Address").getJSONObject("PremisesAddress").getJSONArray("GeospatialInformation").getJSONObject(0);

			// add marker

			double randLat = (ThreadLocalRandom.current().nextInt(0, 8 + 1) - 4) / 10000.0;
			double randLng = (ThreadLocalRandom.current().nextInt(0, 8 + 1) - 4) / 10000.0;

			MarkerGeoCoding latlng = MarkerGeoCoding.latlng(jsonObjLatLng.getDouble("Latitude") + randLat, jsonObjLatLng.getDouble("Longitude") + randLng, "ogcio");

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

}
