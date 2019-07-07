package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "livestream")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class LiveStreamMarker extends Marker {

	@Override
	public String getType() {
		return "livestream";
	}

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		LiveStreamMarker marker = new LiveStreamMarker();
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
		return 1200;
	}

	@Override
	public int getAddDelay() {
		return 15;
	}

	@Override
	public int getVoteDelay() {
		return 10;
	}
}