package org.tactical.minimap.repository.marker;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.tactical.minimap.repository.MarkerResponse;
import org.tactical.minimap.util.Auditable;
import org.tactical.minimap.util.MarkerCache;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "marker", indexes = { @Index(name = "latlng", columnList = "layer,lat,lng,status") })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "marker_type")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public abstract class Marker extends Auditable<String> {
	public static final List<Class<? extends Marker>> ClassList = new ArrayList<Class<? extends Marker>>();

	static {
		ClassList.add(InfoMarker.class);
		ClassList.add(WarningMarker.class);
		ClassList.add(DangerMarker.class);

		ClassList.add(MedicalMarker.class);
		ClassList.add(GroupMarker.class);
		ClassList.add(ProtestingMarker.class);

		ClassList.add(PoliceMarker.class);
		ClassList.add(RiotPoliceMarker.class);
		ClassList.add(TearGasMarker.class);
	}

	@Transient
	public abstract String getIcon();

	@Transient
	public abstract int getIconSize();

	@JsonIgnore
	@Transient
	public abstract int getRate();

	@Transient
	public abstract String getType();

	@JsonIgnore
	@Transient
	public abstract long getMarkerExpire();

	@Transient
	public abstract Marker fill(MarkerDTO markerDTO);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long markerId;

	@Column(precision = 20, scale = 10)
	Double lat;

	@Column(precision = 20, scale = 10)
	Double lng;

	@JsonIgnore
	int upVote;

	@JsonIgnore
	int downVote;

	@Column(nullable = true)
	@Lob
	String message;

	@JsonIgnore
	Long expire = (long) 0;

	@JsonIgnore
	@NotNull
	@Size(max = 1)
	private String status;

	@JsonIgnore
	@NotNull
	private String uuid;

	@JsonIgnore
	@NotNull
	private String layer;

	@JsonIgnore
	@OneToMany(mappedBy = "marker", cascade = CascadeType.ALL)
	List<MarkerResponse> markerResponseList;

	@Transient
	MarkerCache markerCache;

	@Transient
	boolean controllable;

	public Long getMarkerId() {
		return markerId;
	}

	public void setMarkerId(Long markerId) {
		this.markerId = markerId;
	}

	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getLng() {
		return lng;
	}

	public void setLng(Double lng) {
		this.lng = lng;
	}

	public Long getExpire() {
		return expire;
	}

	public void setExpire(Long expire) {
		this.expire = expire;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public List<MarkerResponse> getMarkerResponseList() {
		return markerResponseList;
	}

	public void setMarkerResponseList(List<MarkerResponse> markerResponseList) {
		this.markerResponseList = markerResponseList;
	}

	public MarkerCache getMarkerCache() {
		return markerCache;
	}

	public void setMarkerCache(MarkerCache markerCache) {
		this.markerCache = markerCache;
	}

	public boolean isControllable() {
		return controllable;
	}

	public void setControllable(boolean controllable) {
		this.controllable = controllable;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getUpVote() {
		return upVote;
	}

	public void setUpVote(int upVote) {
		this.upVote = upVote;
	}

	public int getDownVote() {
		return downVote;
	}

	public void setDownVote(int downVote) {
		this.downVote = downVote;
	}

	public String getLayer() {
		return layer;
	}

	public void setLayer(String layer) {
		this.layer = layer;
	}

	@Override
	public String toString() {
		return "Marker [markerId=" + markerId + ", lat=" + lat + ", lng=" + lng + ", upVote=" + upVote + ", downVote=" + downVote + ", message=" + message + ", expire=" + expire + ", status=" + status + ", uuid=" + uuid + ", layer=" + layer + ", markerResponseList=" + markerResponseList + ", markerCache=" + markerCache + ", controllable=" + controllable + "]";
	}

}