package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "medical")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class MedicalMarker extends Marker {

	String icon = "010-first-aid-kit.png";
	int iconSize = 32;
	
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
		return "medical";
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
		return 280;
	}

	@Override
	public int getAddDelay() {
		return 5;
	}
	
	@Override
	public int getVoteDelay() {
		return 5;
	}
	
	@Override
	public String getDescription() {
		return "救護";
	}
	
	@Override
	public int getPulseRate() {
		return 8;
	}
}