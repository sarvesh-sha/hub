package com.optio3.cloud.hub.api.reportData;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CargoSensorData {
	@JsonProperty("floorAreaCovered")
	public double floorAreaCovered;
	@JsonProperty("volumeFilled")
	public double volumeFilled;
}

