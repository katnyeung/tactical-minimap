package org.tactical.minimap.web.result;

import java.util.List;

import org.tactical.minimap.repository.marker.Marker;
import org.tactical.minimap.util.ConstantsUtil;

public class MarkerListResult extends DefaultResult {
	List<Marker> markerList;

	public List<Marker> getMarkerList() {
		return markerList;
	}

	public void setMarkerList(List<Marker> markerList) {
		this.markerList = markerList;
	}

	public static MarkerListResult success(List<Marker> markerList) {
		MarkerListResult mlr = new MarkerListResult();
		mlr.setStatus(ConstantsUtil.STATUS_SUCCESS);
		mlr.setMarkerList(markerList);
		return mlr;
	}

}
