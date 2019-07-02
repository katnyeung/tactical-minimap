package org.tactical.minimap.web.DTO;

public class LayerDTO {

	Long layerId;
	String layerKey;

	String password;

	public Long getLayerId() {
		return layerId;
	}

	public void setLayerId(Long layerId) {
		this.layerId = layerId;
	}

	public String getLayerKey() {
		return layerKey;
	}

	public void setLayerKey(String layerKey) {
		this.layerKey = layerKey;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
