package org.tactical.minimap.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.service.StreetDataService;
import org.tactical.minimap.service.TelegramMessageService;
import org.tactical.minimap.util.MarkerGeoCoding;
import org.tactical.minimap.web.result.DefaultResult;
import org.tactical.minimap.web.result.StatItem;
import org.tactical.minimap.web.result.StatMapResult;
import org.tactical.minimap.web.result.StatResult;

@RestController
@RequestMapping("/stat")
public class StatController {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	TelegramMessageService tgService;

	@Autowired
	RedisService redisService;

	@Autowired
	StreetDataService sdService;

	@GetMapping("/tgLive/")
	public DefaultResult telegramLiveStat() {

		List<MarkerGeoCoding> regionList = sdService.getStatRegionList();

		Map<String, List<StatItem>> regionMap = tgService.getTelegramLiveStat(regionList);

		DefaultResult dr = StatMapResult.success(regionMap);

		return dr;
	}

	@GetMapping("/tg24hr/")
	public DefaultResult update(@RequestParam("hour") Long hour, @RequestParam("count") Long count) {

		List<StatItem> listStat = tgService.getTelegram24hrStat(hour, count);

		DefaultResult dr = StatResult.success(listStat);

		return dr;
	}

}