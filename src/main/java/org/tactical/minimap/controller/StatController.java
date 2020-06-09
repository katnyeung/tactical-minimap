package org.tactical.minimap.controller;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.service.StreetDataService;
import org.tactical.minimap.service.TelegramMessageService;
import org.tactical.minimap.util.MarkerGeoCoding;
import org.tactical.minimap.web.result.CommonResult;
import org.tactical.minimap.web.result.DefaultResult;
import org.tactical.minimap.web.result.StatItem;
import org.tactical.minimap.web.result.StatMapResult;
import org.tactical.minimap.web.result.StatResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

@RestController
@RequestMapping("/stat")
public class StatController {
	public final Logger logger = LoggerFactory.getLogger(getClass());
	public final ObjectMapper om = new ObjectMapper();
	
	//@Value("${PIECE_FOLDER}")
	//String pieceFolder;

	@Autowired
	TelegramMessageService tgService;

	@Autowired
	RedisService redisService;

	@Autowired
	StreetDataService sdService;

	@Autowired
	StringRedisTemplate redisTemplate;

	public final static int convPixel = 5;
	
	@GetMapping("/tgLive/")
	public DefaultResult telegramLiveStat() {

		List<MarkerGeoCoding> regionList = sdService.getStatRegionList();

		Map<String, List<StatItem>> regionMap = tgService.getTelegramLiveStat(regionList);

		DefaultResult dr = StatMapResult.success(regionMap);

		return dr;
	}

	@GetMapping("/streetLive/")
	public DefaultResult streetLiveSTat() {

		Map<String, HashMap<String, Integer>> redisStatMap = tgService.getStreetLiveStat();

		Map<String, List<StatItem>> dbStatMap = new LinkedHashMap<String, List<StatItem>>();

		redisStatMap = sortMap(redisStatMap);

		int count = 0;

		for (Entry<String, HashMap<String, Integer>> entry : redisStatMap.entrySet()) {
			if (count > redisStatMap.entrySet().size() - 5) {
				String key = entry.getKey();

				dbStatMap.put(key, tgService.getStreetStat(key));

			}
			count++;
		}

		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("live", redisStatMap);
		returnMap.put("day", dbStatMap);

		DefaultResult dr = CommonResult.success(returnMap);

		return dr;
	}

	private Map<String, HashMap<String, Integer>> sortMap(Map<String, HashMap<String, Integer>> input) {
		// create list of hashMap
		Map<String, Integer> totalMap = new HashMap<String, Integer>();
		Map<String, HashMap<String, Integer>> sortedMap = new LinkedHashMap<String, HashMap<String, Integer>>();
		if (input != null) {
			for (Entry<String, HashMap<String, Integer>> inputEntry : input.entrySet()) {

				int total = zeroIfNull(inputEntry.getValue().get("popo"));
				total += zeroIfNull(inputEntry.getValue().get("hit"));

				totalMap.put(inputEntry.getKey(), total);
			}
		}

		totalMap = totalMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> {
			throw new AssertionError();
		}, LinkedHashMap::new));

		int count = 0;

		for (Entry<String, Integer> inputEntry : totalMap.entrySet()) {
			if (count++ > (input.keySet().size() - 10)) {
				sortedMap.put(inputEntry.getKey(), input.get(inputEntry.getKey()));
			}
		}

		return sortedMap;
	}

	private int zeroIfNull(Integer value) {
		if (value == null) {
			return 0;
		} else {
			return value;
		}
	}

	@GetMapping("/tg24hr/")
	public DefaultResult update(@RequestParam("hour") Long hour, @RequestParam("count") Long count) {

		List<StatItem> listStat = tgService.getTelegram24hrStat(hour, count);

		DefaultResult dr = StatResult.success(listStat);

		return dr;
	}

	@GetMapping("/streetStat/")
	public DefaultResult update(@RequestParam("key") String key) {

		List<StatItem> listStat = tgService.getStreetStat(key);

		DefaultResult dr = StatResult.success(listStat);

		return dr;
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

	    logger.info("h {} , w {} " , 400, 400);
	    return rotatedImage;
	}
	
	@GetMapping("/heatmap/{id}")
	public ResponseEntity<String> heatmap(@PathVariable("id") String id) {
		try {

			String rankMapString = (String) redisTemplate.opsForHash().get(id, "rankMap");
			HashMap<String, HashMap<String,Integer>> rankMap = om.readValue(rankMapString, HashMap.class);
			
			logger.info("image {},  ", id);
			long millis = System.currentTimeMillis() / 1000;
					
			String url = "https://tdcctv.data.one.gov.hk/" + id + ".JPG?" + millis;
			HttpResponse<byte[]> stringResponse = Unirest.get(url).asBytes();

			ByteArrayInputStream bis = new ByteArrayInputStream(stringResponse.getBody());

			BufferedImage image = ImageIO.read(bis);
			
			image = rotateImage(image, 50.0);

			BufferedImage dest = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

			Color mainColor = new Color(000,128,128);
			logger.info("height {}, width {}" , image.getHeight() , image.getWidth());
			for (int y = 0; y < image.getHeight(); y += convPixel) {
				for (int x = 0; x < image.getWidth(); x += convPixel) {
					HashMap<String, Integer> rankMapItem = rankMap.get((y / convPixel) + ":" + (x / convPixel));
					
					if (rankMapItem != null) {
						String highestValue = rankMapItem.entrySet()
								.stream()
								.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
								.collect(toSingleton()).getKey();

						int pixelGray = conv(x, y, image, convPixel);
						//logger.info("x {} , y {} : {} , {} == {}", x, y, rankMapItem, highestValue, pixelGray);
						
						int highestValueInt = Integer.parseInt(highestValue);
						
						if (!(highestValueInt - 30 < pixelGray && pixelGray < highestValueInt + 30)) {
							for (int y1 = y; y1 < y + convPixel; y1++) {
								for (int x1 = x; x1 < x + convPixel; x1++) {
									dest.setRGB(x1, y1, mainColor.getRGB());
								}
							}

						}
					}

				}
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(dest, "png", baos);
			byte[] bytes = baos.toByteArray();

			String base64Image = new String(Base64.getEncoder().encode(bytes));

			return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(base64Image);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@GetMapping("/heatmap0/{id}")
	public ResponseEntity<String> heatmap0(@PathVariable("id") String id) {
		try {

			String productionMapString = (String) redisTemplate.opsForHash().get(id, "productionMap");
			
			if (productionMapString == null) {
				return ResponseEntity.noContent().build();
			}else {

				HashMap<String,Integer> rankMap = om.readValue(productionMapString, HashMap.class);
				
				logger.info("image {},  ", id);
				long millis = System.currentTimeMillis() / 1000;
						
				String url = "https://tdcctv.data.one.gov.hk/" + id + ".JPG?" + millis;
				HttpResponse<byte[]> stringResponse = Unirest.get(url).asBytes();

				ByteArrayInputStream bis = new ByteArrayInputStream(stringResponse.getBody());

				BufferedImage image = ImageIO.read(bis);

				image = rotateImage(image, 50.0);

				BufferedImage dest = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_ARGB);

				Color mainColor = new Color(000,128,128);
				logger.info("height {}, width {}", image.getHeight(), image.getWidth());
				for (int y = 0; y < image.getHeight(); y += convPixel) {
					for (int x = 0; x < image.getWidth(); x += convPixel) {
						Integer highestValue = rankMap.get((y / convPixel) + ":" + (x / convPixel));

						int pixelGray = conv(x, y, image, convPixel);
						logger.info("x {} , y {} : {} == {}", x, y, highestValue, pixelGray);

						if (!(highestValue - 30 < pixelGray && pixelGray < highestValue + 30)) {
							for (int y1 = y; y1 < y + convPixel; y1++) {
								for (int x1 = x; x1 < x + convPixel; x1++) {
									dest.setRGB(x1, y1, mainColor.getRGB());
								}
							}

						}

					}
				}

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(dest, "png", baos);
				byte[] bytes = baos.toByteArray();

				String base64Image = new String(Base64.getEncoder().encode(bytes));

				return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(base64Image);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@GetMapping("/heatmap1/{id}")
	public ResponseEntity<String> heatmap1(@PathVariable("id") String id) {
		try {

			String rankMapString = (String) redisTemplate.opsForHash().get(id, "rankMap");
			HashMap<String, HashMap<String,Integer>> rankMap = om.readValue(rankMapString, HashMap.class);
			
			logger.info("image {},  ", id);
			long millis = System.currentTimeMillis() / 1000;
					
			String url = "https://tdcctv.data.one.gov.hk/" + id + ".JPG?" + millis;
			HttpResponse<byte[]> stringResponse = Unirest.get(url).asBytes();

			ByteArrayInputStream bis = new ByteArrayInputStream(stringResponse.getBody());

			BufferedImage image = ImageIO.read(bis);

			image = rotateImage(image, 50.0);

			BufferedImage dest = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_ARGB);

			Color mainColor = new Color(000, 128, 128);
			logger.info("height {}, width {}" , image.getHeight() , image.getWidth());
			for (int y = 0; y < image.getHeight(); y += convPixel) {
				for (int x = 0; x < image.getWidth(); x += convPixel) {
					if(x > convPixel) {

						HashMap<String, Integer> rankMapItem = rankMap.get((y / convPixel) + ":" + (x / convPixel));
						
						if (rankMapItem != null) {
							String highestValue = rankMapItem.entrySet()
									.stream()
									.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
									.collect(toSingleton()).getKey();
	
							int pixelGray = conv(x, y, image, convPixel);

							int pixelGrayUpper = conv(x - convPixel, y, image, convPixel);
							
							//logger.info("x {} , y {} : {} , {} == {}", x, y, rankMapItem, highestValue, pixelGray);
							
							int highestValueInt = Integer.parseInt(highestValue);
							
							if (!(highestValueInt - 30 < pixelGray && pixelGray < highestValueInt + 30)) {
								if (Math.abs(pixelGray - pixelGrayUpper) > 20) {
									for (int y1 = y; y1 < y + convPixel; y1++) {
										for (int x1 = x; x1 < x + convPixel; x1++) {
											dest.setRGB(x1, y1, mainColor.getRGB());
										}
									}

								}

							}
						}
					}
				}
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(dest, "png", baos);
			byte[] bytes = baos.toByteArray();

			String base64Image = new String(Base64.getEncoder().encode(bytes));

			return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(base64Image);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	@GetMapping("/heatmap2/{id}")
	public ResponseEntity<String> heatmap2(@PathVariable("id") String id) {
		try {

			String rankMapString = (String) redisTemplate.opsForHash().get(id, "rankMap");
			HashMap<String, HashMap<String,Integer>> rankMap = om.readValue(rankMapString, HashMap.class);
			
			logger.info("image {},  ", id);
			long millis = System.currentTimeMillis() / 1000;
					
			String url = "https://tdcctv.data.one.gov.hk/" + id + ".JPG?" + millis;
			HttpResponse<byte[]> stringResponse = Unirest.get(url).asBytes();

			ByteArrayInputStream bis = new ByteArrayInputStream(stringResponse.getBody());

			BufferedImage image = ImageIO.read(bis);

			image = rotateImage(image, 50.0);

			logger.info("image W {}, H {} " , image.getWidth(), image.getHeight());
			BufferedImage dest = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_ARGB);

			Color mainColor = new Color(128,128,128);

			for (int y = 0; y < image.getHeight(); y += convPixel) {
				for (int x = 0; x < image.getWidth(); x += convPixel) {
					HashMap<String, Integer> rankMapItem = rankMap.get((y / convPixel) + ":" + (x / convPixel));
					if (rankMapItem != null) {
						Entry<String, Integer> highestEntry = rankMapItem.entrySet()
								.stream()
								.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
								.collect(toSingleton());

						String pixelGray = "" + conv(x, y, image, convPixel);

						if (highestEntry.getValue() > 1) {
							//logger.info("x {} , y {} " , x, y);
							for (int y1 = y; y1 < y + convPixel; y1++) {
								for (int x1 = x; x1 < x + convPixel; x1++) {
									dest.setRGB(x1, y1, mainColor.getRGB());
								}
							}

						}
					}

				}
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(dest, "png", baos);
			byte[] bytes = baos.toByteArray();

			String base64Image = new String(Base64.getEncoder().encode(bytes));

			return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(base64Image);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@GetMapping("/heatmap3/{id}")
	public ResponseEntity<String> heatmap3(@PathVariable("id") String id) {
		try {

			String rankMapString = (String) redisTemplate.opsForHash().get(id, "rankMap");
			HashMap<String, HashMap<String,Integer>> rankMap = om.readValue(rankMapString, HashMap.class);
			
			logger.info("image {},  ", id);
			long millis = System.currentTimeMillis() / 1000;
					
			String url = "https://tdcctv.data.one.gov.hk/" + id + ".JPG?" + millis;
			HttpResponse<byte[]> stringResponse = Unirest.get(url).asBytes();

			ByteArrayInputStream bis = new ByteArrayInputStream(stringResponse.getBody());

			BufferedImage image = ImageIO.read(bis);

			image = rotateImage(image, 50.0);

			BufferedImage dest = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_ARGB);

			Color mainColor = new Color(128,128,128);

			for (int y = 0; y < image.getHeight(); y += convPixel) {
				for (int x = 0; x < image.getWidth(); x += convPixel) {
					HashMap<String, Integer> rankMapItem = rankMap.get((y / convPixel) + ":" + (x / convPixel));
					if (rankMapItem != null) {
						Entry<String, Integer> highestEntry = rankMapItem.entrySet()
								.stream()
								.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
								.collect(toSingleton());

						int pixelGray =  (conv(x, y, image, convPixel));

						for (int y1 = y; y1 < y + convPixel; y1++) {
							for (int x1 = x; x1 < x + convPixel; x1++) {
								dest.setRGB(x1, y1, new Color(pixelGray, pixelGray, pixelGray).getRGB());
							}
						}

					}

				}
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(dest, "png", baos);
			byte[] bytes = baos.toByteArray();

			String base64Image = new String(Base64.getEncoder().encode(bytes));

			return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(base64Image);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@GetMapping("/heatmap4/{id}")
	public ResponseEntity<String> heatmap4(@PathVariable("id") String id) {
		try {

			String rankMapString = (String) redisTemplate.opsForHash().get(id, "rankMap");
			HashMap<String, HashMap<String,Integer>> rankMap = om.readValue(rankMapString, HashMap.class);
			
			logger.info("image {},  ", id);
			long millis = System.currentTimeMillis() / 1000;
					
			String url = "https://tdcctv.data.one.gov.hk/" + id + ".JPG?" + millis;
			HttpResponse<byte[]> stringResponse = Unirest.get(url).asBytes();

			ByteArrayInputStream bis = new ByteArrayInputStream(stringResponse.getBody());

			BufferedImage image = ImageIO.read(bis);

			image = rotateImage(image, 50.0);

			BufferedImage dest = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_ARGB);

			Color mainColor = new Color(128,128,128);

			for (int y = 0; y < image.getHeight(); y += convPixel) {
				for (int x = 0; x < image.getWidth(); x += convPixel) {
					HashMap<String, Integer> rankMapItem = rankMap.get((y / convPixel) + ":" + (x / convPixel));
					if (rankMapItem != null) {
						Entry<String, Integer> highestEntry = rankMapItem.entrySet()
								.stream()
								.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
								.collect(toSingleton());

						int pixelGray = Integer.parseInt(highestEntry.getKey());

						if (highestEntry.getValue() > 1) {
							// logger.info("x {} , y {} " , x, y);
							for (int y1 = y; y1 < y + convPixel; y1++) {
								for (int x1 = x; x1 < x + convPixel; x1++) {
									dest.setRGB(x1, y1, new Color(pixelGray, pixelGray, pixelGray).getRGB());
								}
							}

						}
					}

				}
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(dest, "png", baos);
			byte[] bytes = baos.toByteArray();

			String base64Image = new String(Base64.getEncoder().encode(bytes));

			return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(base64Image);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@GetMapping("/heatmap5/{id}")
	public ResponseEntity<String> heatmap5(@PathVariable("id") String id) {
		try {

			String productionMapString = (String) redisTemplate.opsForHash().get(id, "productionMap");
			
			if(productionMapString == null) {
				return ResponseEntity.noContent().build();
			}else {
				HashMap<String,Integer> productionMap = om.readValue(productionMapString, HashMap.class);
				
				logger.info("image {},  ", id);
				long millis = System.currentTimeMillis() / 1000;
						
				String url = "https://tdcctv.data.one.gov.hk/" + id + ".JPG?" + millis;
				HttpResponse<byte[]> stringResponse = Unirest.get(url).asBytes();

				ByteArrayInputStream bis = new ByteArrayInputStream(stringResponse.getBody());

				BufferedImage image = ImageIO.read(bis);

				image = rotateImage(image, 50.0);

				BufferedImage dest = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_ARGB);

				Color mainColor = new Color(128,128,128);

				for (int y = 0; y < image.getHeight(); y += convPixel) {
					for (int x = 0; x < image.getWidth(); x += convPixel) {
						Integer highestValue = productionMap.get((y / convPixel) + ":" + (x / convPixel));

						int pixelGray = highestValue;

						if (highestValue > 1) {
							// logger.info("x {} , y {} " , x, y);
							for (int y1 = y; y1 < y + convPixel; y1++) {
								for (int x1 = x; x1 < x + convPixel; x1++) {
									dest.setRGB(x1, y1, new Color(pixelGray, pixelGray, pixelGray).getRGB());
								}
							}

						}

					}
				}

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(dest, "png", baos);
				byte[] bytes = baos.toByteArray();

				String base64Image = new String(Base64.getEncoder().encode(bytes));

				return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(base64Image);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
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
	
	public static <T> Collector<T, ?, T> toSingleton() {
	    return Collectors.collectingAndThen(
	            Collectors.toList(),
	            list -> {
	            	if( list == null) {
	            		throw new IllegalStateException();
	            	}
	                return list.get(0);
	            }
	    );
	}
	
	public int getGray(int pixel) {
		int red = (pixel >> 16) & 0xff;
		int green = (pixel >> 8) & 0xff;
		int blue = (pixel) & 0xff;

		return (red + green + blue) / 3;
	}

	@PostMapping("/piecePreview/")
	public ResponseEntity<String> piecePreview(@RequestBody Map<String, String> inputMap) {
		String imageFile = inputMap.get("image");
		Integer x = Integer.parseInt(inputMap.get("x"));
		Integer y = Integer.parseInt(inputMap.get("y"));
		try {
			logger.info("image {}, x {} , y {} ", imageFile, x, y);
			long millis = System.currentTimeMillis() / 1000;
					
			String url = "https://tdcctv.data.one.gov.hk/" + imageFile + ".JPG?" + millis;
			HttpResponse<byte[]> stringResponse = Unirest.get(url).asBytes();

			ByteArrayInputStream bis = new ByteArrayInputStream(stringResponse.getBody());

			BufferedImage image = ImageIO.read(bis);

			BufferedImage dest = image.getSubimage(x, y, 20, 20);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(dest, "png", baos);
			byte[] bytes = baos.toByteArray();

			String base64Image = new String(Base64.getEncoder().encode(bytes));

			return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(base64Image);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	

	@PostMapping("/pieceSubmit/")
	public ResponseEntity<String> pieceSubmit(@RequestBody Map<String, String> inputMap) {
		String base64Image = inputMap.get("image");
		String id = inputMap.get("id");
		String type = inputMap.get("type");
		
		logger.info("image : {}", base64Image);
		
		Long timeInMillies = Calendar.getInstance().getTimeInMillis() / 1000;

		byte[] data = Base64.getDecoder().decode(base64Image);
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			BufferedImage image;

			image = ImageIO.read(bis);

			bis.close();
			
		    //File directory = new File(pieceFolder + type + "/" );
		    //if (! directory.exists()){
		    //    directory.mkdir();
		  // }
		    
			//// write the image to a file
			//File outputfile = new File(pieceFolder + type + "/" + id + "_" + timeInMillies + ".PNG");
			//ImageIO.write(image, "png", outputfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ResponseEntity.ok().body("OK");
	}
	
}
