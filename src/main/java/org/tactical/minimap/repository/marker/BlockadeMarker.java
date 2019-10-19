package org.tactical.minimap.repository.marker;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "blockade")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class BlockadeMarker extends Marker {

	@Override
	public String getType() {
		return "blockade";
	}

	@Override
	public String getIcon() {
		return "no-stopping.png";
	}

	@Override
	public int getIconSize() {
		return 38;
	}

	@Override
	public int getUpRate() {
		return 50;
	}

	@Override
	public int getDownRate() {
		return 50;
	}

	@Override
	public long getMarkerExpire() {
		return 160;
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
		return "封路";
	}
}