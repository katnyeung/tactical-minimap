package org.tactical.minimap.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tactical.minimap.DAO.MarkerDAO;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.web.DTO.MarkerDTO;

@Service
public class MarkerService {
	@Autowired
	MarkerDAO<Marker> markerDAO;

	public List<Marker> findMarkers(Double lat, Double lng, Double range) {
		return markerDAO.findAllByLatLng(lat - range, lng - range, lat + range, lng + range);
	}

	public List<Marker> findAllMarkers() {
		return markerDAO.findAll();
	}

	public Marker addMarker(MarkerDTO markerDTO) {
		for (Class<? extends Marker> MarkerClass : Marker.ClassList) {
			try {
				Marker marker = MarkerClass.newInstance();
				if (marker.getType().equals(markerDTO.getType())) {
					marker = marker.fill(markerDTO);
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

	@Transactional
	public void updateStatus(Long markerId, String status) {
		markerDAO.updateStatus(markerId, status);
	}

	public List<Marker> findActiveMarkersNotInCache(List<Long> markerIdList) {
		return markerDAO.findActiveMarkersNotInCache(markerIdList);
	}

	public List<Marker> findActiveMarkers() {
		return markerDAO.findActiveMarkers();
	}
}
