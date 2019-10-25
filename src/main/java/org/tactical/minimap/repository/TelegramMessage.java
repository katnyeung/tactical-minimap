package org.tactical.minimap.repository;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.tactical.minimap.util.Auditable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "telegram_message", indexes = { @Index(name = "message_index", columnList = "groupKey, id") })
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class TelegramMessage extends Auditable<String> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long telegramMessageId;

	@NotNull
	Long id;

	@JsonIgnore
	@Lob
	@NotNull
	String message;

	@JsonIgnore
	@Lob
	@Column(nullable = true)
	String media;

	@JsonIgnore
	@Lob
	@Column(nullable = true)
	String result;

	@JsonIgnore
	@NotNull
	@Size(max = 1)
	String status;

	@NotNull
	@Size(max = 150)
	String groupKey;

	@NotNull
	Date messagedate;

	public Long getTelegramMessageId() {
		return telegramMessageId;
	}

	public void setTelegramMessageId(Long telegramMessageId) {
		this.telegramMessageId = telegramMessageId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getGroupKey() {
		return groupKey;
	}

	public void setGroupKey(String groupKey) {
		this.groupKey = groupKey;
	}

	public Date getMessagedate() {
		return messagedate;
	}

	public void setMessagedate(Date messagedate) {
		this.messagedate = messagedate;
	}

	public String getMedia() {
		return media;
	}

	public void setMedia(String media) {
		this.media = media;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

}