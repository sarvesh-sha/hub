package com.optio3.cloud.hub.api.reportData;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PowerData {
	@JsonProperty("voltage")
	public double voltage;
	@JsonProperty("avgVoltage")
	public double avgVoltage;
	@JsonProperty("avgCurrent")
	public double avgCurrent;
}

