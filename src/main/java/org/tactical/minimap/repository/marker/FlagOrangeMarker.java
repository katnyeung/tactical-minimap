package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "orangeflag")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class FlagOrangeMarker extends Marker {

	String icon = "orange.png";
	int iconSize = 56;
	
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
		return "orangeflag";
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
		return 10;
	}
}