package org.tactical.minimap.repository;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.tactical.minimap.util.Auditable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class StreetData extends Auditable<String> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long streetDataId;

	double lat;

	double lng;

	String streetName;

	String altName;

	String status;

	String streetType;
	
	@OneToMany(mappedBy = "streetData", cascade = CascadeType.ALL)
	List<StreetDataDetail> streetDataDetailList;

	public Long getStreetDataId() {
		return streetDataId;
	}

	public void setStreetDataId(Long streetDataId) {
		this.streetDataId = streetDataId;
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

	public String getStreetName() {
		return streetName;
	}

	public void setStreetName(String streetName) {
		this.streetName = streetName;
	}

	public String getAltName() {
		return altName;
	}

	public void setAltName(String altName) {
		this.altName = altName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStreetType() {
		return streetType;
	}

	public void setStreetType(String streetType) {
		this.streetType = streetType;
	}

	public List<StreetDataDetail> getStreetDataDetailList() {
		return streetDataDetailList;
	}

	public void setStreetDataDetailList(List<StreetDataDetail> streetDataDetailList) {
		this.streetDataDetailList = streetDataDetailList;
	}

}