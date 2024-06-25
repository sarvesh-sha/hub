package com.optio3.cloud.hub.api.reportData;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CpuData {
	@JsonProperty("ramUsed")
	public double ramUsed;
	@JsonProperty("storageUsed")
	public double storageUsed;
	@JsonProperty("avgCpuUtilization")
	public double avgCpuUtilization;
	@JsonProperty("cpuAwakePortion")
	public double cpuAwakePortion;
}

