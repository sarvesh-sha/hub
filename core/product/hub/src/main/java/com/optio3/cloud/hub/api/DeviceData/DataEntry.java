package com.optio3.cloud.hub.api.DeviceData;

import java.util.List;

public class DataEntry {
    public int pID;
    public long ts;
    public List<List<Integer>> d;
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
	public List<List<Integer>> getD() {
		return d;
	}
	public void setD(List<List<Integer>> d) {
		this.d = d;
	}

    // Getters and Setters
    // ... (generate getters and setters for all fields)
}


