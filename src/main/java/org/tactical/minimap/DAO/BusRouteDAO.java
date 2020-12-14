package org.tactical.minimap.DAO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tactical.minimap.repository.BusRoute;

public interface BusRouteDAO extends JpaRepository<BusRoute, Long> {

	@Query("SELECT br FROM BusRoute br WHERE br.route = :route and br.stop = :stop and br.minutes = :minutes")
	List<BusRoute> findByMinutes(@Param("route") String route, @Param("stop") int stop, @Param("minutes") int minutes);

}
