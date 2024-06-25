package com.optio3.cloud.hub.api.reportData;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReportData {

	@JsonProperty("gatewayHeader")
	public List<GatewayHeader> gatewayHeader;
	@JsonProperty("configStreamHeader")
	public List<ConfigStreamHeader> configStreamHeader;
	@JsonProperty("peripheralEntry")
	public List<PeripheralEntry> peripheralEntry;
	@JsonProperty("diagnosticStreamHeader")
	public List<DiagnosticStreamHeader> diagnosticStreamHeader;
	@JsonProperty("dataStreamHeader")
	public List<DataStreamHeader> dataStreamHeader;
	@JsonProperty("cpuData")
	public List<CpuData> cpuData;
	@JsonProperty("powerData")
	public List<PowerData> powerData;
	@JsonProperty("trhData")
	public List<TrhData> trhData;
	@JsonProperty("gnssData")
	public List<GnssData> gnssData;
	@JsonProperty("absData")
	public List<AbsData> absData;
	@JsonProperty("tpsData")
	public List<TpsData> tpsData;
	@JsonProperty("fuelData")
	public List<FuelData> fuelData;
	@JsonProperty("lightingData")
	public List<LightingData> lightingData;
	@JsonProperty("cargoSensorData")
	public List<CargoSensorData> cargoSensorData;
	@JsonProperty("wiliotData")
	public List<WiliotData> wiliotData;
	@JsonProperty("EventProcessedUtcTime")
	public List<String> eventProcessedUtcTime;
	@JsonProperty("PartitionId")
	public List<Integer> partitionId;
	@JsonProperty("EventEnqueuedUtcTime")
	public List<String> eventEnqueuedUtcTime;

	public List<SolarChData> solarChargeData;
	public List<BatteryChData> BatteryChargeData;
}

