package com.optio3.cloud.hub.api.reportData;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GnssData {
	@JsonProperty("altitude")
	public double altitude;
	@JsonProperty("latitude")
	public double latitude;
	@JsonProperty("longitude")
	public double longitude;
	@JsonProperty("status")
	public double status;
}

