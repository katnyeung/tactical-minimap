package org.tactical.minimap.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.tactical.minimap.util.MarkerCache;

@Service
public class RedisService {

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	public void updateMarkerData(MarkerCache markerCache) {
		stringRedisTemplate.opsForHash().putAll(markerCache.getMarkerId().toString(), markerCache.toHashMap());
	}

	public MarkerCache getDataByMarkerId(Long markerId) {
		Map<Object, Object> objMap = stringRedisTemplate.opsForHash().entries(markerId.toString());
		return MarkerCache.fromHashMap(objMap);
	}
}
