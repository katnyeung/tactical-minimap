package org.tactical.minimap.web.DTO;

import java.util.List;

public class MarkerDTO {
	Double lat;
	Double lng;

	String type;
	String message;

	String uuid;

	String layer;

	Long markerId;

	List<ShapeDTO> shapeList;

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

	public List<ShapeDTO> getShapeList() {
		return shapeList;
	}

	public void setShapeList(List<ShapeDTO> shapeList) {
		this.shapeList = shapeList;
	}

}
