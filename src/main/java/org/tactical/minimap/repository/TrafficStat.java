package org.tactical.minimap.repository;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.tactical.minimap.util.Auditable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "traffic_stat", indexes = { @Index(name = "id_datetime", columnList = "id, createdate") })
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class TrafficStat extends Auditable<String> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long trafficStatId;

	int year;
	int month;
	int day;
	int weekday;

	int hour;
	int minute;

	String id;

	Long in;
	Long out;

	int temperature;
	int humidity;
	int weather;
	
	public Long getTrafficStatId() {
		return trafficStatId;
	}

	public void setTrafficStatId(Long trafficStatId) {
		this.trafficStatId = trafficStatId;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public int getDay() {
		return day;
	}

	public void setDay(int day) {
		this.day = day;
	}

	public int getWeekday() {
		return weekday;
	}

	public void setWeekday(int weekday) {
		this.weekday = weekday;
	}

	public int getHour() {
		return hour;
	}

	public void setHour(int hour) {
		this.hour = hour;
	}

	public int getMinute() {
		return minute;
	}

	public void setMinute(int minute) {
		this.minute = minute;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Long getIn() {
		return in;
	}

	public void setIn(Long in) {
		this.in = in;
	}

	public Long getOut() {
		return out;
	}

	public void setOut(Long out) {
		this.out = out;
	}

	public int getTemperature() {
		return temperature;
	}

	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}

	public int getHumidity() {
		return humidity;
	}

	public void setHumidity(int humidity) {
		this.humidity = humidity;
	}

	public int getWeather() {
		return weather;
	}

	public void setWeather(int weather) {
		this.weather = weather;
	}

}