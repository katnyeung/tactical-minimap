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
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.repository.MarkerResponse;
import org.tactical.minimap.repository.TelegramMessage;
import org.tactical.minimap.repository.marker.livestream.FBLiveStreamMarker;
import org.tactical.minimap.repository.marker.livestream.ImageMarker;
import org.tactical.minimap.repository.marker.livestream.TwtichLiveStreamMarker;
import org.tactical.minimap.repository.marker.shape.ShapeMarker;
import org.tactical.minimap.util.Auditable;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.util.MarkerCache;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Entity
@Table(name = "marker", indexes = { @Index(name = "latlng", columnList = "status,layer_id,lat,lng,createdate") })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "marker_type")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public abstract class Marker extends Auditable<String> {
	public static final List<Class<? extends Marker>> ClassList = new ArrayList<Class<? extends Marker>>();

	String icon;
	int iconSize;

	static {

		ClassList.add(InfoMarker.class);
		ClassList.add(RedInfoMarker.class);
		ClassList.add(YellowInfoMarker.class);

		ClassList.add(WarningMarker.class);
		ClassList.add(DangerMarker.class);

		ClassList.add(FlagBlackMarker.class);
		ClassList.add(FlagBlueMarker.class);
		ClassList.add(FlagOrangeMarker.class);
		ClassList.add(FlagRedMarker.class);
		ClassList.add(FlagYellowMarker.class);

		ClassList.add(MedicalMarker.class);
		ClassList.add(GroupMarker.class);
		ClassList.add(SupplyMarker.class);

		ClassList.add(BlockadeMarker.class);
		ClassList.add(ConflictMarker.class);
		ClassList.add(PedestrianMarker.class);

		ClassList.add(PoliceMarker.class);
		ClassList.add(RiotPoliceMarker.class);
		ClassList.add(WaterTruckMarker.class);
		ClassList.add(TearGasMarker.class);

		ClassList.add(FBLiveStreamMarker.class);
		ClassList.add(TwtichLiveStreamMarker.class);

		ClassList.add(ShapeMarker.class);
		ClassList.add(ImageMarker.class);
	}

	public String getIcon() {
		return icon;
	}

	public int getIconSize() {
		return iconSize;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public void setIconSize(int iconSize) {
		this.iconSize = iconSize;
	}

	@JsonIgnore
	@Transient
	public abstract int getUpRate();

	@JsonIgnore
	@Transient
	public abstract int getDownRate();

	@JsonIgnore
	@Transient
	public abstract int getAddDelay();

	@JsonIgnore
	@Transient
	public abstract int getVoteDelay();

	@Transient
	public abstract String getType();

	@Transient
	public abstract int getPulseRate();

	@JsonIgnore
	@Transient
	public abstract long getMarkerExpire();

	@Transient
	public Marker fill(MarkerDTO markerDTO) {
		this.setLat(markerDTO.getLat());
		this.setLng(markerDTO.getLng());
		this.setMessage(markerDTO.getMessage());
		this.setExpire(getMarkerExpire());
		this.setStatus(ConstantsUtil.MARKER_STATUS_ACTIVE);
		this.setUuid(markerDTO.getUuid());
		this.setHour(markerDTO.getHour());
		this.setMinute(markerDTO.getMinute());
		return this;
	}

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

	@Column(nullable = true)
	Integer level = 1;

	Long expire = (long) 0;

	@NotNull
	@Size(max = 1)
	private String status;

	@JsonIgnore
	@NotNull
	private String uuid;

	@JsonInclude(Include.NON_NULL)
	@Column(nullable = true)
	Integer hour;

	@JsonInclude(Include.NON_NULL)
	@Column(nullable = true)
	Integer minute;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "layer_id", referencedColumnName = "layerId")
	Layer layer;

	@JsonIgnore
	@OneToMany(mappedBy = "marker", cascade = CascadeType.ALL)
	List<MarkerResponse> markerResponseList;

	@JsonInclude(Include.NON_NULL)
	@Transient
	MarkerCache markerCache;

	@Transient
	boolean controllable;

	@Transient
	double opacity;

	@Transient
	List<String> keywordList;

	@Transient
	String region;
	
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "telegram_message_id", referencedColumnName = "telegramMessageId")
	TelegramMessage telegramMessage;

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

	public Layer getLayer() {
		return layer;
	}

	public void setLayer(Layer layer) {
		this.layer = layer;
	}

	public double getOpacity() {
		return opacity;
	}

	public void setOpacity(double opacity) {
		this.opacity = opacity;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	@Override
	public String toString() {
		return "Marker [markerId=" + markerId + "]";
	}

	@JsonIgnore
	public String getDescription() {
		return "Marker";
	}

	@JsonIgnore
	public String getLogMessage() {
		return this.getDescription() + ":" + this.getMessage();
	}

	public Integer getHour() {
		return hour;
	}

	public void setHour(Integer hour) {
		this.hour = hour;
	}

	public Integer getMinute() {
		return minute;
	}

	public void setMinute(Integer minute) {
		this.minute = minute;
	}

	public TelegramMessage getTelegramMessage() {
		return telegramMessage;
	}

	public void setTelegramMessage(TelegramMessage telegramMessage) {
		this.telegramMessage = telegramMessage;
	}

	public List<String> getKeywordList() {
		return keywordList;
	}

	public void setKeywordList(List<String> keywordList) {
		this.keywordList = keywordList;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

}