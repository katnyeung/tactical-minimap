package org.tactical.minimap.web.result;

import java.util.List;

public class StatResult extends DefaultResult {

	List<StatItem> listStat;

	public List<StatItem> getListStat() {
		return listStat;
	}

	public void setListStat(List<StatItem> listStat) {
		this.listStat = listStat;
	}

	public static StatResult success(List<StatItem> listStat) {

		StatResult sr = new StatResult();
		sr.setStatus("success");
		sr.setListStat(listStat);
		return sr;
	}

}
