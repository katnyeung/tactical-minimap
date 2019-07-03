package org.tactical.minimap.DAO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.util.ConstantsUtil;

public interface MarkerDAO<T extends Marker> extends JpaRepository<T, Long> {

	@Query("SELECT m FROM Marker m INNER JOIN Layer l WHERE l.layerKey IN (:layerkeys) AND m.status = '" + ConstantsUtil.MARKER_STATUS_ACTIVE + "' AND m.lat BETWEEN :fromLat AND :toLat AND m.lng BETWEEN :fromLng AND :toLng")
	public List<T> findAllByLatLng(@Param("layerkeys") List<String> layerkeys, @Param("fromLat") Double fromLat, @Param("fromLng") Double fromLng, @Param("toLat") Double toLat, @Param("toLng") Double toLng);
	
	@Query("SELECT m FROM Marker m INNER JOIN Layer l WHERE l.layerKey = :layerKey AND m.status = '" + ConstantsUtil.MARKER_STATUS_ACTIVE + "' AND m.lat BETWEEN :fromLat AND :toLat AND m.lng BETWEEN :fromLng AND :toLng")
	public List<T> findByLatLngLayer(@Param("layerKey") String layerKey, @Param("fromLat") Double fromLat, @Param("fromLng") Double fromLng, @Param("toLat") Double toLat, @Param("toLng") Double toLng);

	@Query("SELECT m FROM Marker m WHERE m.status = '" + ConstantsUtil.MARKER_STATUS_ACTIVE + "' AND m.markerId NOT IN :markerIdList")
	public List<Marker> findActiveMarkersNotInCache(@Param("markerIdList") List<Long> markerIdList);

	@Query("SELECT m FROM Marker m WHERE m.status = '" + ConstantsUtil.MARKER_STATUS_ACTIVE + "'")
	public List<Marker> findActiveMarkers();

	@Modifying
	@Query("UPDATE Marker m SET m.status = :status, upVote = :upVote, downVote = :downVote WHERE m.markerId = :markerId")
	public void updateStatusUpDown(@Param("markerId") Long markerId, @Param("status") String status, @Param("upVote") int upVote, @Param("downVote") int downVote);
	

	@Query(value = "SELECT COUNT(1) AS markerCount FROM marker m INNER JOIN layer l ON (m.layer_id = l.layer_id) WHERE l.layerKey = :layerKey AND m.status = 'A' AND m.lat BETWEEN :fromLat AND :toLat AND m.lng BETWEEN :fromLng AND :toLng", nativeQuery = true)
	int getMarkerCountInRange(@Param("layerKey") String layerKey, @Param("fromLat") Double fromLat, @Param("toLat") Double toLat, @Param("fromLng") Double fromLng, @Param("toLng") Double toLng);

}
