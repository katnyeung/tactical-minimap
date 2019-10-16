package org.tactical.minimap.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tactical.minimap.DAO.TelegramMessageDAO;
import org.tactical.minimap.DAO.TelegramMessageRuleDAO;
import org.tactical.minimap.repository.TelegramMessage;
import org.tactical.minimap.repository.TelegramMessageRule;

@Service
public class TelegramMessageService {

	@Autowired
	TelegramMessageDAO telegramMessageDAO;

	@Autowired
	TelegramMessageRuleDAO telegramMessageRuleDAO;

	public List<TelegramMessage> getPendingTelegramMessage() {
		return telegramMessageDAO.findPendingTelegramMessage();
	}

	public List<TelegramMessageRule> getActiveTelegramMessageRules() {
		return telegramMessageRuleDAO.findActiveTelegramMessageRule();
	}
}
