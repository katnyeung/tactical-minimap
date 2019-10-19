package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "redinfo")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RedInfoMarker extends Marker {

	@Override
	public String getType() {
		return "redinfo";
	}

	@Override
	public String getIcon() {
		return "017-pin-6.png";
	}

	@Override
	public int getIconSize() {
		return 38;
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
		return 180;
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
	public String getDescription() {
		return "資訊";
	}
}