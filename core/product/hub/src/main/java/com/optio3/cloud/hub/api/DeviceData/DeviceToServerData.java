package com.optio3.cloud.hub.api.DeviceData;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceToServerData {
	
    public List<Object> gateway;

    public Config config;

    public Diagnostic diagnostic;
	
    public DataEntry trhData;
    
    public DataEntry obtrhData;

    public DataEntry cpuData;

    public DataEntry powerData;
    @JsonProperty("SolChgData")
    public DataEntry solChgData;
    @JsonProperty("BatChgData")
    public DataEntry batChgData;

    public DataEntryDouble gnssData;

    public DataEntry fuelData;

    public DataEntry cargoSensorData;

    public DataEntry wiliotData;
 
    public DataEntry doorData;

    public List<Event> event;
	public List<Object> getGateway() {
		return gateway;
	}
	public void setGateway(List<Object> gateway) {
		this.gateway = gateway;
	}
	public Config getConfig() {
		return config;
	}
	public void setConfig(Config config) {
		this.config = config;
	}
	public Diagnostic getDiagnostic() {
		return diagnostic;
	}
	public void setDiagnostic(Diagnostic diagnostic) {
		this.diagnostic = diagnostic;
	}
	public DataEntry getTrhData() {
		return trhData;
	}
	public void setTrhData(DataEntry trhData) {
		this.trhData = trhData;
	}
	public DataEntry getCpuData() {
		return cpuData;
	}
	public void setCpuData(DataEntry cpuData) {
		this.cpuData = cpuData;
	}
	public DataEntry getPowerData() {
		return powerData;
	}
	public void setPowerData(DataEntry powerData) {
		this.powerData = powerData;
	}
	public DataEntry getSolChgData() {
		return solChgData;
	}
	public void setSolChgData(DataEntry solChgData) {
		this.solChgData = solChgData;
	}
	public DataEntry getBatChgData() {
		return batChgData;
	}
	public void setBatChgData(DataEntry batChgData) {
		this.batChgData = batChgData;
	}
	public DataEntryDouble getGnssData() {
		return gnssData;
	}
	public void setGnssData(DataEntryDouble gnssData) {
		this.gnssData = gnssData;
	}
	public DataEntry getFuelData() {
		return fuelData;
	}
	public void setFuelData(DataEntry fuelData) {
		this.fuelData = fuelData;
	}
	public DataEntry getCargoSensorData() {
		return cargoSensorData;
	}
	public void setCargoSensorData(DataEntry cargoSensorData) {
		this.cargoSensorData = cargoSensorData;
	}
	public DataEntry getWiliotData() {
		return wiliotData;
	}
	public void setWiliotData(DataEntry wiliotData) {
		this.wiliotData = wiliotData;
	}
	public DataEntry getDoorData() {
		return doorData;
	}
	public void setDoorData(DataEntry doorData) {
		this.doorData = doorData;
	}
	public List<Event> getEvent() {
		return event;
	}
	public void setEvent(List<Event> event) {
		this.event = event;
	}

    // Getters and Setters
    // ... (generate getters and setters for all fields)
}