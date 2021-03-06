package org.tactical.minimap.DAO;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tactical.minimap.repository.TelegramMessage;
import org.tactical.minimap.util.ConstantsUtil;

public interface TelegramMessageDAO extends JpaRepository<TelegramMessage, Long> {

	@Query("SELECT t FROM TelegramMessage t WHERE t.messageType IN :messageTypeList AND t.status = '" + ConstantsUtil.TELEGRAM_MESSAGE_PENDING + "' ORDER BY messageDate ASC")
	public List<TelegramMessage> findPendingTelegramMessage(@Param("messageTypeList") List<String> messageTypeList);

	@Modifying
	@Query("UPDATE TelegramMessage t SET t.status = '" + ConstantsUtil.TELEGRAM_MESSAGE_PROCESSED_OK + "' WHERE t.telegramMessageId IN :idList")
	public void updateProcessOK(@Param("idList") List<Long> idList);

	@Modifying
	@Query("UPDATE TelegramMessage t SET t.status = '" + ConstantsUtil.TELEGRAM_MESSAGE_PROCESSED_NOT_OK + "' WHERE t.telegramMessageId IN :idList")
	public void updateProcessNotOK(@Param("idList") List<Long> idList);

	@Query("SELECT t FROM TelegramMessage t WHERE t.id = :id AND t.groupKey = :group ORDER BY messageDate ASC")
	public List<TelegramMessage> findMessageByIdAndGroup(Long id, String group);
	
	@Query(value = "SELECT t.lastupdatedate FROM telegram_message t WHERE t.group_key NOT IN ('rthk','881903') ORDER BY t.lastupdatedate DESC LIMIT 1", nativeQuery = true)
	public Date getLastestLastUpdateDate();
}
