package org.tactical.minimap.web.result;

import java.util.List;

import org.tactical.minimap.util.ConstantsUtil;

public class StringListResult extends DefaultResult {
	List<String> list;

	Long lastTimestamp;

	public Long getLastTimestamp() {
		return lastTimestamp;
	}

	public void setLastTimestamp(Long lastTimestamp) {
		this.lastTimestamp = lastTimestamp;
	}

	public List<String> getList() {
		return list;
	}

	public void setList(List<String> list) {
		this.list = list;
	}

	public static StringListResult success(List<String> stringList) {
		StringListResult mlr = new StringListResult();
		mlr.setStatus(ConstantsUtil.STATUS_SUCCESS);
		mlr.setList(stringList);
		return mlr;
	}

}
