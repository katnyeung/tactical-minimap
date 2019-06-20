package org.tactical.minimap.util;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MarkerCache {
	@JsonIgnore
	Long markerId;
	@JsonIgnore
	double lat;
	@JsonIgnore
	double lng;

	int upVote;
	int downVote;

	Long expire;

	int rate;

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

	public Long getExpire() {
		return expire;
	}

	public void setExpire(Long expire) {
		this.expire = expire;
	}

	public int getRate() {
		return rate;
	}

	public void setRate(int rate) {
		this.rate = rate;
	}

	public Map<String, String> toHashMap() {
		HashMap<String, String> hashMap = new HashMap<>();
		hashMap.put("lat", "" + lat);
		hashMap.put("lng", "" + lng);
		hashMap.put("upVote", "" + upVote);
		hashMap.put("downVote", "" + downVote);

		hashMap.put("rate", "" + rate);

		hashMap.put("expire", "" + expire);
		return hashMap;
	}

	public static MarkerCache fromHashMap(Map<Object, Object> objMap) {
		if (objMap != null && objMap.get("lat") != null && objMap.get("lng") != null) {
			MarkerCache mc = new MarkerCache();
			mc.setLat(Double.parseDouble((String) objMap.get("lat")));
			mc.setLng(Double.parseDouble((String) objMap.get("lng")));
			mc.setUpVote(Integer.parseInt((String) objMap.get("upVote")));
			mc.setDownVote(Integer.parseInt((String) objMap.get("downVote")));
			mc.setExpire(Long.parseLong((String) objMap.get("expire")));
			mc.setRate(Integer.parseInt((String) objMap.get("rate")));
			return mc;
		} else {
			return null;
		}
	}
}
