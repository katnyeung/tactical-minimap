package org.tactical.minimap.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tactical.minimap.DAO.LayerDAO;
import org.tactical.minimap.repository.Layer;

@Service
public class LayerService {
	@Autowired
	LayerDAO layerDAO;

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
}
