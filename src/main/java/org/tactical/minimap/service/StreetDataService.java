package org.tactical.minimap.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tactical.minimap.DAO.StreetDataDAO;
import org.tactical.minimap.DAO.TrafficStatDAO;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.repository.StreetData;
import org.tactical.minimap.repository.TrafficStat;
import org.tactical.minimap.repository.marker.shape.ShapeMarker;
import org.tactical.minimap.util.MarkerGeoCoding;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class StreetDataService {
	public final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	StreetDataDAO streetDataDAO;

	@Autowired
	TrafficStatDAO trafficStatDAO;
	
	@Autowired
	LayerService layerService;
	
	@Autowired
	MarkerService markerService;
	
	@Value("${MAP_FOLDER}")
	String mapFolder;

	Pattern pattern = Pattern.compile("^(.*)\\[(.*),(.*)\\];\\[(.*),(.*):(.*),(.*)\\] .*$");

	@Transactional
	public void save(StreetData streetData) {
		streetDataDAO.save(streetData);
	}

	public StreetData findStreetData(String key) {
		List<StreetData> sdList = streetDataDAO.findStreetDataByName(key);

		if (sdList.size() > 0) {
			return sdList.get(0);
		}

		return null;
	}

	public List<StreetData> findStreetDataList(String streetType, String key) {
		return streetDataDAO.findStreetDataByStreetTypeAndName(streetType, key);
	}

	public String getRegionByLatlng(MarkerGeoCoding latlng) {
		String region = null;

		if (latlng != null) {
			File file = new File(mapFolder + "/pattern_data/v2/region_stat");

			try {
				Scanner scanner = new Scanner(file);

				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					Matcher matcher = pattern.matcher(line);
					if (matcher.find()) {
						String index = matcher.group(1);
						double nwLat = Double.parseDouble(matcher.group(4));
						double nwLng = Double.parseDouble(matcher.group(5));

						double seLat = Double.parseDouble(matcher.group(6));
						double seLng = Double.parseDouble(matcher.group(7));

						if (seLat < latlng.getLat() && latlng.getLat() < nwLat && nwLng < latlng.getLng() && latlng.getLng() < seLng) {
							region = index;
						}

					}

				}
				scanner.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return region;
	}


	public List<MarkerGeoCoding> getStatRegionList() {
		List<MarkerGeoCoding> latlngList = new LinkedList<MarkerGeoCoding>();

		File file = new File(mapFolder + "/pattern_data/v2/region_stat");

		try {
			Scanner scanner = new Scanner(file);

			while (scanner.hasNextLine()) {

				String line = scanner.nextLine();
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					String index = matcher.group(1);
					double lat = Double.parseDouble(matcher.group(2));
					double lng = Double.parseDouble(matcher.group(3));

					MarkerGeoCoding latlng = MarkerGeoCoding.latlng(lat, lng , index);

					latlngList.add(latlng);
				}
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return latlngList;
	}

	public void addTrafficStatMarker(int minute) {
		ObjectMapper om = new ObjectMapper();

		Map<String, String> camMap = new HashMap<String, String>();
		camMap.put("TC604F", "22.363557,114.079930;↖ {in} ↘ {out}");
		camMap.put("K305F", "22.338542,114.152087;← {in} ↘ {out}");
		camMap.put("K202F", "22.319821,114.172500;← {in} → {out}");
		camMap.put("K505F", "22.343103,114.184185;← {in} → {out}");
		camMap.put("K621F", "22.329855,114.210571;↖ {out} ↘ {in}");
		camMap.put("TR111F", "22.364344,114.041566;← {in} → {out}");
		
		Map<String, String> pathMap = new HashMap<String, String>();
		pathMap.put("TC604F", "22.363114,114.080167;22.366190,114.078495");
		pathMap.put("K305F", "22.338468,114.151279;22.338597,114.151916;22.338597,114.151916;22.338364,114.152554");
		pathMap.put("K202F", "22.319856,114.172584;22.319642,114.171646");
		pathMap.put("K505F", "22.343523,114.183488;22.342779,114.185128");
		pathMap.put("K621F", "22.330017,114.210726;22.328733,114.212260");
		pathMap.put("TR111F", "22.363888,114.040988;22.364602,114.041995;22.364602,114.041995;22.364820,114.043495");
		
		Map<String, Double> subGroupMap = new HashMap<String, Double>();
		subGroupMap.put("TC604F", 604.0);
		subGroupMap.put("K305F", 305.0);
		subGroupMap.put("K202F", 202.0);
		subGroupMap.put("K505F", 505.0);
		subGroupMap.put("K621F", 621.0);
		subGroupMap.put("TR111F", 111.0);
		
		TimeZone tz1 = TimeZone.getTimeZone("GMT+08:00");
		Calendar calendarFrom = Calendar.getInstance(tz1);
		calendarFrom.set(Calendar.SECOND, 0);
		calendarFrom.add(Calendar.MINUTE, -minute);

		Calendar calendarTo = Calendar.getInstance(tz1);
		calendarTo.set(Calendar.SECOND, 0);

		for (String cam : camMap.keySet()) {
			String camDetail[] = camMap.get(cam).split(";");

			Double camLat = Double.parseDouble(camDetail[0].split(",")[0]);
			Double camLng = Double.parseDouble(camDetail[0].split(",")[1]);

			String message = camDetail[1];

			String pathLatLng = pathMap.get(cam);

			List<LinkedHashMap<String, Double>> pathShapeList = new LinkedList<LinkedHashMap<String, Double>>();
			
			String[] pathLatLngArray = pathLatLng.split(";");

			for (String pathLatLngItem : pathLatLngArray) {
				String[] latlngObj = pathLatLngItem.split(",");
				LinkedHashMap<String, Double> smd = new LinkedHashMap<String, Double>();
				smd.put("lat", Double.parseDouble(latlngObj[0]));
				smd.put("lng", Double.parseDouble(latlngObj[1]));
				smd.put("group", subGroupMap.get(cam));
				pathShapeList.add(smd);
				
			}			
			
			List<TrafficStat> trafficStatList = trafficStatDAO.findTrafficStatById(cam, calendarFrom.getTime(), calendarTo.getTime());

			logger.info("processing cam {} with lat,lng {},{} : records {}", cam, camLat, camLng, trafficStatList.size());
			
			if(trafficStatList.size() >= minute) {
				Long inMean = (long) 0;
				Long inMax = (long) 0;
				Long inMin = Long.MAX_VALUE;

				Long outMean = (long) 0;
				Long outMax = (long) 0;
				Long outMin = Long.MAX_VALUE;
				
				for(TrafficStat ts : trafficStatList) {
					inMean += ts.getIn();
					outMean += ts.getOut();
					
					if(ts.getIn() > inMax) {
						inMax = ts.getIn();
					}
					if(ts.getIn() < inMin) {
						inMin = ts.getIn();
					}
					
					if(ts.getOut() > outMax) {
						outMax = ts.getOut();
					}
					if(ts.getOut() < outMin) {
						outMin = ts.getOut();
					}
				}
				
				// exclude outliner
				inMean = inMean - inMax - inMin;
				outMean = outMean - outMax - outMin;
				
				inMean /= (trafficStatList.size() - 2);
				outMean /= (trafficStatList.size() - 2);
				
				//builder marker
				message = message.replace("{in}", "" + inMean);
				message = message.replace("{out}", "" + outMean);
				
				SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
				sdf.setTimeZone(tz1);
				
				try {
					ShapeMarker sm = ShapeMarker.class.newInstance();
					
					sm.setIcon("015-pin-8.png");
					sm.setIconSize(20);
					
					Layer layer = layerService.getLayerByKey("traffic");
					
					MarkerDTO markerDTO = new MarkerDTO();
					markerDTO.setLat(camLat);
					markerDTO.setLng(camLng);
					markerDTO.setLayer(layer.getLayerKey());
					markerDTO.setMessage(sdf.format(Calendar.getInstance(tz1).getTime())  + " " + message);
					markerDTO.setUuid("TRAFFIC_STAT");
					markerDTO.setShapeType("polyline_group");
					markerDTO.setType("info");
					
					if(inMean + outMean > 40) {
						markerDTO.setColor("#ff0000");
					} else if(inMean + outMean > 20) {
						markerDTO.setColor("#ffff66");
					} else {
						markerDTO.setColor("#ffffff");
					}
					
					markerDTO.setShapeList(om.writeValueAsString(pathShapeList));

					markerService.addMarker(layer, markerDTO, sm);
					
				} catch (InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				
				
			}
		}
		
	}
}
