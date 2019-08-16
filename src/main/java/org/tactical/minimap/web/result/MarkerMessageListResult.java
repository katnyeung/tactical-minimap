package org.tactical.minimap.web.result;

import java.util.List;

import org.tactical.minimap.util.ConstantsUtil;

public class MarkerMessageListResult extends DefaultResult {
	List<MarkerMessage> markerMessageList;

	public List<MarkerMessage> getMarkerMessageList() {
		return markerMessageList;
	}

	public void setMarkerMessageList(List<MarkerMessage> markerMessageList) {
		this.markerMessageList = markerMessageList;
	}

	public static MarkerMessageListResult success(List<MarkerMessage> markerMessageList, Long timestamp) {
		MarkerMessageListResult mlr = new MarkerMessageListResult();
		mlr.setStatus(ConstantsUtil.STATUS_SUCCESS);
		mlr.setMarkerMessageList(markerMessageList);
		mlr.setRemarks("" + timestamp);
		return mlr;
	}

}
