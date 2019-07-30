package org.tactical.minimap.DAO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.util.ConstantsUtil;

public interface LayerDAO extends JpaRepository<Layer, Long> {
	
	@Query("SELECT l FROM Layer l WHERE l.layerKey = :layerKey")
	List<Layer> findLayersByKey(@Param("layerKey") String layerKey);

	@Query("SELECT l FROM Layer l WHERE l.status = '" + ConstantsUtil.LAYER_STATUS_ACTIVE + "' ")
	List<Layer> findActiveLayers();

	@Query("SELECT l FROM Layer l WHERE l.status = '" + ConstantsUtil.LAYER_STATUS_ACTIVE + "' AND l.layerKey IN :layerKeyList")
	List<Layer> findActiveLayersByLayerkeys(@Param("layerKeyList") List<String> layerKeyList);
	
}
