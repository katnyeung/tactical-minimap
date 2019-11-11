package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "watertruck")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class WaterTruckMarker extends Marker {

	String icon = "water-truck.png";
	int iconSize = 54;
	
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
		return "watertruck";
	}

	@Override
	public int getUpRate() {
		return 30;
	}

	@Override
	public int getDownRate() {
		return 60;
	}
	
	@Override
	public long getMarkerExpire() {
		return 160;
	}

	@Override
	public int getAddDelay() {
		return 20;
	}
	
	@Override
	public int getVoteDelay() {
		return 15;
	}

	@Override
	public String getDescription() {
		return "水車";
	}
	
	@Override
	public int getPulseRate() {
		return 16;
	}
}