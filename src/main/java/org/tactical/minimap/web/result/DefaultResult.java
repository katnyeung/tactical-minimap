package org.tactical.minimap.web.result;

import org.tactical.minimap.util.ConstantsUtil;

import com.fasterxml.jackson.annotation.JsonInclude;

public class DefaultResult {
	String status;

	@JsonInclude(JsonInclude.Include.NON_NULL) //ignore null field on this property only
	String remarks;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public DefaultResult() {
		status = null;
		remarks = null;
	}

	public static DefaultResult success() {
		DefaultResult dr = new DefaultResult();
		dr.setStatus(ConstantsUtil.STATUS_SUCCESS);
		return dr;
	}

	public static DefaultResult success(String remark) {
		DefaultResult dr = new DefaultResult();
		dr.setStatus(ConstantsUtil.STATUS_SUCCESS);
		dr.setRemarks(remark);
		return dr;
	}

	public static DefaultResult error(String remark) {
		DefaultResult dr = new DefaultResult();
		dr.setStatus(ConstantsUtil.STATUS_ERROR);
		dr.setRemarks(remark);
		return dr;
	}
}
