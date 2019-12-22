package org.tactical.minimap.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
import org.tactical.minimap.web.result.CommonResult;
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

	@GetMapping("/streetLive/")
	public DefaultResult streetLiveSTat() {

		Map<String, HashMap<String, Integer>> regionMap = tgService.getStreetLiveStat();

		DefaultResult dr = CommonResult.success(sortMap(regionMap));

		return dr;	
	}
	
	private Map<String, HashMap<String, Integer>> sortMap(Map<String, HashMap<String, Integer>> input){
		// create list of hashMap
		Map<String, Integer> totalMap = new HashMap<String, Integer>();
		Map<String, HashMap<String, Integer>> sortedMap = new LinkedHashMap<String, HashMap<String, Integer>>();
                int count = 0; 	
		for(Entry<String, HashMap<String, Integer>> inputEntry : input.entrySet()) {
						
			int total = zeroIfNull(inputEntry.getValue().get("popo"));
			total += zeroIfNull(inputEntry.getValue().get("hit"));
			
			if(total > 20 && count++ > (input.keySet().size() - 10)) {
				totalMap.put(inputEntry.getKey(), total);
			}
		}
		
		totalMap = totalMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(
                Map.Entry::getKey, 
                Map.Entry::getValue, 
                (x,y)-> {throw new AssertionError();},
                LinkedHashMap::new
        ));
		
		for(Entry<String, Integer> inputEntry : totalMap.entrySet()) {
			sortedMap.put(inputEntry.getKey(), input.get(inputEntry.getKey()));
		}
		
		return sortedMap;
	}
    
	private int zeroIfNull(Integer value) {
		if(value == null) {
			return 0;
		}else {
			return value;
		}
	}

	@GetMapping("/tg24hr/")
	public DefaultResult update(@RequestParam("hour") Long hour, @RequestParam("count") Long count) {

		List<StatItem> listStat = tgService.getTelegram24hrStat(hour, count);

		DefaultResult dr = StatResult.success(listStat);

		return dr;
	}

}
