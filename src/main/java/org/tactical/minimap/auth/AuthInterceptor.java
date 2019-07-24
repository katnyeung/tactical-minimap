package org.tactical.minimap.auth;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.service.LayerService;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.util.CookieUtil;
import org.tactical.minimap.web.result.DefaultResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AuthInterceptor implements HandlerInterceptor {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	LayerService layerService;

	@Autowired
	RedisService redisService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

		String ipAddress = request.getHeader("X-FORWARDED-FOR");
		if (ipAddress == null) {
			ipAddress = request.getRemoteAddr();
		}

		if (!request.getRequestURI().contains("/js/") && !request.getRequestURI().contains("/css/") && !request.getRequestURI().contains("/m/")) {
			logger.info("IP [" + ipAddress + "] : " + request.getRequestURI());
		}

		if (handler instanceof HandlerMethod) {

			HandlerMethod hm = (HandlerMethod) handler;
			Method method = hm.getMethod();

			if (method.isAnnotationPresent(Auth.class)) {
				if (request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

					String layerKey = pathVariables.get("layerKey");

					if (layerKey != null) {

						HttpSession session = request.getSession();

						String uuid = CookieUtil.getUUID(request, response, session);
						Set<String> loggedLayer = redisService.getLoggedLayers(uuid);

						Layer layer = layerService.getLayerByKey(layerKey);

						if (layer != null) {
							if (layer.getPassword() == null || (layer.getPassword() != null && loggedLayer.contains(layer.getLayerKey()))) {
								return true;
							} else {
								if (method.getReturnType() == String.class) {
									response.sendRedirect("/");
									return false;
								} else {
									returnMessage(response, "login required");
									return false;
								}
							}
						} else {
							returnMessage(response, "layer not registered");
							return false;
						}

					}
				}

			}
		}

		return true;
	}

	private void returnMessage(HttpServletResponse response, String message) throws IOException {

		ObjectMapper om = new ObjectMapper();

		response.setContentType("application/json; charset=utf-8");
		ServletOutputStream sos = response.getOutputStream();
		sos.print(om.writeValueAsString(DefaultResult.error(message)));

		sos.close();
	}
}