package org.tactical.minimap.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.DTO.MarkerDTO;
import org.tactical.minimap.web.result.DefaultResult;
import org.tactical.minimap.web.result.MarkerListResult;

@RestController
@RequestMapping("/marker")
public class MarkerRestController {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	MarkerService markerService;

	@PostMapping("/add")
	public DefaultResult addMarker(MarkerDTO markerDTO) {
		markerService.addMarker(markerDTO);

		return DefaultResult.success();
	}

	@GetMapping("/listAll")
	public DefaultResult getAllMarker() {

		List<Marker> marketList = markerService.findAllMarkers();

		return MarkerListResult.success(marketList);
	}

	@GetMapping("/list")
	public DefaultResult getMarker(MarkerDTO markerDTO) {
		Double lat = markerDTO.getLat();
		Double lng = markerDTO.getLng();

		List<Marker> marketList = markerService.findMarkers(lat, lng, ConstantsUtil.RANGE);

		return MarkerListResult.success(marketList);

	}
}
