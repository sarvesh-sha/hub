package com.optio3.cloud.hub.api.DeviceData;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonParser {
	public static void main(String[] args) {
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectMapper objectMapper1 = new ObjectMapper();

		try {
			// Replace 'input.json' with the path to your JSON file
			List<DeviceToServerData> dataObjects = objectMapper.readValue(new File("C:\\montage\\444.json"),
					new TypeReference<List<DeviceToServerData>>() {
					});
			int j= 0;
			List<DeviceToServerData> dataObjects1 =  new ArrayList<DeviceToServerData>();
			for (DeviceToServerData data : dataObjects)

			// DeviceToServerData data = objectMapper.readValue(new
			// File("C:\\montage\\input1.json"), DeviceToServerData.class);
			{
				System.out.print(data);
				j++;
				dataObjects1.add(data);
				if(j==500)
				{
					objectMapper1.enable(SerializationFeature.INDENT_OUTPUT);
			        String jsonOutput = objectMapper1.writeValueAsString(dataObjects1);
				}
				//JsonParser jsonParser = new JsonParser();
				//ReportData reportData1 = jsonParser.mapToReportData(data);

				// Print the populated ReportData
				//printReportData(reportData1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ZonedDateTime convertTimeStamp(Long timestamp) {
		// Long timestamp = reportData.configStreamHeader.get(0).timestamp;
		System.out.println("Event Processed UTC Time: " + timestamp);
		if (timestamp == 0) {
			System.out.println("Event Processed UTC Time is 0  returnin null" );
			return null;
		}
		Instant i = Instant.ofEpochSecond(timestamp);
		System.out.println("Event Processed UTC Time: " + i);

		ZonedDateTime t = ZonedDateTime.ofInstant(i, ZoneOffset.UTC);

		System.out.println("Event Processed UTC Time: " + t);
		System.out.println("timestamp" + timestamp);

		return t;

	}

	public ReportData mapToReportData(DeviceToServerData data) {
		ReportData reportData = new ReportData();

		// Map GatewayHeader
		List<ReportData.GatewayHeader> gatewayHeaders = new ArrayList<>();
		ReportData.GatewayHeader gatewayHeader = new ReportData.GatewayHeader();
		gatewayHeader.protocolVersion = (Integer) data.getGateway().get(0);
		gatewayHeader.sequenceNumber = (Integer) data.getGateway().get(1);
		gatewayHeader.gatewayFirmwareVer = String.valueOf(data.getGateway().get(2));
		gatewayHeader.customerID = String.valueOf(data.getGateway().get(3));
		try {
			gatewayHeader.gatewayID = (String) data.getGateway().get(4);
		} catch (Exception e)

		{
			gatewayHeader.gatewayID = "123456789";
		}
		gatewayHeaders.add(gatewayHeader);
		reportData.gatewayHeader = gatewayHeaders;

		// Map ConfigStreamHeader
		List<ReportData.ConfigStreamHeader> configStreamHeaders = new ArrayList<>();
		ReportData.ConfigStreamHeader configStreamHeader = new ReportData.ConfigStreamHeader();
		if (data.getConfig() != null) {
			configStreamHeader.streamType = 1; // Assuming streamType is 1
			configStreamHeader.timestamp = data.getConfig().getTs();
			configStreamHeader.peripheralCount = data.getConfig().getPt().size();
			configStreamHeaders.add(configStreamHeader);
			reportData.configStreamHeader = configStreamHeaders;

			// Map PeripheralEntry
			List<ReportData.PeripheralEntry> peripheralEntries = new ArrayList<>();

			for (Peripheral p : data.getConfig().getPt()) {
				ReportData.PeripheralEntry entry = new ReportData.PeripheralEntry();
				entry.parentIndex = p.getPI();
				entry.deviceType = p.getDT();
				entry.manufacturerCode = (int) p.getMC();
				entry.modelCode = (int) p.getDid();
				entry.lastSeen = p.getLS();
				entry.healthValue = p.getHV();
				peripheralEntries.add(entry);
			}
			reportData.peripheralEntry = peripheralEntries;
		}
		// Map DiagnosticStreamHeader
		List<ReportData.DiagnosticStreamHeader> diagnosticStreamHeaders = new ArrayList<>();
		if (data.getDiagnostic() != null) {
			ReportData.DiagnosticStreamHeader diagnosticStreamHeader = new ReportData.DiagnosticStreamHeader();
			diagnosticStreamHeader.streamType = data.getDiagnostic().getFt();
			diagnosticStreamHeader.peripheralID = data.getDiagnostic().getPID();
			diagnosticStreamHeader.startTimestamp = data.getDiagnostic().getTs();
			diagnosticStreamHeaders.add(diagnosticStreamHeader);
			reportData.diagnosticStreamHeader = diagnosticStreamHeaders;
		}
		if (data.getCpuData() != null) {
			// Map DataStreamHeader (example for cpuData)
			List<ReportData.DataStreamHeader> dataStreamHeaders = new ArrayList<>();
			ReportData.DataStreamHeader dataStreamHeader = new ReportData.DataStreamHeader();
			dataStreamHeader.streamType = 1; // Assuming streamType is 1
			dataStreamHeader.peripheralID = data.getCpuData().getPID();
			dataStreamHeader.startTimestamp = data.getCpuData().getTs();
			dataStreamHeaders.add(dataStreamHeader);
			reportData.dataStreamHeader = dataStreamHeaders;
		}
		// Map CPU Data
		List<ReportData.CpuData> cpuDataList = new ArrayList<>();
		if (data.getCpuData() != null) {
			for (List<Integer> cpuData : data.getCpuData().getD()) {
				ReportData.CpuData cpuEntry = new ReportData.CpuData();
				cpuEntry.ramUsed = cpuData.get(0);
				cpuEntry.storageUsed = cpuData.get(1);
				cpuEntry.avgCpuUtilization = cpuData.get(2);
				cpuEntry.cpuAwakePortion = cpuData.get(3);
				cpuDataList.add(cpuEntry);
			}
			reportData.cpuData = cpuDataList;
		}

		if (data.getPowerData() != null && data.getPowerData().getD() != null) {
			List<ReportData.PowerData> powerDataList = new ArrayList<>();
			ReportData.PowerData powerData = new ReportData.PowerData();
			powerData.timeStamp = convertTimeStamp(data.getPowerData().getTs());
			System.out.println("powerData.timeStamp" + powerData.timeStamp);
			if (powerData.timeStamp != null) {
				for (List<Integer> powerEntry : data.getPowerData().getD()) {
					if (powerEntry.size() >= 3) {

						powerData.voltage = powerEntry.get(0);
						powerData.avgVoltage = powerEntry.get(1);
						powerData.avgCurrent = powerEntry.get(2);
						System.out.println("powerData"+powerData);
						powerDataList.add(powerData);
					} else {
						// Handle incomplete or malformed powerData entries
						// This example assumes each powerData entry has at least 3 values
						System.err.println("Incomplete powerData entry: " + powerEntry);
					}
				}

				reportData.powerData = powerDataList;
			}
		}

		// Map SolChgData
		if (data.getSolChgData() != null && data.getSolChgData().getD() != null) {
			List<ReportData.SolarChData> solChgDataList = new ArrayList<>();

			for (List<Integer> solChgEntry : data.getSolChgData().getD()) {
				if (solChgEntry.size() >= 3) {
					ReportData.SolarChData solChgData = new ReportData.SolarChData();
					solChgDataList.add(solChgData);
				} else {
					// Handle incomplete or malformed solChgData entries
					System.err.println("Incomplete solChgData entry: " + solChgEntry);
				}
			}

			reportData.solarChargeData = solChgDataList;
		}

		if (data.getGnssData() != null && data.getGnssData().getD() != null) {
			List<ReportData.GnssData> gnssDataList = new ArrayList<>();
			ReportData.GnssData gnssData = new ReportData.GnssData();
			gnssData.timeStamp = convertTimeStamp(data.getGnssData().getTs());
			
			if (gnssData.timeStamp != null) {
				System.out.println("gnssData.timeStam1p" + gnssData.timeStamp);
				for (List<Double> gnssEntry : data.getGnssData().getD()) {

					gnssData.altitude = gnssEntry.get(0);
					gnssData.latitude = gnssEntry.get(1);
					gnssData.longitude = gnssEntry.get(2);
					gnssData.speed = gnssEntry.get(3);
					gnssData.status = gnssEntry.get(4);
					System.out.println("gnssData.timeStam1p"+ gnssData.latitude + " : "+gnssData.longitude);
					gnssDataList.add(gnssData);
				}

				reportData.gnssData = gnssDataList;
			}
		}

		// Example mapping for fuelData
		if (data.getFuelData() != null && data.getFuelData().getD() != null) {
			List<ReportData.FuelData> fuelDataList = new ArrayList<>();

			for (List<Integer> fuelEntry : data.getFuelData().getD()) {
				ReportData.FuelData fuelData = new ReportData.FuelData();
				fuelData.fuelRemaining = fuelEntry.get(0);
				fuelData.consumptionPerHour = fuelEntry.get(1);
				fuelData.timeToEmpty = fuelEntry.get(2);
				fuelDataList.add(fuelData);
			}

			reportData.fuelData = fuelDataList;
		}

		// Example mapping for cargoSensorData
		if (data.getCargoSensorData() != null && data.getCargoSensorData().getD() != null) {
			List<ReportData.CargoSensorData> cargoSensorDataList = new ArrayList<>();

			for (List<Integer> cargoEntry : data.getCargoSensorData().getD()) {
				ReportData.CargoSensorData cargoSensorData = new ReportData.CargoSensorData();
				cargoSensorData.floorAreaCovered = cargoEntry.get(0);
				cargoSensorData.volumeFilled = cargoEntry.get(1);
				cargoSensorDataList.add(cargoSensorData);
			}

			reportData.cargoSensorData = cargoSensorDataList;
		}
		// Map trhData
		if (data.getTrhData() != null && data.getTrhData().getD() != null) {
			List<ReportData.TrhData> trhDataList = new ArrayList<>();
			ReportData.TrhData trhData = new ReportData.TrhData();
			trhData.timeStamp = convertTimeStamp(data.getTrhData().getTs());
			System.out.println("trhData.timeStamp" + trhData.timeStamp);
			if (trhData.timeStamp != null) {
				for (List<Integer> trhEntry : data.getTrhData().getD()) {
					if (trhEntry.size() >= 2) {

						trhData.temperature = trhEntry.get(0);
						trhData.relativeHumidity = trhEntry.get(1);
						trhData.voltage = trhEntry.get(2);
						System.out.println("trhdata.timeStam1p"+trhData.temperature + " :" +trhData.relativeHumidity +"  ; "+trhData.voltage);
						trhDataList.add(trhData);
					} else {
						// Handle incomplete or malformed trhData entries
						System.err.println("Incomplete trhData entry: " + trhEntry);
					}
				}

				reportData.trhData = trhDataList;
			}
		}

		if (data.getDoorData() != null && data.getDoorData().d != null) {
			List<ReportData.DoorData> doorDataList = new ArrayList<>();

			for (List<Integer> doorEntry : data.getDoorData().getD()) {
				if (doorEntry.size() >= 3) {
					ReportData.DoorData doorData = new ReportData.DoorData();
					doorDataList.add(doorData);
				} else {
					// Handle incomplete or malformed solChgData entries
					System.err.println("Incomplete solChgData entry: " + doorDataList);
				}
			}

			reportData.doorData = doorDataList;
		}

		return reportData;
	}

	public static void printReportData(ReportData reportData) {
		if (reportData == null) {
			System.out.println("ReportData is null.");
			return;
		}

		System.out.println("Gateway Header:");
		if (reportData.gatewayHeader != null) {
			for (ReportData.GatewayHeader gh : reportData.gatewayHeader) {
				if (gh != null) {
					System.out.println("  Protocol Version: " + gh.protocolVersion);
					System.out.println("  Sequence Number: " + gh.sequenceNumber);
					System.out.println("  Gateway Firmware Version: " + gh.gatewayFirmwareVer);
					System.out.println("  Customer ID: " + gh.customerID);
					System.out.println("  Gateway ID: " + gh.gatewayID);
				}
			}
		} else {
			System.out.println("  No Gateway Header data.");
		}

		System.out.println("\nConfig Stream Header:");
		if (reportData.configStreamHeader != null) {
			for (ReportData.ConfigStreamHeader csh : reportData.configStreamHeader) {
				if (csh != null) {
					System.out.println("  Stream Type: " + csh.streamType);
					System.out.println("  Peripheral Count: " + csh.peripheralCount);
					System.out.println("  Timestamp: " + csh.timestamp);
				}
			}
		} else {
			System.out.println("  No Config Stream Header data.");
		}

		System.out.println("\nPeripheral Entries:");
		if (reportData.peripheralEntry != null) {
			for (ReportData.PeripheralEntry pe : reportData.peripheralEntry) {
				if (pe != null) {
					System.out.println("  Parent Index: " + pe.parentIndex);
					System.out.println("  Device Type: " + pe.deviceType);
					System.out.println("  Manufacturer Code: " + pe.manufacturerCode);
					System.out.println("  Model Code: " + pe.modelCode);
					System.out.println("  Last Seen: " + pe.lastSeen);
					System.out.println("  Health Value: " + pe.healthValue);
				}
			}
		} else {
			System.out.println("  No Peripheral Entries data.");
		}

		System.out.println("\nDiagnostic Stream Header:");
		if (reportData.diagnosticStreamHeader != null) {
			for (ReportData.DiagnosticStreamHeader dsh : reportData.diagnosticStreamHeader) {
				if (dsh != null) {
					System.out.println("  Stream Type: " + dsh.streamType);
					System.out.println("  Peripheral ID: " + dsh.peripheralID);
					System.out.println("  Start Timestamp: " + dsh.startTimestamp);
				}
			}
		} else {
			System.out.println("  No Diagnostic Stream Header data.");
		}

		System.out.println("\nData Stream Header:");
		if (reportData.dataStreamHeader != null) {
			for (ReportData.DataStreamHeader dsh : reportData.dataStreamHeader) {
				if (dsh != null) {
					System.out.println("  Stream Type: " + dsh.streamType);
					System.out.println("  Peripheral ID: " + dsh.peripheralID);
					System.out.println("  Start Timestamp: " + dsh.startTimestamp);
				}
			}
		} else {
			System.out.println("  No Data Stream Header data.");
		}

		System.out.println("\nCPU Data:");
		if (reportData.cpuData != null) {
			for (ReportData.CpuData cd : reportData.cpuData) {
				if (cd != null) {
					System.out.println("  RAM Used: " + cd.ramUsed);
					System.out.println("  Storage Used: " + cd.storageUsed);
					System.out.println("  Avg CPU Utilization: " + cd.avgCpuUtilization);
					System.out.println("  CPU Awake Portion: " + cd.cpuAwakePortion);
				}
			}
		} else {
			System.out.println("  No CPU Data.");
		}

		// Add similar null checks for other lists (e.g., powerData, trhData, etc.)
	}
}
