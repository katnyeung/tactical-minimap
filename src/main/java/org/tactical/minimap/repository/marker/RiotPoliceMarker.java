package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "riotpolice")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RiotPoliceMarker extends Marker {

	String icon = "riot.png";
	int iconSize = 48;
	
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
		return "riotpolice";
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
		return "防暴";
	}
	
	@Override
	public int getPulseRate() {
		return 14;
	}
}