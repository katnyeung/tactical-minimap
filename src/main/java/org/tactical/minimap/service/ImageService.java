package org.tactical.minimap.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tactical.minimap.DAO.ImageDAO;
import org.tactical.minimap.repository.Image;

import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.DimensionConstrain;
import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;

@Service
public class ImageService {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	ImageDAO imageDAO;

	public void saveImage(Image image) {
		imageDAO.save(image);
	}
	
	public void resizeImage(File fromFile, File outputFile, String ext, int size) throws IOException {
		logger.debug("target file : " + fromFile);
		logger.debug("Content Type : " + ext);
		logger.debug("Size : " + size);

		// resize image
		BufferedImage in = ImageIO.read(fromFile);

		if (in.getWidth() > size || in.getHeight() > size) {
			ResampleOp resizeOp = new ResampleOp(DimensionConstrain.createMaxDimension(size, -1));

			resizeOp.setFilter(ResampleFilters.getLanczos3Filter());
			resizeOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);

			BufferedImage scaledImage = resizeOp.filter(in, null);

			ImageIO.write(scaledImage, ext, outputFile);
			logger.debug("Write file complete");
		}

	}
}
