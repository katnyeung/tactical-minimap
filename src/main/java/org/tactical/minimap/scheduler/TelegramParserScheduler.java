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
	Pattern policeMarkerPattern = Pattern.compile("([0-9][0-9])*?(?:隻|名|個|綠|白|架)*?(?:閃燈)*?(?:藍|白)*?(?:大|小)*?\\s*?(EU|eu|衝|警車|警|籠|豬籠|軍裝|豬龍|豬)");
	Pattern blackFlagPattern = Pattern.compile("(黑旗)");
	Pattern orangeFlagPattern = Pattern.compile("(橙旗)");
	Pattern blueFlagPattern = Pattern.compile("(藍旗)");
	Pattern tearGasPattern = Pattern.compile("(催淚|催淚彈|tg|TG)");
	Pattern riotPolicePattern = Pattern.compile("([0-9][0-9])*?(?:隻|名|個|綠|白)*?\\s*?(防暴|速龍)");
	Pattern waterCarPattern = Pattern.compile("(水炮)");
	Pattern groupPattern = Pattern.compile("(安全|safe|Safe|clear|冇狗)");
	Pattern dangerPattern = Pattern.compile("(制服|拉左|被捕)");

	Pattern blockPattern = Pattern.compile("(關閉|落閘|全封|封站|封路)");

	@Async
	@Scheduled(fixedRate = 10000)
	public void doParse() throws IOException, ParseException {
		telegramMessageService.initialConfig();

		Unirest.config().verifySsl(false);
		
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

						message = telegramMessageService.processData(message, "region", keyMap, 40);

						message = telegramMessageService.processData(message, "street", keyMap, 50);

						message = telegramMessageService.processData(message, "district", keyMap, 25);

						message = telegramMessageService.processData(message, "building", keyMap, 15);

						message = telegramMessageService.processData(message, "plaza", keyMap, 15);

						message = telegramMessageService.processData(message, "mtr", keyMap, 50);

						message = telegramMessageService.processData(message, "wildcard", keyMap, 5);

						message = telegramMessageService.processData(message, "additional", keyMap, 10);

						if (keyMap.keySet().size() == 0) {
							logger.info("cannot hit any street pattern. mark to fail " + telegramMessage.getTelegramMessageId());
							notOkIdList.add(telegramMessage.getTelegramMessageId());

						} else {

							// save parse result
							Gson gson = new Gson();
							telegramMessage.setResult(gson.toJson(keyMap));
							
							telegramMessageService.saveTelegramMessage(telegramMessage);

							// get telegram channel setting
							TelegramChannel tc = telegramMessageService.getChannelByGroupName(telegramMessage.getGroupKey());

							if (tc.getSearchPrefix() != null && !tc.getSearchPrefix().equals("")) {
								keyMap.put(tc.getSearchPrefix(), 100);
							}

							MarkerGeoCoding latlng = null;
							
							// by pass internet search with only 1 result
							if(keyMap.size() == 1) {
								for(String key : keyMap.keySet()) {
									StreetData sd = streetDataService.findStreetData(key);
									if(sd != null) {										
										latlng = MarkerGeoCoding.latlng(sd.getLat(), sd.getLng());
									}
								}
							}
														
							// step 2 if default setting not found or keyMap size > 1 , go internet search
							if(latlng == null) {
								if ((tc.getGeoCodeMethod() != null && tc.getGeoCodeMethod().equals("google")) || keyMap.containsKey("交界") || keyMap.containsKey("太和路")) {

									keyMap.entrySet().removeIf(e -> e.getKey().matches("(交界)"));
									
									latlng = doGoogle(keyMap, tc);
									
								} else {
									
									boolean haveStreet = false;
									int totalScore = 0;
									for (String key : keyMap.keySet()) {
										if (key.matches(".*(道|路|街|橋).*")) {
											haveStreet = true;
										}
										totalScore += keyMap.get(key);
									}

									if (haveStreet || totalScore < 100) {
										keyMap.entrySet().removeIf(e -> e.getValue() < 15);

										keyMap.put("香港", 200);

										latlng = doArcgis(keyMap, tc);
									} else {

										keyMap.put("香港", 200);

										latlng = doGeoDataHK(keyMap, tc);

									}
								}
							}
							
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
									
									String lineColor = "red";
									
									if (telegramMessage.getMedia() != null) {
										marker = ImageMarker.class.newInstance();

										String fileName = telegramMessage.getMedia().replaceAll(".*\\/(.*)$", "$1");
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
											} catch (NumberFormatException nfe) {
												logger.info("process level error, group count {}", isPoliceMatcher.groupCount());
											}
										}
										marker = PoliceMarker.class.newInstance();
										marker.setLevel(level);
										lineColor = "#ed6312";
									} else {
										marker = InfoMarker.class.newInstance();
										lineColor = "#395aa3";
									}

									// rephase as shape

									ShapeMarker shapeMarker = processShapeMarker(keyMap, markerDTO, latlng);
									
									if(shapeMarker != null) {
										shapeMarker.setIcon(marker.getIcon());
										shapeMarker.setIconSize(marker.getIconSize());
										
										markerDTO.setColor(lineColor);
										logger.info("shape color : {}" , lineColor);
										
										marker = shapeMarker;
									}
									
									logger.info("adding marker " + marker.getType());

									double randLat = (ThreadLocalRandom.current().nextInt(0, 80 + 1) - 40) / 100000.0;
									double randLng = (ThreadLocalRandom.current().nextInt(0, 80 + 1) - 40) / 100000.0;
									
									markerDTO.setLat(markerDTO.getLat() + randLat);
									markerDTO.setLng(markerDTO.getLng()+  randLng);
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

	private ShapeMarker processShapeMarker(Map<String, Integer> keyMap, MarkerDTO markerDTO, MarkerGeoCoding latlng) throws InstantiationException, IllegalAccessException, JsonProcessingException {
		ShapeMarker shapeMarker = null;

		ObjectMapper om = new ObjectMapper();
		
		List<LinkedHashMap<String, Double>> shapeList = new LinkedList<LinkedHashMap<String, Double>>();

		boolean anyShapeHit = false;
		
		for (String key : keyMap.keySet()) {

			List<StreetData> sdList = streetDataService.findStreetDataList("ST", key);

			for (StreetData sd : sdList) {
				
				List<LinkedHashMap<String, Double>> subShapeList = new LinkedList<LinkedHashMap<String, Double>>();
				
				//logger.info("sd : {} " , sd);
				List<StreetDataDetail> sddList = sd.getStreetDataDetailList();
				
				for (StreetDataDetail sdd : sddList) {

					double lat = sdd.getLat();
					double lng = sdd.getLng();
					
					LinkedHashMap<String, Double> smd = new LinkedHashMap<String, Double>();

					double fromLat = latlng.getLat() - 0.002;
					double fromLng = latlng.getLng() - 0.002;
					double toLat = latlng.getLat() + 0.002;
					double toLng = latlng.getLng() + 0.002;

					if ((fromLat < lat && lat <= toLat) && (fromLng < lng && lng <= toLng)) {
						logger.info("{} < {} < {} , {} < {} < {}", fromLat, lat, toLat, fromLng, lng, toLng);


						smd.put("lat", lat);
						smd.put("lng", lng);
						
						smd.put("group", Double.parseDouble(sdd.getGroupId()));
						
						subShapeList.add(smd);
						
						smd.put("in", 1.0);
					} else {
						smd.put("in", 0.0);
					}

				}
				
				boolean anyPointInRange = false;
				for (LinkedHashMap<String, Double> subShape : subShapeList) {
					if (subShape.get("in") > 0) {
						anyPointInRange = true;
					}
				}
				if (anyPointInRange) {
					shapeList.addAll(subShapeList);
					anyShapeHit = true;
				}
			}

		}
		
		markerDTO.setShapeType("polyline_group");
		markerDTO.setShapeList(om.writeValueAsString(shapeList));
		
		if(anyShapeHit) {
			shapeMarker = ShapeMarker.class.newInstance();
		}
		
		return shapeMarker;
	}
				
				


	private MarkerGeoCoding doGoogle(final HashMap<String, Integer> keyMap, TelegramChannel tc) {
		
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

			latlng.setLat(jsonObjLatLng.getDouble("lat"));
			latlng.setLng(jsonObjLatLng.getDouble("lng"));

			return latlng;
		} else {
			return null;
		}
	}

	private MarkerGeoCoding doGeoDataHK(final HashMap<String, Integer> keyMap, TelegramChannel tc) {

		// filter out the key weight < 15
		for (String key : keyMap.keySet()) {
			if (keyMap.get(key) < 15) {
				keyMap.remove(key);
			}
		}

		final Map<String, Integer> sortedMap = keyMap.entrySet()
				.stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.limit(4)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		logger.info("keyMap {} ", sortedMap);
		// get geo location
		HttpResponse<JsonNode> response = Unirest.get("https://geodata.gov.hk/gs/api/v1.0.0/locationSearch")
				.queryString("q", String.join(" ", sortedMap.keySet())).asJson();

		//logger.info("geodata return {} ", response.getBody().toPrettyString());

		JSONArray bodyArray = response.getBody().getArray();

		for (Object obj : bodyArray) {
			JSONObject jsonObjectXY = (JSONObject) obj;

			// Note these are x, y so lng, lat
			double x = jsonObjectXY.getDouble("x");
			double y = jsonObjectXY.getDouble("y");
			ProjCoordinate dstCoord = markerService.toWGS84(x, y);

			//logger.info("dest x y {} {} ", dstCoord.x, dstCoord.y);

			if (tc.getFromLat() > 0 && tc.getFromLng() > 0 && tc.getToLat() > 0 && tc.getToLng() > 0) {
				if (tc.getFromLat() < dstCoord.y && dstCoord.y < tc.getToLat() && tc.getFromLng() < dstCoord.x && dstCoord.x < tc.getToLng()) {

					MarkerGeoCoding latlng = new MarkerGeoCoding();

					latlng.setLat(dstCoord.y);
					latlng.setLng(dstCoord.x);

					return latlng;
				}
			}else {
				MarkerGeoCoding latlng = new MarkerGeoCoding();

				latlng.setLat(dstCoord.y);
				latlng.setLng(dstCoord.x);
				
				return latlng;
			}

		}
		return null;
	}

	private MarkerGeoCoding doArcgis(final HashMap<String, Integer> keyMap, TelegramChannel tc) {

		final Map<String, Integer> sortedMap = keyMap.entrySet()
				.stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.limit(4)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		logger.info("keyMap {} ", sortedMap);
		
		// get geo location
		HttpResponse<JsonNode> response = Unirest.get("https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/findAddressCandidates")
				.queryString("f","json")
				.queryString("outFields","matchAddr")
				.queryString("singleLine", String.join(" ", sortedMap.keySet())).asJson();

		logger.info("arcgis return {} ", response.getBody().toPrettyString());

		JSONObject bodyObject = response.getBody().getObject();

		JSONArray bodyArray = bodyObject.getJSONArray("candidates");
		
		for (Object obj : bodyArray) {
			JSONObject jsonObjectLatLng = (JSONObject) obj;

			// Note these are x, y so lng, lat
			double lat = jsonObjectLatLng.getJSONObject("location").getDouble("y");
			double lng = jsonObjectLatLng.getJSONObject("location").getDouble("x");

			logger.info("dest x y {} {} ", lng, lat);

			if (tc.getFromLat() > 0 && tc.getFromLng() > 0 && tc.getToLat() > 0 && tc.getToLng() > 0) {
				if (tc.getFromLat() < lat && lat < tc.getToLat() && tc.getFromLng() < lng && lng < tc.getToLng()) {

					MarkerGeoCoding latlng = new MarkerGeoCoding();

					latlng.setLat(lat);
					latlng.setLng(lng);

					return latlng;
				}
			} else {
				MarkerGeoCoding latlng = new MarkerGeoCoding();

				latlng.setLat(lat);
				latlng.setLng(lng);

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

		logger.info("ogcio return {} ", response.getBody().toPrettyString());

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

}
