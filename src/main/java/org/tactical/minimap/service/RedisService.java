package org.tactical.minimap.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

@Service
public class RedisService {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

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
		mc.setRate(marker.getRate());
		mc.setLayer(marker.getLayer());
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

	public boolean voteLock(Long markerId, String uuid, int time) {
		String key = ConstantsUtil.REDIS_MARKER_RESPONSE_PREFIX + ":" + markerId.toString() + ":" + uuid;
		String value = stringRedisTemplate.opsForValue().get(key);

		if (value == null) {

			stringRedisTemplate.opsForValue().set(key, "1");
			stringRedisTemplate.expire(key, time, TimeUnit.SECONDS);

			return true;
		} else {
			return false;
		}
	}

	public boolean addLock(String layer, String uuid, int time) {
		String key = ConstantsUtil.REDIS_MARKER_LOCK_PREFIX + ":" + layer + ":" + uuid;
		String value = stringRedisTemplate.opsForValue().get(key);

		if (value == null) {

			stringRedisTemplate.opsForValue().set(key, "1");
			stringRedisTemplate.expire(key, time, TimeUnit.SECONDS);

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

	public List<String> findKeys(String query) {
		return scanKeys(query);
	}

	public List<String> scanKeys(String pattern) {
		List<String> keySet = new ArrayList<>();

		//logger.info("Searching for pattern {}", pattern);
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

}
