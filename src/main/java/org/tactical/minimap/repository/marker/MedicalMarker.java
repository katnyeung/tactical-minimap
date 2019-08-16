package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "medical")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class MedicalMarker extends Marker {

	@Override
	public String getType() {
		return "medical";
	}

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		MedicalMarker marker = new MedicalMarker();
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
		return "010-first-aid-kit.png";
	}

	@Override
	public int getIconSize() {
		return 32;
	}
	
	@Override
	public int getUpRate() {
		return 90;
	}

	@Override
	public int getDownRate() {
		return 90;
	}
	
	@Override
	public long getMarkerExpire() {
		return 280;
	}

	@Override
	public int getAddDelay() {
		return 5;
	}
	
	@Override
	public int getVoteDelay() {
		return 5;
	}
	
	@Override
	public String getDescription() {
		return "救護";
	}
}