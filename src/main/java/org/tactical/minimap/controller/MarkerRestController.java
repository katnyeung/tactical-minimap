package org.tactical.minimap.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.service.MarkerResponseService;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.util.CookieUtil;
import org.tactical.minimap.util.MarkerCache;
import org.tactical.minimap.web.DTO.MarkerDTO;
import org.tactical.minimap.web.DTO.MarkerResponseDTO;
import org.tactical.minimap.web.result.DefaultResult;
import org.tactical.minimap.web.result.MarkerListResult;

@RestController
@RequestMapping("/marker")
public class MarkerRestController {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	MarkerService markerService;

	@Autowired
	MarkerResponseService markerResponseService;

	@Autowired
	RedisService redisService;

	@PostMapping("/add")
	public DefaultResult addMarker(HttpServletRequest request, HttpServletResponse response, HttpSession session,
			MarkerDTO markerDTO) {
		String uuid = CookieUtil.getUUID(request, response, session);
		markerDTO.setUuid(uuid);
		logger.info("uuid : " + uuid);
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

		List<Marker> markerList = markerService.findMarkers(lat, lng, ConstantsUtil.RANGE);

		markerList.stream().forEach(m -> m.setMarkerCache(redisService.getMarkerCacheByMarkerId(m.getMarkerId())));

		return MarkerListResult.success(markerList);

	}

	@GetMapping("/up")
	public DefaultResult voteUp(MarkerResponseDTO markerResponseDTO, HttpServletRequest request,
			HttpServletResponse response, HttpSession session) {
		Long markerId = markerResponseDTO.getMarkerId();

		int expireRate = markerResponseService.getExpireRate(markerResponseDTO.getLat(), markerResponseDTO.getLng(),
				ConstantsUtil.RANGE);

		String uuid = CookieUtil.getUUID(request, response, session);

		if (vote(uuid, markerId, expireRate, "up")) {
			return DefaultResult.success();
		} else {
			return DefaultResult.error("Please Wait " + ConstantsUtil.REDIS_MARKER_INTERVAL_IN_SECOND + " seconds");
		}

	}

	@GetMapping("/down")
	public DefaultResult voteDown(MarkerResponseDTO markerResponseDTO, HttpServletRequest request,
			HttpServletResponse response, HttpSession session) {
		Long markerId = markerResponseDTO.getMarkerId();

		int expireRate = markerResponseService.getExpireRate(markerResponseDTO.getLat(), markerResponseDTO.getLng(),
				ConstantsUtil.RANGE);

		String uuid = CookieUtil.getUUID(request, response, session);

		if (vote(uuid, markerId, expireRate, "down")) {
			return DefaultResult.success();
		} else {
			return DefaultResult.error("Please Wait " + ConstantsUtil.REDIS_MARKER_INTERVAL_IN_SECOND + " seconds");
		}

	}

	public boolean vote(String uuid, Long markerId, int expireRate, String type) {
		if (redisService.voteLock(markerId, uuid, ConstantsUtil.REDIS_MARKER_INTERVAL_IN_SECOND)) {
			Marker marker = markerService.findMarkerByMarkerId(markerId);

			MarkerCache mc = redisService.getMarkerCacheByMarkerId(markerId);
			if (type.equals("up")) {
				mc.setUpVote(mc.getUpVote() + 1);
				mc.setExpire(mc.getExpire() + expireRate);
				markerResponseService.upVote(marker, uuid);

			} else if (type.equals("down")) {
				mc.setDownVote(mc.getDownVote() + 1);
				mc.setExpire(mc.getExpire() + expireRate);
				markerResponseService.downVote(marker, uuid);
			}

			redisService.saveMarkerCache(mc);

			return true;
		} else {
			return false;
		}
	}
}
