package com.optio3.cloud.hub.api.reportData;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigStreamHeader {
	@JsonProperty("streamType")
	public int streamType;
	@JsonProperty("unused1")
	public int unused1;
	@JsonProperty("peripheralCount")
	public int peripheralCount;
	@JsonProperty("unused2")
	public int unused2;
	@JsonProperty("timestamp")
	public long timestamp;
}

