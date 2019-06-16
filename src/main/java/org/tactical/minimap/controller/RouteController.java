package org.tactical.minimap.controller;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class RouteController {
	Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	@GetMapping(path = "/")
	public String index(HttpSession session, Model model) {
		logger.info("index_openlayers");
		return "index_openlayers";
	}

	@GetMapping(path = "/2")
	public String index2(HttpSession session, Model model) {
		logger.info("index");
		return "index";
	}
}
