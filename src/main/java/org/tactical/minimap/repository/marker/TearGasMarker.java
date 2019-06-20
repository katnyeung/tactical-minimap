package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "teargas")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class TearGasMarker extends Marker {

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
		return "teargas";
	}

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		TearGasMarker marker = new TearGasMarker();
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
		return "009-gas-mask.png";
	}

	@Override
	public int getIconSize() {
		return 60;
	}

	@Override
	public int getRate() {
		// TODO Auto-generated method stub
		return 5;
	}

	@Override
	public long getMarkerExpire() {
		// TODO Auto-generated method stub
		return 60;
	}

}