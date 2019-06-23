package org.tactical.minimap.scheduler;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.tactical.minimap.repository.marker.Marker;
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

	@Scheduled(fixedRate = 3000)
	// @Scheduled(cron = "0 0 */4 * * *")
	public void makerManager() {
		// logger.info("Cron Task :: Execution Time - " +
		// dateTimeFormatter.format(LocalDateTime.now()));

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

		for (Marker marker : markerList) {
			logger.info("Processing : " + marker);
			redisService.saveMarkerCache(marker);
		}

	}

}
