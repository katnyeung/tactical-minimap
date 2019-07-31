package org.tactical.minimap.controller;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.naming.SizeLimitExceededException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;
import org.tactical.minimap.auth.Auth;
import org.tactical.minimap.repository.Image;
import org.tactical.minimap.service.ImageService;
import org.tactical.minimap.service.LayerService;
import org.tactical.minimap.service.MarkerResponseService;
import org.tactical.minimap.service.MarkerService;
import org.tactical.minimap.service.RedisService;
import org.tactical.minimap.util.ConstantsUtil;
import org.tactical.minimap.web.result.DefaultResult;
import org.tactical.minimap.web.result.UploadImageResult;

@RestController
@RequestMapping("/images")
public class ImageController {
	public final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	MarkerService markerService;

	@Autowired
	LayerService layerService;

	@Autowired
	MarkerResponseService markerResponseService;

	@Autowired
	RedisService redisService;

	@Autowired
	ImageService imageService;

	@ResponseBody
	@GetMapping(path = "/**")
	public String getImageDefault(HttpServletRequest request, HttpSession session, Model model) {
		return "404 not found";
	}

	@ResponseBody
	@GetMapping(path = "/i/**", produces = MediaType.IMAGE_JPEG_VALUE)
	public ResponseEntity<byte[]> getImage(HttpServletRequest request, HttpSession session, Model model) {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

		path = path.replaceAll("^/images/i", "");

		try {
			return imageService.readImage(path);
		} catch (IOException ioex) {
			return null;
		}

	}

	@Auth
	@ResponseBody
	@PostMapping(value = "/imageUpload")
	public DefaultResult uploadImageContent(@RequestParam("file") MultipartFile file) throws IOException, NullPointerException, SizeLimitExceededException {
		UploadImageResult result = new UploadImageResult();

		try {

			String uuidPrefix = UUID.randomUUID().toString().replaceAll("-", "");

			String ext = FilenameUtils.getExtension(file.getOriginalFilename());

			String filename = uuidPrefix + "." + ext;
			
			File targetFile = imageService.uploadImage(file, filename);

			imageService.resizeImage(targetFile, targetFile, ext, 300);

			Image image = new Image();
			image.setFilename(filename);
			image.setStoredPath(targetFile.getAbsolutePath());
			image.setStatus(ConstantsUtil.MARKER_STATUS_ACTIVE);
			image.setSize(targetFile.getTotalSpace());

			imageService.saveImage(image);

			result.setFilePath(filename);
			result.setStatus(ConstantsUtil.STATUS_SUCCESS);

			return result;

		} catch (IOException ex) {
			ex.printStackTrace();

			result.setStatus(ConstantsUtil.STATUS_ERROR);
			result.setRemarks(ex.getLocalizedMessage());

		}

		return result;
	}

}
