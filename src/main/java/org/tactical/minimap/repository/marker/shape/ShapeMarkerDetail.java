package org.tactical.minimap.repository.marker.shape;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.tactical.minimap.util.Auditable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ShapeMarkerDetail extends Auditable<String> {

	@JsonIgnore
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long shapeMarkerDetailId;

	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "marker_id", referencedColumnName = "markerId")
	ShapeMarker shapeMarker;

	Long seq;

	Double lat;
	Double lng;

	public Long getShapeMarkerDetailId() {
		return shapeMarkerDetailId;
	}

	public void setShapeMarkerDetailId(Long shapeMarkerDetailId) {
		this.shapeMarkerDetailId = shapeMarkerDetailId;
	}

	public ShapeMarker getShapeMarker() {
		return shapeMarker;
	}

	public void setShapeMarker(ShapeMarker shapeMarker) {
		this.shapeMarker = shapeMarker;
	}

	public Long getSeq() {
		return seq;
	}

	public void setSeq(Long seq) {
		this.seq = seq;
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