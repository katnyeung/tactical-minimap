package org.tactical.minimap.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.tactical.minimap.repository.MarkerResponse;

public interface MarkerResponseDAO<T extends MarkerResponse> extends JpaRepository<T, Long> {

	@Query(value = "SELECT GREATEST(50 - count(1),1) as markerCount FROM marker_response mr INNER JOIN marker m ON mr.marker_id = m.marker_id WHERE m.status = 'A' AND m.lat BETWEEN :fromLat AND :toLat AND m.lng BETWEEN :fromLng AND :toLng", nativeQuery = true)
	int getExpireRate(Double fromLat, Double toLat, Double fromLng, Double toLng);

}
