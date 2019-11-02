package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "conflict")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ConflictMarker extends Marker {

	@Override
	public String getType() {
		return "conflict";
	}

	@Override
	public String getIcon() {
		return "conflict.png";
	}

	@Override
	public int getIconSize() {
		return 32;
	}

	@Override
	public int getUpRate() {
		return 50;
	}

	@Override
	public int getDownRate() {
		return 50;
	}

	@Override
	public long getMarkerExpire() {
		return 120;
	}

	@Override
	public int getAddDelay() {
		return 15;
	}
	
	@Override
	public int getVoteDelay() {
		return 15;
	}
	
	@Override
	public int getPulseRate() {
		return 8;
	}
}