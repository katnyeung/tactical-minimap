package org.tactical.minimap.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tactical.minimap.DAO.MarkerDAO;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.web.DTO.MarkerDTO;

@Service
public class MarkerService {
	@Autowired
	MarkerDAO markerDAO;

	public List<Marker> findMarkers(Double lat, Double lng, Double range) {
		return markerDAO.findAllByLatLng(lat - range, lng - range, lat + range, lng + range);
	}

	public List<Marker> findAllMarkers() {
		return markerDAO.findAll();
	}

	public Marker addMarker(MarkerDTO markerDTO) {
		for (Class<? extends Marker> MarkerClass : Marker.MarkerClassList) {
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
}
