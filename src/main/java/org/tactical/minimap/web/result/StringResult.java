package org.tactical.minimap.web.result;

import com.fasterxml.jackson.annotation.JsonInclude;

public class StringResult extends DefaultResult {
	@JsonInclude(JsonInclude.Include.NON_NULL) // ignore null field on this property only
	String text;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public static StringResult success(String text) {

		StringResult sr = new StringResult();
		sr.setText(text);
		return sr;
	}

}
