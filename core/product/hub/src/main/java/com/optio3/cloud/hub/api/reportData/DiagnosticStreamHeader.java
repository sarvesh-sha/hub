package com.optio3.cloud.hub.api.reportData;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiagnosticStreamHeader {
	@JsonProperty("streamType")
	public double streamType;
	@JsonProperty("bytesPayload")
	public double bytesPayload;
	@JsonProperty("dataType")
	public double dataType;
	@JsonProperty("peripheralID")
	public double peripheralID;
	@JsonProperty("startTimestamp")
	public double startTimestamp;
}

