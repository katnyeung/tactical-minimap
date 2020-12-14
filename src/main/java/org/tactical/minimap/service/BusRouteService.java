package org.tactical.minimap.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tactical.minimap.DAO.BusRouteDAO;
import org.tactical.minimap.repository.BusRoute;

@Service
public class BusRouteService {
	@Autowired
	BusRouteDAO busRouteDAO;

	public BusRoute getBusRoute(String route, int stop, int minutes) {
		List<BusRoute> busRouteList = busRouteDAO.findByMinutes(route, stop, minutes);
		
		if (busRouteList.size() > 0) {
			return busRouteList.get(0);
		} else {
			return null;
		}
	}
}
