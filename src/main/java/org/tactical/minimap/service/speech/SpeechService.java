package org.tactical.minimap.service.speech;

import java.util.Calendar;
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
import org.tactical.minimap.web.result.DefaultResult;
import org.tactical.minimap.web.result.StringListResult;
import org.tactical.minimap.web.result.StringResult;

@Component
public abstract class SpeechService {
	public final Logger logger = LoggerFactory.getLogger(getClass());

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

	public DefaultResult getSpeechByMarkerIdList(List<String> layerKeys, List<Long> markerIdList, double fromLat, double fromLng) {
		logger.info("getting speech from {} " , markerIdList);
		
		List<Marker> markerList = markerService.findActiveMarkersByMarkerIds(layerKeys, markerIdList);
		StringBuilder sbMessageList = new StringBuilder();

		for (Marker marker : markerList) {
			String message = marker.getMessage();
			
			// process text message , time to hour minutes
			message = convertMessage(marker.getMessage());
			
			//process the distance if any
			if(fromLat > 0 && fromLng > 0) {
				double distance = distFrom(fromLat, fromLng, marker.getLat(), marker.getLng());
				message = "距離你" + (int)(distance * 1000) + "米. " + message;
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

	public String convertMessage(String message) {
		String processedMessage = message;

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
				StringBuilder sb = new StringBuilder();
				if (diffHour > 0) {
					sb.append(diffHour);
					sb.append("小時");
				}
				if (diffMinute > 0) {
					sb.append(diffMinute);
					sb.append("分鐘");
				}
				sb.append("前");
				logger.info(sb.toString());
				processedMessage = matcher.replaceFirst(sb.toString());
			}
		}
		// space the digit

		// and space to pattern that found by parser

		// remove the channel message
		if (processedMessage.lastIndexOf("#") >= 0) {
			processedMessage = processedMessage.substring(0, message.lastIndexOf("#"));
		}
		// replace green object, EU , vcity safe, to longer term
		processedMessage = processedMessage.replaceAll("(eu|EU)", "衝鋒車");

		logger.info("making speech request : {} ", message);

		return processedMessage;
	}

	public abstract String getSpeech(String text);
}
