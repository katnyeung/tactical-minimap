package org.tactical.minimap.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.web.DTO.MarkerWebSocketDTO;
import org.tactical.minimap.web.result.DefaultResult;
import org.tactical.minimap.web.result.MarkerResult;
import org.tactical.minimap.web.result.MarkerResultListResult;

@Controller
public class MarkerEventController {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;

	@Autowired
	MarkerService markerService;

	@Autowired
	RedisService redisService;

	@MessageMapping("/update/{uuid}")
	public void update(@DestinationVariable("uuid") String uuid, MarkerWebSocketDTO markerWSDTO) throws Exception {
		DefaultResult result = triggerUpdate(markerWSDTO);

		redisService.saveLoggedUser(uuid, markerWSDTO);
		
		simpMessagingTemplate.convertAndSend("/markers/list/" + uuid, result);
	}

	public DefaultResult triggerUpdate(MarkerWebSocketDTO markerWSDTO) {

		List<MarkerResult> markerResultList = markerService.processMarkers(markerWSDTO);

		return MarkerResultListResult.success(markerResultList);

	}

}