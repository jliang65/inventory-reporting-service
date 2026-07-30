package com.jeff.inventoryreporting.dto;

public class JobMessage {

	private Long jobId;

	public JobMessage() {
	}

	public JobMessage(Long jobId) {
		this.jobId = jobId;
	}

	public Long getJobId() {
		return jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}
}
