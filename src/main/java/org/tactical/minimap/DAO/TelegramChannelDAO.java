package org.tactical.minimap.DAO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tactical.minimap.repository.TelegramChannel;

public interface TelegramChannelDAO extends JpaRepository<TelegramChannel, Long> {
//tc.status = '" + ConstantsUtil.TELEGRAM_CHANNEL_ACTIVE + "' AND
	@Query("SELECT tc FROM TelegramChannel tc WHERE tc.groupName = :groupName")
	public List<TelegramChannel> findTelegramChannelByGroupName(@Param("groupName") String groupName);

}
