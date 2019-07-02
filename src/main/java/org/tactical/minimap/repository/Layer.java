package org.tactical.minimap.repository;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.util.Auditable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "layer", indexes = { @Index(name = "layerKey", columnList = "layerId, layerKey") })
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public abstract class Layer extends Auditable<String> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long layerId;

	@JsonIgnore
	@OneToMany(mappedBy = "layer", cascade = CascadeType.ALL)
	List<Marker> markerList;

	@NotNull
	@Size(max = 100)
	String layerKey;

	@Column(nullable = true)
	@Size(max = 30)
	String password;

	@NotNull
	@Size(max = 1)
	String status;

	@Column(nullable = true)
	int duration;

	public Long getLayerId() {
		return layerId;
	}

	public void setLayerId(Long layerId) {
		this.layerId = layerId;
	}

	public List<Marker> getMarkerList() {
		return markerList;
	}

	public void setMarkerList(List<Marker> markerList) {
		this.markerList = markerList;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getLayerKey() {
		return layerKey;
	}

	public void setLayerKey(String layerKey) {
		this.layerKey = layerKey;
	}

}