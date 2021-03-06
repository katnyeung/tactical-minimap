package org.tactical.minimap.DAO;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.util.ConstantsUtil;

public interface MarkerDAO<T extends Marker> extends JpaRepository<T, Long> {

	@Query("SELECT m FROM Marker m INNER JOIN m.layer l WHERE l.layerKey IN :layerKeys AND m.status = '" + ConstantsUtil.MARKER_STATUS_ACTIVE + "' AND m.lat BETWEEN :fromLat AND :toLat AND m.lng BETWEEN :fromLng AND :toLng AND UNIX_TIMESTAMP(now()) < (UNIX_TIMESTAMP(m.createdate) + m.expire) ORDER BY m.createdate DESC")
	public List<T> findActiveMarkersByLatLng(@Param("layerKeys") List<String> layerKeys, @Param("fromLat") Double fromLat, @Param("fromLng") Double fromLng, @Param("toLat") Double toLat, @Param("toLng") Double toLng);
	
	@Query("SELECT m FROM Marker m INNER JOIN m.layer l WHERE l.layerKey IN :layerKeys AND m.status = '" + ConstantsUtil.MARKER_STATUS_ACTIVE + "' AND m.lat BETWEEN :fromLat AND :toLat AND m.lng BETWEEN :fromLng AND :toLng AND (UNIX_TIMESTAMP(m.createdate) * 1000) > :timestamp ORDER BY m.createdate ASC")
	public List<T> findLatestActiveMarkersByLatLng(@Param("layerKeys") List<String> layerKeys, @Param("fromLat") Double fromLat, @Param("fromLng") Double fromLng, @Param("toLat") Double toLat, @Param("toLng") Double toLng , @Param("timestamp") Long timestamp);

	@Query("SELECT m FROM Marker m INNER JOIN m.layer l WHERE l.layerKey IN :layerKeys AND m.status = '" + ConstantsUtil.MARKER_STATUS_ACTIVE + "' AND m.markerId IN :markerIdList ORDER BY m.createdate ASC")
	public List<T> findActiveMarkersByMarkerIds(@Param("layerKeys") List<String> layerKeys, @Param("markerIdList") List<Long> markerIdList);

	@Query("SELECT m FROM Marker m INNER JOIN m.layer l WHERE m.lastupdatedate < :lastMarkerDate AND l.layerKey <> 'public' AND l.layerKey IN :layerKeys AND m.status = '" + ConstantsUtil.MARKER_STATUS_DEACTIVED + "' AND m.lat BETWEEN :fromLat AND :toLat AND m.lng BETWEEN :fromLng AND :toLng ORDER BY m.lastupdatedate DESC")
	public List<T> findDeactiveMarkersByLatLng(Pageable pageable, @Param("lastMarkerDate") Date lastMarkerDate, @Param("layerKeys") List<String> layerKeys, @Param("fromLat") Double fromLat, @Param("fromLng") Double fromLng, @Param("toLat") Double toLat, @Param("toLng") Double toLng);

	@Query("SELECT m FROM Marker m INNER JOIN m.layer l WHERE l.layerKey = :layerKey AND m.status = '" + ConstantsUtil.MARKER_STATUS_ACTIVE + "' AND m.lat BETWEEN :fromLat AND :toLat AND m.lng BETWEEN :fromLng AND :toLng")
	public List<T> findByLatLngLayer(@Param("layerKey") String layerKey, @Param("fromLat") Double fromLat, @Param("fromLng") Double fromLng, @Param("toLat") Double toLat, @Param("toLng") Double toLng);

	@Query("SELECT m FROM Marker m WHERE m.status = '" + ConstantsUtil.MARKER_STATUS_ACTIVE + "' AND m.markerId NOT IN :markerIdList")
	public List<Marker> findActiveMarkersNotInCache(@Param("markerIdList") List<Long> markerIdList);

	@Query("SELECT m FROM Marker m WHERE m.status = '" + ConstantsUtil.MARKER_STATUS_ACTIVE + "'")
	public List<Marker> findActiveMarkers();

	@Modifying
	@Query("UPDATE Marker m SET m.status = :status, upVote = :upVote, downVote = :downVote WHERE m.markerId = :markerId")
	public void updateStatusUpDown(@Param("markerId") Long markerId, @Param("status") String status, @Param("upVote") int upVote, @Param("downVote") int downVote);

	@Query(value = "SELECT COUNT(1) AS markerCount FROM marker m INNER JOIN layer l ON (m.layer_id = l.layer_id) WHERE l.layer_key = :layerKey AND m.status = 'A' AND m.lat BETWEEN :fromLat AND :toLat AND m.lng BETWEEN :fromLng AND :toLng", nativeQuery = true)
	int getMarkerCountInRange(@Param("layerKey") String layerKey, @Param("fromLat") Double fromLat, @Param("toLat") Double toLat, @Param("fromLng") Double fromLng, @Param("toLng") Double toLng);

}
