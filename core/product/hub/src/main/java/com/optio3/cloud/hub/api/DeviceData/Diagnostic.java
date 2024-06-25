package com.optio3.cloud.hub.api.DeviceData;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

class Diagnostic {
    @JsonProperty("ft")
    private int ft;

    @JsonProperty("pID")
    private int pID;

    @JsonProperty("ts")
    private long ts;

    @JsonProperty("d")
    private List<List<Integer>> d;

    // Getters and setters
    public int getFt() {
        return ft;
    }

    public void setFt(int ft) {
        this.ft = ft;
    }

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

    // toString method for debugging
    @Override
    public String toString() {
        return "Diagnostic{" +
                "ft=" + ft +
                ", pID=" + pID +
                ", ts=" + ts +
                ", d=" + d +
                '}';
    }
}