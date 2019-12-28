package org.tactical.minimap.DAO;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tactical.minimap.repository.TelegramChatStat;
import org.tactical.minimap.web.result.StatItem;

public interface TelegramChatStatDAO extends JpaRepository<TelegramChatStat, Long> {

	@Query(value = "SELECT ifnull(MAX(tcs.group),0) + 1 FROM telegram_chat_stat tcs", nativeQuery = true)
	int getMaxGroup();

	@Query("FROM TelegramChatStat tcs WHERE tcs.count > :count AND CONCAT(tcs.year,LPAD(tcs.month,2,0),LPAD(tcs.day,2,0),LPAD(tcs.hour,2,0)) IN :dayBackTimeList GROUP BY tcs.key ORDER BY CONCAT(tcs.year,LPAD(tcs.month,2,0),LPAD(tcs.day,2,0),LPAD(tcs.hour,2,0)) ASC")
	List<TelegramChatStat> getStatByDate(@Param("dayBackTimeList") List<String> dayBackTimeList, @Param("count") Long count);

	@Query("SELECT new org.tactical.minimap.web.result.StatItem(concat(LPAD(tcs.year,4,0), '-', LPAD(tcs.month,2,0),'-', LPAD(tcs.day,2,0),' ', LPAD(tcs.hour,2,0) , ':00:00') , sum(tcs.count), '') FROM TelegramChatStat tcs WHERE (tcs.region = :street or tcs.key = :street) AND tcs.createdate > :dateCutOff GROUP BY tcs.hour, tcs.day, tcs.month, tcs.year ")
	List<StatItem> getStatByKeyword(@Param("street") String street, @Param("dateCutOff") Date dateCutOff);

}
