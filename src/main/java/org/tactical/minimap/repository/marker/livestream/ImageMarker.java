package org.tactical.minimap.repository.marker.livestream;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "image")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ImageMarker extends Marker {

	String icon = "image.png";
	int iconSize = 22;
	
	@Override
	public String getIcon() {
		return this.icon;
	}
	
	@Override
	public int getIconSize() {
		return this.iconSize;
	}
	
	@NotNull
	String imagePath;

	@Override
	public String getType() {
		return "image";
	}

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		this.setLat(markerDTO.getLat());
		this.setLng(markerDTO.getLng());
		this.setMessage(markerDTO.getMessage());
		this.setExpire(getMarkerExpire());
		this.setStatus(ConstantsUtil.MARKER_STATUS_ACTIVE);
		this.setUuid(markerDTO.getUuid());
		this.setHour(markerDTO.getHour());
		this.setMinute(markerDTO.getMinute());
		this.setImagePath(markerDTO.getImagePath());
		this.setRegion(markerDTO.getRegion());
		return this;
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
		return 200;
	}

	@Override
	public int getAddDelay() {
		return 10;
	}

	@Override
	public int getVoteDelay() {
		return 10;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	@Override
	public String getDescription() {
		return "影像";
	}
	
	@Override
	public int getPulseRate() {
		return 8;
	}
}