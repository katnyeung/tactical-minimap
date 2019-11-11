package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "warning")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class WarningMarker extends Marker {

	String icon = "warning.png";
	int iconSize = 34;
	
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
		return "warning";
	}

	@Override
	public int getUpRate() {
		// TODO Auto-generated method stub
		return 60;
	}

	@Override
	public int getDownRate() {
		return 60;
	}

	@Override
	public long getMarkerExpire() {
		return 120;
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
		return "警示";
	}
	
	@Override
	public int getPulseRate() {
		return 16;
	}
}