package org.tactical.minimap.scheduled;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SupportTaskScheduler {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

	@Scheduled(cron = "0 0 */4 * * *")
	public void makerManager() {
		logger.info("Cron Task :: Execution Time - " + dateTimeFormatter.format(LocalDateTime.now()));

	}

}
