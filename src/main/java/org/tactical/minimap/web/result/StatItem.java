package org.tactical.minimap.web.result;

import com.fasterxml.jackson.annotation.JsonInclude;

public class StatItem {

	String text;

	Long weight;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	String label;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Long getWeight() {
		return weight;
	}

	public void setWeight(Long weight) {
		this.weight = weight;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

}
