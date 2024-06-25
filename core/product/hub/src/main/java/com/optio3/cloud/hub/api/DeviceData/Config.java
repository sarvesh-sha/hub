package com.optio3.cloud.hub.api.DeviceData;

import java.util.List;

public class Config {
    public long ts;
    public List<Peripheral> pt;
	public long getTs() {
		return ts;
	}
	public void setTs(long ts) {
		this.ts = ts;
	}
	public List<Peripheral> getPt() {
		return pt;
	}
	public void setPt(List<Peripheral> pt) {
		this.pt = pt;
	}

    // Getters and Setters
    // ... (generate getters and setters for all fields)
}



