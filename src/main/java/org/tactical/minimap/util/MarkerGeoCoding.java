package org.tactical.minimap.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MarkerGeoCoding {
	double lat;
	double lng;

	@JsonProperty("label")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	String label;

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLng() {
		return lng;
	}

	public void setLng(double lng) {
		this.lng = lng;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public static MarkerGeoCoding latlng(double lat, double lng) {
		MarkerGeoCoding latlng = new MarkerGeoCoding();
		latlng.setLat(lat);
		latlng.setLng(lng);
		return latlng;
	}
}
