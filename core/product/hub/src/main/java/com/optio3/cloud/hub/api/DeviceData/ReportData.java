package com.optio3.cloud.hub.api.DeviceData;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReportData {

	public static class GatewayHeader {
		@JsonProperty("protocolVersion")
		public long protocolVersion;
		@JsonProperty("sequenceNumber")
		public long sequenceNumber;
		@JsonProperty("gatewayFirmwareVer")
		public String gatewayFirmwareVer;
		@JsonProperty("customerID")
		public String customerID;
		@JsonProperty("gatewayID")
		public String gatewayID;
	}

	public static class ConfigStreamHeader {
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

	public static class PeripheralEntry {
		@JsonProperty("parentIndex")
		public int parentIndex;
		@JsonProperty("deviceType")
		public int deviceType;
		@JsonProperty("manufacturerCode")
		public int manufacturerCode;
		@JsonProperty("modelCode")
		public int modelCode;
		@JsonProperty("lastSeen")
		public double lastSeen;
		@JsonProperty("healthValue")
		public int healthValue;
	}

	public static class DiagnosticStreamHeader {
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

	public static class DataStreamHeader {
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

	public static class CpuData {
		@JsonProperty("ramUsed")
		public double ramUsed;
		@JsonProperty("storageUsed")
		public double storageUsed;
		@JsonProperty("avgCpuUtilization")
		public double avgCpuUtilization;
		@JsonProperty("cpuAwakePortion")
		public double cpuAwakePortion;
	}

	public static class PowerData {
		@JsonProperty("voltage")
		public double voltage;
		@JsonProperty("avgVoltage")
		public double avgVoltage;
		@JsonProperty("avgCurrent")
		public double avgCurrent;
		public ZonedDateTime timeStamp;
	}

	public static class SolarChData {
		public double batteryPercentatge;
		public double batteryTemprature;
		public double chgTemprature;
		public double ambTemprature;
		public double chargeStatus;
		public double fault1;
		public double fault2;
		public double fault3;
		public double secondstonext;
	}

	
	public static class BatteryChData {
		public double batteryVoltage;
		public double batteryCurrent;
		public double vbusVoltage;
		public double vbusCurrent;
				public double fault0;
		public double fault1;
		public double fault3;
		public double secondstonext;
	}
	public static class TrhData {
		@JsonProperty("temperature")
		public double temperature; // to handle null values
		@JsonProperty("relativeHumidity")
		public double relativeHumidity;
		@JsonProperty("voltage")
		public double voltage;
		public ZonedDateTime timeStamp;
	}

	public static class GnssData {
		@JsonProperty("altitude")
		public double altitude;
		@JsonProperty("latitude")
		public double latitude;
		@JsonProperty("longitude")
		public double longitude;
		@JsonProperty("speed")
		public double speed;
		@JsonProperty("status")
		public double status;
		public ZonedDateTime timeStamp;
		
		
	}

	public static class AbsData {
		@JsonProperty("brakeTemps")
		public List<Double> brakeTemps;
	}

	public static class TpsData {
		@JsonProperty("tirePressures")
		public List<Double> tirePressures;
	}

	public static class FuelData {
		@JsonProperty("fuelRemaining")
		public double fuelRemaining;
		@JsonProperty("consumptionPerHour")
		public double consumptionPerHour;
		@JsonProperty("timeToEmpty")
		public double timeToEmpty;
	}

	public static class LightingData {
		@JsonProperty("circuitOnTime")
		public double circuitOnTime;
	}

	public static class CargoSensorData {
		@JsonProperty("floorAreaCovered")
		public double floorAreaCovered;
		@JsonProperty("volumeFilled")
		public double volumeFilled;
	}

	public static class WiliotData {
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

	
	public static class DoorData {
		@JsonProperty("open")
		public int doorOpenStatus;
		@JsonProperty("temper")
		public double doorTemperStatus;
	}
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
	public List<DoorData> doorData;

	public List<DoorData> getDoorData() {
		return doorData;
	}

	public void setDoorData(List<DoorData> doorData) {
		this.doorData = doorData;
	}

	public static void main(String[] args) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			DeviceToServerData deviceToServerData = mapper.readValue(new File("C:\\Users\\Dell\\mc.json"),
					DeviceToServerData.class);
			// ReportData reportData = mapper.readValue(new
			// File("C:\\Users\\Dell\\mc.json"), ReportData.class);
			ReportData reportData = new ReportData();

			System.out.println("Parsed JSON Data:");
			System.out.println("Gateway Header Protocol Version: " + reportData.gatewayHeader.get(0).gatewayID);
			if (reportData.configStreamHeader != null && reportData.configStreamHeader.size() > 0) {
				Long timestamp = reportData.configStreamHeader.get(0).timestamp;
				System.out.println("Event Processed UTC Time: " + timestamp);
				Instant i = Instant.ofEpochSecond(timestamp);
				System.out.println("Event Processed UTC Time: " + i);

				ZonedDateTime z = ZonedDateTime.ofInstant(i, ZoneOffset.UTC);

				System.out.println("Event Processed UTC Time: " + z);

			}
			if (reportData.powerData != null && reportData.powerData.size() > 0) {
				double voltage = reportData.powerData.get(0).voltage;
			}

			if (reportData.gnssData != null && reportData.gnssData.size() > 0) {
				double latitude = reportData.gnssData.get(0).latitude;
				double longitude = reportData.gnssData.get(0).longitude;
				double altitude = reportData.gnssData.get(0).altitude;
			}

			double temprature;

			if (reportData.trhData != null && reportData.trhData.size() > 0) {
				temprature = reportData.trhData.get(0).temperature;

			}
			System.out.println("Event Processed UTC Time: " + reportData.eventProcessedUtcTime);
			// Continue to print other fields as necessary
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void printDeviceToServerData(DeviceToServerData data) {
	    if (data == null) {
	        System.out.println("DeviceToServerData object is null. Cannot print data.");
	        return;
	    }

	    System.out.println("Gateway: " + (data.getGateway() != null ? data.getGateway() : "N/A"));

	    System.out.println("\nConfig:");
	    if (data.getConfig() != null) {
	        printConfig(data.getConfig());
	    } else {
	        System.out.println("Config data is null.");
	    }

	    System.out.println("\nDiagnostic:");
	    if (data.getDiagnostic() != null) {
	        printDiagnostic(data.getDiagnostic());
	    } else {
	        System.out.println("Diagnostic data is null.");
	    }

	    System.out.println("\nTRH Data:");
	    if (data.getTrhData() != null) {
	        printDataEntry(data.getTrhData());
	    } else {
	        System.out.println("TRH Data is null.");
	    }

	    System.out.println("\nCPU Data:");
	    if (data.getCpuData() != null) {
	        printDataEntry(data.getCpuData());
	    } else {
	        System.out.println("CPU Data is null.");
	    }

	    System.out.println("\nPower Data:");
	    if (data.getPowerData() != null) {
	        printDataEntry(data.getPowerData());
	    } else {
	        System.out.println("Power Data is null.");
	    }

	    System.out.println("\nSolar Charge Data:");
	    if (data.getSolChgData() != null) {
	        printDataEntry(data.getSolChgData());
	    } else {
	        System.out.println("Solar Charge Data is null.");
	    }

	    System.out.println("\nBattery Charge Data:");
	    if (data.getBatChgData() != null) {
	        printDataEntry(data.getBatChgData());
	    } else {
	        System.out.println("Battery Charge Data is null.");
	    }

	    System.out.println("\nGNSS Data:");
	    if (data.getGnssData() != null) {
	        printDataEntry(data.getGnssData());
	    } else {
	        System.out.println("GNSS Data is null.");
	    }

	    System.out.println("\nFuel Data:");
	    if (data.getFuelData() != null) {
	        printDataEntry(data.getFuelData());
	    } else {
	        System.out.println("Fuel Data is null.");
	    }

	    System.out.println("\nCargo Sensor Data:");
	    if (data.getCargoSensorData() != null) {
	        printDataEntry(data.getCargoSensorData());
	    } else {
	        System.out.println("Cargo Sensor Data is null.");
	    }

	    System.out.println("\nWiliot Data:");
	    if (data.getWiliotData() != null) {
	        printDataEntry(data.getWiliotData());
	    } else {
	        System.out.println("Wiliot Data is null.");
	    }

	    System.out.println("\nDoor Data:");
	    if (data.getDoorData() != null) {
	        printDataEntry(data.getDoorData());
	    } else {
	        System.out.println("Door Data is null.");
	    }

	    System.out.println("\nEvents:");
	    if (data.getEvent() != null) {
	        printEvents(data.getEvent());
	    } else {
	        System.out.println("Events data is null.");
	    }
	}


	public static void printConfig(Config config) {
		System.out.println("Timestamp: " + config.getTs());
		System.out.println("Peripheral Tree:");
		for (Peripheral p : config.getPt()) {
			printPeripheral(p);
		}
	}

	public static void printPeripheral(Peripheral p) {
		System.out.println("Peripheral:");
		System.out.println("  Parent Index: " + p.getPI());
		System.out.println("  Device Type: " + p.getDT());
		System.out.println("  Manufacturer Code: " + p.getMC());
		System.out.println("  Device ID: " + p.getDid());
		System.out.println("  Last Seen: " + p.getLS());
		System.out.println("  Health Value: " + p.getHV());
	}

	public static void printDiagnostic(Diagnostic diagnostic) {
		System.out.println("  Format: " + diagnostic.getFt());
		System.out.println("  Peripheral ID: " + diagnostic.getPID());
		System.out.println("  Timestamp: " + diagnostic.getTs());
		System.out.println("  Diagnostic Data: " + diagnostic.getD());
	}

	public static void printDataEntry(DataEntryDouble dataEntry) {
		System.out.println("  Peripheral ID: " + dataEntry.getPID());
		System.out.println("  Timestamp: " + dataEntry.getTs());
		System.out.println("  Data:");
		for (List<Double> data : dataEntry.getD()) {
			System.out.println("    " + data);
		}
	}

	public static void printDataEntry(DataEntry dataEntry) {
		System.out.println("  Peripheral ID: " + dataEntry.getPID());
		System.out.println("  Timestamp: " + dataEntry.getTs());
		System.out.println("  Data:");
		for (List<Integer> data : dataEntry.getD()) {
			System.out.println("    " + data);
		}
	}

	public static void printEvents(List<Event> events) {
		for (Event event : events) {
			System.out.println("Event:");
			System.out.println("  Event Type: " + event.getEt());
			System.out.println("  Timestamp: " + event.getTs());
			System.out.println("  Position: " + event.getPs());
			// if (event.getAgo() != null)
			{
				System.out.println("  Stopped Seconds Ago: " + event.getAgo());
			}
			// if (event.getAc() != null)
			{
				System.out.println("  Acceleration: " + event.getAc());
			}
			// if (event.getPID() != null)
			{
				System.out.println("  Peripheral ID: " + event.getpID());
			}
			// if (event.getPe() != null)
			{
				System.out.println("  Power Event: " + event.getPe());
			}
			if (event.getV() != null) {
				System.out.println("  Voltage: " + event.getV());
			}
			if (event.getC() != null) {
				System.out.println("  Current: " + event.getC());
			}
			// if (event.getDe() != null)
			{
				System.out.println("  Door Event: " + event.getDe());
			}
		}
	}
}
