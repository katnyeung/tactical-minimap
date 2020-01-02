package org.tactical.minimap.util;

import java.util.HashMap;

public class TelegramResult {

	String label;
	HashMap<String, Integer> data;

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public HashMap<String, Integer> getData() {
		return data;
	}

	public void setData(HashMap<String, Integer> data) {
		this.data = data;
	}

}
