package com.optio3.cloud.hub.api.reportData;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WiliotData {
	@JsonProperty("expectedBridges")
	public double expectedBridges;
	@JsonProperty("otherBridges")
	public double otherBridges;
	@JsonProperty("distinctPixels")
	public double distinctPixels;
	@JsonProperty("upPackets")
	public double upPackets;
	@JsonProperty("downPackets")
	public double downPackets;
}
