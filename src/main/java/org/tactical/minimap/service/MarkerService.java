package org.tactical.minimap.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.osgeo.proj4j.BasicCoordinateTransform;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.ProjCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tactical.minimap.DAO.MarkerDAO;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.repository.TelegramMessage;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.repository.marker.livestream.ImageMarker;
import org.tactical.minimap.repository.marker.shape.ShapeMarker;
import org.tactical.minimap.repository.marker.shape.ShapeMarkerDetail;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.util.MarkerCache;
import org.tactical.minimap.util.TelegramResult;
import org.tactical.minimap.web.DTO.MarkerDTO;
import org.tactical.minimap.web.DTO.MarkerWebSocketDTO;
import org.tactical.minimap.web.result.MarkerResult;
import org.tactical.minimap.web.result.MarkerResultListResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MarkerService {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final Pattern timePattern = Pattern.compile("([0-2][0-3])\\:?([0-5][0-9])");

	@Autowired
	MarkerDAO<Marker> markerDAO;

	@Autowired
	RedisService redisService;

	@Autowired
	LayerService layerService;

	@Autowired
	TelegramMessageService telegramMessageService;

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;

	@PersistenceContext
	EntityManager em;

	public List<MarkerResult> findMultiLayerMarkersResponse(String uuid, List<Long> markerIdList, List<String> layerKeys, Double lat, Double lng, Double range) {

		ObjectMapper om = new ObjectMapper();
		
		Set<String> loggedLayers = layerService.getLoggedLayers(uuid);

		List<MarkerResult> mrList = new LinkedList<MarkerResult>();

		List<Marker> markerList = markerDAO.findActiveMarkersByLatLng(layerKeys, lat - range, lng - range, lat + range, lng + range);
		List<Long> processedList = new ArrayList<Long>();

		Set<Integer> streetGroupSet = new HashSet<Integer>();

		for (Marker marker : markerList) {
			boolean isControllable = false;
			if (loggedLayers.contains(marker.getLayer().getLayerKey())) {
				isControllable = true;
			}

			MarkerResult mr;

			MarkerCache mc = redisService.getMarkerCacheByMarkerId(marker.getMarkerId());

			double opacity = getMarkerOpacity(marker);

			// if street overlap , remove current street group
			removeOverlapStreet(marker, streetGroupSet);

			// process marker new line and wrap issue
			marker.setMessage(marker.getMessage().replaceAll("\\n+", "\n").replaceAll("(\\S{30})", "$1\n"));

			// group policeMarker to a numberMarker
			
			// group Image marker to a imageListMarker
			
			// set keywords list to marker
			List<String> keywordList = new ArrayList<String>();
			TelegramMessage telegramMessage = marker.getTelegramMessage();
			if(telegramMessage != null && telegramMessage.getResult() != null) {
				try {
					TelegramResult tr = om.readValue(telegramMessage.getResult(), TelegramResult.class);
					for (Entry<String, Integer> entry : tr.getData().entrySet()) {
						keywordList.add(entry.getKey());
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			marker.setKeywordList(keywordList);
			
			if (markerIdList != null) {
				if (!markerIdList.contains(marker.getMarkerId())) {
					// new marker, not in client list
					mr = MarkerResult.makeResult(marker.getMarkerId()).status("A").marker(marker).cache(mc).opacity(opacity).controllable(isControllable);
				} else {
					// marker exist in client
					if (Calendar.getInstance().getTimeInMillis() - marker.getLastupdatedate().getTime() < 10000) {
						mr = MarkerResult.makeResult(marker.getMarkerId()).status("U").marker(marker).cache(mc).opacity(opacity).controllable(isControllable);
					} else {
						mr = MarkerResult.makeResult(marker.getMarkerId()).status("O").cache(mc).opacity(opacity).controllable(isControllable);
					}

				}

				// processed, remove the Id from list
				processedList.add(marker.getMarkerId());
			} else {
				// request marker first time
				mr = MarkerResult.makeResult(marker.getMarkerId()).status("A").marker(marker).cache(mc).opacity(opacity).controllable(isControllable);
			}

			mrList.add(mr);
		}

		if (markerIdList != null) {
			for (Long markerId : markerIdList) {
				// marker no more in process
				if (!processedList.contains(markerId)) {
					MarkerResult mr = MarkerResult.makeResult(markerId).status("X");
					mrList.add(mr);
				}

			}
		}

		return mrList;
	}

	public Marker removeOverlapStreet(Marker marker, Set<Integer> streetGroupSet) {
		Set<Integer> tempGroupList = new HashSet<Integer>();

		if (marker instanceof ShapeMarker) {

			ShapeMarker shapeMarker = (ShapeMarker) marker;
			List<ShapeMarkerDetail> smdList = new ArrayList<ShapeMarkerDetail>();
			boolean haveFilteredGroup = false;

			for (ShapeMarkerDetail smd : shapeMarker.getShapeMarkerDetailList()) {
				if (streetGroupSet.contains(smd.getSubGroup())) {
					// remove whole group
					haveFilteredGroup = true;
				} else {
					tempGroupList.add(smd.getSubGroup());
					smdList.add(smd);
				}
			}

			streetGroupSet.addAll(tempGroupList);

			if (haveFilteredGroup) {
				shapeMarker.setShapeMarkerDetailList(smdList);
			}

		}

		return marker;
	}

	public List<Marker> findMultiLayerMarkers(List<String> layerKeys, Double lat, Double lng, Double range) {
		List<Marker> markerList = markerDAO.findActiveMarkersByLatLng(layerKeys, lat - range, lng - range, lat + range, lng + range);
		markerList = markerList.stream().limit(80).collect(Collectors.toList());

		// for deactive marker
		Optional<Marker> lastMarker = markerList.parallelStream().min(Comparator.comparing(Marker::getLastupdatedate));

		Date lastMarkerDate = null;
		if (lastMarker.isPresent()) {
			lastMarkerDate = lastMarker.get().getLastupdatedate();
		} else {
			lastMarkerDate = Calendar.getInstance().getTime();
		}

		if (markerList.size() < ConstantsUtil.MARKER_LIST_SIZE) {
			int remainRecord = ConstantsUtil.MARKER_LIST_SIZE - markerList.size();
			List<Marker> deactiveMarkerList = markerDAO.findDeactiveMarkersByLatLng(PageRequest.of(0, remainRecord), lastMarkerDate, layerKeys, lat - range, lng - range, lat + range, lng + range);

			markerList.addAll(deactiveMarkerList);
		}

		return markerList;
	}

	public List<Marker> findMarkers(String layer, Double lat, Double lng, Double range) {
		return markerDAO.findByLatLngLayer(layer, lat - range, lng - range, lat + range, lng + range);
	}

	public Marker addMarker(Layer layer, MarkerDTO markerDTO, Marker marker) {
		logger.info("Adding Marker : " + marker.getClass().getName());

		TimeZone tz1 = TimeZone.getTimeZone("GMT+08:00");
		Calendar cal1 = Calendar.getInstance(tz1);

		int hour = cal1.get(Calendar.HOUR_OF_DAY);
		int minute = cal1.get(Calendar.MINUTE);

		int patternHour = Integer.MAX_VALUE;
		int patternMinute = Integer.MAX_VALUE;

		Matcher timeMatcher = timePattern.matcher(markerDTO.getMessage());
		if (timeMatcher.find() && timeMatcher.groupCount() > 1) {
			try {
				patternHour = Integer.parseInt(timeMatcher.group(1));
				patternMinute = Integer.parseInt(timeMatcher.group(2));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}

		// only within 120 minutes, else assign current time
		if (Math.abs((patternHour * 60 + patternMinute) - (hour * 60 + minute)) <= 120) {
			markerDTO.setHour(patternHour);
			markerDTO.setMinute(patternMinute);
		} else {
			markerDTO.setHour(hour);
			markerDTO.setMinute(minute);
		}

		marker = marker.fill(markerDTO);

		if (marker != null) {

			if (markerDTO.getTelegramMessageId() != null) {
				TelegramMessage telegramMessage = telegramMessageService.getTelegramMessageById(markerDTO.getTelegramMessageId());
				marker.setTelegramMessage(telegramMessage);
			}

			marker.setLayer(layer);

			if (layer.getPassword() != null && !layer.getPassword().equals("")) {
				marker.setExpire(marker.getExpire() * layer.getExpireMultiplier());
			}

			markerDAO.save(marker);

		} else {
			return null;
		}

		return marker;
	}

	public Marker findMarkerByMarkerId(Long markerId) {
		Optional<Marker> optionalMarker = markerDAO.findById(markerId);

		return optionalMarker.get();
	}

	@Transactional(readOnly = false)
	public void updateStatusUpDown(Long markerId, String status, int upVote, int downVote) {
		markerDAO.updateStatusUpDown(markerId, status, upVote, downVote);
	}

	public List<Marker> findActiveMarkersNotInCache(List<Long> markerIdList) {
		return markerDAO.findActiveMarkersNotInCache(markerIdList);
	}

	public List<Marker> findActiveMarkers() {
		return markerDAO.findActiveMarkers();
	}

	public void moveMarker(Marker marker, Double lat, Double lng) {
		if (marker instanceof ShapeMarker) {
			Double diffLat = lat - marker.getLat();
			Double diffLng = lng - marker.getLng();

			ShapeMarker shapeMarker = (ShapeMarker) marker;
			List<ShapeMarkerDetail> shapeList = shapeMarker.getShapeMarkerDetailList();
			for (ShapeMarkerDetail smd : shapeList) {
				smd.setLat(smd.getLat() + diffLat);
				smd.setLng(smd.getLng() + diffLng);
			}
		}

		marker.setLat(lat);
		marker.setLng(lng);
		marker.setCreatedate(Calendar.getInstance().getTime());

		markerDAO.save(marker);
	}

	public void deleteMarker(Marker marker) {
		marker.setStatus(ConstantsUtil.MARKER_STATUS_DEACTIVED);
		markerDAO.save(marker);
	}

	public void updateMessage(Marker marker, String message) {
		marker.setMessage(message);
		markerDAO.save(marker);
	}

	public void update(Marker marker) {
		markerDAO.save(marker);
	}

	public int getMarkerCountInRange(String layerKey, Double lat, Double lng, Double range) {
		return markerDAO.getMarkerCountInRange(layerKey, lat - range, lat + range, lng - range, lng + range);
	}

	public double getTotalUpVoteInList(List<Marker> markerList) {
		double totalUpVote = 0.0;
		for (Marker marker : markerList) {
			totalUpVote = marker.getUpVote();
		}

		return totalUpVote;
	}

	public double getMarkerOpacity(Marker marker) {

		// set opacity
		int minute = 60 * 1000;
		Date currentDate = Calendar.getInstance().getTime();
		double weight = 1.0;

		if (marker.getLastupdatedate().getTime() + (18 * minute) <= currentDate.getTime()) {
			weight = 0.6;
		} else if (marker.getLastupdatedate().getTime() + (15 * minute) <= currentDate.getTime()) {
			weight = 0.7;
		} else if (marker.getLastupdatedate().getTime() + (12 * minute) <= currentDate.getTime()) {
			weight = 0.8;
		} else if (marker.getLastupdatedate().getTime() + (6 * minute) <= currentDate.getTime()) {
			weight = 0.9;
		}

		return weight;

	}

	public boolean pulseMarker(Marker marker) {

		MarkerCache mc = redisService.getMarkerCacheByMarkerId(marker.getMarkerId());

		if (mc != null) {

			mc.setPulse(mc.getPulse() + marker.getPulseRate());

			redisService.saveMarkerCache(mc);

			return true;
		} else {
			return false;
		}

	}

	public void copyMarkerToLayer(Marker marker, String layerKey, String uuid) throws InstantiationException, IllegalAccessException, JsonProcessingException {
		logger.info("copying marker : " + marker.getMarkerId() + " to " + layerKey);

		ObjectMapper om = new ObjectMapper();

		Layer layer = layerService.getLayerByKey(layerKey);

		MarkerDTO markerDTO = new MarkerDTO();
		markerDTO.setLat(marker.getLat());
		markerDTO.setLng(marker.getLng());
		markerDTO.setMessage(marker.getMessage());
		markerDTO.setType(marker.getType());
		markerDTO.setHour(marker.getHour());
		markerDTO.setMinute(marker.getMinute());
		markerDTO.setUuid(uuid);

		if (marker instanceof ShapeMarker) {
			ShapeMarker shapeMarker = (ShapeMarker) marker;

			markerDTO.setShapeType(shapeMarker.getShapeType());
			markerDTO.setColor(shapeMarker.getColor());
			List<Map<String, Double>> shapeList = new ArrayList<Map<String, Double>>();
			for (ShapeMarkerDetail smd : shapeMarker.getShapeMarkerDetailList()) {
				LinkedHashMap<String, Double> map = new LinkedHashMap<String, Double>();
				map.put("lat", smd.getLat());
				map.put("lng", smd.getLng());
				shapeList.add(map);
			}
			markerDTO.setShapeList(om.writeValueAsString(shapeList));

		} else if (marker instanceof ImageMarker) {
			markerDTO.setImagePath(((ImageMarker) marker).getImagePath());
		}

		Marker cloneMarker = marker.getClass().newInstance();

		cloneMarker = cloneMarker.fill(markerDTO);

		cloneMarker.setLayer(layer);

		if (layer.getPassword() != null && !layer.getPassword().equals("")) {
			cloneMarker.setExpire(marker.getExpire());
		}

		markerDAO.save(cloneMarker);
	}

	public void broadcastUpdateToAllLoggedUser() {
		List<String> keyList = redisService.findKeys(ConstantsUtil.REDIS_USER_PREFIX + ":*");
		for (String key : keyList) {
			String uuid = key.replace(ConstantsUtil.REDIS_USER_PREFIX + ":", "");
			MarkerWebSocketDTO markerWSDTO = redisService.getLoggedUser(uuid);

			logger.info("broadcast to user {} : {} ", uuid, markerWSDTO);

			List<MarkerResult> markerResultList = processMarkers(markerWSDTO);

			simpMessagingTemplate.convertAndSend("/markers/list/" + uuid, MarkerResultListResult.success(markerResultList));
		}
	}

	public List<MarkerResult> processMarkers(MarkerWebSocketDTO markerWSDTO) {
		Map<String, String> layerMap = new HashMap<String, String>();
		List<Long> markerIdList = new ArrayList<Long>();

		Pattern pattern = Pattern.compile("([0-9a-zA-Z-_]*)\\$([a-zA-Z]*)");

		for (String layerKey : markerWSDTO.getLayerString().split(",")) {
			Matcher matcher = pattern.matcher(layerKey);

			if (matcher.find()) {
				if (matcher.groupCount() > 1) {
					layerMap.put(matcher.group(1), matcher.group(2));
				}
			}
		}

		if (markerWSDTO.getMarkerIdList().length() > 0) {
			for (String markerId : markerWSDTO.getMarkerIdList().split(",")) {
				markerIdList.add(Long.parseLong(markerId));
			}
		}

		return findMultiLayerMarkersResponse(markerWSDTO.getUuid(), markerIdList, layerMap.keySet().stream().collect(Collectors.toList()), markerWSDTO.getLat(), markerWSDTO.getLng(), ConstantsUtil.RANGE);
	}

	public List<Marker> findLatestActiveMarkersInRange(List<String> layerKeys, double fromLat, double fromLng, double toLat, double toLng, Long timestamp) {
		return markerDAO.findLatestActiveMarkersByLatLng(layerKeys, fromLat, fromLng, toLat, toLng, timestamp);
	}

	public List<Marker> findActiveMarkersByMarkerIds(List<String> layerKeys, List<Long> markerIdList) {
		return markerDAO.findActiveMarkersByMarkerIds(layerKeys, markerIdList);
	}

	public ProjCoordinate toWGS84(double x, double y) {

		CRSFactory factory = new CRSFactory();
		CoordinateReferenceSystem srcCrs = factory.createFromName("EPSG:2326");
		CoordinateReferenceSystem dstCrs = factory.createFromName("EPSG:4326");

		BasicCoordinateTransform transform = new BasicCoordinateTransform(srcCrs, dstCrs);

		ProjCoordinate srcCoord = new ProjCoordinate(x, y);
		ProjCoordinate dstCoord = new ProjCoordinate();

		// Writes result into dstCoord
		transform.transform(srcCoord, dstCoord);

		return dstCoord;
	}

	public ProjCoordinate toHK1980(double lat, double lng) {

		CRSFactory factory = new CRSFactory();
		CoordinateReferenceSystem srcCrs = factory.createFromName("EPSG:4326");
		CoordinateReferenceSystem dstCrs = factory.createFromName("EPSG:2326");

		BasicCoordinateTransform transform = new BasicCoordinateTransform(srcCrs, dstCrs);

		ProjCoordinate srcCoord = new ProjCoordinate(lng, lat);
		ProjCoordinate dstCoord = new ProjCoordinate();

		// Writes result into dstCoord
		transform.transform(srcCoord, dstCoord);

		return dstCoord;
	}
}
