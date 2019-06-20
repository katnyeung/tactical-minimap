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
		WarningMarker marker = new WarningMarker();
		marker.setLat(markerDTO.getLat());
		marker.setLng(markerDTO.getLng());
		marker.setExpire(getMarkerExpire());
		marker.setStatus(ConstantsUtil.MARKER_STATUS_ACTIVE);
		marker.setMessage(markerDTO.getMessage());
		marker.setUuid(markerDTO.getUuid());
		return marker;
	}

	@Override
	public String getIcon() {
		return "warning.png";
	}

	@Override
	public int getIconSize() {
		// TODO Auto-generated method stub
		return 42;
	}

	@Override
	public int getRate() {
		// TODO Auto-generated method stub
		return 5;
	}

	@Override
	public long getMarkerExpire() {
		return 60;
	}

}