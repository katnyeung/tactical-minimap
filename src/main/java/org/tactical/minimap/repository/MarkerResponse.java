package org.tactical.minimap.repository;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.util.Auditable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "marker_response_type")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public abstract class MarkerResponse extends Auditable<String> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long markerResponseId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "marker_id", referencedColumnName = "markerId")
	Marker marker;

	String uuid;

	public Long getMarkerResponseId() {
		return markerResponseId;
	}

	public void setMarkerResponseId(Long markerResponseId) {
		this.markerResponseId = markerResponseId;
	}

	public Marker getMarker() {
		return marker;
	}

	public void setMarker(Marker marker) {
		this.marker = marker;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}