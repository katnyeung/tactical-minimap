package org.tactical.minimap.scheduler;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.tactical.minimap.DAO.TrafficStatDAO;
import org.tactical.minimap.repository.TrafficStat;
import org.tactical.minimap.service.StreetDataService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

@Component
public class TrafficScheduler {

	public final Logger logger = LoggerFactory.getLogger(getClass());
	public final ObjectMapper om = new ObjectMapper();
	public final static int convPixel = 5;

	@Autowired
	StringRedisTemplate redisTemplate;
	
	@Autowired
	TrafficStatDAO tsDAO;

	@Autowired
	StreetDataService sdService;
	
	@Async
	@Scheduled(fixedRate = 600000)
	public void addTrafficStatMarker() {
		logger.info("adding traffic stat marker");
		sdService.addTrafficStatMarker(10);
	}

	@Async
	@Scheduled(fixedRate = 60000)
	public void dataParser() {

		try {
			HashMap<String, String> inMap = new HashMap<String, String>();
			HashMap<String, String> outMap = new HashMap<String, String>();
			HashMap<String, String> tempMap = new HashMap<String, String>();
			
			inMap.put("TC604F", "0:0,200:200,0:200,400:200");
			outMap.put("TC604F", "0:200,200:400,200:200,400:400");
			tempMap.put("TC604F", "Tsing Yi");
			
			inMap.put("K305F", "0:0,200:200,0:200,400:200");
			outMap.put("K305F", "0:200,200:400,200:200,400:400");
			tempMap.put("K305F", "Sham Shui Po");
			
			inMap.put("K202F", "0:245,190:395,190:245,395:395");
			outMap.put("K202F", "0:0,190:245,190:0,395:245");
			tempMap.put("K202F", "Hong Kong Observatory");
			
			inMap.put("K505F", "0:0,200:200,0:200,400:200");
			outMap.put("K505F", "0:200,200:400,200:200,400:400");
			tempMap.put("K505F", "Wong Tai Sin");
			
			inMap.put("K621F", "0:0,200:215,200:0,400:215");
			outMap.put("K621F", "0:215,200:400,200:215,400:400");
			tempMap.put("K621F", "Kwun Tong");
			
			inMap.put("TR111F", "0:0,200:180,200:0,400:180");
			outMap.put("TR111F", "0:180,200:400,200:180,400:400");
			tempMap.put("TR111F", "Tuen Mun");
			
			processImageToBackground("TC604F", 20, 90.0);
			calculateForegroundTraffic("TC604F", inMap, outMap, tempMap, 25, 2, 90.0);

			processImageToBackground("K305F", 20, -60.0);
			calculateForegroundTraffic("K305F", inMap, outMap, tempMap, 30, 8, -60.0);

			processImageToBackground("K202F", 20, -45.0);
			calculateForegroundTraffic("K202F", inMap, outMap, tempMap, 30, 6, -45.0);

			processImageToBackground("K505F", 20, 30.0);
			calculateForegroundTraffic("K505F", inMap, outMap, tempMap, 30, 6, 30.0);
			
			processImageToBackground("K621F", 20, 50.0);
			calculateForegroundTraffic("K621F", inMap, outMap, tempMap, 30, 4, 50.0);

			processImageToBackground("TR111F", 20, -50.0);
			calculateForegroundTraffic("TR111F", inMap, outMap, tempMap, 30, 5, -50.0);
			
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public BufferedImage rotateImage(BufferedImage originalImage, double degree) {
	    int w = originalImage.getWidth();
	    int h = originalImage.getHeight();
	    double toRad = Math.toRadians(degree);
	   // int hPrime = (int) (w * Math.abs(Math.sin(toRad)) + h * Math.abs(Math.cos(toRad)));
	   // int wPrime = (int) (h * Math.abs(Math.sin(toRad)) + w * Math.abs(Math.cos(toRad)));
	    
	    BufferedImage rotatedImage = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
	    Graphics2D g = rotatedImage.createGraphics();
	    g.setColor(Color.BLACK);
	    g.fillRect(0, 0, 400, 400);  // fill entire area
	    g.translate(400/2, 400/2);
	    g.rotate(toRad);
	    g.translate(-w/2, -h/2);
	    g.drawImage(originalImage, 0, 0, null);
	    g.dispose();  // release used resources before g is garbage-collected

	    return rotatedImage;
	}
	
	private void calculateForegroundTraffic(String id, HashMap<String, String> inMap, HashMap<String, String> outMap, HashMap<String, String> tempMap, int pixelThreadHold, int scale , double angle) {

		String in = inMap.get(id);
		String out = outMap.get(id);

		String in1Start = in.split(",")[0];
		String in1End = in.split(",")[1];
		
		String in2Start = in.split(",")[2];
		String in2End = in.split(",")[3];
		
		String out1Start = out.split(",")[0];
		String out1End = out.split(",")[1];
		
		String out2Start = out.split(",")[2];
		String out2End = out.split(",")[3];

		int inX1 = Integer.parseInt(in1Start.split(":")[0]);
		int inY1 = Integer.parseInt(in1Start.split(":")[1]);
		int inX2 = Integer.parseInt(in1End.split(":")[0]);
		int inY2 = Integer.parseInt(in1End.split(":")[1]);
		
		int inX3 = Integer.parseInt(in2Start.split(":")[0]);
		int inY3 = Integer.parseInt(in2Start.split(":")[1]);
		int inX4 = Integer.parseInt(in2End.split(":")[0]);
		int inY4 = Integer.parseInt(in2End.split(":")[1]);
		
		int outX1 = Integer.parseInt(out1Start.split(":")[0]);
		int outY1 = Integer.parseInt(out1Start.split(":")[1]);
		int outX2 = Integer.parseInt(out1End.split(":")[0]);
		int outY2 = Integer.parseInt(out1End.split(":")[1]);
		
		int outX3 = Integer.parseInt(out2Start.split(":")[0]);
		int outY3 = Integer.parseInt(out2Start.split(":")[1]);
		int outX4 = Integer.parseInt(out2End.split(":")[0]);
		int outY4 = Integer.parseInt(out2End.split(":")[1]);

		try {
			long inTrafficCount = 0;
			long outTrafficCount = 0;

			long millis = System.currentTimeMillis() / 1000;

			String timestampString = (String) redisTemplate.opsForHash().get(id, "timestamp");
			String productionMapString = null;

			if (timestampString != null) {
				Long timestamp = Long.parseLong(timestampString);
				
				if (millis - 1 * 60 * 60 > timestamp) {
					logger.info("cleaning {}, redis time {} <-> now {} ", id, timestamp, millis);

					redisTemplate.opsForHash().delete(id, "productionMap");
					redisTemplate.opsForHash().delete("id", "rankMap");
					redisTemplate.opsForHash().delete(id, "timestamp");
				}

				productionMapString = (String) redisTemplate.opsForHash().get(id, "productionMap");
			} 
			
			if(productionMapString != null) {
				HashMap<String, Integer> productionMap = om.readValue(productionMapString, HashMap.class);
				if(productionMap != null) {
					// get weather information
					
					JSONObject weatherObj = Unirest.get("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=rhrread&lang=en")
							.asJson()
							.getBody()
							.getObject();
					
					JSONArray tempArray = weatherObj.getJSONObject("temperature").getJSONArray("data");

					Integer temperature = null;
					Integer humidity = null;
					Integer weather = null;
					
					for (Object item : tempArray) {
						JSONObject obj = (JSONObject) item;

						if (obj.getString("place").equals(tempMap.get(id))) {
							temperature = obj.getInt("value");
						}
					}


					weather = weatherObj.getJSONArray("icon").getInt(0);
					
					humidity = weatherObj.getJSONObject("humidity").getJSONArray("data").getJSONObject(0).getInt("value");

					String url = "https://tdcctv.data.one.gov.hk/" + id + ".JPG?" + millis;

					HttpResponse<byte[]> stringResponse = Unirest.get(url).asBytes();

					ByteArrayInputStream bis = new ByteArrayInputStream(stringResponse.getBody());

					BufferedImage image = ImageIO.read(bis);

					image = rotateImage(image, angle);
					
					for (int y = 0; y < image.getHeight(); y += convPixel) {
						for (int x = 0; x < image.getWidth(); x += convPixel) {
							if(x > convPixel) {

								Integer productionPixelValue = productionMap.get((y / convPixel) + ":" + (x / convPixel));

								if (productionPixelValue != null) {

									int currentImagePixelGray = conv(x, y, image, convPixel);

									int currentImagePixelGrayUpper = conv(x - convPixel, y , image, convPixel);

									//logger.info("x {} , y {} : {} == {}", x, y, pixelValue, pixelGray);

									if (!(productionPixelValue - pixelThreadHold < currentImagePixelGray && currentImagePixelGray < productionPixelValue + pixelThreadHold)) {
										// hog top
										if (Math.abs(currentImagePixelGray - currentImagePixelGrayUpper) > 20) {
											if (inX1 <= x && x <= inX2 && inY1 <= y && y <= inY2) {
												inTrafficCount++;
											}
											if (inX3 <= x && x <= inX4 && inY3 <= y && y <= inY4) {
												inTrafficCount++;
											}
											if (outX1 <= x && x <= outX2 && outY1 <= y && y <= outY2) {
												outTrafficCount++;
											}
											if (outX3 <= x && x <= outX4 && outY3 <= y && y <= outY4) {
												outTrafficCount++;
											}
										}
									}
								}
							}
						}
					}

					inTrafficCount = inTrafficCount / scale;
					outTrafficCount = outTrafficCount / scale;

					logger.info("saving traffic stat {} in {}, out {}", id, inTrafficCount, outTrafficCount);
					
					// update the trafficCount to DB
					TimeZone tz1 = TimeZone.getTimeZone("GMT+08:00");
					Calendar curTime = Calendar.getInstance(tz1);
					TrafficStat ts = new TrafficStat();

					ts.setId(id);
					ts.setYear(curTime.get(Calendar.YEAR));
					ts.setMonth(curTime.get(Calendar.MONTH) + 1);
					ts.setDay(curTime.get(Calendar.DAY_OF_MONTH));
					ts.setWeekday(curTime.get(Calendar.DAY_OF_WEEK));

					ts.setHour(curTime.get(Calendar.HOUR_OF_DAY));

					ts.setMinute(curTime.get(Calendar.MINUTE));

					ts.setIn(inTrafficCount);
					ts.setOut(outTrafficCount);

					ts.setTemperature(temperature);
					ts.setHumidity(humidity);
					ts.setWeather(weather);
					
					tsDAO.save(ts);
				}

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void processImageToBackground(String id, int step, double angle) throws JsonProcessingException {

		long millis = System.currentTimeMillis() / 1000;
		
		String countString = (String) redisTemplate.opsForHash().get(id, "count");

		if (countString == null) {
			countString = "1";
		}

		int count = Integer.parseInt(countString);

		HashMap<String, HashMap<String, Integer>> rankMap = calculateBackground(id, angle);

		if (rankMap != null) {
			redisTemplate.opsForHash().put(id, "rankMap", om.writeValueAsString(rankMap));

			redisTemplate.opsForHash().put(id, "count", "" + ++count);

			if (count > step) {
				HashMap<String, Integer> backgroundMap = populateMap(rankMap);

				redisTemplate.opsForHash().put(id, "timestamp", "" + millis);
				
				redisTemplate.opsForHash().put(id, "productionMap", om.writeValueAsString(backgroundMap));

				// reset redis
				redisTemplate.opsForHash().put(id, "rankMap", "{}");

				// count reset
				redisTemplate.opsForHash().put(id, "count", "1");
			}
		}
	}

	private HashMap<String, Integer> populateMap(HashMap<String, HashMap<String, Integer>> rankMap) {
		HashMap<String, Integer> productionMap = new HashMap<String, Integer>();
		
		for (String key  : rankMap.keySet()) {
			HashMap<String, Integer> rankItem = rankMap.get(key);
			
			String highestValue = rankItem.entrySet()
					.stream()
					.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
					.collect(toSingleton()).getKey();

			productionMap.put(key, Integer.parseInt(highestValue));

		}
		return productionMap;
		
	}

	public static <T> Collector<T, ?, T> toSingleton() {
		return Collectors.collectingAndThen(Collectors.toList(), list -> {
			if (list == null) {
				throw new IllegalStateException();
			}
			return list.get(0);
		});
	}

	public HashMap<String, HashMap<String, Integer>> calculateBackground(String id, double angle) {

		long millis = System.currentTimeMillis() / 1000;
		Unirest.config().verifySsl(false);

		try {

			HttpResponse<byte[]> stringResponse = Unirest.get("https://tdcctv.data.one.gov.hk/" + id + ".JPG?r=" + millis).asBytes();

			byte[] imageByte = stringResponse.getBody();

			byte[] md5Byte = MessageDigest.getInstance("MD5").digest(imageByte);

			String md5String = Base64.getEncoder().encodeToString(md5Byte);

			String lastMD5 = (String) redisTemplate.opsForHash().get(id, "md5");

			
			if (lastMD5 == null || (lastMD5 != null && !lastMD5.equals(md5String))) {

				logger.info("updating image {} , md5 {} <=> {}", id, lastMD5, md5String);
				
				ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);

				BufferedImage image = ImageIO.read(bis);

				image = rotateImage(image, angle);
				
				int[][] pixel = getGrayPixels(image);
				
				int[][] overlay = new int[image.getHeight() / convPixel][image.getWidth() / convPixel];

				int y1 = 0;

				for (int y = 0; y <= pixel.length - convPixel; y += convPixel) {
					int x1 = 0;

					for (int x = 0; x <= pixel[y].length - convPixel; x += convPixel) {
						// logger.info("{}:{} < {},{}:{} < {} : {}", y, y + convPixel, pixel.length, x, x + convPixel, pixel[y].length, pixel[y][x]);
						overlay[y1][x1] = conv(x, y, pixel, convPixel);

						// logger.info("overlay[{}][{}] : {}", y1, x1, overlay[y1][x1]);
						x1++;
					}
					y1++;
				}

				// store the hashset with rank
				HashMap<String, HashMap<String, Integer>> rankMap = null;

				String rankMapString = (String) redisTemplate.opsForHash().get(id, "rankMap");
				if (rankMapString != null) {
					rankMap = om.readValue(rankMapString, HashMap.class);
				}

				if (rankMap == null) {
					rankMap = new HashMap<String, HashMap<String, Integer>>();
				}

				for (int y = 0; y < overlay.length; y++) {
					for (int x = 0; x < overlay[y].length; x++) {
						HashMap<String, Integer> rankMapItem = rankMap.get(y + ":" + x);

						if (rankMapItem == null) {
							rankMapItem = new HashMap<String, Integer>();
						}

						String targetValue = "" + (overlay[y][x] / 1);

						if (rankMapItem.get(targetValue) != null) {
							rankMapItem.put(targetValue, rankMapItem.get(targetValue) + 1);
						} else {
							rankMapItem.put(targetValue, 1);
						}

						rankMap.put(y + ":" + x, rankMapItem);
					}
				}

				redisTemplate.opsForHash().put(id, "md5", "" + md5String);
				
				return rankMap ;
				
			}

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private int conv(int x, int y, int[][] pixel, int i) {
		int mean = 0;
		for (int y1 = y; y1 < y + i; y1++) {
			for (int x1 = x; x1 < x + i; x1++) {
				mean += pixel[y1][x1];
			}
		}
		mean = mean / (i * i);

		return mean;
	}

	private int conv(int x, int y, BufferedImage image, int i) {
		int mean = 0;
		for (int y1 = y; y1 < y + i; y1++) {
			for (int x1 = x; x1 < x + i; x1++) {
				mean += getGray(image.getRGB(x1, y1));
			}
		}
		mean = mean / (i * i);

		return mean;
	}

	public int getGray(int pixel) {
		int red = (pixel >> 16) & 0xff;
		int green = (pixel >> 8) & 0xff;
		int blue = (pixel) & 0xff;

		return (red + green + blue) / 3;
	}
	private static int[][] getGrayPixels(BufferedImage image) {

		int w = image.getWidth();
		int h = image.getHeight();

		int[][] result = new int[h][w];

		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				int pixel = image.getRGB(j, i);
				
				int red = (pixel >> 16) & 0xff;
				int green = (pixel >> 8) & 0xff;
				int blue = (pixel) & 0xff;

				result[i][j] = (red + green + blue) / 3;

			}
		}

		return result;
	}
}
