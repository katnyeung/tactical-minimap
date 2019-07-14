package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "group")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class GroupMarker extends Marker {

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
		return "group";
	}

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		GroupMarker marker = new GroupMarker();
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
		return "group.png";
	}

	@Override
	public int getIconSize() {
		return 45;
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
		return 2400;
	}

	@Override
	public int getAddDelay() {
		return 15;
	}
	
	@Override
	public int getVoteDelay() {
		return 15;
	}
}