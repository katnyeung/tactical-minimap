package org.tactical.minimap.util;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MarkerCache {
	@JsonIgnore
	Long markerId;
	
	@JsonIgnore
	double lat;
	@JsonIgnore
	double lng;

	@JsonProperty("u")
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	int upVote = 0;

	@JsonProperty("d")
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	int downVote = 0;

	Long expire;

	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	int pulse = 0;
	
	@JsonIgnore
	int upRate;

	@JsonIgnore
	int downRate;

	@JsonIgnore
	String layer;

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
		if (expire < 0) {
			return (long) 0;
		} else {
			return expire;
		}
	}

	public void setExpire(Long expire) {
		this.expire = expire;
	}

	public int getUpRate() {
		return upRate;
	}

	public void setUpRate(int upRate) {
		this.upRate = upRate;
	}

	public int getDownRate() {
		return downRate;
	}

	public void setDownRate(int downRate) {
		this.downRate = downRate;
	}

	public String getLayer() {
		return layer;
	}

	public void setLayer(String layer) {
		this.layer = layer;
	}

	public int getPulse() {
		return pulse;
	}

	public void setPulse(int pulse) {
		this.pulse = pulse;
	}

	public Map<String, String> toHashMap() {
		HashMap<String, String> hashMap = new HashMap<>();
		hashMap.put("lat", "" + lat);
		hashMap.put("lng", "" + lng);
		hashMap.put("upVote", "" + upVote);
		hashMap.put("downVote", "" + downVote);
		hashMap.put("upRate", "" + upRate);
		hashMap.put("downRate", "" + downRate);
		hashMap.put("expire", "" + expire);
		hashMap.put("layer", "" + layer);
		hashMap.put("pulse", "" + pulse);
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
			mc.setUpRate(Integer.parseInt((String) objMap.get("upRate")));
			mc.setDownRate(Integer.parseInt((String) objMap.get("downRate")));
			mc.setLayer((String) objMap.get("layer"));
			mc.setPulse(Integer.parseInt((String) objMap.get("pulse")));
			return mc;
		} else {
			return null;
		}
	}
}
