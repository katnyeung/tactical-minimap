package org.tactical.minimap.repository;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.tactical.minimap.util.Auditable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class TelegramMessageRule extends Auditable<String> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long telegramMessageRuleId;

	@Lob
	String rule;

	@Lob
	String goal;

	@NotNull
	@Size(max = 1)
	String status;

	Integer priority;

	public Long getTelegramMessageRuleId() {
		return telegramMessageRuleId;
	}

	public void setTelegramMessageRuleId(Long telegramMessageRuleId) {
		this.telegramMessageRuleId = telegramMessageRuleId;
	}

	public String getRule() {
		return rule;
	}

	public void setRule(String rule) {
		this.rule = rule;
	}

	public String getGoal() {
		return goal;
	}

	public void setGoal(String goal) {
		this.goal = goal;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

}