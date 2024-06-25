package com.optio3.cloud.hub.api.reportData;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrhData {
	@JsonProperty("temperature")
	public double temperature; // to handle null values
	@JsonProperty("relativeHumidity")
	public double relativeHumidity;
	@JsonProperty("voltage")
	public double voltage;
}

