package org.tactical.minimap.controller;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.tactical.minimap.auth.Auth;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.service.LayerService;
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

import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("/marker")
public class MarkerRestController {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	MarkerService markerService;

	@Autowired
	LayerService layerService;

	@Autowired
	MarkerResponseService markerResponseService;

	@Autowired
	RedisService redisService;

	@Auth
	@PostMapping("/add")
	public DefaultResult addMarkerByLayer(HttpServletRequest request, HttpServletResponse response, HttpSession session, MarkerDTO markerDTO) {
		String uuid = CookieUtil.getUUID(request, response, session);
		markerDTO.setUuid(uuid);

		for (Class<? extends Marker> MarkerClass : Marker.ClassList) {
			try {
				Marker marker = MarkerClass.newInstance();

				if (marker.getType().equals(markerDTO.getType())) {
					logger.info("handing add marker request : " + markerDTO);
					Layer layer = layerService.getLayerByKey(markerDTO.getLayer());

					return createMarker(marker, markerDTO, layer, uuid);

				}
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		return DefaultResult.error("some error happen");
	}

	private DefaultResult createMarker(Marker marker, MarkerDTO markerDTO, Layer layer, String uuid) throws InstantiationException, IllegalAccessException {
		String lockedTimeInMillis = redisService.addMarkerLock(layer.getLayerKey(), uuid, marker.getAddDelay());

		if (lockedTimeInMillis == null) {
			markerService.addMarker(layer, markerDTO, marker);
			return DefaultResult.success();
		} else {
			Calendar lockedTime = Calendar.getInstance();
			lockedTime.setTimeInMillis(Long.parseLong(lockedTimeInMillis));
			lockedTime.add(Calendar.SECOND, marker.getAddDelay());

			Calendar currentTime = Calendar.getInstance();

			Double remainSecond = (lockedTime.getTimeInMillis() - currentTime.getTimeInMillis()) / 1000.0;

			if (remainSecond < 0) {

				markerService.addMarker(layer, markerDTO, marker);

				redisService.updateLock(layer.getLayerKey(), uuid, marker.getAddDelay());

				return DefaultResult.success();
			}

			return DefaultResult.error("Please wait " + remainSecond + " seconds");
		}

	}

	@GetMapping("/{layerKeys}/list")
	public DefaultResult getMarker(@PathVariable("layerKeys") String layerKeys, MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		String uuid = CookieUtil.getUUID(request, response, session);
		
		Map<String, String> layerMap = new HashMap<String, String>();

		Double lat = markerDTO.getLat();
		Double lng = markerDTO.getLng();

		Pattern pattern = Pattern.compile("([0-9a-zA-Z-_]*)\\$([a-zA-Z]*)");

		for (String layerKey : layerKeys.split(",")) {
			Matcher matcher = pattern.matcher(layerKey);

			if (matcher.find()) {
				if (matcher.groupCount() > 1) {
					layerMap.put(matcher.group(1), matcher.group(2));
				}
			}
		}
		
		List<Marker> markerList = markerService.findMultiLayerMarkers(layerMap.keySet().stream().collect(Collectors.toList()), lat, lng, ConstantsUtil.RANGE);

		markerService.addMarkerCache(markerList, uuid);

		return MarkerListResult.success(markerList);

	}

	@Auth
	@PostMapping("/move")
	public DefaultResult move(MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {

		Marker marker = markerService.findMarkerByMarkerId(markerDTO.getMarkerId());

		markerService.moveMarker(marker, markerDTO.getLat(), markerDTO.getLng());

		return DefaultResult.success();

	}

	@Auth
	@PostMapping("/pulse")
	public DefaultResult pulse(MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {

		Marker marker = markerService.findMarkerByMarkerId(markerDTO.getMarkerId());

		markerService.pulseMarker(marker);

		return DefaultResult.success();

	}

	@Auth
	@PostMapping("/copy")
	public DefaultResult copy(MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		String uuid = CookieUtil.getUUID(request, response, session);

		Marker marker = markerService.findMarkerByMarkerId(markerDTO.getMarkerId());

		try {
			markerService.copyMarkerToLayer(marker, markerDTO.getLayer(), uuid);
		} catch (InstantiationException | IllegalAccessException | JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return DefaultResult.success();

	}

	@Auth
	@PostMapping("/delete")
	public DefaultResult delete(MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {

		Marker marker = markerService.findMarkerByMarkerId(markerDTO.getMarkerId());

		markerService.deleteMarker(marker);
		redisService.deleteKey(ConstantsUtil.REDIS_MARKER_PREFIX + ":" + markerDTO.getMarkerId());

		return DefaultResult.success();

	}

	@Auth
	@PostMapping("/updateMessage")
	public DefaultResult updateMessage(MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {

		Marker marker = markerService.findMarkerByMarkerId(markerDTO.getMarkerId());

		markerService.updateMessage(marker, markerDTO.getMessage());

		return DefaultResult.success();

	}

	@PostMapping("/up")
	public DefaultResult voteUp(MarkerResponseDTO markerResponseDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		Long markerId = markerResponseDTO.getMarkerId();

		MarkerCache mc = redisService.getMarkerCacheByMarkerId(markerId);

		String uuid = CookieUtil.getUUID(request, response, session);

		Double remainSecond = vote(uuid, markerId, mc.getUpRate(), "up");

		if (remainSecond == null) {
			logger.info("up " + (mc.getUpRate() * mc.getUpVote()) + " second. #" + markerId);
			return DefaultResult.success();
		} else {
			return DefaultResult.error("Please wait " + remainSecond + " seconds");
		}
	}

	@PostMapping("/down")
	public DefaultResult voteDown(MarkerResponseDTO markerResponseDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		Long markerId = markerResponseDTO.getMarkerId();

		MarkerCache mc = redisService.getMarkerCacheByMarkerId(markerId);

		String uuid = CookieUtil.getUUID(request, response, session);

		Double remainSecond = vote(uuid, markerId, mc.getDownRate(), "down");

		if (remainSecond == null) {
			logger.info("down" + mc.getUpRate() + " second. #" + markerId);
			return DefaultResult.success();
		} else {
			return DefaultResult.error("Please wait " + remainSecond + " seconds");
		}
	}

	public Double vote(String uuid, Long markerId, int expireRate, String type) {
		Marker marker = markerService.findMarkerByMarkerId(markerId);

		String lockedTimeInMillis = redisService.addVoteLock(markerId, uuid, marker.getVoteDelay());

		if (lockedTimeInMillis == null) {

			MarkerCache mc = redisService.getMarkerCacheByMarkerId(markerId);

			if (marker.getLayer().getPassword() != null && !marker.getLayer().getPassword().equals("")) {
				expireRate = expireRate * ConstantsUtil.LOGGED_MARKER_VOTE_MULTIPLER;
			}

			if (type.equals("up")) {
				mc.setUpVote(mc.getUpVote() + 1);
				// make it curve
				mc.setExpire(mc.getExpire() + (expireRate * mc.getUpVote()));
				markerResponseService.upVote(marker, uuid);

				marker.setLastupdatedate(Calendar.getInstance().getTime());
				markerService.update(marker);
			} else if (type.equals("down")) {
				mc.setDownVote(mc.getDownVote() + 1);
				mc.setExpire(mc.getExpire() - expireRate);
				markerResponseService.downVote(marker, uuid);
			}
			redisService.saveMarkerCache(mc);

			return null;
		} else {
			Calendar currentTime = Calendar.getInstance();
			Calendar lockedTime = Calendar.getInstance();
			lockedTime.setTimeInMillis(Long.parseLong(lockedTimeInMillis));
			lockedTime.add(Calendar.SECOND, marker.getVoteDelay());

			return ((lockedTime.getTimeInMillis() - currentTime.getTimeInMillis()) / 1000.0);
		}
	}
}
