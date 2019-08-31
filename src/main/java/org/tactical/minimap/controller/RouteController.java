package org.tactical.minimap.controller;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.activation.FileTypeMap;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.service.LayerService;
import org.tactical.minimap.service.MarkerService;
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

	@Autowired
	MarkerService markerService;

	@Value("${MAP_FOLDER}")
	String mapFolder;

	@ResponseBody
	@GetMapping(path = "/m/**", produces = MediaType.IMAGE_JPEG_VALUE)
	public ResponseEntity<byte[]> getMapImage(HttpServletRequest request, HttpSession session, Model model) {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		path = path.replaceAll("^/m", "");

		File img = null;
		byte[] bytes = null;

		try {
			img = new File(mapFolder + path);

			bytes = Files.readAllBytes(img.toPath());
		} catch (IOException ioex) {
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.ok().contentType(MediaType.valueOf(FileTypeMap.getDefaultFileTypeMap().getContentType(img))).body(bytes);
	}

	@GetMapping(path = "/")
	public String index(HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) {
		return "redirect:/l/" + ConstantsUtil.DEFAULT_LAYER + "/12/" + ConstantsUtil.DEFAULT_LAT + "/" + ConstantsUtil.DEFAULT_LNG;
	}

	@GetMapping(path = "/l/{layerKeys}")
	public String layer(@PathVariable("layerKeys") String layerKeys, HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) {
		return "redirect:/" + layerKeys + "/12/" + ConstantsUtil.DEFAULT_LAT + "/" + ConstantsUtil.DEFAULT_LNG;
	}

	@GetMapping(path = "/l/{layerKeys}/{zoom}/{lat}/{lng}")
	public String layerXY(@PathVariable("layerKeys") String layerKeys, @PathVariable("zoom") Integer zoom, @PathVariable("lat") Double lat, @PathVariable("lng") Double lng, HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) {
		String uuid = CookieUtil.getUUID(request, response, session);
		Map<String, String> layerMap = new HashMap<String, String>();

		Pattern pattern = Pattern.compile("([0-9a-zA-Z-_]*)\\$([a-zA-Z]*)");

		for (String layerKey : layerKeys.split(",")) {
			Matcher matcher = pattern.matcher(layerKey);

			if (matcher.find()) {
				if (matcher.groupCount() > 1) {
					layerMap.put(matcher.group(1), matcher.group(2));
				}
			}
		}

		if (layerMap.size() == 0) {
			return "redirect:/";
		}

		model.addAttribute("key", uuid);
		model.addAttribute("zoom", zoom);

		model.addAttribute("layerKeysString", layerKeys);

		model.addAttribute("layerKeys", layerMap.keySet());
		model.addAttribute("layerMap", layerMap);

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

				if (layerDTO.getExpireMultiplier() != null) {
					layer.setExpireMultiplier(layerDTO.getExpireMultiplier());
				} else {
					layer.setExpireMultiplier(10);
				}

				layer.setStatus(ConstantsUtil.LAYER_STATUS_ACTIVE);

				layerService.save(layer);
				return DefaultResult.success();
			} else {

				return DefaultResult.error("password required");
			}

		} else {
			layer.setPassword(layerDTO.getPassword());
			layer.setStatus(ConstantsUtil.LAYER_STATUS_ACTIVE);

			if (layerDTO.getExpireMultiplier() != null) {
				layer.setExpireMultiplier(layerDTO.getExpireMultiplier());
			} else {
				layer.setExpireMultiplier(10);
			}

			layerService.save(layer);
			return DefaultResult.success();
		}
	}

}
