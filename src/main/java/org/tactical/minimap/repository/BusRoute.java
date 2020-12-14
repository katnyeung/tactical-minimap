package org.tactical.minimap.repository;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "bus_route", indexes = { @Index(name = "route", columnList = "route, stop, seq") })
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class BusRoute {

	@JsonIgnore
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long busRouteSeq;

	int seq;

	double lat;

	double lng;

	String route;

	@Column(nullable = true)
	int stop;

	@Column(nullable = true)
	int minutes;

	public int getSeq() {
		return seq;
	}

	public void setSeq(int seq) {
		this.seq = seq;
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

	public String getRoute() {
		return route;
	}

	public void setRoute(String route) {
		this.route = route;
	}

	public int getMinutes() {
		return minutes;
	}

	public void setMinutes(int minutes) {
		this.minutes = minutes;
	}

	public Long getBusRouteSeq() {
		return busRouteSeq;
	}

	public void setBusRouteSeq(Long busRouteSeq) {
		this.busRouteSeq = busRouteSeq;
	}

	public int getStop() {
		return stop;
	}

	public void setStop(int stop) {
		this.stop = stop;
	}

}