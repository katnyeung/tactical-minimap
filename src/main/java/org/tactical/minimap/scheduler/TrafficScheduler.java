package org.tactical.minimap.scheduler;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

@Component
public class TrafficScheduler {

	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	StringRedisTemplate redisTemplate;
	
	@Async
	@Scheduled(fixedRate = 180000)
	public void dataoneParser() throws IOException {

		Unirest.config().verifySsl(false);

		try {
			logger.info("get image");
			long millis = System.currentTimeMillis() / 1000;
					
			String url = "https://tdcctv.data.one.gov.hk/TC604F.JPG?r=" + millis;
			HttpResponse<byte[]> stringResponse = Unirest.get(url).asBytes();
			
			byte[] imageByte = stringResponse.getBody();
			
			byte[] md5Byte = MessageDigest.getInstance("MD5").digest(imageByte);
			
			String md5String = Base64.getEncoder().encodeToString(md5Byte);
			

			ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);

			BufferedImage image = ImageIO.read(bis);
			
			int[][] pixel = convertTo2DWithoutUsingGetRGB(image);

			for (int y = 0; y < pixel.length; y++) {
				for (int x = 0; x < pixel[y].length; x++) {
					logger.info("{},{} : {}", x, y, pixel[y][x]);
				}
			}
			
			logger.info("md5 : {} " , md5String);

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	   private static int[][] convertTo2DWithoutUsingGetRGB(BufferedImage image) {

	      final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
	      final int width = image.getWidth();
	      final int height = image.getHeight();
	      final boolean hasAlphaChannel = image.getAlphaRaster() != null;

	      int[][] result = new int[height][width];
	      if (hasAlphaChannel) {
	         final int pixelLength = 4;
	         for (int pixel = 0, row = 0, col = 0; pixel + 3 < pixels.length; pixel += pixelLength) {
	            int argb = 0;
	            argb += (((int) pixels[pixel] & 0xff) << 24); // alpha
	            argb += ((int) pixels[pixel + 1] & 0xff); // blue
	            argb += (((int) pixels[pixel + 2] & 0xff) << 8); // green
	            argb += (((int) pixels[pixel + 3] & 0xff) << 16); // red
	            result[row][col] = argb;
	            col++;
	            if (col == width) {
	               col = 0;
	               row++;
	            }
	         }
	      } else {
	         final int pixelLength = 3;
	         for (int pixel = 0, row = 0, col = 0; pixel + 2 < pixels.length; pixel += pixelLength) {
	            int argb = 0;
	            argb += -16777216; // 255 alpha
	            argb += ((int) pixels[pixel] & 0xff); // blue
	            argb += (((int) pixels[pixel + 1] & 0xff) << 8); // green
	            argb += (((int) pixels[pixel + 2] & 0xff) << 16); // red
	            result[row][col] = argb;
	            col++;
	            if (col == width) {
	               col = 0;
	               row++;
	            }
	         }
	      }

	      return result;
	   }
	
}
