package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "blueflag")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class FlagBlueMarker extends Marker {

	@Override
	public String getType() {
		return "blueflag";
	}

	@Override
	public String getIcon() {
		return "blue.png";
	}

	@Override
	public int getIconSize() {
		return 56;
	}

	@Override
	public int getUpRate() {
		return 15;
	}

	@Override
	public int getDownRate() {
		return 40;
	}
	
	@Override
	public long getMarkerExpire() {
		return 240;
	}

	@Override
	public int getAddDelay() {
		return 20;
	}
	
	@Override
	public int getVoteDelay() {
		return 25;
	}
	
	@Override
	public String getDescription() {
		return "危險";
	}
	
	@Override
	public int getPulseRate() {
		return 12;
	}
}