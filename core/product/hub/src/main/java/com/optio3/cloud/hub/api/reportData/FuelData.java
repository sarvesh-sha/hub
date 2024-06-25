package com.optio3.cloud.hub.api.reportData;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FuelData {
	@JsonProperty("fuelRemaining")
	public double fuelRemaining;
	@JsonProperty("consumptionPerHour")
	public double consumptionPerHour;
	@JsonProperty("timeToEmpty")
	public double timeToEmpty;
}

