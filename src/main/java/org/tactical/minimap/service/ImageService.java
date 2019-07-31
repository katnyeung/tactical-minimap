package org.tactical.minimap.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.activation.FileTypeMap;
import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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

	@Value("${UPLOAD_FOLDER}")
	String uploadFolder;

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

	public File uploadImage(MultipartFile file, String fileName) throws IllegalStateException, IOException {
		File targetFile = new File(uploadFolder + fileName);
		logger.info("upload file to : " + targetFile.getAbsolutePath());

		Path filepath = targetFile.toPath();

		try (OutputStream os = Files.newOutputStream(filepath)) {
			os.write(file.getBytes());
		}

		return targetFile;
	}

	public ResponseEntity<byte[]> readImage(String path) throws IOException {
		logger.info("reading : " + uploadFolder + path);
		File img = new File(uploadFolder + path);
		byte[] bytes = Files.readAllBytes(img.toPath());

		return ResponseEntity.ok().contentType(MediaType.valueOf(FileTypeMap.getDefaultFileTypeMap().getContentType(img))).body(bytes);
	}

}