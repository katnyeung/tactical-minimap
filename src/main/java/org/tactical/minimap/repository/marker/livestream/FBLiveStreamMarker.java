package org.tactical.minimap.repository.marker.livestream;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "fb_livestream")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class FBLiveStreamMarker extends Marker {

	@Override
	public String getType() {
		return "fb_livestream";
	}

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		if (markerDTO.getMessage() != null && !markerDTO.getMessage().equals("")) {
			FBLiveStreamMarker marker = new FBLiveStreamMarker();
			marker.setLat(markerDTO.getLat());
			marker.setLng(markerDTO.getLng());
			marker.setMessage(markerDTO.getMessage());
			marker.setExpire(getMarkerExpire());
			marker.setStatus(ConstantsUtil.MARKER_STATUS_ACTIVE);
			marker.setUuid(markerDTO.getUuid());
			return marker;
		} else {
			return null;
		}
	}

	@Override
	public String getIcon() {
		return "live.png";
	}

	@Override
	public int getIconSize() {
		return 42;
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
		return 480;
	}

	@Override
	public int getAddDelay() {
		return 15;
	}

	@Override
	public int getVoteDelay() {
		return 10;
	}
	
	@Override
	public int getPulseRate() {
		return 8;
	}
}