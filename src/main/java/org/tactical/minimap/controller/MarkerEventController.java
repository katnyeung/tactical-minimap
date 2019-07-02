package org.tactical.minimap.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
	public SseEmitter doNotify(@RequestParam("key") String key, @RequestParam("layerKey") String layerKey, @RequestParam("lat") Double lat, @RequestParam("lng") Double lng) throws InterruptedException, IOException {
		final MarkerUserSseEmitter emitter = new MarkerUserSseEmitter();
		emitter.setUuid(key);
		emitter.setLayerKey(layerKey);
		emitter.setLat(lat);
		emitter.setLng(lng);

		markerEventScheduler.addEmitter(emitter);
		emitter.onCompletion(() -> markerEventScheduler.removeEmitter(emitter));
		emitter.onTimeout(() -> markerEventScheduler.removeEmitter(emitter));

		return emitter;
	}
}