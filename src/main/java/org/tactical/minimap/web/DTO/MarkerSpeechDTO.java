package org.tactical.minimap.web.DTO;

import java.util.List;

public class MarkerSpeechDTO {
	Double fromLat;
	Double fromLng;

	int deg;

	Double toLat;
	Double toLng;

	List<Long> markerIdList;

	Long timestamp;

	public int getDeg() {
		return deg;
	}

	public void setDeg(int deg) {
		this.deg = deg;
	}

	public Double getFromLat() {
		return fromLat;
	}

	public void setFromLat(Double fromLat) {
		this.fromLat = fromLat;
	}

	public Double getFromLng() {
		return fromLng;
	}

	public void setFromLng(Double fromLng) {
		this.fromLng = fromLng;
	}

	public Double getToLat() {
		return toLat;
	}

	public void setToLat(Double toLat) {
		this.toLat = toLat;
	}

	public Double getToLng() {
		return toLng;
	}

	public void setToLng(Double toLng) {
		this.toLng = toLng;
	}

	public List<Long> getMarkerIdList() {
		return markerIdList;
	}

	public void setMarkerIdList(List<Long> markerIdList) {
		this.markerIdList = markerIdList;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

}
