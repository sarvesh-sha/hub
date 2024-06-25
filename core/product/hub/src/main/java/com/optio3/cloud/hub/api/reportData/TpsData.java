package com.optio3.cloud.hub.api.reportData;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TpsData {
	@JsonProperty("tirePressures")
	public List<Double> tirePressures;
}

