package org.tactical.minimap.web.result;

import java.util.Calendar;
import java.util.Date;

import org.tactical.minimap.util.ConstantsUtil;

public class DefaultResult {
	Date datetime;
	String status;
	String remarks;

	public Date getDatetime() {
		return datetime;
	}

	public void setDatetime(Date datetime) {
		this.datetime = datetime;
	}

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
		datetime = Calendar.getInstance().getTime();
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
