package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "teargas")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class TearGasMarker extends Marker {

	String icon = "teargas.png";
	int iconSize = 52;
	
	@Override
	public String getIcon() {
		return this.icon;
	}
	
	@Override
	public int getIconSize() {
		return this.iconSize;
	}
	
	@Override
	public String getType() {
		return "teargas";
	}

	@Override
	public int getUpRate() {
		return 25;
	}

	@Override
	public int getDownRate() {
		return 50;
	}

	@Override
	public long getMarkerExpire() {
		return 220;
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
	public String getDescription() {
		return "催淚彈";
	}
	
	@Override
	public int getPulseRate() {
		return 20;
	}
}