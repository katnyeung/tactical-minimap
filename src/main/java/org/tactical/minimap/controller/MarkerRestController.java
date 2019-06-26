package org.tactical.minimap.controller;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	@PostMapping("/{layer}/add")
	public DefaultResult addMarker(@PathVariable("layer") String layer, HttpServletRequest request, HttpServletResponse response, HttpSession session, MarkerDTO markerDTO) {
		String uuid = CookieUtil.getUUID(request, response, session);
		markerDTO.setUuid(uuid);

		for (Class<? extends Marker> MarkerClass : Marker.ClassList) {
			try {
				Marker marker = MarkerClass.newInstance();

				if (marker.getType().equals(markerDTO.getType())) {
					String lockedTimeString = redisService.addLock(layer, uuid);
					if (lockedTimeString != null) {

						Calendar lockedTime = Calendar.getInstance();
						lockedTime.setTimeInMillis(Long.parseLong(lockedTimeString));
						lockedTime.add(Calendar.SECOND, marker.getDelay());

						Calendar currentTime = Calendar.getInstance();

						Long diffInSecond = currentTime.getTimeInMillis() - lockedTime.getTimeInMillis();

						if (diffInSecond > 0) {
							markerService.addMarker(layer, markerDTO, marker);
							redisService.updateLock(layer, uuid);
						} else {
							return DefaultResult.error("Please wait " + (diffInSecond / 1000.0 * -1) + " seconds");
						}

					} else {
						markerService.addMarker(layer, markerDTO, marker);
					}

				}

			} catch (InstantiationException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return DefaultResult.success();
	}

	@GetMapping("/{layer}/listAll")
	public DefaultResult getAllMarker(@PathVariable("layer") String layer) {

		List<Marker> marketList = markerService.findAllMarkers(layer);

		return MarkerListResult.success(marketList);
	}

	@GetMapping("/{layer}/list")
	public DefaultResult getMarker(@PathVariable("layer") String layer, MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		String uuid = CookieUtil.getUUID(request, response, session);

		Double lat = markerDTO.getLat();
		Double lng = markerDTO.getLng();

		List<Marker> markerList = markerService.findMarkers(layer, lat, lng, ConstantsUtil.RANGE);

		markerService.addMarkerCache(markerList, uuid);

		return MarkerListResult.success(markerList);

	}

	@PostMapping("/move")
	public DefaultResult move(MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {

		String uuid = CookieUtil.getUUID(request, response, session);

		Marker marker = markerService.findMarkerByMarkerId(markerDTO.getMarkerId());

		if (marker.getUuid().equals(uuid)) {
			markerService.moveMarker(marker, markerDTO.getLat(), markerDTO.getLng());
			return DefaultResult.success();
		} else {
			return DefaultResult.error("Please wait " + ConstantsUtil.REDIS_MARKER_RESPONSE_INTERVAL_IN_SECOND + " seconds");
		}

	}

	@PostMapping("/delete")
	public DefaultResult delete(MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {

		String uuid = CookieUtil.getUUID(request, response, session);

		Marker marker = markerService.findMarkerByMarkerId(markerDTO.getMarkerId());

		if (marker.getUuid().equals(uuid)) {
			markerService.deleteMarker(marker);
			redisService.deleteKey(ConstantsUtil.REDIS_MARKER_PREFIX + ":" + markerDTO.getMarkerId());
			return DefaultResult.success();
		} else {
			return DefaultResult.error("Please wait " + ConstantsUtil.REDIS_MARKER_RESPONSE_INTERVAL_IN_SECOND + " seconds");
		}

	}

	@PostMapping("/updateMessage")
	public DefaultResult updateMessage(MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {

		String uuid = CookieUtil.getUUID(request, response, session);

		Marker marker = markerService.findMarkerByMarkerId(markerDTO.getMarkerId());

		if (marker.getUuid().equals(uuid)) {
			markerService.updateMessage(marker, markerDTO.getMessage());

			return DefaultResult.success();
		} else {
			return DefaultResult.error("Please wait " + ConstantsUtil.REDIS_MARKER_RESPONSE_INTERVAL_IN_SECOND + " seconds");
		}

	}

	@PostMapping("/up")
	public DefaultResult voteUp(MarkerResponseDTO markerResponseDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		Long markerId = markerResponseDTO.getMarkerId();

		MarkerCache mc = redisService.getMarkerCacheByMarkerId(markerId);

		String uuid = CookieUtil.getUUID(request, response, session);

		String lockedTimeInMillis = vote(uuid, markerId, mc.getUpRate(), "up");

		if (lockedTimeInMillis == null) {
			logger.info("up " + (mc.getUpRate() * mc.getUpVote()) + " second. #" + markerId);
			return DefaultResult.success();
		} else {
			Calendar currentTime = Calendar.getInstance();
			Calendar lockedTime = Calendar.getInstance();
			lockedTime.setTimeInMillis(Long.parseLong(lockedTimeInMillis));
			lockedTime.add(Calendar.SECOND, ConstantsUtil.REDIS_MARKER_RESPONSE_INTERVAL_IN_SECOND);

			return DefaultResult.error("Please wait " + ((lockedTime.getTimeInMillis() - currentTime.getTimeInMillis()) / 1000.0) + " seconds");
		}
	}

	@PostMapping("/down")
	public DefaultResult voteDown(MarkerResponseDTO markerResponseDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		Long markerId = markerResponseDTO.getMarkerId();

		MarkerCache mc = redisService.getMarkerCacheByMarkerId(markerId);

		String uuid = CookieUtil.getUUID(request, response, session);

		String lockedTimeInMillis = vote(uuid, markerId, mc.getDownRate(), "down");

		if (lockedTimeInMillis == null) {
			logger.info("down" + mc.getUpRate() + " second. #" + markerId);
			return DefaultResult.success();
		} else {
			Calendar currentTime = Calendar.getInstance();
			Calendar lockedTime = Calendar.getInstance();
			lockedTime.setTimeInMillis(Long.parseLong(lockedTimeInMillis));
			lockedTime.add(Calendar.SECOND, ConstantsUtil.REDIS_MARKER_RESPONSE_INTERVAL_IN_SECOND);

			return DefaultResult.error("Please wait " + ((lockedTime.getTimeInMillis() - currentTime.getTimeInMillis()) / 1000.0) + " seconds");
		}
	}

	public String vote(String uuid, Long markerId, int expireRate, String type) {
		String lockedTimeInMillis = redisService.addVoteLock(markerId, uuid, ConstantsUtil.REDIS_MARKER_RESPONSE_INTERVAL_IN_SECOND);

		if (lockedTimeInMillis == null) {
			Marker marker = markerService.findMarkerByMarkerId(markerId);

			MarkerCache mc = redisService.getMarkerCacheByMarkerId(markerId);
			if (type.equals("up")) {
				mc.setUpVote(mc.getUpVote() + 1);
				// make it curve
				mc.setExpire(mc.getExpire() + (expireRate * mc.getUpVote()));
				markerResponseService.upVote(marker, uuid);

			} else if (type.equals("down")) {
				mc.setDownVote(mc.getDownVote() + 1);
				mc.setExpire(mc.getExpire() - expireRate);
				markerResponseService.downVote(marker, uuid);
			}

			redisService.saveMarkerCache(mc);

			return null;
		} else {
			return lockedTimeInMillis;
		}
	}
}
