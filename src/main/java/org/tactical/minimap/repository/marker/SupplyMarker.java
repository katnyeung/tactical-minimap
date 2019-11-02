package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

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
	public String getIcon() {
		return "supply.png";
	}

	@Override
	public int getIconSize() {
		return 36;
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
		return 240;
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
		return "補給";
	}
	
	@Override
	public int getPulseRate() {
		return 8;
	}
}