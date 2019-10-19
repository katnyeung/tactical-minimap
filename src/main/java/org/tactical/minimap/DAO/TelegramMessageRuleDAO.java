package org.tactical.minimap.DAO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.tactical.minimap.repository.TelegramMessageRule;
import org.tactical.minimap.util.ConstantsUtil;

public interface TelegramMessageRuleDAO extends JpaRepository<TelegramMessageRule, Long> {

	@Query("SELECT tr FROM TelegramMessageRule tr WHERE tr.status = '" + ConstantsUtil.TELEGRAM_MESSAGE_RULE_ACTIVE + "' ORDER BY tr.priority DESC")
	public List<TelegramMessageRule> findActiveTelegramMessageRule();

}
