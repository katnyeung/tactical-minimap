package org.tactical.minimap.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tactical.minimap.scheduler.MarkerEventScheduler;
import org.tactical.minimap.util.MarkerUserSseEmitter;

@Controller
public class MarkerEventController {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	MarkerEventScheduler markerEventScheduler;

	final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	@GetMapping("/markerEvents")
	public SseEmitter doNotify(@RequestParam("key") String key, @RequestParam("layerKeys") String layerKeys, @RequestParam("lat") Double lat, @RequestParam("lng") Double lng) throws InterruptedException, IOException {
		Map<String, String> layerMap = new HashMap<String, String>();

		Pattern pattern = Pattern.compile("([0-9a-zA-Z-_]*)\\$([a-zA-Z]*)");

		for (String layerKey : layerKeys.split(",")) {
			Matcher matcher = pattern.matcher(layerKey);

			if (matcher.find()) {
				if (matcher.groupCount() > 1) {
					layerMap.put(matcher.group(1), matcher.group(2));
				}
			}
		}
		
		final MarkerUserSseEmitter emitter = new MarkerUserSseEmitter();
		emitter.setUuid(key);
		emitter.setLayerKeys(layerMap.keySet().stream().collect(Collectors.toList()));
		emitter.setLat(lat);
		emitter.setLng(lng);

		markerEventScheduler.addEmitter(emitter);
		emitter.onCompletion(() -> markerEventScheduler.removeEmitter(emitter));
		emitter.onTimeout(() -> markerEventScheduler.removeEmitter(emitter));

		return emitter;
	}
}