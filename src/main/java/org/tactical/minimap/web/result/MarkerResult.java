package org.tactical.minimap.web.result;

import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.util.MarkerCache;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MarkerResult {
	Long markerId;

	String status;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	Marker marker;
	
	@JsonProperty("o")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	double opacity;
	
	@JsonProperty("c")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	boolean controllable;
	
	@JsonProperty("l")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	String layer;
	
	@JsonProperty("mc")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	MarkerCache markerCache;

	public Long getMarkerId() {
		return markerId;
	}

	public void setMarkerId(Long markerId) {
		this.markerId = markerId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Marker getMarker() {
		return marker;
	}

	public void setMarker(Marker marker) {
		this.marker = marker;
	}

	public MarkerCache getMarkerCache() {
		return markerCache;
	}

	public void setMarkerCache(MarkerCache markerCache) {
		this.markerCache = markerCache;
	}

	public double getOpacity() {
		return opacity;
	}

	public void setOpacity(double opacity) {
		this.opacity = opacity;
	}

	public Boolean getControllable() {
		return controllable;
	}

	public void setControllable(Boolean controllable) {
		this.controllable = controllable;
	}

	public String getLayer() {
		return layer;
	}

	public void setLayer(String layer) {
		this.layer = layer;
	}

	public static MarkerResult makeResult(Long markerId) {
		MarkerResult mr = new MarkerResult();
		mr.setMarkerId(markerId);
		return mr;
	}

	public MarkerResult status(String status) {
		this.setStatus(status);
		return this;
	}

	public MarkerResult layer(String layer) {
		this.setLayer(layer);
		return this;
	}
	
	public MarkerResult marker(Marker m) {
		this.setMarker(m);
		return this;
	}

	public MarkerResult cache(MarkerCache mc) {
		this.setMarkerCache(mc);
		return this;
	}

	public MarkerResult opacity(double opacity) {
		this.setOpacity(opacity);
		return this;
	}

	public MarkerResult controllable(boolean controllable) {
		this.controllable = controllable;
		return this;
	}

	@Override
	public String toString() {
		return "MarkerResult [markerId=" + markerId + ", status=" + status + ", marker=" + marker + ", markerCache=" + markerCache + ", opacity=" + opacity + ", controllable=" + controllable + "]";
	}

}
