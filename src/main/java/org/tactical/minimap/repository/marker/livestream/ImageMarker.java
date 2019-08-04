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

	@NotNull
	String imagePath;

	@Override
	public String getType() {
		return "image";
	}

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		ImageMarker marker = new ImageMarker();
		marker.setLat(markerDTO.getLat());
		marker.setLng(markerDTO.getLng());
		marker.setMessage(markerDTO.getMessage());
		marker.setExpire(getMarkerExpire());
		marker.setStatus(ConstantsUtil.MARKER_STATUS_ACTIVE);
		marker.setUuid(markerDTO.getUuid());
		marker.setImagePath(markerDTO.getImagePath());
		return marker;
	}

	@Override
	public String getIcon() {
		return "image.png";
	}

	@Override
	public int getIconSize() {
		return 42;
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
}