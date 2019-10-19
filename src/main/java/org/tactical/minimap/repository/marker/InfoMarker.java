package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "info")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class InfoMarker extends Marker {

	@Override
	public String getType() {
		return "info";
	}

	@Override
	public String getIcon() {
		return "019-pin-4.png";
	}

	@Override
	public int getIconSize() {
		return 34;
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