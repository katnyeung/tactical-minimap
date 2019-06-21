package org.tactical.minimap.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tactical.minimap.DAO.MarkerDAO;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;

@Service
public class MarkerService {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	MarkerDAO<Marker> markerDAO;

	public List<Marker> findMarkers(String layer, Double lat, Double lng, Double range) {
		return markerDAO.findAllByLatLng(layer, lat - range, lng - range, lat + range, lng + range);
	}

	public List<Marker> findAllMarkers(String layer) {
		return markerDAO.findAll();
	}

	public Marker addMarker(String layer, MarkerDTO markerDTO) {
		for (Class<? extends Marker> MarkerClass : Marker.ClassList) {
			try {
				Marker marker = MarkerClass.newInstance();
				logger.info("Adding Marker : " + marker.getClass().getName());
				if (marker.getType().equals(markerDTO.getType())) {
					marker = marker.fill(markerDTO);
					marker.setLayer(layer);
					markerDAO.save(marker);
				}

			} catch (InstantiationException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return null;
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
}
