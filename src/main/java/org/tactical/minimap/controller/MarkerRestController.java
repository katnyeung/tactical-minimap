package org.tactical.minimap.controller;

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
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.tactical.minimap.service.speech.SpeechService;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.util.CookieUtil;
import org.tactical.minimap.util.MarkerCache;
import org.tactical.minimap.web.DTO.MarkerDTO;
import org.tactical.minimap.web.DTO.MarkerResponseDTO;
import org.tactical.minimap.web.DTO.MarkerSpeechDTO;
import org.tactical.minimap.web.result.DefaultResult;
import org.tactical.minimap.web.result.MarkerResult;
import org.tactical.minimap.web.result.MarkerResultListResult;
import org.tactical.minimap.web.result.StringListResult;

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

	@Autowired
	@Qualifier("EkhoSpeechService")
	SpeechService speechService;

	@Auth
	@PostMapping("/add")
	public DefaultResult addMarkerByLayer(HttpServletRequest request, HttpServletResponse response, HttpSession session, MarkerDTO markerDTO) {
		String uuid = CookieUtil.getUUID(request, response, session);
		markerDTO.setUuid(uuid);

		for (Class<? extends Marker> MarkerClass : Marker.ClassList) {
			try {
				Marker marker = MarkerClass.newInstance();

				if (marker.getType().equals(markerDTO.getType())) {
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
		int addMarkerDelay;

		if (layer.getPassword() != null && !layer.getPassword().equals("")) {
			addMarkerDelay = marker.getAddDelay() / ConstantsUtil.LOGGED_MARKER_VOTE_MULTIPLER;
		} else {
			addMarkerDelay = marker.getAddDelay();
		}

		String lockedTimeInMillis = redisService.addMarkerLock(layer.getLayerKey(), uuid, addMarkerDelay);

		if (lockedTimeInMillis == null) {
			Marker result = markerService.addMarker(layer, markerDTO, marker);
			if (result != null) {

				markerService.broadcastUpdateToAllLoggedUser();

				return DefaultResult.success();
			} else {
				return DefaultResult.error("please fill in required content");
			}
		} else {
			Calendar lockedTime = Calendar.getInstance();
			lockedTime.setTimeInMillis(Long.parseLong(lockedTimeInMillis));
			lockedTime.add(Calendar.SECOND, marker.getAddDelay());

			Calendar currentTime = Calendar.getInstance();

			Double remainSecond = (lockedTime.getTimeInMillis() - currentTime.getTimeInMillis()) / 1000.0;

			if (remainSecond < 0) {

				Marker result = markerService.addMarker(layer, markerDTO, marker);

				if (result == null) {
					return DefaultResult.error("please fill in required content");
				}

				redisService.updateLock(layer.getLayerKey(), uuid, marker.getAddDelay());

				markerService.broadcastUpdateToAllLoggedUser();

				return DefaultResult.success();
			}

			return DefaultResult.error("Please wait " + remainSecond + " seconds");
		}

	}

	@GetMapping("/{layerKeys}/list")
	public DefaultResult getMarkerResult(@PathVariable("layerKeys") String layerKeys, MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {
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
		logger.info("marker DTO : list : " + markerDTO.getMarkerIdList());
		List<MarkerResult> markerResultList = markerService.findMultiLayerMarkersResponse(uuid, markerDTO.getMarkerIdList(), layerMap.keySet().stream().collect(Collectors.toList()), lat, lng, ConstantsUtil.RANGE);

		return MarkerResultListResult.success(markerResultList);
	}

	@PostMapping("/{layerKeys}/speech")
	public DefaultResult speech(@PathVariable("layerKeys") String layerKeys, MarkerSpeechDTO msDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		Map<String, String> layerMap = new HashMap<String, String>();

		Pattern pattern = Pattern.compile("([0-9a-zA-Z-_]*)\\$([a-zA-Z]*)");

		for (String layerKey : layerKeys.split(",")) {
			Matcher matcher = pattern.matcher(layerKey);

			if (matcher.find()) {
				if (matcher.groupCount() > 1) {
					layerMap.put(matcher.group(1), matcher.group(2));
				}
			}
		}

		StringListResult slr = speechService.getSpeechFromCoord(layerMap.keySet().stream().collect(Collectors.toList()), msDTO.getFromLat(), msDTO.getFromLng(), msDTO.getToLat(), msDTO.getToLng(), msDTO.getTimestamp());

		slr.setStatus("success");
		
		return slr;
	}

	@Auth
	@PostMapping("/move")
	public DefaultResult move(MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {

		Marker marker = markerService.findMarkerByMarkerId(markerDTO.getMarkerId());

		markerService.moveMarker(marker, markerDTO.getLat(), markerDTO.getLng());

		markerService.broadcastUpdateToAllLoggedUser();

		return DefaultResult.success();

	}

	@Auth
	@PostMapping("/pulse")
	public DefaultResult pulse(MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {

		Marker marker = markerService.findMarkerByMarkerId(markerDTO.getMarkerId());

		if (markerService.pulseMarker(marker)) {

			markerService.broadcastUpdateToAllLoggedUser();

			return DefaultResult.success();
		} else {
			return DefaultResult.error("marker not ready");
		}
	}

	@Auth
	@PostMapping("/copy")
	public DefaultResult copy(MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		String uuid = CookieUtil.getUUID(request, response, session);

		Marker marker = markerService.findMarkerByMarkerId(markerDTO.getMarkerId());

		try {
			markerService.copyMarkerToLayer(marker, markerDTO.getLayer(), uuid);

			markerService.broadcastUpdateToAllLoggedUser();
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

		markerService.broadcastUpdateToAllLoggedUser();

		return DefaultResult.success();
	}

	@Auth
	@PostMapping("/updateMessage")
	public DefaultResult updateMessage(MarkerDTO markerDTO, HttpServletRequest request, HttpServletResponse response, HttpSession session) {

		Marker marker = markerService.findMarkerByMarkerId(markerDTO.getMarkerId());

		markerService.updateMessage(marker, markerDTO.getMessage());

		markerService.broadcastUpdateToAllLoggedUser();

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
