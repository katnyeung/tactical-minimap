package org.tactical.minimap.DAO;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tactical.minimap.repository.TrafficStat;

public interface TrafficStatDAO extends JpaRepository<TrafficStat, Long> {

	@Query("SELECT ts FROM TrafficStat ts WHERE ts.id = :id AND ts.createdate BETWEEN :dateFrom AND :dateTo")
	public List<TrafficStat> findTrafficStatById(@Param("id") String id, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo);

}
