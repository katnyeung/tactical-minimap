package org.tactical.minimap.repository.marker;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.tactical.minimap.repository.MarkerResponse;
import org.tactical.minimap.repository.User;
import org.tactical.minimap.util.Auditable;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "marker", indexes = { @Index(name = "latlng", columnList = "lat,lng") })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "marker_type")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public abstract class Marker extends Auditable<String> {
	public static final List<Class<? extends Marker>> MarkerClassList = new ArrayList<Class<? extends Marker>>();

	static {
		MarkerClassList.add(InfoMarker.class);
		MarkerClassList.add(WarningMarker.class);
	}

	@Transient
	public abstract String getType();

	@Transient
	public abstract Marker fill(MarkerDTO markerDTO);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long markerId;

	@Column(precision = 20, scale = 10)
	Double lat;

	@Column(precision = 20, scale = 10)
	Double lng;

	Long expire = (long) 0;

	@JsonIgnore
	@NotNull
	@Size(max = 1)
	private String status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", referencedColumnName = "userId")
	User user;

	@OneToMany(mappedBy = "marker", cascade = CascadeType.ALL)
	List<MarkerResponse> markerResponseList;

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

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

}