package org.tactical.minimap.web.socket;

import java.util.List;

import org.tactical.minimap.repository.marker.Marker;

public class ResponseSocketData {
	List<Marker> markerList;

	public List<Marker> getMarkerList() {
		return markerList;
	}

	public void setMarkerList(List<Marker> markerList) {
		this.markerList = markerList;
	}

}
