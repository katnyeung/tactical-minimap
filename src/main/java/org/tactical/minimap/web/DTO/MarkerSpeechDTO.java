package org.tactical.minimap.web.DTO;

public class MarkerSpeechDTO {
	Double fromLat;
	Double fromLng;

	Double toLat;
	Double toLng;

	Long timestamp;

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

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

}
