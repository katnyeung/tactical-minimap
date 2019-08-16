package org.tactical.minimap.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
import org.tactical.minimap.web.result.MarkerMessage;

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
		mc.setUpRate(marker.getUpRate());
		mc.setDownRate(marker.getDownRate());
		mc.setLayer(marker.getLayer().getLayerKey());
		mc.setPulse(ConstantsUtil.PULSE_RATE);

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

	public void addMarkerMessage(String layer, Long markerId, String type, String message, Long dateTime) {
		Long queueLength = stringRedisTemplate.opsForList().leftPush(ConstantsUtil.MARKER_MESSAGE_QUEUE_KEY + ":" + layer, dateTime + "`" + markerId + "`" + type + "`" + message);

		if (queueLength > ConstantsUtil.MARKER_MESSAGE_QUEUE_SIZE) {
			stringRedisTemplate.opsForList().trim(ConstantsUtil.MARKER_MESSAGE_QUEUE_KEY + ":" + layer, 0, ConstantsUtil.MARKER_MESSAGE_QUEUE_SIZE);
		}
	}

	public List<MarkerMessage> getMarkerMessage(List<String> layerList, Long timeStampInMilles) {
		List<MarkerMessage> messageList = new ArrayList<MarkerMessage>();
		for (String layer : layerList) {
			List<String> rawMessageList = stringRedisTemplate.opsForList().range(ConstantsUtil.MARKER_MESSAGE_QUEUE_KEY + ":" + layer, 0, ConstantsUtil.MARKER_MESSAGE_QUEUE_SIZE);
			for (String rawMessage : rawMessageList) {
				String[] rawMessageArray = rawMessage.split("`");

				if (rawMessageArray.length > 2) {
					
					Long messageTimeStampInMilles = Long.parseLong(rawMessageArray[0]);
					if (messageTimeStampInMilles > timeStampInMilles) {
						logger.info("raw : " + rawMessage + " - " + rawMessageArray.length);
						
						MarkerMessage mm = new MarkerMessage();

						mm.setTime(new Date(messageTimeStampInMilles));
						mm.setMarkerId(Long.parseLong(rawMessageArray[1]));
						mm.setType(rawMessageArray[2]);
						mm.setMessage(rawMessageArray[3]);

						messageList.add(mm);

					}
				}
			}
		}

		Collections.sort(messageList, new Comparator<MarkerMessage>() {
			@Override
			public int compare(MarkerMessage msg1, MarkerMessage msg2) {
				return msg1.getTime().compareTo(msg2.getTime());
			}
		});

		return messageList.stream().limit(ConstantsUtil.MARKER_MESSAGE_QUEUE_SIZE).collect(Collectors.toList());
	}

}
