package org.tactical.minimap.scheduler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.util.MarkerUserSseEmitter;

@Service
public class MarkerEventScheduler {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	final List<MarkerUserSseEmitter> emitters = new CopyOnWriteArrayList<>();

	@Autowired
	MarkerService markerService;

	@Autowired
	RedisService redisService;

	public void addEmitter(final MarkerUserSseEmitter emitter) {
		emitters.add(emitter);
	}

	public void removeEmitter(final MarkerUserSseEmitter emitter) {
		emitters.remove(emitter);
	}

	@Async
	@Scheduled(fixedRate = 3000)
	public void doNotify() throws IOException {
		List<MarkerUserSseEmitter> deadEmitters = new ArrayList<>();

		emitters.forEach(emitter -> {
			try {

				List<String> layerKeyList = emitter.getLayerKeys();

				List<Marker> markerList = markerService.findMultiLayerMarkers(layerKeyList, emitter.getLat(), emitter.getLng(), ConstantsUtil.RANGE);

				markerService.addMarkerCache(markerList, emitter.getUuid());

				emitter.send(SseEmitter.event().data(markerList));
			} catch (Exception e) {
				deadEmitters.add(emitter);
			}
		});
		emitters.removeAll(deadEmitters);
	}

}
