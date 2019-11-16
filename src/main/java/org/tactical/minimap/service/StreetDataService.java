package org.tactical.minimap.service;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tactical.minimap.DAO.StreetDataDAO;
import org.tactical.minimap.repository.StreetData;

@Service
public class StreetDataService {
	@Autowired
	StreetDataDAO streetDataDAO;

	@Transactional
	public void save(StreetData streetData) {
		streetDataDAO.save(streetData);
	}

	public StreetData findStreetData(String key) {

		List<StreetData> sdList = streetDataDAO.findStreetDataByName(key);

		if (sdList.size() > 0) {
			return sdList.get(0);
		}

		return null;
	}

	public List<StreetData> findStreetDataList(String streetType, String key) {

		return streetDataDAO.findStreetDataByStreetTypeAndName(streetType, key);

	}
}
