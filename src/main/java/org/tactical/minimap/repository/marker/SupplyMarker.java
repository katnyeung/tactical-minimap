package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "supply")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class SupplyMarker extends Marker {

	@Override
	public String getType() {
		return "supply";
	}

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		SupplyMarker marker = new SupplyMarker();
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
		return "supply.png";
	}

	@Override
	public int getIconSize() {
		return 52;
	}

	@Override
	public int getRate() {
		return 10;
	}

	@Override
	public long getMarkerExpire() {
		return 240;
	}

}