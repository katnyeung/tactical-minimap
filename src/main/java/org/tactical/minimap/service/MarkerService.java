package org.tactical.minimap.service;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tactical.minimap.DAO.MarkerDAO;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.repository.marker.shape.ShapeMarker;
import org.tactical.minimap.repository.marker.shape.ShapeMarkerDetail;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.util.MarkerCache;
import org.tactical.minimap.web.DTO.MarkerDTO;

@Service
public class MarkerService {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	MarkerDAO<Marker> markerDAO;

	@Autowired
	RedisService redisService;

	public List<Marker> findMultiLayerMarkers(List<String> layerKeys, Double lat, Double lng, Double range) {
		return markerDAO.findAllByLatLng(layerKeys, lat - range, lng - range, lat + range, lng + range);
	}

	public List<Marker> findMarkers(String layer, Double lat, Double lng, Double range) {
		return markerDAO.findByLatLngLayer(layer, lat - range, lng - range, lat + range, lng + range);
	}

	public Marker addMarker(Layer layer, MarkerDTO markerDTO, Marker marker) {
		logger.info("Adding Marker : " + marker.getClass().getName());
		marker = marker.fill(markerDTO);
		marker.setLayer(layer);

		markerDAO.save(marker);

		return marker;
	}

	public Marker findMarkerByMarkerId(Long markerId) {
		Optional<Marker> optionalMarker = markerDAO.findById(markerId);

		return optionalMarker.get();
	}

	@Transactional(readOnly = false)
	public void updateStatusUpDown(Long markerId, String status, int upVote, int downVote) {
		markerDAO.updateStatusUpDown(markerId, status, upVote, downVote);
	}

	public List<Marker> findActiveMarkersNotInCache(List<Long> markerIdList) {
		return markerDAO.findActiveMarkersNotInCache(markerIdList);
	}

	public List<Marker> findActiveMarkers() {
		return markerDAO.findActiveMarkers();
	}

	public void moveMarker(Marker marker, Double lat, Double lng) {
		if(marker instanceof ShapeMarker) {
			Double diffLat = lat - marker.getLat();
			Double diffLng = lng - marker.getLng();
						
			ShapeMarker shapeMarker = (ShapeMarker) marker;
			List<ShapeMarkerDetail> shapeList = shapeMarker.getShapeMarkerDetailList();
			for(ShapeMarkerDetail smd : shapeList) {
				smd.setLat(smd.getLat() + diffLat);
				smd.setLng(smd.getLng() + diffLng);
			}
		}

		marker.setLat(lat);
		marker.setLng(lng);
		
		markerDAO.save(marker);
	}

	public void deleteMarker(Marker marker) {
		marker.setStatus(ConstantsUtil.MARKER_STATUS_DEACTIVED);
		markerDAO.save(marker);
	}

	public void updateMessage(Marker marker, String message) {
		marker.setMessage(message);
		markerDAO.save(marker);
	}

	public int getMarkerCountInRange(String layerKey, Double lat, Double lng, Double range) {
		return markerDAO.getMarkerCountInRange(layerKey, lat - range, lat + range, lng - range, lng + range);
	}

	public double getTotalUpVoteInList(List<Marker> markerList) {
		double totalUpVote = 0.0;
		for (Marker marker : markerList) {
			totalUpVote = marker.getUpVote();
		}

		return totalUpVote;
	}

	public void addMarkerCache(List<Marker> markerList, String uuid) {
		Set<String> loggedLayer = redisService.getLoggedLayer(uuid);

		HashMap<Long, MarkerCache> markerCacheMap = new HashMap<>();
		double maxUpVoteInList = 0.0;

		for (Marker marker : markerList) {
			MarkerCache mc = redisService.getMarkerCacheByMarkerId(marker.getMarkerId());
			if (mc != null) {
				markerCacheMap.put(marker.getMarkerId(), mc);
				if (mc.getUpVote() > maxUpVoteInList) {
					maxUpVoteInList = mc.getUpVote();
				}
			}
		}

		for (Marker marker : markerList) {
			MarkerCache mc = markerCacheMap.get(marker.getMarkerId());

			if (loggedLayer.contains(marker.getLayer().getLayerKey())) {
				marker.setControllable(true);
			}

			marker.setOpacity(1);

			if (mc != null) {
				marker.setMarkerCache(mc);

				// set opacity
				if (maxUpVoteInList == 0.0) {
					marker.setOpacity(1);
				} else {
					double weight = 0.2 / maxUpVoteInList;
					marker.setOpacity((mc.getUpVote() * weight) + 0.8);

				}
			}
		}
	}

}
