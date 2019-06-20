package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "protesting")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ProtestingMarker extends Marker {

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
		return "protesting";
	}

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		ProtestingMarker marker = new ProtestingMarker();
		marker.setLat(markerDTO.getLat());
		marker.setLng(markerDTO.getLng());
		marker.setMessage(markerDTO.getMessage());
		marker.setExpire(getMarkerExpire());
		marker.setStatus(ConstantsUtil.MARKER_STATUS_ACTIVE);
		marker.setUuid(markerDTO.getUuid());
		return marker;
	}

	@Override
	public String getIcon() {
		return "protesting.png";
	}

	@Override
	public int getIconSize() {
		return 50;
	}

	@Override
	public int getRate() {
		return 5;
	}

	@Override
	public long getMarkerExpire() {
		return 60;
	}

}