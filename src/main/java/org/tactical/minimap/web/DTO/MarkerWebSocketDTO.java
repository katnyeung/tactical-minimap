package org.tactical.minimap.web.DTO;

import java.util.HashMap;
import java.util.Map;

public class MarkerWebSocketDTO {
	Double lat;
	Double lng;

	String uuid;

	String layerString;

	String markerIdList;
	
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

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getLayerString() {
		return layerString;
	}

	public void setLayerString(String layerString) {
		this.layerString = layerString;
	}

	public String getMarkerIdList() {
		return markerIdList;
	}

	public void setMarkerIdList(String markerIdList) {
		this.markerIdList = markerIdList;
	}

	@Override
	public String toString() {
		return "MarkerWebSocketDTO [lat=" + lat + ", lng=" + lng + ", uuid=" + uuid + ", layerString=" + layerString + ", markerIdList=" + markerIdList + "]";
	}
	
	public Map<String, String> toHashMap() {
		HashMap<String, String> hashMap = new HashMap<>();
		hashMap.put("lat", "" + lat);
		hashMap.put("lng", "" + lng);
		hashMap.put("uuid", "" + uuid);
		hashMap.put("layerString", "" + layerString);
		hashMap.put("markerIdList", "" + markerIdList);
		return hashMap;
	}
	
	public static MarkerWebSocketDTO fromHashMap(Map<Object, Object> objMap) {
		if (objMap != null && objMap.get("lat") != null && objMap.get("lng") != null) {
			MarkerWebSocketDTO markerWSDTO = new MarkerWebSocketDTO();
			markerWSDTO.setLat(Double.parseDouble((String) objMap.get("lat")));
			markerWSDTO.setLng(Double.parseDouble((String) objMap.get("lng")));
			markerWSDTO.setUuid((String)objMap.get("uuid"));
			markerWSDTO.setLayerString((String)objMap.get("layerString"));
			markerWSDTO.setMarkerIdList((String)objMap.get("markerIdList"));
			return markerWSDTO;
		} else {
			return null;
		}
	}
}
