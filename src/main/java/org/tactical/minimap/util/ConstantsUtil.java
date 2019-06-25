package org.tactical.minimap.util;

public class ConstantsUtil {
	public static final String STATUS_SUCCESS = "success";
	public static final String STATUS_ERROR = "error";
	public static final Double RANGE = 1.0;

	public static final String REDIS_MARKER_PREFIX = "marker";
	public static final String REDIS_MARKER_RESPONSE_PREFIX = "marker_response";
	public static final String REDIS_MARKER_LOCK_PREFIX = "marker_lock";

	public static final int REDIS_MARKER_RESPONSE_INTERVAL_IN_SECOND = 15;
	public static final int REDIS_MARKER_ADD_INTERVAL_IN_SECOND = 30;

	public static final String MARKER_STATUS_ACTIVE = "A";
	public static final String MARKER_STATUS_DEACTIVED = "D";
	public static final Object DEFAULT_LAYER = "public";

	public static final Object DEFAULT_LAT = "22.2757";
	public static final Object DEFAULT_LNG = "114.1648";

}
