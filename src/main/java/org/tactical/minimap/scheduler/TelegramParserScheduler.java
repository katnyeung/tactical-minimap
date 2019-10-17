package org.tactical.minimap.scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.repository.TelegramMessage;
import org.tactical.minimap.repository.TelegramMessageRule;
import org.tactical.minimap.repository.marker.InfoMarker;
import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.repository.marker.PoliceMarker;
import org.tactical.minimap.service.LayerService;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.service.TelegramMessageService;
import org.tactical.minimap.web.DTO.MarkerDTO;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

@Service
public class TelegramParserScheduler {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	TelegramMessageService telegrameMessageService;

	@Autowired
	MarkerService markerService;

	@Autowired
	LayerService layerService;

	@Autowired
	RedisService redisService;

	@Value("${API_KEY}")
	String apiKey;

	@Async
	@Scheduled(fixedRate = 300000)
	public void doParse() throws IOException, ParseException {
		StandardAnalyzer analyzer = new StandardAnalyzer();
		Directory index = new ByteBuffersDirectory();

		IndexWriterConfig config = new IndexWriterConfig(analyzer);

		IndexWriter w = new IndexWriter(index, config);

		processData(w, "data/region.chi", 10);

		processData(w, "data/subDistrict.chi", 10);

		processData(w, "data/building.chi", 30);

		processData(w, "data/estate.chi", 20);

		processData(w, "data/street.chi", 15);

		processData(w, "data/village.chi", 20);

		w.close();

		// time pattern
		Pattern timePattern = Pattern.compile("([0-9][0-9]\\:?[0-9][0-9])");
		// marker pattern
		Pattern policeMarkerPattern = Pattern.compile("([0-9])*?名*?(?:閃燈)*?(?:藍|白)*?(?:大|小)*?(EU|eu|衝|警車|警|籠|豬籠|防暴|軍裝)");

		// get message
		List<TelegramMessage> telegramMessageList = telegrameMessageService.getPendingTelegramMessage();
		logger.info("fetcing telegram Message List from DB {}", telegramMessageList);

		List<Long> okIdList = new ArrayList<Long>();
		List<Long> notOkIdList = new ArrayList<Long>();

		for (TelegramMessage telegramMessage : telegramMessageList) {
			String message = telegramMessage.getMessage();

			Matcher matcher = timePattern.matcher(message);

			if (matcher.find()) {
				// String time = matcher.group(1).replaceAll(":", "");

				// convert message
				HashMap<String, String> messageRules = getRules();

				for (String mrKey : messageRules.keySet()) {
					message = message.replaceAll(mrKey, messageRules.get(mrKey));
				}

				logger.info("processing message {}", message);

				// process message
				Query q = new QueryParser("name", analyzer).parse(message);
				
				IndexReader reader = DirectoryReader.open(index);
				IndexSearcher searcher = new IndexSearcher(reader);
				TopDocs docs = searcher.search(q, 50);
				
				ScoreDoc[] hits = docs.scoreDocs;
				System.out.println("Found " + hits.length + " hits.");
				for(int i=0;i<hits.length;++i) {
				    int docId = hits[i].doc;
				    
				    Document d = searcher.doc(docId);
				    System.out.println((i + 1) + ". " + d.get("name") + "," + hits[i].score);
				}
/*
				// get geo location
				HttpResponse<JsonNode> response = Unirest.get("https://maps.googleapis.com/maps/api/geocode/json").queryString("key", apiKey).queryString("address", String.join(" ", keyMap.keySet())).asJson();

				JSONObject body = response.getBody().getObject();

				logger.info("google return {} ", response.getBody().toPrettyString());

				if (body.get("status").equals("OK")) {
					JSONObject latlng = body.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location");

					// add marker
					Layer layer = layerService.getLayerByKey("scout");

					MarkerDTO markerDTO = new MarkerDTO();
					markerDTO.setLat(latlng.getDouble("lat"));
					markerDTO.setLng(latlng.getDouble("lng"));
					markerDTO.setLayer("scout");
					markerDTO.setMessage(message);
					markerDTO.setUuid("TELEGRAM_BOT");

					try {
						// analyst target icon
						int level = 1;
						Marker marker = null;

						Matcher isPoliceMatcher = policeMarkerPattern.matcher(message);

						if (isPoliceMatcher.find()) {
							if (isPoliceMatcher.groupCount() > 1) {
								level = Integer.parseInt(isPoliceMatcher.group(1));
							}
							marker = PoliceMarker.class.newInstance();
							marker.setLevel(level);
						} else {
							marker = InfoMarker.class.newInstance();
						}

						logger.info("adding marker " + marker.getType());

						markerService.addMarker(layer, markerDTO, marker);

						okIdList.add(telegramMessage.getTelegramMessageId());
					} catch (InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
						notOkIdList.add(telegramMessage.getTelegramMessageId());
					}

				} else {
					notOkIdList.add(telegramMessage.getTelegramMessageId());

				}
				*/
			}
		}

		if (okIdList.size() > 0)
			telegrameMessageService.updateTelegramMessageOK(okIdList);

		if (notOkIdList.size() > 0)
			telegrameMessageService.updateTelegramMessageNotOK(notOkIdList);

	}

	private HashMap<String, String> getRules() {
		HashMap<String, String> ruleMap = new HashMap<String, String>();

		List<TelegramMessageRule> telegramMessageRuleList = telegrameMessageService.getActiveTelegramMessageRules();

		for (TelegramMessageRule tmr : telegramMessageRuleList) {
			ruleMap.put(tmr.getRule(), tmr.getGoal());
		}

		return ruleMap;
	}

	private void processData(IndexWriter w, String filePath, int i) throws IOException {
		InputStream in = this.getClass().getClassLoader().getResourceAsStream(filePath);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = reader.readLine();
		while (line != null) {
			line = reader.readLine();
			if (line != null) {
				Document doc = new Document();
				doc.add(new TextField("name", line, Field.Store.YES));
				w.addDocument(doc);
			}
		}
		reader.close();
	}

	public static int BoyerMooreHorspoolSimpleSearch(char[] pattern, char[] text) {
		int patternSize = pattern.length;
		int textSize = text.length;

		int i = 0, j = 0;

		while ((i + patternSize) <= textSize) {
			j = patternSize - 1;
			while (text[i + j] == pattern[j]) {
				j--;
				if (j < 0)
					return i;
			}
			i++;
		}
		return -1;
	}

}
