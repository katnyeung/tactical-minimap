package org.tactical.minimap.DAO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.util.ConstantsUtil;

public interface MarkerDAO<T extends Marker> extends JpaRepository<T, Long> {

	@Query("SELECT m FROM Marker m")
	public List<T> findAllMarker();

	@Query("SELECT m FROM Marker m WHERE m.status = '" + ConstantsUtil.MARKER_STATUS_ACTIVE + "' AND m.lat BETWEEN :fromLat AND :toLat AND m.lng BETWEEN :fromLng AND :toLng")
	public List<T> findAllByLatLng(@Param("fromLat") Double fromLat, @Param("fromLng") Double fromLng, @Param("toLat") Double toLat, @Param("toLng") Double toLng);

	@Query("SELECT m FROM Marker m WHERE m.status = '" + ConstantsUtil.MARKER_STATUS_ACTIVE + "' AND m.markerId NOT IN :markerIdList")
	public List<Marker> findActiveMarkersNotInCache(@Param("markerIdList") List<Long> markerIdList);

	@Query("SELECT m FROM Marker m WHERE m.status = '" + ConstantsUtil.MARKER_STATUS_ACTIVE + "'")
	public List<Marker> findActiveMarkers();

	@Modifying
	@Query("UPDATE Marker m SET m.status = :status WHERE m.markerId = :markerId")
	public void updateStatus(@Param("markerId") Long markerId, @Param("status") String status);
}
