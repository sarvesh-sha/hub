package com.optio3.cloud.hub.api.reportData;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GatewayHeader {
	@JsonProperty("protocolVersion")
	public double protocolVersion;
	@JsonProperty("sequenceNumber")
	public double sequenceNumber;
	@JsonProperty("gatewayFirmwareVer")
	public String gatewayFirmwareVer;
	@JsonProperty("customerID")
	public String customerID;
	@JsonProperty("gatewayID")
	public String gatewayID;
}

