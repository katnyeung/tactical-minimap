package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "police")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class PoliceMarker extends Marker {

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
		return "police";
	}

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		PoliceMarker marker = new PoliceMarker();
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
		return "police.png";
	}

	@Override
	public int getIconSize() {
		return 42;
	}

	@Override
	public int getRate() {
		// TODO Auto-generated method stub
		return 6;
	}

	@Override
	public long getMarkerExpire() {
		// TODO Auto-generated method stub
		return 160;
	}

}