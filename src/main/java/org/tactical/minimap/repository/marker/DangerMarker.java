package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "danger")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class DangerMarker extends Marker {

	@NotNull
	int level;

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	@Override
	public String getType() {
		return "danger";
	}

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIcon() {
		return "<i class=\"fas fa-exclamation-triangle\" style=\"color:red\"></i>";
	}

}