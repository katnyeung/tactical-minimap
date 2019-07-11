package org.tactical.minimap.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tactical.minimap.DAO.LayerDAO;
import org.tactical.minimap.repository.Layer;

@Service
public class LayerService {
	@Autowired
	LayerDAO layerDAO;

	@Autowired
	RedisService redisService;

	@Autowired
	LayerService layerService;

	public Layer getLayerByKey(String key) {
		List<Layer> layerList = layerDAO.findLayersByKey(key);

		return !layerList.isEmpty() ? layerList.get(0) : null;
	}

	public List<Layer> findActiveLayers() {
		return layerDAO.findActiveLayers();

	}

	public void save(Layer layer) {
		layerDAO.save(layer);
	}
	
	public List<Layer> getActiveLayers(){
		return layerService.findActiveLayers();
	}
	
	public Set<String> getLoggedLayers(String uuid) {
		Set<String> loggedLayers = redisService.getLoggedLayers(uuid);
		List<Layer> validLayers = getActiveLayers();
		
		for (Layer layer : validLayers) {
			if (layer.getPassword() == null || (layer.getPassword() != null && (layer.getPassword().equals("-1") || layer.getPassword().contentEquals("")))) {
				loggedLayers.add(layer.getLayerKey());
			}
		}

		return loggedLayers;
	}
}
