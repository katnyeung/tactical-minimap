package org.tactical.minimap.util;

import java.util.HashMap;
import java.util.Map;

public class MarkerCache {
	Long markerId;
	double lat;
	double lng;

	int upVote;
	int downVote;

	int expire;

	public Long getMarkerId() {
		return markerId;
	}

	public void setMarkerId(Long markerId) {
		this.markerId = markerId;
	}

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

	public int getUpVote() {
		return upVote;
	}

	public void setUpVote(int upVote) {
		this.upVote = upVote;
	}

	public int getDownVote() {
		return downVote;
	}

	public void setDownVote(int downVote) {
		this.downVote = downVote;
	}

	public int getExpire() {
		return expire;
	}

	public void setExpire(int expire) {
		this.expire = expire;
	}

	public Map<String, Object> toHashMap() {
		HashMap<String, Object> hashMap = new HashMap<>();
		hashMap.put("lat", lat);
		hashMap.put("lng", lng);
		hashMap.put("upVote", upVote);
		hashMap.put("downVote", downVote);

		hashMap.put("expire", expire);
		return hashMap;
	}

	public static MarkerCache fromHashMap(Map<Object, Object> objMap) {
		MarkerCache mc = new MarkerCache();
		mc.setLat(Double.parseDouble((String) objMap.get("lat")));

		mc.setLng(Double.parseDouble((String) objMap.get("lng")));
		mc.setUpVote(Integer.parseInt((String) objMap.get("upVote")));
		mc.setDownVote(Integer.parseInt((String) objMap.get("downVote")));
		mc.setExpire(Integer.parseInt((String) objMap.get("expire")));

		return mc;
	}
}
