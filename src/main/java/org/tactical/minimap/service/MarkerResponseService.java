package org.tactical.minimap.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tactical.minimap.DAO.MarkerResponseDAO;
import org.tactical.minimap.repository.DownResponse;
import org.tactical.minimap.repository.MarkerResponse;
import org.tactical.minimap.repository.UpResponse;
import org.tactical.minimap.repository.marker.Marker;

@Service
public class MarkerResponseService {
	@Autowired
	MarkerResponseDAO<MarkerResponse> markerResponseDAO;

	public void upVote(Marker marker, String uuid) {
		UpResponse up = new UpResponse();
		up.setMarker(marker);
		up.setUuid(uuid);
		markerResponseDAO.save(up);
	}

	public void downVote(Marker marker, String uuid) {
		DownResponse down = new DownResponse();
		down.setMarker(marker);
		down.setUuid(uuid);
		markerResponseDAO.save(down);
	}

	public int getExpireRate(Double lat, Double lng, Double range) {
		return markerResponseDAO.getExpireRate(lat - range, lat + range, lng - range, lng + range);
	}
}
