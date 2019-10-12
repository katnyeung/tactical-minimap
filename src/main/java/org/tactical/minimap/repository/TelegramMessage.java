package org.tactical.minimap.repository;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.tactical.minimap.util.Auditable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
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
	@NotNull
	@Size(max = 1)
	String status;

	@NotNull
	@Size(max = 150)
	String groupKey;

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

}