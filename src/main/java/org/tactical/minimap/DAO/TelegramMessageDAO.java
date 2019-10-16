package org.tactical.minimap.DAO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tactical.minimap.repository.TelegramMessage;
import org.tactical.minimap.util.ConstantsUtil;

public interface TelegramMessageDAO extends JpaRepository<TelegramMessage, Long> {

	@Query("SELECT t FROM TelegramMessage t WHERE t.status = '" + ConstantsUtil.TELEGRAM_MESSAGE_PENDING + "' ORDER BY messageDate ASC")
	public List<TelegramMessage> findPendingTelegramMessage();

	@Modifying
	@Query("UPDATE TelegramMessage t SET t.status = :status WHERE t.telegramMessageId = :telegramMessageId")
	public void updateStatusUpDown(@Param("telegramMessageId") Long telegramMessageId, @Param("status") String status);

}
