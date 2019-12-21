package org.tactical.minimap.web.result;

public class CommonResult extends DefaultResult {

	Object data;

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public static CommonResult success(Object obj) {

		CommonResult sr = new CommonResult();
		sr.setStatus("success");
		sr.setData(obj);
		return sr;
	}

}
