package org.tactical.minimap.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CookieUtil {
	public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
		Cookie cookie = new Cookie(name, value);
		cookie.setPath("/");
		cookie.setMaxAge(maxAge);
		response.addCookie(cookie);
	}

	public static Map<String, String> readCookieMap(HttpServletRequest request) {
		Map<String, String> cookieMap = new HashMap<String, String>();
		Cookie[] cookies = request.getCookies();
		if (null != cookies) {
			for (Cookie cookie : cookies) {
				cookieMap.put(cookie.getName(), cookie.getValue());
			}
		}
		return cookieMap;
	}

	public static String handleCookie(HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> cookieMap = CookieUtil.readCookieMap(request);
		String userKey = cookieMap.get("userKey");

		if (userKey == null) {
			String uuid = UUID.randomUUID().toString().replaceAll("-", "");
			// set cookie
			CookieUtil.addCookie(response, "userKey", uuid, 7 * 24 * 60 * 60);
		}
		return userKey;
	}

	public static String getUUID(HttpServletRequest request, HttpServletResponse response, HttpSession session) {

		String uuid = (String) session.getAttribute("key");

		if (uuid == null) {
			Map<String, String> cookieMap = CookieUtil.readCookieMap(request);
			uuid = cookieMap.get("key");
			if (uuid == null) {
				uuid = UUID.randomUUID().toString().replaceAll("-", "");
				CookieUtil.addCookie(response, "key", uuid, 60 * 60 * 24);
			}

			session.setAttribute("key", uuid);
		}

		return uuid;
	}

}
