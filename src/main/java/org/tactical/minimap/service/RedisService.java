package org.tactical.minimap.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.util.MarkerCache;
import org.tactical.minimap.web.DTO.MarkerWebSocketDTO;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RedisService {
	public final Logger logger = LoggerFactory.getLogger(getClass());
	final ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	public void saveLoggedUser(String uuid, MarkerWebSocketDTO markerWSDTO) {
		stringRedisTemplate.opsForHash().putAll(ConstantsUtil.REDIS_USER_PREFIX + ":" + uuid, markerWSDTO.toHashMap());
		stringRedisTemplate.expire(ConstantsUtil.REDIS_USER_PREFIX + ":" + uuid, 35, TimeUnit.SECONDS);
	}

	public MarkerWebSocketDTO getLoggedUser(String uuid) {
		Map<Object, Object> objMap = stringRedisTemplate.opsForHash().entries(ConstantsUtil.REDIS_USER_PREFIX + ":" + uuid);
		if (objMap != null) {
			return MarkerWebSocketDTO.fromHashMap(objMap);
		} else {
			return null;
		}
	}

	public void saveMarkerCache(MarkerCache markerCache) {
		stringRedisTemplate.opsForHash().putAll(ConstantsUtil.REDIS_MARKER_PREFIX + ":" + markerCache.getMarkerId().toString(), markerCache.toHashMap());
	}

	public void saveMarkerCache(Marker marker) {
		MarkerCache mc = new MarkerCache();
		mc.setDownVote(0);
		mc.setUpVote(0);
		mc.setLat(marker.getLat());
		mc.setLng(marker.getLng());
		mc.setMarkerId(marker.getMarkerId());
		mc.setExpire(marker.getExpire());
		mc.setUpRate(marker.getUpRate());
		mc.setDownRate(marker.getDownRate());
		mc.setLayer(marker.getLayer().getLayerKey());
		mc.setPulse(marker.getPulseRate());

		saveMarkerCache(mc);
	}

	public MarkerCache getMarkerCacheByMarkerId(Long markerId) {
		Map<Object, Object> objMap = stringRedisTemplate.opsForHash().entries(ConstantsUtil.REDIS_MARKER_PREFIX + ":" + markerId.toString());
		MarkerCache mc = MarkerCache.fromHashMap(objMap);
		if (mc != null) {
			mc.setMarkerId(markerId);
		}
		return mc;
	}

	public String addVoteLock(Long markerId, String uuid, int time) {
		String key = ConstantsUtil.REDIS_MARKER_RESPONSE_LOCK_PREFIX + ":" + markerId.toString() + ":" + uuid;
		String value = stringRedisTemplate.opsForValue().get(key);

		if (value == null) {
			stringRedisTemplate.opsForValue().set(key, "" + Calendar.getInstance().getTimeInMillis());
			stringRedisTemplate.expire(key, time, TimeUnit.SECONDS);

			return null;
		} else {
			return value;
		}
	}

	public String addMarkerLock(String layerKey, String uuid, int time) {
		String key = ConstantsUtil.REDIS_MARKER_LOCK_PREFIX + ":" + layerKey + ":" + uuid;
		String value = stringRedisTemplate.opsForValue().get(key);

		if (value == null) {
			stringRedisTemplate.opsForValue().set(key, "" + Calendar.getInstance().getTimeInMillis());
			return null;
		} else {
			return value;
		}
	}

	public String getMarkerLock(String layerKey, String uuid) {
		String key = ConstantsUtil.REDIS_MARKER_LOCK_PREFIX + ":" + layerKey + ":" + uuid;
		return stringRedisTemplate.opsForValue().get(key);
	}

	public boolean updateLock(String layerKey, String uuid, int time) {
		String key = ConstantsUtil.REDIS_MARKER_LOCK_PREFIX + ":" + layerKey + ":" + uuid;
		String value = stringRedisTemplate.opsForValue().get(key);

		if (value != null) {
			stringRedisTemplate.opsForValue().set(key, "" + Calendar.getInstance().getTimeInMillis());

			return true;
		} else {
			return false;
		}
	}

	public List<MarkerCache> findAllMarkerCache() {
		List<String> keysList = findKeys(ConstantsUtil.REDIS_MARKER_PREFIX + ":*");
		List<MarkerCache> markerCacheList = new ArrayList<>();
		for (String key : keysList) {
			Long markerId = Long.parseLong(key.replaceAll(ConstantsUtil.REDIS_MARKER_PREFIX + ":", ""));
			markerCacheList.add(this.getMarkerCacheByMarkerId(markerId));
		}
		return markerCacheList;
	}

	public void setTempKey(String key, String value, int time) {
		stringRedisTemplate.opsForValue().set(key, value);
		stringRedisTemplate.expire(key, time, TimeUnit.SECONDS);
	}

	public List<String> findKeys(String query) {
		return scanKeys(query);
	}

	public List<String> scanKeys(String pattern) {
		List<String> keySet = new ArrayList<>();

		// logger.info("Searching for pattern {}", pattern);
		Iterable<byte[]> byters = stringRedisTemplate.execute(new RedisCallback<Iterable<byte[]>>() {

			@Override
			public Iterable<byte[]> doInRedis(RedisConnection connection) throws DataAccessException {

				List<byte[]> binaryKeys = new ArrayList<byte[]>();

				ScanOptions.ScanOptionsBuilder scanOptionsBuilder = new ScanOptions.ScanOptionsBuilder();
				scanOptionsBuilder.match(pattern);
				Cursor<byte[]> cursor = connection.scan(scanOptionsBuilder.build());
				while (cursor.hasNext()) {
					binaryKeys.add(cursor.next());
				}

				try {
					cursor.close();
				} catch (IOException e) {
					logger.error("Had a problem", e);
				}

				return binaryKeys;
			}
		});

		for (byte[] byteArr : byters) {
			keySet.add(new String(byteArr));
		}
		return keySet;
	}

	public void deleteKey(String key) {
		stringRedisTemplate.delete(key);
	}

	public Set<String> getLoggedLayers(String uuid) {
		return stringRedisTemplate.opsForSet().members(ConstantsUtil.USER_LOGGED_LAYER_PREFIX + ":" + uuid);
	}

	public void addLoggedLayer(String layerKey, String uuid) {
		stringRedisTemplate.opsForSet().add(ConstantsUtil.USER_LOGGED_LAYER_PREFIX + ":" + uuid, layerKey);
		stringRedisTemplate.expire(ConstantsUtil.USER_LOGGED_LAYER_PREFIX + ":" + uuid, 1, TimeUnit.DAYS);
	}

	public void logoutLayer(String layerKey) {
		List<String> keys = scanKeys(ConstantsUtil.USER_LOGGED_LAYER_PREFIX + ":*");

		for (String key : keys) {
			stringRedisTemplate.opsForSet().remove(key, layerKey);
		}
	}
	
	public void incrKeyByGroup(String group, String key) {
		stringRedisTemplate.opsForHash().increment(group, key, (long) 1);
	}
	
	public void incrKeyByGroup(String group, String key, long value) {
		stringRedisTemplate.opsForHash().increment(group, key, value);
	}
	
	public Object getGroupByKey(String group , String key) {
		return stringRedisTemplate.opsForHash().get(group, key);
	}
	
	public Set<Object> getHashGroup(String group) {
		return stringRedisTemplate.opsForHash().keys(group);
	}

	public String getActiveGroupKey() {
		if (stringRedisTemplate.opsForList().size(ConstantsUtil.TELEGRAM_STAT_GROUP_KEY) > 0) {
			return stringRedisTemplate.opsForList().index(ConstantsUtil.TELEGRAM_STAT_GROUP_KEY, 0);
		} else {
			return null;
		}
	}

	public void addActiveGroupKey(String currentTimeKey) {
		stringRedisTemplate.opsForList().rightPush(ConstantsUtil.TELEGRAM_STAT_GROUP_KEY, currentTimeKey);
	}

	public String popActiveGroupKey() {
		String currentKey = stringRedisTemplate.opsForList().leftPop(ConstantsUtil.TELEGRAM_STAT_GROUP_KEY);

		stringRedisTemplate.delete(currentKey);
		return currentKey;
	}

}
