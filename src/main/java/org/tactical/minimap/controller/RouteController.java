package org.tactical.minimap.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.util.CookieUtil;

@Controller
@RequestMapping("/")
public class RouteController {
	Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	@GetMapping(path = "/")
	public String index(HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) {
		return "redirect:/" + ConstantsUtil.DEFAULT_LAYER + "/10/" + ConstantsUtil.DEFAULT_LAT + "/" + ConstantsUtil.DEFAULT_LNG;
	}

	@GetMapping(path = "/{layerKey}")
	public String layer(@PathVariable("layerKey") String layerKey, HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) {
		return "redirect:/" + layerKey + "/10/" + ConstantsUtil.DEFAULT_LAT + "/" + ConstantsUtil.DEFAULT_LNG;
	}

	@GetMapping(path = "/{layerKey}/{zoom}/{lat}/{lng}")
	public String layerXY(@PathVariable("layerKey") String layerKey, @PathVariable("zoom") Integer zoom, @PathVariable("lat") Double lat, @PathVariable("lng") Double lng, HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) {

		String uuid = CookieUtil.getUUID(request, response, session);

		model.addAttribute("key", uuid);
		model.addAttribute("zoom", zoom);
		model.addAttribute("layerKey", layerKey);
		model.addAttribute("lat", lat);
		model.addAttribute("lng", lng);

		return "index";
	}
}
