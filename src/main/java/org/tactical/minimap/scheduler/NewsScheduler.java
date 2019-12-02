package org.tactical.minimap.scheduler;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.tactical.minimap.repository.TelegramMessage;
import org.tactical.minimap.service.TelegramMessageService;

@Component
public class NewsScheduler {

	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	TelegramMessageService telegramMessageService;

	@Async
	@Scheduled(fixedRate = 10000)
	public void newParser() throws IOException {

		TimeZone tz1 = TimeZone.getTimeZone("GMT+8");
		Calendar curTime = Calendar.getInstance(tz1);
		Pattern linkPattern = Pattern.compile("newsdetail.aspx\\?ItemId=(\\d*)&csid=461_1600");
		Pattern timePattern = Pattern.compile(".*\\(([0-9]{1,2}):([0-9]{1,2}).*\\)");

		Document doc = Jsoup.connect("https://www.881903.com/Page/ZH-TW/News.aspx?csid=461_1600").get();
		logger.info("{}", doc.title());

		Elements newsHeadlines = doc.select(".newsAboutArticleRow");
		for (Element headline : newsHeadlines) {
			Elements linkElements = headline.select("a");
			for (Element link : linkElements) {
				int hour = 0;
				int minute = 0;
				Matcher matcher = linkPattern.matcher(link.attr("href"));
				Matcher timeMatcher = timePattern.matcher(link.text());
				if (timeMatcher.matches()) {
					hour = Integer.parseInt(timeMatcher.group(1));
					minute = Integer.parseInt(timeMatcher.group(2));
				}
				if (matcher.matches()) {
					long id = Long.parseLong(matcher.group(1));
					// logger.info("id : {} ", id);
					List<TelegramMessage> tgList = telegramMessageService.findMessageByIdAndGroup(id, "881903");
					if (tgList.size() > 0) {

					} else {
						logger.info("{} , {} ", link.attr("href"), link.text());
						Document detailDoc = Jsoup.connect("https://www.881903.com/Page/ZH-TW/" + link.attr("href")).get();
						Elements newsDetail = detailDoc.select("#divnewsTextContent");
						logger.info("detail : {} ", newsDetail.text());
						TelegramMessage message = new TelegramMessage();
						message.setGroupKey("881903");
						message.setId(id);
						if (hour > 0 && minute > 0) {
							message.setMessage(hour + "" + minute + " " + newsDetail.text());
						} else {
							message.setMessage(curTime.get(Calendar.HOUR_OF_DAY) + "" + curTime.get(Calendar.MINUTE) + newsDetail.text());
						}
						message.setStatus("P");
						message.setMessageType("S");
						message.setMessagedate(curTime.getTime());
						logger.info("{}", message);

						telegramMessageService.saveTelegramMessage(message);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}
