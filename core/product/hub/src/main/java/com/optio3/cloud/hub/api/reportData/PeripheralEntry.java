package com.optio3.cloud.hub.api.reportData;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PeripheralEntry {
	@JsonProperty("parentIndex")
	public int parentIndex;
	@JsonProperty("deviceType")
	public int deviceType;
	@JsonProperty("manufacturerCode")
	public int manufacturerCode;
	@JsonProperty("modelCode")
	public int modelCode;
	@JsonProperty("lastSeen")
	public double lastSeen;
	@JsonProperty("healthValue")
	public int healthValue;
}

