package org.tactical.minimap.util;

public class MarkerGeoCoding {
	double lat;
	double lng;

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

	public static MarkerGeoCoding latlng(double lat, double lng) {
		MarkerGeoCoding latlng = new MarkerGeoCoding();
		latlng.setLat(lat);
		latlng.setLng(lng);
		return latlng;
	}
}
