package org.tactical.minimap.repository.marker;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Lob;

import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "info")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class InfoMarker extends Marker {

	@Column(nullable = true)
	@Lob
	String message;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String getType() {
		return "info";
	}

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		InfoMarker infoMarker = new InfoMarker();
		infoMarker.setLat(markerDTO.getLat());
		infoMarker.setLng(markerDTO.getLng());
		infoMarker.setMessage(markerDTO.getMessage());
		infoMarker.setExpire((long) 120);
		infoMarker.setStatus(ConstantsUtil.MARKER_STATUS_ACTIVE);
		infoMarker.setUuid(markerDTO.getUuid());
		return infoMarker;
	}

}