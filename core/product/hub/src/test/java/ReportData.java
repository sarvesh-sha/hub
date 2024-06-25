

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
        // Generate nodes
        ReportData reportData = new ReportData();
        reportData.gatewayHeader = generateGatewayHeaders(10);
        reportData.configStreamHeader = generateConfigStreamHeaders(10);
        reportData.peripheralEntry = generatePeripheralEntries(10);
        reportData.diagnosticStreamHeader = generateDiagnosticStreamHeaders(10);
        reportData.dataStreamHeader = generateDataStreamHeaders(10);
        reportData.cpuData = generateCpuData(10);
        reportData.powerData = generatePowerData(10);
        reportData.trhData = generateTrhData(10);
        reportData.gnssData = generateGnssData(10);

        // Generate JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            mapper.writeValue(new File("c:\\montage\\reportData.json"), reportData);
            System.out.println("JSON written to reportData.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<GatewayHeader> generateGatewayHeaders(int count) {
        List<GatewayHeader> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GatewayHeader item = new GatewayHeader();
            item.protocolVersion = i;
            item.sequenceNumber = i;
            item.gatewayFirmwareVer = "v" + i;
            item.customerID = "cust" + i;
            item.gatewayID = "gw" + i;
            list.add(item);
        }
        return list;
    }

    private static List<ConfigStreamHeader> generateConfigStreamHeaders(int count) {
        List<ConfigStreamHeader> list = new ArrayList<>();
        Random random = new Random();
        long baseTime = System.currentTimeMillis() / 1000L;
        for (int i = 0; i < count; i++) {
            ConfigStreamHeader item = new ConfigStreamHeader();
            item.streamType = i;
            item.unused1 = i;
            item.peripheralCount = i;
            item.unused2 = i;
            item.timestamp = baseTime + random.nextInt(10 * 24 * 60 * 60); // Random time within 10 days
            list.add(item);
        }
        return list;
    }

    private static List<PeripheralEntry> generatePeripheralEntries(int count) {
        List<PeripheralEntry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PeripheralEntry item = new PeripheralEntry();
            item.parentIndex = i;
            item.deviceType = i;
            item.manufacturerCode = i;
            item.modelCode = i;
            item.lastSeen = System.currentTimeMillis() / 1000.0 + i * 100;
            item.healthValue = i;
            list.add(item);
        }
        return list;
    }

    private static List<DiagnosticStreamHeader> generateDiagnosticStreamHeaders(int count) {
        List<DiagnosticStreamHeader> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DiagnosticStreamHeader item = new DiagnosticStreamHeader();
            item.streamType = i;
            item.bytesPayload = i;
            item.dataType = i;
            item.peripheralID = i;
            item.startTimestamp = System.currentTimeMillis() / 1000.0 + i * 100;
            list.add(item);
        }
        return list;
    }

    private static List<DataStreamHeader> generateDataStreamHeaders(int count) {
        List<DataStreamHeader> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DataStreamHeader item = new DataStreamHeader();
            item.streamType = i;
            item.bytesPayload = i;
            item.dataType = i;
            item.peripheralID = i;
            item.startTimestamp = System.currentTimeMillis() / 1000.0 + i * 100;
            list.add(item);
        }
        return list;
    }

    private static List<CpuData> generateCpuData(int count) {
        List<CpuData> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CpuData item = new CpuData();
            item.ramUsed = i * 10.0;
            item.storageUsed = i * 20.0;
            item.avgCpuUtilization = i * 30.0;
            item.cpuAwakePortion = i * 40.0;
            list.add(item);
        }
        return list;
    }

    private static List<PowerData> generatePowerData(int count) {
        List<PowerData> list = new ArrayList<>();
        Random random = new Random();
        long baseTime = System.currentTimeMillis() / 1000L;
        for (int i = 0; i < count; i++) {
            PowerData item = new PowerData();
            item.voltage = i * 10.0;
            item.avgVoltage = i * 20.0;
            item.avgCurrent = i * 30.0;
            item.timeStamp = ZonedDateTime.ofInstant(Instant.ofEpochSecond(baseTime + random.nextInt(10 * 24 * 60 * 60)), ZoneOffset.UTC);
            list.add(item);
        }
        return list;
    }

    private static List<TrhData> generateTrhData(int count) {
        List<TrhData> list = new ArrayList<>();
        Random random = new Random();
        long baseTime = System.currentTimeMillis() / 1000L;
        for (int i = 0; i < count; i++) {
            TrhData item = new TrhData();
            item.temperature = i * 10.0;
            item.relativeHumidity = i * 20.0;
            item.voltage = i * 30.0;
            item.timeStamp = ZonedDateTime.ofInstant(Instant.ofEpochSecond(baseTime + random.nextInt(10 * 24 * 60 * 60)), ZoneOffset.UTC);
            list.add(item);
        }
        return list;
    }

    private static List<GnssData> generateGnssData(int count) {
        List<GnssData> list = new ArrayList<>();
        Random random = new Random();
        long baseTime = System.currentTimeMillis() / 1000L;
        for (int i = 0; i < count; i++) {
            GnssData item = new GnssData();
            item.altitude = i * 10.0;
            item.latitude = i * 20.0;
            item.longitude = i * 30.0;
            item.speed = i * 40.0;
            item.status = i * 50.0;
            item.timeStamp = ZonedDateTime.ofInstant(Instant.ofEpochSecond(baseTime + random.nextInt(10 * 24 * 60 * 60)), ZoneOffset.UTC);
            list.add(item);
        }
        return list;
    }
}
