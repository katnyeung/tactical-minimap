package org.tactical.minimap.DAO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tactical.minimap.repository.marker.Marker;

public interface MarkerDAO<T extends Marker> extends JpaRepository<T, Long> {

	@Query("SELECT m FROM Marker m")
	public List<T> findAllMarker();

	@Query("SELECT m FROM Marker m WHERE lat BETWEEN :fromLat AND :toLat AND lng BETWEEN :fromLng AND :toLng")
	public List<T> findAllByLatLng(@Param("fromLat") Double fromLat, @Param("fromLng") Double fromLng, @Param("toLat") Double toLat, @Param("toLng") Double toLng);

}
