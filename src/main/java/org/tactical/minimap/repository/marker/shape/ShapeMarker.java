package org.tactical.minimap.repository.marker.shape;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

@Entity
@DiscriminatorValue(value = "shape")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ShapeMarker extends Marker {
	
	String icon = "point.png";
	int iconSize = 36;
	
	@Override
	public String getIcon() {
		return this.icon;
	}
	
	@Override
	public int getIconSize() {
		return this.iconSize;
	}
	
	@Transient
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@JsonProperty("shapeList")
	@OneToMany(mappedBy = "shapeMarker", cascade = CascadeType.ALL)
	List<ShapeMarkerDetail> shapeMarkerDetailList;

	@NotNull
	String shapeType;

	@NotNull
	String color;
	
	@Override
	public Marker fill(MarkerDTO markerDTO) {
		try {
			this.setLat(markerDTO.getLat());
			this.setLng(markerDTO.getLng());
			this.setMessage(markerDTO.getMessage());
			this.setExpire(getMarkerExpire());
			this.setStatus(ConstantsUtil.MARKER_STATUS_ACTIVE);
			this.setUuid(markerDTO.getUuid());

			this.setShapeType(markerDTO.getShapeType());
			this.setColor(markerDTO.getColor());
			
			List<ShapeMarkerDetail> shapeMarkerDetailList = new ArrayList<ShapeMarkerDetail>();
			ObjectMapper om = new ObjectMapper();

			List<LinkedHashMap<String, Double>> shapeList;

			shapeList = om.readValue(markerDTO.getShapeList(), List.class);
			
			for (LinkedHashMap<String, Double> shapeMap : shapeList) {
				ShapeMarkerDetail smd = new ShapeMarkerDetail();
				smd.setLat(shapeMap.get("lat"));
				smd.setLng(shapeMap.get("lng"));
				
				if(shapeMap.get("group") != null) {
					smd.setSubGroup(shapeMap.get("group").intValue());
				}
				
				smd.setShapeMarker(this);
				shapeMarkerDetailList.add(smd);
			}

			this.setShapeMarkerDetailList(shapeMarkerDetailList);

			this.setHour(markerDTO.getHour());
			this.setMinute(markerDTO.getMinute());
			
			return this;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

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

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public void setIconSize(int iconSize) {
		this.iconSize = iconSize;
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
		return 3;
	}

	@Override
	public int getVoteDelay() {
		return 15;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}
	
	@Override
	public String getDescription() {
		return "圖標";
	}
	
	@Override
	public int getPulseRate() {
		return 8;
	}
}