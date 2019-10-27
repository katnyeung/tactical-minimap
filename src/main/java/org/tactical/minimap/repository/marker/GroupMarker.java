package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "group")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class GroupMarker extends Marker {

	@Override
	public String getType() {
		return "group";
	}

	@Override
	public String getIcon() {
		return "protection.png";
	}

	@Override
	public int getIconSize() {
		return 32;
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
		return 15;
	}
	
	@Override
	public String getDescription() {
		return "安全";
	}
}