package org.tactical.minimap.service.speech;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.web.result.StringListResult;

@Component
public abstract class SpeechService {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	MarkerService markerService;

	public StringListResult getSpeechFromCoord(List<String> layerKeys, double fromLat, double fromLng, double toLat, double toLng, Long timestamp) {
		List<String> base64wavList = new LinkedList<String>();

		List<Marker> markerList = markerService.findLatestActiveMarkersInRange(layerKeys, fromLat, fromLng, toLat, toLng, timestamp);
		logger.info(" speech marker list {}, {}",timestamp, markerList);
		Long lastTimestamp = timestamp;
		
		for (Marker marker : markerList) {
			//process text message , time to hour minutes
			String message = marker.getMessage();
			// time pattern
			Pattern timePattern = Pattern.compile("([2][0-3]|[0-5][0-9])\\:?([0-5][0-9])");
			Matcher matcher = timePattern.matcher(marker.getMessage());
			
			if(matcher.find()) {
				if (matcher.groupCount() > 1) {
					int hour = Integer.parseInt(matcher.group(1));
					int minute = Integer.parseInt(matcher.group(2));

					if (hour > 12) {
						hour -= 12;
					}
					String hourUnit = "點";
					
					if (hour == 0)
						hourUnit = "時";
					message = matcher.replaceFirst(hour + hourUnit + " " + minute + "分");
				}
			}
			// replace the channel message
			if(message.lastIndexOf("#") >= 0) {
				message = message.substring(0, message.lastIndexOf("#"));
			}
			
			String base64wavString = this.getSpeech(message);
			base64wavList.add(base64wavString);
			
			lastTimestamp = marker.getCreatedate().getTime();
		}
		
		StringListResult slr = new StringListResult();
		slr.setList(base64wavList);
		slr.setLastTimestamp(lastTimestamp);
		
		return slr;
	}

	public abstract String getSpeech(String text);
}
