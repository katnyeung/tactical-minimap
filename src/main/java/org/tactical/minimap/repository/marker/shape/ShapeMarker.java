package org.tactical.minimap.repository.marker.shape;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;
import org.tactical.minimap.web.DTO.ShapeDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "shape")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ShapeMarker extends Marker {

	@JsonIgnore
	@OneToMany(mappedBy = "shapeMarker", cascade = CascadeType.ALL)
	List<ShapeMarkerDetail> shapeMarkerDetailList;

	@NotNull
	String shapeType;

	@NotNull
	int level;

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		ShapeMarker marker = new ShapeMarker();
		marker.setLat(markerDTO.getLat());
		marker.setLng(markerDTO.getLng());
		marker.setMessage(markerDTO.getMessage());
		marker.setExpire(getMarkerExpire());
		marker.setStatus(ConstantsUtil.MARKER_STATUS_ACTIVE);
		marker.setUuid(markerDTO.getUuid());

		List<ShapeMarkerDetail> shapeMarkerDetailList = new ArrayList<ShapeMarkerDetail>();

		for (ShapeDTO shapeDTO : markerDTO.getShapeList()) {
			ShapeMarkerDetail smd = new ShapeMarkerDetail();
			smd.setLat(shapeDTO.getLat());
			smd.setLng(shapeDTO.getLng());
		}

		marker.setShapeMarkerDetailList(shapeMarkerDetailList);

		return marker;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public List<ShapeMarkerDetail> getShapeMarkerDetailList() {
		return shapeMarkerDetailList;
	}

	public void setShapeMarkerDetailList(List<ShapeMarkerDetail> shapeMarkerDetailList) {
		this.shapeMarkerDetailList = shapeMarkerDetailList;
	}

	public String getShapeType() {
		return shapeType;
	}

	public void setShapeType(String shapeType) {
		this.shapeType = shapeType;
	}

	@Override
	public String getType() {
		return "shape";
	}

	@Override
	public String getIcon() {
		return "015-pin-8";
	}

	@Override
	public int getIconSize() {
		return 48;
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
}