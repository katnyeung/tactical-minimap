package org.tactical.minimap.service.speech;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.service.TelegramMessageService;
import org.tactical.minimap.web.result.DefaultResult;
import org.tactical.minimap.web.result.StringListResult;
import org.tactical.minimap.web.result.StringResult;

@Component
public abstract class SpeechService {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	TelegramMessageService telegramMessageService;

	@Autowired
	MarkerService markerService;

	public StringListResult getSpeechFromCoord(List<String> layerKeys, double fromLat, double fromLng, double toLat, double toLng, Long timestamp) {
		List<String> base64wavList = new LinkedList<String>();

		List<Marker> markerList = markerService.findLatestActiveMarkersInRange(layerKeys, fromLat, fromLng, toLat, toLng, timestamp);
		logger.info(" speech marker list {}, {}", timestamp, markerList);
		Long lastTimestamp = timestamp;

		for (Marker marker : markerList) {
			// process text message , time to hour minutes
			String message = convertMessage(marker.getMessage());

			String base64wavString = this.getSpeech(message);
			base64wavList.add(base64wavString);

			lastTimestamp = marker.getCreatedate().getTime();
		}

		StringListResult slr = new StringListResult();
		slr.setList(base64wavList);
		slr.setLastTimestamp(lastTimestamp);

		return slr;
	}

	public DefaultResult getSpeechByMarkerIdList(List<String> layerKeys, List<Long> markerIdList, double fromLat, double fromLng, int degree) {
		logger.info("getting speech from {} ", markerIdList);

		List<Marker> markerList = markerService.findActiveMarkersByMarkerIds(layerKeys, markerIdList);
		StringBuilder sbMessageList = new StringBuilder();

		for (Marker marker : markerList) {
			String message = marker.getMessage();

			// process text message , time to hour minutes
			message = convertMessage(marker.getMessage());

			message = processTime(message);

			// process the distance if any
			if (fromLat > 0 && fromLng > 0) {
				String distanceMessage = "";

				double distance = distFrom(fromLat, fromLng, marker.getLat(), marker.getLng());
				double markerToUserDegree = bearing(fromLat, fromLng, marker.getLat(), marker.getLng());

				String direction = getDirection(markerToUserDegree) + "面";

				if (degree > 0) {
					direction = getFacing((markerToUserDegree - degree) % 360) + "方";
					logger.info(" marker degree {} , user degree {}, direction msg : {}", markerToUserDegree, degree, direction);
				}

				if (distance * 1000 > 1000) {
					distanceMessage = "。距離 你 " + direction + (int) (distance) + " 公里。 ";
				} else {
					distanceMessage = "。 距離 你 " + direction + (int) (distance * 1000) + " 米。";
				}

				message = message.replaceAll("=distance=", distanceMessage);
			} else {
				message = message.replaceAll("=distance=", "");
			}

			sbMessageList.append(message);
			sbMessageList.append("。 ");
		}

		String base64wavString = this.getSpeech(sbMessageList.toString());

		StringResult sr = StringResult.success(base64wavString);
		return sr;
	}

	public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
		double earthRadius = 6371.0; // miles (or 6371.0 kilometers)
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double sindLat = Math.sin(dLat / 2);
		double sindLng = Math.sin(dLng / 2);
		double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2) * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double dist = earthRadius * c;

		return dist;
	}

	public static double bearing(double lat1, double lon1, double lat2, double lon2) {
		double longitude1 = lon1;
		double longitude2 = lon2;
		double latitude1 = Math.toRadians(lat1);
		double latitude2 = Math.toRadians(lat2);
		double longDiff = Math.toRadians(longitude2 - longitude1);
		double y = Math.sin(longDiff) * Math.cos(latitude2);
		double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);
		return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;

	}

	public static String getDirection(double resultDegree) {

		String coordNames[] = { "北", "東北", "東", "東南", "南", "西南", "西", "西北", "北" };

		double directionid = Math.round(resultDegree / 45);
		if (directionid < 0) {
			directionid = directionid + 8;
		}

		String compasLoc = coordNames[(int) directionid];

		return compasLoc;
	}

	public static String getFacing(double resultDegree) {

		String coordNames[] = { "前", "右前", "右", "右後", "後", "左後", "左", "左前", "前" };

		double directionid = Math.round(resultDegree / 45);
		if (directionid < 0) {
			directionid = directionid + 8;
		}

		String compasLoc = coordNames[(int) directionid];

		return compasLoc;
	}

	public String processTime(String message) {
		String processedMessage = message;

		StringBuilder sb = new StringBuilder(" ");
		// time pattern
		Pattern timePattern = Pattern.compile("([2][0-3]|[0-5][0-9])\\:?([0-5][0-9])");
		Matcher matcher = timePattern.matcher(processedMessage);

		if (matcher.find()) {
			if (matcher.groupCount() > 1) {
				int hour = Integer.parseInt(matcher.group(1));
				int minute = Integer.parseInt(matcher.group(2));

				TimeZone tz1 = TimeZone.getTimeZone("GMT+8");
				Calendar cal1 = Calendar.getInstance(tz1);

				int currentHour = cal1.get(Calendar.HOUR_OF_DAY);
				int currentMinute = cal1.get(Calendar.MINUTE);

				int diffMinute = currentMinute - minute;
				int diffHour = currentHour - hour;

				if (diffMinute < 0) {
					diffMinute += 60;
					diffHour -= 1;
				}
				if (diffHour < 0) {
					diffHour += 24;
				}
				if (diffHour > 0) {
					sb.append(diffHour);
					sb.append("小時");
				}
				if (diffMinute > 0) {
					sb.append(diffMinute);
					sb.append("分鐘");
				}
				sb.append("前 ");
				logger.info(sb.toString());
				processedMessage = matcher.replaceFirst("");
			}
		}

		return sb.toString().replaceAll("([^0-9])2([^0-9])", "$1兩$2") + " =distance= " + processedMessage;
	}

	public String convertMessage(String message) {
		String processedMessage = message;

		processedMessage = processedMessage.replaceAll("\r?\n", "");

		// and space to pattern that found by parser
		telegramMessageService.initialConfig();

		HashMap<String, Integer> keyMap = new HashMap<String, Integer>();

		try {
			telegramMessageService.processData(message, "street", keyMap, 30);
			telegramMessageService.processData(message, "district", keyMap, 25);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (String key : keyMap.keySet()) {
			processedMessage = processedMessage.replaceAll("(" + key + ")", " $1 ");
		}
		// remove the channel message
		if (processedMessage.lastIndexOf("#", processedMessage.length()) >= 0) {
			processedMessage = processedMessage.replaceAll("(.*)#.*$", "$1");
		}

		// replace green object, EU , vcity safe, to longer term

		processedMessage = processedMessage.replaceAll("(#)", " ");
		processedMessage = processedMessage.replaceAll("(eu|EU|Eu)", "衝鋒 ");
		processedMessage = processedMessage.replaceAll("(曱甴| green object|blue object)", "警察 ");

		processedMessage = processedMessage.replaceAll("(safe|clear)", "安全 ");
		// space the digit

		logger.info("making speech request : {} ", processedMessage);

		return processedMessage;
	}

	public abstract String getSpeech(String text);
}
