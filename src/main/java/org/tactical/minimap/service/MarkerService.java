package org.tactical.minimap.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MarkerService {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	MarkerDAO<Marker> markerDAO;

	@Autowired
	RedisService redisService;

	@Autowired
	LayerService layerService;

	@PersistenceContext
	EntityManager em;

	public List<Marker> findMultiLayerMarkers(List<String> layerKeys, Double lat, Double lng, Double range) {
		return markerDAO.findAllByLatLng(layerKeys, lat - range, lng - range, lat + range, lng + range);
	}
	
	public List<Marker> findMarkers(String layer, Double lat, Double lng, Double range) {
		return markerDAO.findByLatLngLayer(layer, lat - range, lng - range, lat + range, lng + range);
	}

	public Marker addMarker(Layer layer, MarkerDTO markerDTO, Marker marker) {
		logger.info("Adding Marker : " + marker.getClass().getName());

		marker = marker.fill(markerDTO);

		if (marker != null) {
			marker.setLayer(layer);

			if (layer.getPassword() != null && !layer.getPassword().equals("")) {
				marker.setExpire(marker.getExpire() * layer.getExpireMultiplier());
			}

			markerDAO.save(marker);

		} else {
			return null;
		}

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
		if (marker instanceof ShapeMarker) {
			Double diffLat = lat - marker.getLat();
			Double diffLng = lng - marker.getLng();

			ShapeMarker shapeMarker = (ShapeMarker) marker;
			List<ShapeMarkerDetail> shapeList = shapeMarker.getShapeMarkerDetailList();
			for (ShapeMarkerDetail smd : shapeList) {
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

	public void update(Marker marker) {
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
		Set<String> loggedLayers = layerService.getLoggedLayers(uuid);

		for (Marker marker : markerList) {
			MarkerCache mc = redisService.getMarkerCacheByMarkerId(marker.getMarkerId());

			if (loggedLayers.contains(marker.getLayer().getLayerKey())) {
				marker.setControllable(true);
			}

			marker.setOpacity(1);

			if (mc != null) {
				marker.setMarkerCache(mc);

				// set opacity
				int minute = 60 * 1000;
				Date currentDate = Calendar.getInstance().getTime();
				double weight = 1.0;

				if (marker.getLastupdatedate().getTime() + (18 * minute) <= currentDate.getTime()) {
					weight = 0.6;
				} else if (marker.getLastupdatedate().getTime() + (15 * minute) <= currentDate.getTime()) {
					weight = 0.7;
				} else if (marker.getLastupdatedate().getTime() + (12 * minute) <= currentDate.getTime()) {
					weight = 0.8;
				} else if (marker.getLastupdatedate().getTime() + (6 * minute) <= currentDate.getTime()) {
					weight = 0.9;
				}

				marker.setOpacity(1 * weight);

			}
		}
	}

	public boolean pulseMarker(Marker marker) {

		MarkerCache mc = redisService.getMarkerCacheByMarkerId(marker.getMarkerId());

		if (mc != null) {

			mc.setPulse(ConstantsUtil.PULSE_RATE);

			redisService.saveMarkerCache(mc);

			return true;
		} else {
			return false;
		}

	}

	public void copyMarkerToLayer(Marker marker, String layerKey, String uuid) throws InstantiationException, IllegalAccessException, JsonProcessingException {
		logger.info("copying marker : " + marker.getMarkerId() + " to " + layerKey);

		ObjectMapper om = new ObjectMapper();

		Layer layer = layerService.getLayerByKey(layerKey);

		MarkerDTO markerDTO = new MarkerDTO();
		markerDTO.setLat(marker.getLat());
		markerDTO.setLng(marker.getLng());
		markerDTO.setMessage(marker.getMessage());
		markerDTO.setType(marker.getType());
		markerDTO.setUuid(uuid);

		if (marker instanceof ShapeMarker) {
			ShapeMarker shapeMarker = (ShapeMarker) marker;

			markerDTO.setShapeType(shapeMarker.getShapeType());
			List<Map<String, Double>> shapeList = new ArrayList<Map<String, Double>>();
			for (ShapeMarkerDetail smd : shapeMarker.getShapeMarkerDetailList()) {
				LinkedHashMap<String, Double> map = new LinkedHashMap<String, Double>();
				map.put("lat", smd.getLat());
				map.put("lng", smd.getLng());
				shapeList.add(map);
			}
			markerDTO.setShapeList(om.writeValueAsString(shapeList));

		}

		Marker cloneMarker = marker.getClass().newInstance();

		cloneMarker = cloneMarker.fill(markerDTO);

		cloneMarker.setLayer(layer);

		if (layer.getPassword() != null && !layer.getPassword().equals("")) {
			cloneMarker.setExpire(marker.getExpire() * layer.getExpireMultiplier());
		}

		markerDAO.save(cloneMarker);
	}

}
