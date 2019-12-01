package org.tactical.minimap.web.result;

import java.util.List;
import java.util.Map;

public class StatMapResult extends DefaultResult {

	Map<String, List<StatItem>> mapStat;

	public Map<String, List<StatItem>> getMapStat() {
		return mapStat;
	}

	public void setMapStat(Map<String, List<StatItem>> mapStat) {
		this.mapStat = mapStat;
	}

	public static StatMapResult success(Map<String, List<StatItem>> mapStat) {

		StatMapResult sr = new StatMapResult();
		sr.setStatus("success");
		sr.setMapStat(mapStat);
		return sr;
	}

}
