package org.tactical.minimap.web.DTO;

import java.util.List;

public class MarkerDTO {
	Double lat;
	Double lng;

	String type;
	String message;

	String uuid;

	Long telegramMessageId;
	
	String layer;

	Long markerId;

	String shapeType;

	String shapeList;

	String imagePath;

	String color;

	Integer hour;

	Integer minute;

	List<Long> markerIdList;

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Long getMarkerId() {
		return markerId;
	}

	public void setMarkerId(Long markerId) {
		this.markerId = markerId;
	}

	public String getLayer() {
		return layer;
	}

	public void setLayer(String layer) {
		this.layer = layer;
	}

	public String getShapeType() {
		return shapeType;
	}

	public void setShapeType(String shapeType) {
		this.shapeType = shapeType;
	}

	public String getShapeList() {
		return shapeList;
	}

	public void setShapeList(String shapeList) {
		this.shapeList = shapeList;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public Integer getHour() {
		return hour;
	}

	public void setHour(Integer hour) {
		this.hour = hour;
	}

	public Integer getMinute() {
		return minute;
	}

	public void setMinute(Integer minute) {
		this.minute = minute;
	}

	public List<Long> getMarkerIdList() {
		return markerIdList;
	}

	public void setMarkerIdList(List<Long> markerIdList) {
		this.markerIdList = markerIdList;
	}

	public Long getTelegramMessageId() {
		return telegramMessageId;
	}

	public void setTelegramMessageId(Long telegramMessageId) {
		this.telegramMessageId = telegramMessageId;
	}

}
