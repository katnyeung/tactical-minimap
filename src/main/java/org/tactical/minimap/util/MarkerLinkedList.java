package org.tactical.minimap.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarkerLinkedList {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	double lat;
	double lng;

	MarkerLinkedList nextMarker;

	MarkerCache markerCache;

	public MarkerLinkedList getNextMarker() {
		return nextMarker;
	}

	public void setNextMarker(MarkerLinkedList nextMarker) {
		this.nextMarker = nextMarker;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLng() {
		return lng;
	}

	public void setLng(double lng) {
		this.lng = lng;
	}

	public MarkerCache getMarkerCache() {
		return markerCache;
	}

	public void setMarkerCache(MarkerCache markerCache) {
		this.markerCache = markerCache;
	}

	public static MarkerLinkedList latlng(double lat, double lng) {
		MarkerLinkedList mll = new MarkerLinkedList();
		mll.setLat(lat);
		mll.setLng(lng);
		return mll;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}

		if (o.getClass() == this.getClass()) {

			MarkerLinkedList mll = (MarkerLinkedList) o;
			double fromLat = mll.getLat() - 0.0025;
			double fromLng = mll.getLng() - 0.0025;

			double toLat = mll.getLat() + 0.0025;
			double toLng = mll.getLng() + 0.0025;
			
			if (fromLat < this.getLat() && this.getLat() < toLat && fromLng < this.getLng() && this.getLng() < toLng) {
				return true;
			} else {
				return false;
			}

		} else {
			return false;
		}
	}

	public boolean hasNext() {
		return nextMarker != null;
	}

	public MarkerLinkedList next() {
		return nextMarker;
	}

	@Override
	public String toString() {
		return "MarkerLinkedList [lat=" + lat + ", lng=" + lng + ", markerCache=" + markerCache + "]";
	}

}
