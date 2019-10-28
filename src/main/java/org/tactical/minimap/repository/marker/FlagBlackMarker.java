package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "blackflag")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class FlagBlackMarker extends Marker {

	@Override
	public String getType() {
		return "blackflag";
	}

	@Override
	public String getIcon() {
		return "black.png";
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
		return 180;
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