package org.tactical.minimap.repository;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.tactical.minimap.util.Auditable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class TelegramChannel extends Auditable<String> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long telegramChannelId;

	@Size(max = 255)
	String searchPrefix;

	@Size(max = 255)
	String groupKey;

	@Size(max = 255)
	String groupName;

	@Size(max = 100)
	Long channelId;

	@Size(max = 100)
	Long accessHash;

	@NotNull
	@Size(max = 1)
	String status;

	double fromLat;
	double fromLng;

	double toLat;
	double toLng;

	@Column(nullable = true)
	String geoCodeMethod;

	int priority = 0;

	public Long getTelegramChannelId() {
		return telegramChannelId;
	}

	public void setTelegramChannelId(Long telegramChannelId) {
		this.telegramChannelId = telegramChannelId;
	}

	public String getSearchPrefix() {
		return searchPrefix;
	}

	public void setSearchPrefix(String searchPrefix) {
		this.searchPrefix = searchPrefix;
	}

	public String getGroupKey() {
		return groupKey;
	}

	public void setGroupKey(String groupKey) {
		this.groupKey = groupKey;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public Long getChannelId() {
		return channelId;
	}

	public void setChannelId(Long channelId) {
		this.channelId = channelId;
	}

	public Long getAccessHash() {
		return accessHash;
	}

	public void setAccessHash(Long accessHash) {
		this.accessHash = accessHash;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public double getFromLat() {
		return fromLat;
	}

	public void setFromLat(double fromLat) {
		this.fromLat = fromLat;
	}

	public double getFromLng() {
		return fromLng;
	}

	public void setFromLng(double fromLng) {
		this.fromLng = fromLng;
	}

	public double getToLat() {
		return toLat;
	}

	public void setToLat(double toLat) {
		this.toLat = toLat;
	}

	public double getToLng() {
		return toLng;
	}

	public void setToLng(double toLng) {
		this.toLng = toLng;
	}

	public String getGeoCodeMethod() {
		return geoCodeMethod;
	}

	public void setGeoCodeMethod(String geoCodeMethod) {
		this.geoCodeMethod = geoCodeMethod;
	}

}