package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "danger")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class DangerMarker extends Marker {

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
		return "danger";
	}

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		DangerMarker dm = new DangerMarker();
		dm.setLat(markerDTO.getLat());
		dm.setLng(markerDTO.getLng());
		dm.setMessage(markerDTO.getMessage());
		dm.setExpire((long) 60);
		dm.setStatus(ConstantsUtil.MARKER_STATUS_ACTIVE);
		dm.setUuid(markerDTO.getUuid());
		return dm;
	}

	@Override
	public String getIcon() {
		return "emer/009-gas-mask.png";
	}

}