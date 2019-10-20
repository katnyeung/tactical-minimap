package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "yellowflag")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class FlagYellowMarker extends Marker {

	@Override
	public String getType() {
		return "yellowflag";
	}
	
	@Override
	public String getIcon() {
		return "yellow.png";
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
}