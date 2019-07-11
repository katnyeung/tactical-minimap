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
	@Transient
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@JsonProperty("shapeList")
	@OneToMany(mappedBy = "shapeMarker", cascade = CascadeType.ALL)
	List<ShapeMarkerDetail> shapeMarkerDetailList;

	@NotNull
	String shapeType;

	@NotNull
	int level;

	@Override
	public Marker fill(MarkerDTO markerDTO) {
		try {
			ShapeMarker marker = new ShapeMarker();
			marker.setLat(markerDTO.getLat());
			marker.setLng(markerDTO.getLng());
			marker.setMessage(markerDTO.getMessage());
			marker.setExpire(getMarkerExpire());
			marker.setStatus(ConstantsUtil.MARKER_STATUS_ACTIVE);
			marker.setUuid(markerDTO.getUuid());

			marker.setShapeType(markerDTO.getShapeType());

			List<ShapeMarkerDetail> shapeMarkerDetailList = new ArrayList<ShapeMarkerDetail>();
			logger.info("shape list : " + markerDTO.getShapeList());
			ObjectMapper om = new ObjectMapper();

			List<LinkedHashMap<String, Double>> shapeList;

			shapeList = om.readValue(markerDTO.getShapeList(), List.class);
			
			for (LinkedHashMap<String, Double> shapeMap : shapeList) {
				ShapeMarkerDetail smd = new ShapeMarkerDetail();
				smd.setLat(shapeMap.get("lat"));
				smd.setLng(shapeMap.get("lng"));
				smd.setShapeMarker(marker);
				shapeMarkerDetailList.add(smd);
			}

			marker.setShapeMarkerDetailList(shapeMarkerDetailList);

			return marker;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

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
		return "016-pin-7.png";
	}

	@Override
	public int getIconSize() {
		return 34;
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
		return 600;
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