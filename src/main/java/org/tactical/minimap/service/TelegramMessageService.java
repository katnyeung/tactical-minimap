package org.tactical.minimap.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tactical.minimap.DAO.TelegramChannelDAO;
import org.tactical.minimap.DAO.TelegramMessageDAO;
import org.tactical.minimap.DAO.TelegramMessageRuleDAO;
import org.tactical.minimap.repository.TelegramChannel;
import org.tactical.minimap.repository.TelegramMessage;
import org.tactical.minimap.repository.TelegramMessageRule;

@Service
public class TelegramMessageService {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	TelegramMessageDAO telegramMessageDAO;

	@Autowired
	TelegramMessageRuleDAO telegramMessageRuleDAO;

	@Autowired
	TelegramChannelDAO telegramChannelDAO;

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

	public List<TelegramMessage> getPendingTelegramMessage() {
		return telegramMessageDAO.findPendingTelegramMessage();
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
}
