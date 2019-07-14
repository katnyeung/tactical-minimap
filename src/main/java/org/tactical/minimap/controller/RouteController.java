package org.tactical.minimap.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.service.LayerService;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.util.CookieUtil;
import org.tactical.minimap.web.DTO.LayerDTO;
import org.tactical.minimap.web.result.DefaultResult;

@Controller
@RequestMapping("/")
public class RouteController {
	Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	@Autowired
	RedisService redisService;

	@Autowired
	LayerService layerService;

	@GetMapping(path = "/")
	public String index(HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) {
		return "redirect:/" + ConstantsUtil.DEFAULT_LAYER + "/10/" + ConstantsUtil.DEFAULT_LAT + "/" + ConstantsUtil.DEFAULT_LNG;
	}

	@GetMapping(path = "/{layerKeys}")
	public String layer(@PathVariable("layerKeys") String layerKeys, HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) {
		return "redirect:/" + layerKeys + "/10/" + ConstantsUtil.DEFAULT_LAT + "/" + ConstantsUtil.DEFAULT_LNG;
	}

	@GetMapping(path = "/{layerKeys}/{zoom}/{lat}/{lng}")
	public String layerXY(@PathVariable("layerKeys") String layerKeys, @PathVariable("zoom") Integer zoom, @PathVariable("lat") Double lat, @PathVariable("lng") Double lng, HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) {

		String uuid = CookieUtil.getUUID(request, response, session);

		model.addAttribute("key", uuid);
		model.addAttribute("zoom", zoom);
		model.addAttribute("layerKeys", layerKeys);
		model.addAttribute("lat", lat);
		model.addAttribute("lng", lng);

		Set<String> loggedLayers = layerService.getLoggedLayers(uuid);

		model.addAttribute("validLayers", layerService.getActiveLayers().stream().map(Layer::getLayerKey).collect(Collectors.toList()));
		model.addAttribute("loggedLayers", loggedLayers);

		return "index";
	}

	@ResponseBody
	@GetMapping("/login")
	public DefaultResult loginLayer(HttpServletRequest request, HttpServletResponse response, HttpSession session, LayerDTO layerDTO) {
		String uuid = CookieUtil.getUUID(request, response, session);

		Set<String> loggedLayer = redisService.getLoggedLayers(uuid);

		if (!loggedLayer.contains(layerDTO.getLayerKey())) {
			Layer layer = layerService.getLayerByKey(layerDTO.getLayerKey());
			if (layer != null) {
				if (layer.getPassword() != null && layer.getPassword().equals(layerDTO.getPassword())) {

					redisService.addLoggedLayer(layer.getLayerKey(), uuid);

					return DefaultResult.success("logged ok");
				} else {
					return DefaultResult.error("password incorrect");
				}
			} else {
				return DefaultResult.error("layer not reigstered");
			}

		} else {
			return DefaultResult.error("already logged");
		}

	}

	@ResponseBody
	@PostMapping("/register")
	public DefaultResult registerLayer(HttpServletRequest request, HttpServletResponse response, HttpSession session, LayerDTO layerDTO) {
		Layer layer = layerService.getLayerByKey(layerDTO.getLayerKey());

		if (layer == null) {
			if (layerDTO.getPassword() != null) {
				layer = new Layer();
				layer.setLayerKey(layerDTO.getLayerKey());
				layer.setPassword(layerDTO.getPassword());
				layer.setDuration(24);
				layer.setStatus(ConstantsUtil.LAYER_STATUS_ACTIVE);

				layerService.save(layer);
				return DefaultResult.success();
			} else {

				return DefaultResult.error("password required");
			}

		} else {
			layer.setPassword(layerDTO.getPassword());
			layer.setStatus(ConstantsUtil.LAYER_STATUS_ACTIVE);
			layerService.save(layer);
			return DefaultResult.error("layer already registered");
		}
	}

}
