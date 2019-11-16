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

	String streetAddress;

	String altName;
	
	String thirdName;

	String status;

	String streetType;

	String faciType;

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

	public String getFaciType() {
		return faciType;
	}

	public void setFaciType(String faciType) {
		this.faciType = faciType;
	}

	public void setStreetType(String streetType) {
		this.streetType = streetType;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public List<StreetDataDetail> getStreetDataDetailList() {
		return streetDataDetailList;
	}

	public void setStreetDataDetailList(List<StreetDataDetail> streetDataDetailList) {
		this.streetDataDetailList = streetDataDetailList;
	}

	public String getThirdName() {
		return thirdName;
	}

	public void setThirdName(String thirdName) {
		this.thirdName = thirdName;
	}

	@Override
	public String toString() {
		return "StreetData [streetDataId=" + streetDataId + ", lat=" + lat + ", lng=" + lng + ", streetName=" + streetName + ", streetAddress=" + streetAddress + ", altName=" + altName + ", status=" + status + ", streetType=" + streetType + ", faciType=" + faciType + ", streetDataDetailList="
				+ streetDataDetailList + "]";
	}

}