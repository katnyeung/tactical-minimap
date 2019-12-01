package org.tactical.minimap.service;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tactical.minimap.DAO.TelegramChannelDAO;
import org.tactical.minimap.DAO.TelegramChatStatDAO;
import org.tactical.minimap.DAO.TelegramMessageDAO;
import org.tactical.minimap.DAO.TelegramMessageRuleDAO;
import org.tactical.minimap.repository.TelegramChannel;
import org.tactical.minimap.repository.TelegramChatStat;
import org.tactical.minimap.repository.TelegramMessage;
import org.tactical.minimap.repository.TelegramMessageRule;
import org.tactical.minimap.util.MarkerGeoCoding;
import org.tactical.minimap.web.result.StatItem;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;

@Service
public class TelegramMessageService {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	TelegramMessageDAO telegramMessageDAO;

	@Autowired
	TelegramMessageRuleDAO telegramMessageRuleDAO;

	@Autowired
	TelegramChannelDAO telegramChannelDAO;

	@Autowired
	TelegramChatStatDAO telegramChatStatDAO;

	@Autowired
	RedisService redisService;

	@Value("${PATTERN_FOLDER}")
	String patternFolder;

	@Value("${MAP_FOLDER}")
	String mapFolder;

	static Map<String, List<String>> patternMap;

	public void initialConfig() {
		try {
			patternMap = new HashMap<String, List<String>>();

			prepareData("region", mapFolder + patternFolder + "/v2/region");

			prepareData("district", mapFolder + patternFolder + "/v2/recreation");

			prepareData("district", mapFolder + patternFolder + "/v2/district");

			prepareData("building", mapFolder + patternFolder + "/v2/estate.building");

			prepareData("wildcard", mapFolder + patternFolder + "/v2/estate.building_wildcard");

			prepareData("building", mapFolder + patternFolder + "/v2/28hse.building");

			prepareData("building", mapFolder + patternFolder + "/v2/building");

			prepareData("plaza", mapFolder + patternFolder + "/v2/plaza");

			prepareData("wildcard", mapFolder + patternFolder + "/v2/plaza_wildcard");

			prepareData("street", mapFolder + patternFolder + "/v2/street");

			prepareData("wildcard", mapFolder + patternFolder + "/v2/street_wildcard");

			prepareData("mtr", mapFolder + patternFolder + "/v2/mtr");

			prepareData("additional", mapFolder + patternFolder + "/v2/additional");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void prepareData(String category, String filePath) throws IOException {
		List<String> patternList = new LinkedList<String>();

		File file = new File(filePath);
		Scanner sc = new Scanner(file);

		while (sc.hasNextLine()) {
			patternList.add(sc.nextLine());
		}

		sc.close();

		patternList.sort((s1, s2) -> s2.length() - s1.length());

		if (patternMap.get(category) != null) {
			List<String> currentStringList = patternMap.get(category);
			currentStringList.addAll(patternList);
		} else {
			patternMap.put(category, patternList);
		}

	}

	public String processData(String message, String category, HashMap<String, Integer> keyMap, final int categoryWeight) throws IOException {
		List<String> patternList = patternMap.get(category);
		for (String pattern : patternList) {
			int weight = categoryWeight;

			String processingPattern = pattern;

			String replaceToPattern = null;
			if (processingPattern.indexOf("|") > 0) {
				replaceToPattern = processingPattern.substring(processingPattern.indexOf("|") + 1, processingPattern.length());
				processingPattern = processingPattern.substring(0, processingPattern.indexOf("|"));
			}

			Pattern addressPattern = Pattern.compile(processingPattern);
			Matcher addressMatcher = addressPattern.matcher(message);

			if (addressMatcher.find()) {
				double weightMultipler = (((message.length() * 1.0) - addressMatcher.start()) / message.length()) * 30;
				weight += weightMultipler;

				if (keyMap.keySet().size() == 0) {
					addPattern(processingPattern, weight, keyMap, addressMatcher.group(0), replaceToPattern);
				} else {
					boolean isExist = false;
					for (String key : keyMap.keySet()) {
						if (key.matches(".*" + pattern + ".*")) {
							logger.info("matched pattern {} {}", category, pattern);
							isExist = true;
						}
					}
					if (!isExist) {
						addPattern(processingPattern, weight, keyMap, addressMatcher.group(0), replaceToPattern);
					}
					logger.info("{}", keyMap);
				}

				message = message.replaceAll(processingPattern, "");
			}
		}
		logger.info(" after replace message : {} ", message);

		return message;
	}

	private void addPattern(String pattern, Integer weight, HashMap<String, Integer> keyMap, String patternMessage, String replaceToPattern) {
		if (replaceToPattern != null) {
			String newPattern = patternMessage.replaceAll(pattern, replaceToPattern);
			keyMap.put(newPattern, weight);
		} else {
			keyMap.put(pattern, weight);
		}
	}

	public List<TelegramMessage> getPendingTelegramMessage(List<String> messageTypeList) {
		return telegramMessageDAO.findPendingTelegramMessage(messageTypeList);
	}

	@Transactional(readOnly = false)
	public void updateTelegramMessageOK(List<Long> idList) {
		telegramMessageDAO.updateProcessOK(idList);
	}

	@Transactional(readOnly = false)
	public void updateTelegramMessageNotOK(List<Long> idList) {
		telegramMessageDAO.updateProcessNotOK(idList);
	}

	@Transactional(readOnly = false)
	public void saveTelegramMessage(TelegramMessage tm) {
		telegramMessageDAO.save(tm);
	}

	public List<TelegramMessageRule> getActiveTelegramMessageRules() {
		return telegramMessageRuleDAO.findActiveTelegramMessageRule();
	}

	public TelegramChannel getChannelByGroupName(String groupKey) {
		List<TelegramChannel> telegramChannelList = telegramChannelDAO.findTelegramChannelByGroupName(groupKey);

		if (telegramChannelList.size() > 0) {
			return telegramChannelList.get(0);
		} else {
			return null;
		}
	}

	public void processChatMessage(String chatMessage, String region) {
		Pattern policeMarkerPattern = Pattern.compile("([0-9][0-9])*?(?:隻|名|個|綠|白|架)*?(?:閃燈)*?(?:藍|白)*?(?:大|小)*?\\s*?(EU|eu|Eu|衝|警車|警|籠|豬籠|軍裝|豬龍|豬)");

		// get active group
		String group = redisService.getActiveGroupKey();

		// extract terms to redis
		for (String patternKey : patternMap.keySet()) {
			List<String> keyList = patternMap.get(patternKey);
			for (String key : keyList) {
				CustomDictionary.add(key);
			}
		}

		Segment segment = HanLP.newSegment();
		segment.enableCustomDictionaryForcing(true);

		final char[] charArray = chatMessage.toCharArray();
		CustomDictionary.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<CoreDictionary.Attribute>() {
			@Override
			public void hit(int begin, int end, CoreDictionary.Attribute value) {
				System.out.printf("[%d:%d]=%s %s\n", begin, end, new String(charArray, begin, end - begin), value);
			}
		});

		List<Term> listTerm = segment.seg(chatMessage);
		
		for (Term term : listTerm) {
			logger.info("term {} {}", term.word, term.nature);

			if (term.nature.toString().matches(".*(?:n).*")) {
				if (!existExcludeWord(term.word)) {
					Matcher policeMatcher = policeMarkerPattern.matcher(term.word);

					if (policeMatcher.matches()) {
						String count = policeMatcher.group(1);
						String termWord = "popo" + ((region != null && !region.equals("")) ? ":" + region : "");
						if (count != null) {
							redisService.incrKeyByGroup(group, termWord, Long.parseLong(count));
						} else {
							redisService.incrKeyByGroup(group, termWord);
						}
						
					} else {
						String termWord = term.word + ((region != null && !region.equals("")) ? ":" + region : "");

						redisService.incrKeyByGroup(group, termWord);
					}

				}

			}
		}
	}

	private boolean existExcludeWord(String word) {

		if (word.matches(".*#.*")) {
			return true;
		}

		return false;
	}

	@Transactional
	public void processGroupKey() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		Calendar curTime = Calendar.getInstance();
		curTime.set(Calendar.SECOND, 0);
		curTime.set(Calendar.MINUTE, 0);
		String currentTimeKey = sdf.format(curTime.getTime());

		String groupKey = redisService.getActiveGroupKey();

		if (groupKey == null) {
			redisService.addActiveGroupKey(currentTimeKey);
		} else {
			try {
				Date date = sdf.parse(groupKey);
				Calendar dataTime = Calendar.getInstance();
				dataTime.setTime(date);
				dataTime.add(Calendar.HOUR_OF_DAY, 1);

				logger.info("date {} , compareTo {} : {} ", dataTime.getTime().getTime(), curTime.getTime().getTime(), dataTime.getTime().getTime() < curTime.getTime().getTime());
				if (dataTime.getTime().getTime() < curTime.getTime().getTime()) {

					// store currentValue to DB
					Set<Object> objectSet = redisService.getHashGroup(groupKey);

					int currentGroup = telegramChatStatDAO.getMaxGroup();

					for (Object obj : objectSet) {
						String key = (String) obj;
						String region = null;

						Long value = Long.parseLong((String) redisService.getGroupByKey(groupKey, key));

						if (key.indexOf(":") > 0) {
							String[] keyGroup = key.split(":");
							key = keyGroup[0];
							region = keyGroup[1];
						}

						TelegramChatStat tcs = new TelegramChatStat();
						tcs.setCount(value);
						tcs.setKey(key);
						tcs.setYear(curTime.get(Calendar.YEAR));
						tcs.setMonth(curTime.get(Calendar.MONTH) + 1);
						tcs.setDay(curTime.get(Calendar.DAY_OF_MONTH));
						tcs.setHour(curTime.get(Calendar.HOUR_OF_DAY));

						tcs.setMinute(0);

						tcs.setGroup(currentGroup);

						if (region != null) {
							tcs.setRegion(region);
						}

						telegramChatStatDAO.save(tcs);
					}

					// add new key
					redisService.addActiveGroupKey(sdf.format(curTime.getTime()));

					// pop the key
					redisService.popActiveGroupKey();

				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public Map<String, List<StatItem>> getTelegramLiveStat(List<MarkerGeoCoding> regionList) {
		Map<String, List<StatItem>> mapStat = new HashMap<String, List<StatItem>>();

		String activeKey = redisService.getActiveGroupKey();
		if (activeKey != null) {
			Set<Object> keySet = redisService.getHashGroup(activeKey);
			for (Object keyObj : keySet) {
				String key = (String) keyObj;
				for (MarkerGeoCoding regionCode : regionList) {
					String region = regionCode.getLabel();
					if (mapStat.get("total") == null) {
						mapStat.put("total", new LinkedList<StatItem>());
					}
					if (mapStat.get(region) == null) {
						mapStat.put(region, new LinkedList<StatItem>());
					}

					if (region != null && !region.equals("")) {
						if (key.contains(":" + region)) {
							Long value = Long.parseLong((String) redisService.getGroupByKey(activeKey, key));
							StatItem si = new StatItem();
							si.setText(key.replace(":" + region, ""));
							si.setWeight(value);
							mapStat.get(region).add(si);
						}
					}

				}

				Long value = Long.parseLong((String) redisService.getGroupByKey(activeKey, key));
				StatItem si = new StatItem();
				si.setText(key.replaceAll(":.*$", ""));
				si.setWeight(value);

				mapStat.get("total").add(si);
			}

			return mapStat;
		}

		return null;
	}

	public List<StatItem> getTelegram24hrStat() {
		List<String> dayBackTimeList = new ArrayList<String>();

		for (int i = 0; i < 48; i++) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.HOUR_OF_DAY, -i);

			int year = cal.get(Calendar.YEAR);
			int month = cal.get(Calendar.MONTH) + 1;
			int day = cal.get(Calendar.DAY_OF_MONTH);
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			StringBuilder dayBackItem = new StringBuilder();

			dayBackItem.append(year);
			dayBackItem.append(lpad(month));
			dayBackItem.append(lpad(day));
			dayBackItem.append(lpad(hour));

			dayBackTimeList.add(dayBackItem.toString());
		}

		logger.info("day back list {} ", dayBackTimeList);
		List<TelegramChatStat> listTgChatStat = telegramChatStatDAO.getStatByDate(dayBackTimeList);

		logger.info("result {} ", listTgChatStat);
		List<StatItem> listStat = new LinkedList<StatItem>();
		for (TelegramChatStat tcs : listTgChatStat) {
			StatItem si = new StatItem();
			si.setText(tcs.getKey());
			si.setWeight(tcs.getCount());
			si.setLabel(lpad(tcs.getMonth()) + "-" + lpad(tcs.getDay()) + " " + lpad(tcs.getHour()));
			listStat.add(si);
		}

		return listStat;
	}

	private String lpad(int value) {
		return String.format("%02d", value);
	}
}
