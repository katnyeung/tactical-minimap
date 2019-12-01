package org.tactical.minimap.util;

public class ConstantsUtil {
	public static final String STATUS_SUCCESS = "success";
	public static final String STATUS_ERROR = "error";
	public static final Double RANGE = 1.0;

	public static final String REDIS_USER_PREFIX = "user";
	
	public static final String REDIS_MARKER_PREFIX = "marker";
	public static final String REDIS_MARKER_RESPONSE_LOCK_PREFIX = "marker_response";
	public static final String REDIS_MARKER_LOCK_PREFIX = "marker_lock";

	public static final int REDIS_MARKER_RESPONSE_INTERVAL_IN_SECOND = 15;
	public static final int REDIS_MARKER_ADD_INTERVAL_IN_SECOND = 30;

	public static final String MARKER_STATUS_ACTIVE = "A";
	public static final String MARKER_STATUS_DEACTIVED = "D";
	public static final Object DEFAULT_LAYER = "public$green,scout$orange,static$red";

	public static final String LAYER_STATUS_ACTIVE = "A";
	public static final String LAYER_STATUS_DEACTIVED = "D";

	public static final String TELEGRAM_MESSAGE_PENDING = "P";
	public static final String TELEGRAM_MESSAGE_PROCESSED_OK = "O";
	public static final String TELEGRAM_MESSAGE_PROCESSED_NOT_OK = "X";

	public static final String TELEGRAM_MESSAGE_RULE_ACTIVE = "A";
	public static final String TELEGRAM_MESSAGE_RULE_DEACTIVE = "O";

	public static final String TELEGRAM_CHANNEL_ACTIVE = "A";
	
	public static final Object DEFAULT_LAT = "22.3202";
	public static final Object DEFAULT_LNG = "114.1711";

	public static final String USER_LOGGED_LAYER_PREFIX = "user_logged_layer";

	public static final int LOGGED_MARKER_VOTE_MULTIPLER = 5;

	public static final String MARKER_MESSAGE_QUEUE_KEY = "marker_message_queue";
	public static final long MARKER_MESSAGE_QUEUE_SIZE = 10;
	public static final int MARKER_LIST_SIZE = 10;
	public static final String TELEGRAM_STAT_GROUP_KEY = "telegram_stat";

}
