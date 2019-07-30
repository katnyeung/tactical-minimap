package org.tactical.minimap.web.result;

public class UploadImageResult extends DefaultResult {
	String filePath;

	public UploadImageResult() {
		super();
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

}
