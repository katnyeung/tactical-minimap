package org.tactical.minimap.scheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.service.LayerService;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.util.MarkerCache;
import org.tactical.minimap.util.MarkerLinkedList;

@Component
public class SupportTaskScheduler {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	RedisService redisService;

	@Autowired
	MarkerService markerService;

	@Autowired
	LayerService layerService;
	
	@Async
	@Scheduled(fixedRate = 3000)
	public void makerManager() {
		List<MarkerCache> markerCacheList = redisService.findAllMarkerCache();
		List<Long> markerIdList = new ArrayList<>();
		logger.info("SupportTaskScheduler - processing {} markers", markerIdList.size());
		
		int markerCount = markerIdList.size();
		
		for (MarkerCache mc : markerCacheList) {
			if (mc.getExpire() <= 0) {
				// turn expire = 0 to D active
				logger.info("> Updating " + mc.getMarkerId() + " to D");
				markerService.updateStatusUpDown(mc.getMarkerId(), ConstantsUtil.MARKER_STATUS_DEACTIVED, mc.getUpVote(), mc.getDownVote());
				redisService.deleteKey(ConstantsUtil.REDIS_MARKER_PREFIX + ":" + mc.getMarkerId());
				
			} else {
				// count down the timer of those marker in redis
				mc.setExpire((long) (mc.getExpire() - (3 + (markerCount / 15.0))));
				
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

			marker.setExpire(marker.getExpire() - (2 * 5));

			markerCacheList.add(redisService.saveMarkerCache(marker));
		}

		// group marker cache with region
		markerCacheList = markerCacheList.stream()
				.sorted(Comparator.comparingLong(MarkerCache::getMarkerId).reversed())
				.collect(Collectors.toList());

		List<MarkerLinkedList> infoMasterList = new ArrayList<MarkerLinkedList>();
		List<MarkerLinkedList> popoMasterList = new ArrayList<MarkerLinkedList>();
		List<MarkerLinkedList> riotMasterList = new ArrayList<MarkerLinkedList>();
		List<MarkerLinkedList> imageMasterList = new ArrayList<MarkerLinkedList>();
		List<MarkerLinkedList> warningMasterList = new ArrayList<MarkerLinkedList>();
		List<MarkerLinkedList> blueFlagMasterList = new ArrayList<MarkerLinkedList>();
		
		for (MarkerCache mc : markerCacheList) {
			if (mc.getExpire() <= 0) {

			} else {
				// create the markerTrain
				MarkerLinkedList mll = MarkerLinkedList.latlng(mc.getLat(), mc.getLng());
				mll.setMarkerCache(mc);

				List<MarkerLinkedList> masterList = null;

				if (mc.getIcon().equals("popo.png")) {
					masterList = popoMasterList;
				} else if (mc.getIcon().equals("riot.png")) {
					masterList = riotMasterList;
				} else if (mc.getIcon().equals("015-pin-8.png")) {
					masterList = infoMasterList;
				} else if (mc.getType().equals("image") || mc.getIcon().equals("image.png")) {
					masterList = imageMasterList;
				} else if (mc.getType().equals("warning")) {
					masterList = warningMasterList;
				} else if (mc.getType().equals("info")) {
					masterList = infoMasterList;
				} else if (mc.getIcon().equals("blue.png")) {
					masterList = blueFlagMasterList;
				}

				if (masterList != null) {
					MarkerLinkedList node = findMaster(mll, masterList);
					
					if (node != null) {
						while (node.hasNext()) {
							node = node.next();
						}
						node.setNextMarker(mll);
					} else {
						// master node
						masterList.add(mll);
					}
				}
			}
		}

		// after fill up the linked list, reverse backfill the MarkerCache
		reverseFillMarkerCache(infoMasterList);
		reverseFillMarkerCache(popoMasterList);
		reverseFillMarkerCache(riotMasterList);
		reverseFillMarkerCache(imageMasterList);
		reverseFillMarkerCache(warningMasterList);
		reverseFillMarkerCache(blueFlagMasterList);
		
	}

	private void reverseFillMarkerCache(List<MarkerLinkedList> mllList) {
		for (MarkerLinkedList node : mllList) {

			if (node.hasNext()) {
				// have a group
				int totalWeight = 0;
				
				MarkerLinkedList masterNode = node;

				while (node.hasNext()) {
					node = node.next();
					
					totalWeight += node.getMarkerCache().getWeight();
					
					node.getMarkerCache().setGroup("r:" + masterNode.getMarkerCache().getMarkerId());
					redisService.saveMarkerCache(node.getMarkerCache());
				}
				totalWeight += masterNode.getMarkerCache().getWeight();
				
				masterNode.getMarkerCache().setGroup("m:" + totalWeight);
				redisService.saveMarkerCache(masterNode.getMarkerCache());
			}
		}
	}

	private MarkerLinkedList findMaster(MarkerLinkedList mll, List<MarkerLinkedList> masterList) {
		for (MarkerLinkedList masterTemp : masterList) {
			if (masterTemp.equals(mll)) {
				return masterTemp;
			}
		}
		return null;
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
