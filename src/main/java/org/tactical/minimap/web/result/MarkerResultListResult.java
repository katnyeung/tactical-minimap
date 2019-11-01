package org.tactical.minimap.web.result;

import java.util.List;

import org.tactical.minimap.util.ConstantsUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MarkerResultListResult extends DefaultResult {

	@JsonProperty("list")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	List<MarkerResult> markerResultList;

	public List<MarkerResult> getMarkerResultList() {
		return markerResultList;
	}

	public void setMarkerResultList(List<MarkerResult> markerResultList) {
		this.markerResultList = markerResultList;
	}

	public static MarkerResultListResult success(List<MarkerResult> markerResultList) {
		MarkerResultListResult mlr = new MarkerResultListResult();
		mlr.setStatus(ConstantsUtil.STATUS_SUCCESS);
		mlr.setMarkerResultList(markerResultList);
		return mlr;
	}

}
