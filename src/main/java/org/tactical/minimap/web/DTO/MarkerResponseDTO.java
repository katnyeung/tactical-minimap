package org.tactical.minimap.web.DTO;

public class MarkerResponseDTO {
	Long markerId;
	Double lat;
	Double lng;

	public Long getMarkerId() {
		return markerId;
	}

	public void setMarkerId(Long markerId) {
		this.markerId = markerId;
	}

	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getLng() {
		return lng;
	}

	public void setLng(Double lng) {
		this.lng = lng;
	}

}
