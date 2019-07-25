package org.tactical.minimap.web.DTO;

public class LayerDTO {

	Long layerId;
	String layerKey;

	String password;

	Integer expireMultiplier;

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

	public Integer getExpireMultiplier() {
		return expireMultiplier;
	}

	public void setExpireMultiplier(Integer expireMultiplier) {
		this.expireMultiplier = expireMultiplier;
	}

}
