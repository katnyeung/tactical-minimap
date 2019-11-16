package org.tactical.minimap.DAO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tactical.minimap.repository.StreetData;

public interface StreetDataDAO extends JpaRepository<StreetData, Long> {

	@Query("SELECT sd FROM StreetData sd WHERE sd.streetName = :streetName")
	public List<StreetData> findStreetDataByName(@Param("streetName") String streetName);

	@Query("SELECT sd FROM StreetData sd WHERE sd.streetType = :streetType AND sd.streetName = :streetName")
	public List<StreetData> findStreetDataByStreetTypeAndName(@Param("streetType") String streetType, @Param("streetName") String streetName);
}
