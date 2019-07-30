package org.tactical.minimap.scheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.service.LayerService;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.util.MarkerCache;

@Component
public class SupportTaskScheduler {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	RedisService redisService;

	@Autowired
	MarkerService markerService;

	@Autowired
	LayerService layerService;

	@Scheduled(fixedRate = 3000)
	public void makerManager() {
		List<MarkerCache> markerCacheList = redisService.findAllMarkerCache();
		List<Long> markerIdList = new ArrayList<>();

		for (MarkerCache mc : markerCacheList) {
			if (mc.getExpire() <= 0) {
				// turn expire = 0 to D active
				logger.info("> Updating " + mc.getMarkerId() + " to D");
				markerService.updateStatusUpDown(mc.getMarkerId(), ConstantsUtil.MARKER_STATUS_DEACTIVED, mc.getUpVote(), mc.getDownVote());
				redisService.deleteKey(ConstantsUtil.REDIS_MARKER_PREFIX + ":" + mc.getMarkerId());
			} else {
				// count down the timer of those marker in redis
				mc.setExpire(mc.getExpire() - 3);
				if(mc.getPulse() > 0) {
					mc.setPulse(mc.getPulse() - 1);
				}
				redisService.saveMarkerCache(mc);
			}
			markerIdList.add(mc.getMarkerId());
		}

		List<Marker> markerList = null;

		// find the Marker status = A and not exist in redis
		if (markerIdList != null && markerIdList.size() > 0) {
			markerList = markerService.findActiveMarkersNotInCache(markerIdList);
		} else {
			markerList = markerService.findActiveMarkers();
		}

		// put new marker on map
		for (Marker marker : markerList) {
			logger.info("Processing : " + marker);

			int markerCount = markerService.getMarkerCountInRange(marker.getLayer().getLayerKey(), marker.getLat(), marker.getLng(), ConstantsUtil.RANGE);

			marker.setExpire(marker.getExpire() - (markerCount * 5));

			redisService.saveMarkerCache(marker);
		}

	}
	
	@Scheduled(cron = "0 0 */4 * * *")
	public void layerManager() {
		// handle layer expire
		List<Layer> activeLayerList = layerService.findActiveLayers();
		for (Layer layer : activeLayerList) {
			if (layer.getDuration() > 0) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(layer.getCreatedate());
				calendar.add(Calendar.HOUR_OF_DAY, layer.getDuration());
				//logger.info(calendar + " : " + Calendar.getInstance() + " " + calendar.compareTo(Calendar.getInstance()) );
				if (calendar.compareTo(Calendar.getInstance()) < 0) {
					logger.info("Setting : " + layer.getLayerKey() + " to deactive");
					layer.setStatus(ConstantsUtil.LAYER_STATUS_DEACTIVED);
					layerService.save(layer);
					
					redisService.logoutLayer(layer.getLayerKey());
				}
			}
		}
	}
}
