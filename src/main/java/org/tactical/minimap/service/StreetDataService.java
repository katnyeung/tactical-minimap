package org.tactical.minimap.service;

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

}
