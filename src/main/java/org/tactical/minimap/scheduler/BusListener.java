package org.tactical.minimap.scheduler;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListeners;
import org.springframework.stereotype.Component;
import org.tactical.minimap.repository.BusRoute;
import org.tactical.minimap.repository.Layer;
import org.tactical.minimap.repository.marker.BusMarker;
import org.tactical.minimap.service.BusRouteService;
import org.tactical.minimap.service.LayerService;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.util.BusRouteMessage;
import org.tactical.minimap.web.DTO.MarkerDTO;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BusListener {
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	LayerService layerService;
	
	@Autowired
	MarkerService markerService;
	
	@Autowired
	BusRouteService busRouteService;
	
	ObjectMapper om = new ObjectMapper();
	
	@KafkaListeners({@KafkaListener(topics="busRoute")})
	public void listen(ConsumerRecord<?, ?> record) throws JsonParseException, JsonMappingException, IOException {
		String key = record.key().toString();
		String value = record.value().toString();
		
		Pattern pattern = Pattern.compile("(\\d*)-(\\d*)");
		Matcher matcher = pattern.matcher(key);
		
		if(matcher.find()) {
			String route = matcher.group(1);
			int stop = Integer.parseInt(matcher.group(2));

			List<BusRouteMessage> busRouteMessageList = om.readValue(value, new TypeReference<List<BusRouteMessage>>() {});

			for (BusRouteMessage busRouteMessage : busRouteMessageList) {
				int minutes = Integer.parseInt(busRouteMessage.getMinutes());
				String remark = busRouteMessage.getRemark();
				
				logger.info("route {}", route);
				logger.info("stop {}", stop);
				logger.info("minutes {}", minutes);
				logger.info("remark {}", remark);

				if (!busRouteMessage.getRemark().equals("Scheduled")) {
					// fetch lat lng
					BusRoute busRoute = busRouteService.getBusRoute(route, stop, minutes);
					
					if(busRoute != null) {
						
						BusMarker busMarker = new BusMarker();

						Layer layer = layerService.getLayerByKey("bus");

						MarkerDTO markerDTO = new MarkerDTO();
						markerDTO.setLat(busRoute.getLat());
						markerDTO.setLng(busRoute.getLng());
						markerDTO.setLayer(layer.getLayerKey());
						markerDTO.setMessage(remark);
						markerDTO.setUuid("BUS_ROUTE_" + route);


						markerService.addMarker(layer, markerDTO, busMarker);
					}

				}
				

			}
		}
		
		
	}
}
/*		

List<BusRouteMessage> = om.readValuerecord.value();

BusMarker busMarker = new BusMarker();

Layer layer = layerService.getLayerByKey("traffic");

MarkerDTO markerDTO = new MarkerDTO();
markerDTO.setLat(camLat);
markerDTO.setLng(camLng);
markerDTO.setLayer(layer.getLayerKey());
markerDTO.setMessage(sdf.format(Calendar.getInstance(tz1).getTime())  + " " + message);
markerDTO.setUuid("TRAFFIC_STAT");
markerDTO.setShapeType("polyline_group");
markerDTO.setType("bus");

markerDTO.setColor("#ff0000");


markerService.addMarker(layer, markerDTO, busMarker);

*/