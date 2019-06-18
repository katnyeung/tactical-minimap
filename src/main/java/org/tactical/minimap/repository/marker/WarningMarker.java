package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "warning")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class WarningMarker extends Marker {

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
		return "warning";
	}

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		WarningMarker warningMarker = new WarningMarker();
		warningMarker.setLat(markerDTO.getLat());
		warningMarker.setLng(markerDTO.getLng());
		warningMarker.setExpire((long) 60);
		warningMarker.setStatus(ConstantsUtil.MARKER_STATUS_ACTIVE);
		return warningMarker;
	}
}