package org.tactical.minimap.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tactical.minimap.repository.MarkerResponse;

public interface MarkerResponseDAO<T extends MarkerResponse> extends JpaRepository<T, Long> {

	@Query(value = "SELECT GREATEST(50 - count(1),1) as markerCount FROM marker m WHERE m.layer = :layer AND m.status = 'A' AND m.lat BETWEEN :fromLat AND :toLat AND m.lng BETWEEN :fromLng AND :toLng", nativeQuery = true)
	int getExpireRate(@Param("layer") String layer, @Param("fromLat") Double fromLat, @Param("toLat") Double toLat, @Param("fromLng") Double fromLng, @Param("toLng") Double toLng);

}
