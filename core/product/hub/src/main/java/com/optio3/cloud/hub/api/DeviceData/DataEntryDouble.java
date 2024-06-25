package com.optio3.cloud.hub.api.DeviceData;

import java.util.List;

public class DataEntryDouble {
    public int pID;
    public long ts;
    public List<List<Double>> d;
	public int getPID() {
		return pID;
	}
	public void setPID(int pID) {
		this.pID = pID;
	}
	public long getTs() {
		return ts;
	}
	public void setTs(long ts) {
		this.ts = ts;
	}
	public List<List<Double>> getD() {
		return d;
	}
	public void setD(List<List<Double>> d) {
		this.d = d;
	}

    // Getters and Setters
    // ... (generate getters and setters for all fields)
}


