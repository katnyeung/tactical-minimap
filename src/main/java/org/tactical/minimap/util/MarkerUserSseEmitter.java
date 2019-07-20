package org.tactical.minimap.util;

import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class MarkerUserSseEmitter extends SseEmitter {

	String uuid;

	List<String> layerKeys;

	Double lat;

	Double lng;

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public List<String> getLayerKeys() {
		return layerKeys;
	}

	public void setLayerKeys(List<String> layerKeys) {
		this.layerKeys = layerKeys;
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
