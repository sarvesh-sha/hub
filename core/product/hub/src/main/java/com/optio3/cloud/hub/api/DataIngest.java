/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.hibernate.ObjectNotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.api.DeviceData.DeviceToServerData;
import com.optio3.cloud.hub.api.DeviceData.JsonParser;
import com.optio3.cloud.hub.api.DeviceData.ReportData;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.model.asset.Device;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration.InstanceState;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementSampleRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.asset.RelationshipRecord;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.hub.persistence.asset.AssetRecord.WellKnownMetadata;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.ipn.objects.IpnLocation;
import com.optio3.util.CollectionUtils;

import io.swagger.annotations.Api;

@Api(tags = { "DataIngest" }) // For Swagger
@Optio3RestEndpoint(name = "DataIngest") // For Optio3 Shell
@Path("/v1/data-ingest")
@Optio3RequestLogLevel(Severity.Debug)
public class DataIngest {
	@Inject
	private HubConfiguration m_cfg;

	@Optio3Dao
	private SessionProvider m_sessionProvider;

	// --//
	static final ZonedDateTime baseTime = ZonedDateTime.of(2023, 10, 12, 0, 0, 0, 1238, ZoneId.systemDefault());

	@GET
	@Path("demodata")
	@Produces(MediaType.APPLICATION_JSON)
	public boolean hasDemoData() {
		return m_cfg.developerSettings.includeDemoData;
	}

	@GET
	@Path("trigger-demo-messages")
	@Produces(MediaType.APPLICATION_JSON)
	public boolean triggerDemoMessages() {
		if (m_cfg.developerSettings.includeDemoData) {
			// TODO: trigger demo messages.

			return true;
		}

		return false;
	}

	private RecordLocator<GatewayAssetRecord> m_loc_gateway;
	private RecordLocator<NetworkAssetRecord> m_loc_network;

	protected void afterNetworkCreation(SessionHolder sessionHolder, GatewayAssetRecord rec_gateway,
			NetworkAssetRecord rec_network) throws Exception {
		rec_gateway.setWarningThreshold(2 * 24 * 60);
		rec_gateway.setAlertThreshold(3 * 24 * 60);
	}

	protected NetworkAssetRecord createInstanceId(SessionHolder sessionHolder, String instanceId,
			GatewayAssetRecord rec_gateway) throws Exception {
		RecordHelper<GatewayAssetRecord> helper_gateway = sessionHolder.createHelper(GatewayAssetRecord.class);
		RecordHelper<NetworkAssetRecord> helper_network = sessionHolder.createHelper(NetworkAssetRecord.class);
		RecordHelper<LocationRecord> helper_location = sessionHolder.createHelper(LocationRecord.class);

		LocationRecord rec_location = new LocationRecord();
		rec_location.setPhysicalName(String.format("Tracker for %s", instanceId));
		rec_location.setType(LocationType.TRUCK);
		helper_location.persist(rec_location);

		// --//

		NetworkAssetRecord rec_network = new NetworkAssetRecord();
		rec_network.setPhysicalName(String.format("Network for %s", instanceId));
		rec_network.setLocation(rec_location);
		rec_network.setSamplingPeriod(1800);
		helper_network.persist(rec_network);

		// --//

		rec_gateway.setPhysicalName(String.format("Gateway for %s", instanceId));
		rec_gateway.setLocation(rec_location);
		rec_gateway.setState(AssetState.passive);
		helper_gateway.persist(rec_gateway);

		rec_gateway.getBoundNetworks().add(rec_network);

		return rec_network;
	}

	public List<AssetRecord> createDevice(@QueryParam("physicalName") String deviceId) throws Exception {
		String id_root = null;
		String id_eqip = null;
		List<AssetRecord> assetRecords = new ArrayList<AssetRecord>();
		AssetRecord rec_root1 = null;
		try {
			String id_child1 = null;
			String id_child2 = null;

			try (SessionHolder holder = m_sessionProvider.newSessionWithTransaction()) {
				RecordHelper<LogicalAssetRecord> helper_assetLogical = holder.createHelper(LogicalAssetRecord.class);
				LocationRecord rec_loc = new LocationRecord();
				rec_loc.setDisplayName(deviceId + " # Trailer");
				rec_loc.setType(LocationType.GARAGE);
				holder.persistEntity(rec_loc);
				LogicalAssetRecord lrec = new LogicalAssetRecord();

				lrec.modifyTags((tags) -> {
					tags.addTag(AssetRecord.WellKnownTags.isEquipment, false);
					tags.setValuesForTag(AssetRecord.WellKnownTags.equipmentClassId, Sets.newHashSet("4096"));
				});

				// lrec.setSysId("1893");
				lrec.setPhysicalName(deviceId + "_equip");

				lrec.setLocation(rec_loc);
				helper_assetLogical.persist(lrec);
				id_eqip = lrec.getSysId();
				RecordHelper<AssetRecord> assetHelper = holder.createHelper(AssetRecord.class);

				String instanceId = String.format("%s__%d", 10, 10);
				InstanceState state = resolveInstanceId(holder, "Test MG14");
				m_loc_gateway = holder.createLocator(state.rec_gateway);
				m_loc_network = holder.createLocator(state.rec_network);
				RecordHelper<AssetRecord> helper_asset = holder.createHelper(AssetRecord.class);
				Device device = new Device();
				device.firmwareVersion = "dsds";

				rec_root1 = device.newRecord();
				rec_root1.setSysId(deviceId);
				rec_root1.modifyTags((tags) -> {
					tags.addTag(AssetRecord.WellKnownTags.isEquipment, false);
					tags.setValuesForTag(AssetRecord.WellKnownTags.equipmentClassId, Sets.newHashSet("8193"));
				});
				rec_root1.setState(AssetState.operational);
				rec_root1.setPhysicalName(deviceId);// 181235678910991
				rec_root1.setLocation(rec_loc); // rec_root.setFirmwareVersion("root1");

				helper_asset.persist(rec_root1);
				RelationshipRecord.addRelation(holder, lrec, rec_root1, AssetRelationship.controls);
				System.out.println("lrec : 	" + lrec.getSysId() + " rec_root : 	" + rec_root1.getSysId()
						+ " rec_loc : 	" + rec_loc.getSysId());
				// rec_root.linkToParent(assetHelper, lrec);
				// sessionHolder.persistEntity(rec_root);
				id_root = rec_root1.getSysId();
				System.out.printf("id_root: %s%n", id_root);

				String id_loc = rec_loc.getSysId();
				IpnLocation obj = new IpnLocation();
				obj.latitude = 123456.1234;
				obj.longitude = 123456.1234;
				obj.altitude = 1000;

				DeviceElementRecord rec_child3 = new DeviceElementRecord();
				rec_child3.setDisplayName("Latitude");
				rec_child3.setPointClassId("4098");
				rec_child3.setIdentifier("latitude");
				rec_child3.setContents(IpnLocation.getObjectMapper(), obj);
				// rec_child3.setLocation(rec_loc);
				// rec_child3.setPointClassId(WellKnownPointClass. .value());
				assetHelper.persist(rec_child3);
				
				

				String id_child3 = rec_child3.getSysId();
				System.out.printf("id_child3: %s%n", id_child3);

				rec_child3.linkToParent(assetHelper, rec_root1);

				DeviceElementRecord rec_child4 = new DeviceElementRecord();
				rec_child4.setDisplayName("Longitude");
				rec_child4.setIdentifier("longitude");
				rec_child4.setPointClassId("4097");
				rec_child4.setContents(IpnLocation.getObjectMapper(), obj);
	
				assetHelper.persist(rec_child4);
				// rec_child4.setLocation(rec_loc);
				// rec_child4.setPointClassId(WellKnownPointClass.LocationLongitude.name());
				String id_child4 = rec_child4.getSysId();
				System.out.printf("id_child4: %s%n", id_child4);
				rec_child4.linkToParent(assetHelper, rec_root1);

				RelationshipRecord.addRelation(holder, rec_root1, rec_child3, AssetRelationship.controls);

				RelationshipRecord.addRelation(holder, rec_root1, rec_child4, AssetRelationship.controls);

				AssetRecord rec_root = device.newRecord();
				rec_root.setSysId(deviceId + "_1");

				rec_root.modifyTags((tags) -> {
					tags.addTag(AssetRecord.WellKnownTags.isEquipment, false);
					tags.setValuesForTag(AssetRecord.WellKnownTags.equipmentClassId, Sets.newHashSet("8194"));
				});
				rec_root.setState(AssetState.operational);
				rec_root.setPhysicalName(deviceId);// 181235678910991
				rec_root.setLocation(rec_loc); // rec_root.setFirmwareVersion("root1");

				helper_asset.persist(rec_root);
				RelationshipRecord.addRelation(holder, lrec, rec_root, AssetRelationship.controls);
				System.out.println("lrec : 	" + lrec.getSysId() + " rec_root : 	" + rec_root.getSysId()
						+ " rec_loc : 	" + rec_loc.getSysId());
				// rec_root.linkToParent(assetHelper, lrec);
				// sessionHolder.persistEntity(rec_root);
				String id_root1 = rec_root.getSysId();
				// id_root = id_root + "," + id_root1;
				System.out.printf("id_root: %s%n", id_root1);
				DeviceElementRecord rec_child1 = new DeviceElementRecord();
				rec_child1.setDisplayName("Altitude");
				rec_child1.setIdentifier("Altitude");
				rec_child1.setPointClassId("4100");
				rec_child1.setEquipmentClassId("8194");
				// rec_child1.setLocation(null);
				assetHelper.persist(rec_child1);
				id_child1 = rec_child1.getSysId();
				System.out.printf("id_child1: %s%n", id_child1);
				rec_child1.linkToParent(assetHelper, rec_root);

				DeviceElementRecord rec_child2 = new DeviceElementRecord();
				rec_child2.setDisplayName("Temprature");
				rec_child2.setIdentifier("Temprature");
				rec_child2.setPointClassId("45057");
				rec_child2.setEquipmentClassId("8194");
				assetHelper.persist(rec_child2);
				id_child2 = rec_child2.getSysId();
				System.out.printf("id_child2: %s%n", id_child2);
				rec_child2.linkToParent(assetHelper, rec_root);

				DeviceElementRecord rec_child5 = new DeviceElementRecord();
				rec_child5.setDisplayName("Main Voltage");
				rec_child5.setIdentifier("Main Voltage");
				assetHelper.persist(rec_child5);
				rec_child5.setPointClassId("12289");
				rec_child5.setEquipmentClassId("8194");
				String id_child5 = rec_child5.getSysId();
				System.out.printf("id_child4: %s%n", id_child5);
				rec_child5.linkToParent(assetHelper, rec_root);

				DeviceElementRecord rec_child6 = new DeviceElementRecord();
				rec_child6.setDisplayName("Humidity");
				rec_child6.setIdentifier("humidity");
				rec_child6.setPointClassId("12289");
				rec_child6.setEquipmentClassId("8194");
				assetHelper.persist(rec_child6);
				String id_child6 = rec_child6.getSysId();
				System.out.printf("id_child6: %s%n", id_child6);
				rec_child6.linkToParent(assetHelper, rec_root);

				DeviceElementRecord rec_child7 = new DeviceElementRecord();
				rec_child7.setDisplayName("Voltage Power");
				rec_child7.setIdentifier("voltage_power");
				rec_child7.setEquipmentClassId("8194");
				rec_child7.setPointClassId("12289");
				assetHelper.persist(rec_child7);
				String id_child7 = rec_child7.getSysId();
				System.out.printf("id_child7: %s%n", id_child7);
				rec_child7.linkToParent(assetHelper, rec_root);

				DeviceElementRecord rec_child8 = new DeviceElementRecord();
				rec_child8.setDisplayName("Average Voltage Power");
				rec_child8.setIdentifier("average_voltage_power");
				rec_child8.setPointClassId("12289");
				rec_child8.setEquipmentClassId("8194");
				assetHelper.persist(rec_child8);
				String id_child8 = rec_child6.getSysId();
				System.out.printf("id_child6: %s%n", id_child8);
				rec_child8.linkToParent(assetHelper, rec_root);

				DeviceElementRecord rec_child9 = new DeviceElementRecord();
				rec_child9.setDisplayName("Average Current Power");
				
				
				rec_child9.setIdentifier("average_current_power");
				rec_child9.setPointClassId("12289");
				rec_child9.setEquipmentClassId("8194");
				assetHelper.persist(rec_child9);
				String id_child9 = rec_child6.getSysId();
				System.out.printf("id_child6: %s%n", id_child9);
				rec_child9.linkToParent(assetHelper, rec_root);
				rec_child9.setPropertyTypeExtractorClass(TypeExtractor.class);
				DeviceElementRecord rec_child10 = new DeviceElementRecord();
				rec_child10.setDisplayName("Ground Speed");
				rec_child10.setIdentifier("ground_speed");
				
				assetHelper.persist(rec_child10);
				String id_child10 = rec_child10.getSysId();
				 MetadataMap metadata = rec_child10.getMetadata();
				
				 rec_child10.setAssignedUnits(com.optio3.protocol.model.EngineeringUnits.kilometers_per_hour);
				 
				 rec_child10.assignTags(Sets.newHashSet("speed","velocity") , false,true,false);
				
				System.out.printf("id_child9: %s%n", id_child10);
				rec_child10.linkToParent(assetHelper, rec_root);
				
				
				
				DeviceElementRecord rec_child11 = new DeviceElementRecord();
				rec_child11.setDisplayName("Ground Speed");
				rec_child11.setIdentifier("ground_speed");
				assetHelper.persist(rec_child11);
				String id_child11 = rec_child11.getSysId();
				System.out.printf("id_child9: %s%n", id_child11);
				rec_child11.linkToParent(assetHelper, rec_root);
				
				
				RelationshipRecord.addRelation(holder, rec_root, rec_child1, AssetRelationship.controls);
				RelationshipRecord.addRelation(holder, rec_root, rec_child2, AssetRelationship.controls);
				
				RelationshipRecord.addRelation(holder, rec_root, rec_child5, AssetRelationship.controls);
				RelationshipRecord.addRelation(holder, rec_root, rec_child6, AssetRelationship.controls);
				RelationshipRecord.addRelation(holder, rec_root, rec_child7, AssetRelationship.controls);
				RelationshipRecord.addRelation(holder, rec_root, rec_child8, AssetRelationship.controls);
				RelationshipRecord.addRelation(holder, rec_root, rec_child9, AssetRelationship.controls);
				RelationshipRecord.addRelation(holder, rec_root, rec_child10, AssetRelationship.controls);

				holder.commit();
				assetRecords.add(rec_root1);	
				assetRecords.add(rec_root);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return assetRecords;
	}

	public final InstanceState resolveInstanceId(SessionHolder sessionHolder, String instanceId) throws Exception {
		RecordHelper<GatewayAssetRecord> helper_gateway = sessionHolder.createHelper(GatewayAssetRecord.class);

		InstanceState res = new InstanceState();

		TypedRecordIdentity<GatewayAssetRecord> ri_gateway = GatewayAssetRecord.findByInstanceId(helper_gateway,
				instanceId);
		if (ri_gateway == null) {
			helper_gateway.lockTableUntilEndOfTransaction(10, TimeUnit.SECONDS);

			res.createdGateway = true;
			res.rec_gateway = new GatewayAssetRecord();
			res.rec_gateway.setInstanceId(instanceId);

			// if (shouldNotifyNewGateway(instanceId)) {
			// res.rec_gateway.putMetadata(GatewayAssetRecord.WellKnownMetadata.reportAsNew,
			// true);
			// }

			res.rec_network = createInstanceId(sessionHolder, instanceId, res.rec_gateway);
			if (res.rec_network != null) {
				res.createdNetwork = true;

				afterNetworkCreation(sessionHolder, res.rec_gateway, res.rec_network);
			} else {
				helper_gateway.persist(res.rec_gateway);
			}
		} else {
			res.rec_gateway = sessionHolder.fromIdentity(ri_gateway);
			res.rec_network = CollectionUtils.firstElement(res.rec_gateway.getBoundNetworks());
		}

		return res;
	}

	@POST
	@Path("simulated-gateway/createDeviceElementWithArray")
	@Consumes(MediaType.APPLICATION_JSON)
	public String createDeviceElementWithArray(String input) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		List<DeviceToServerData> dataObjects = objectMapper.readValue(input, new TypeReference<List<DeviceToServerData>>() {});
    	for(DeviceToServerData data : dataObjects)
    	{
    		createDeviceElement(data);
    	}
    	return "success";
	}
	
	@POST
	@Path("simulated-gateway/createDeviceElement")
	@Consumes(MediaType.APPLICATION_JSON)
	public String createDeviceElement(String input)
	{
		
		System.out.println("input " + input);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			DeviceToServerData createDeviceElement = objectMapper.readValue(input, DeviceToServerData.class);
			
			try {
				createDeviceElement(createDeviceElement);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "success";
	}
	
	public String createDeviceElement(DeviceToServerData input) throws Exception {
		System.out.println("input " + input);

		ObjectMapper objectMapper = new ObjectMapper();
		String gatewayId = null;
	//	ReportData reportData = null;
		double latitude = 0;
		double longitude = 0;
		double altitude = 0;
		double voltage = 0;
		double temprature = 0;
		double humidity = 0; 
		double voltagePower = 0;
		double voltageAvgPower = 0;
		double averageCurrentPower = 0;
		double speed = 0;
		
		try {
			// Replace 'input.json' with the path to your JSON file
			
		//	reportData = JsonParser.mapToReportData(input);
			JsonParser jsonParser = new JsonParser();
			ReportData   reportData = jsonParser.mapToReportData(input);
			// reportData = mapper.readValue(input, ReportData.class);
			System.out.println("Parsed JSON Data:");
			System.out.println("Gateway Header Protocol Version: " + reportData.gatewayHeader.get(0).gatewayID);
			gatewayId = reportData.gatewayHeader.get(0).gatewayID;
			//System.out.println("Event Processed UTC Time: " + reportData.eventProcessedUtcTime);

			if (reportData.gnssData != null && reportData.gnssData.size() > 0) {
				latitude = reportData.gnssData.get(0).latitude;
				longitude = reportData.gnssData.get(0).longitude;
				altitude = reportData.gnssData.get(0).altitude;
				speed = reportData.gnssData.get(0).speed;
				System.out.println("altitude ***" + altitude);
			}
			if (reportData.powerData != null && reportData.powerData.size() > 0) {
				System.out.println("voltage *** " + voltage);
				voltagePower = reportData.powerData.get(0).voltage;
				voltageAvgPower = reportData.powerData.get(0).avgVoltage;
				averageCurrentPower = reportData.powerData.get(0).avgCurrent;
			}

			if (reportData.trhData != null && reportData.trhData.size() > 0) {
				System.out.println("temprature ***" + temprature);
				temprature = reportData.trhData.get(0).temperature;
				humidity = reportData.trhData.get(0).relativeHumidity;
				voltage = reportData.trhData.get(0).voltage;

			}
			// Continue to print other fields as necessary
	

		String id_child1 = null;
		String id_child2 = null;

		try (SessionHolder holder = m_sessionProvider.newSessionWithTransaction()) {
			double latitude1 = latitude;
			double longitude1 = longitude;
			double altitude1 = altitude;
			double voltage1 = voltage;
			double temprature1 = temprature;
			double humidity1 = humidity;
			double voltagePower1 = voltagePower;
			double voltageAvgPower1 = voltageAvgPower;
			double averageCurrentPower1 = averageCurrentPower;
			double speed1 = speed;
		/*	Long timestamp = reportData.configStreamHeader.get(0).timestamp;
			System.out.println("Event Processed UTC Time: " + timestamp);
			Instant i = Instant.ofEpochSecond(timestamp);
			System.out.println("Event Processed UTC Time: " + i);

			ZonedDateTime t = ZonedDateTime.ofInstant(i, ZoneOffset.UTC);

			System.out.println("Event Processed UTC Time: " + t);
			System.out.println("timestamp" + timestamp);
*/
			RecordHelper<AssetRecord> assetHelper = holder.createHelper(AssetRecord.class);

			RecordHelper<DeviceElementRecord> dHelper = holder.createHelper(DeviceElementRecord.class);
			RecordHelper<DeviceElementSampleRecord> helperSample = holder.createHelper(DeviceElementSampleRecord.class);
			AssetRecord rec_root = null;
			AssetRecord rec_root1 = null;
			try {
				rec_root = assetHelper.get(gatewayId);
				rec_root1 = assetHelper.get(gatewayId + "_1");
			} catch (ObjectNotFoundException ex) {
				List<AssetRecord> assetRecords = createDevice(gatewayId);
				rec_root = assetRecords.get(0);
				rec_root1 = assetRecords.get(1);
			}
			/*
			 * if (rec_root1 == null)
			 * 
			 * { createDevice(gatewayId); }
			 */
			String instanceId = String.format("%s__%d", 10, 10);
			InstanceState state = resolveInstanceId(holder, instanceId);
			m_loc_gateway = holder.createLocator(state.rec_gateway);
			m_loc_network = holder.createLocator(state.rec_network);

			RecordHelper<AssetRecord> helper_asset = holder.createHelper(AssetRecord.class);
			Device device = new Device();
			device.firmwareVersion = "dsds";
			String[] idRootList = gatewayId.split(",");

			// System.out.println(" rec_root " + rec_root);
			// System.out.println(" rec_root " + rec_root.getPhysicalName());
			// rec_root = assetHelper.get(gatewayId);

			rec_root.enumerateChildren(holder.createHelper(DeviceElementRecord.class), true, -1, null, (rec_child) -> {
				System.out.printf("  child: %s%n", rec_child.getSysId() + rec_child.getDisplayName());

				try (DeviceElementRecord.ArchiveDescriptor lazy_archive = rec_child.ensureArchive(helperSample,
						baseTime)) {
					// DeviceElementSampleRecord rec_archive = lazy_archive.getRecord();

					TimeSeries ts = lazy_archive.getTimeSeries();
					System.out.println("rec_child.getDisplayName()" + rec_child.getDisplayName());
					if (rec_child.getDisplayName().equals("Altitude") && reportData.gnssData != null && reportData.gnssData.size() > 0) {

						ts.addSample(TimeSeries.SampleResolution.Max1Hz, reportData.gnssData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 3, reportData.gnssData.get(0).altitude);
						System.out.println("altitude123 ***" + altitude1 + " "+reportData.gnssData.get(0).timeStamp);
					}
					if (rec_child.getDisplayName().equals("Latitude") && reportData.gnssData != null && reportData.gnssData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz,  reportData.gnssData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 10, reportData.gnssData.get(0).latitude);
						System.out.println("latitude113 ***" + latitude1 + " "+reportData.gnssData.get(0).timeStamp);
					}
					if (rec_child.getDisplayName().equals("Longitude") && reportData.gnssData != null && reportData.gnssData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz,  reportData.gnssData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 10, reportData.gnssData.get(0).longitude);
						System.out.println("altitude123 ***" + longitude1 + " "+reportData.gnssData.get(0).timeStamp);
					}

					if (rec_child.getDisplayName().equals("Temprature") && reportData.trhData != null && reportData.trhData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz, reportData.trhData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 3, temprature1);
						System.out.println("temprature1113 ***" + temprature1 + " "+reportData.trhData.get(0).timeStamp );
					}

					if (rec_child.getDisplayName().equals("Main Voltage") && reportData.trhData != null && reportData.trhData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz, reportData.trhData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 3, voltage1);
						System.out.println("voltage11113 ***" + voltage1 + " "+reportData.trhData.get(0).timeStamp);
					}
					if (rec_child.getDisplayName().equals("Humidity") && reportData.trhData != null && reportData.trhData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz, reportData.trhData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 3, humidity1);
						System.out.println("humidity111113 ***" + humidity1 + " "+reportData.trhData.get(0).timeStamp);
					}
					if (rec_child.getDisplayName().equals("Voltage Power") && reportData.powerData != null && reportData.powerData.size() > 0 ) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz, reportData.powerData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 3, voltagePower1);
						System.out.println("voltagePower11113 ***" + voltagePower1 + " "+reportData.powerData.get(0).timeStamp);
					}
					if (rec_child.getDisplayName().equals("Average Current Power") && reportData.powerData != null && reportData.powerData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz, reportData.powerData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 3, averageCurrentPower1);
						System.out.println("averageCurrentPower111113 ***" + averageCurrentPower1 + " "+reportData.powerData.get(0).timeStamp);
					}
					if (rec_child.getDisplayName().equals("Ground Speed") && reportData.powerData != null && reportData.powerData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz, reportData.powerData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 3, speed1);
						System.out.println("speed13 ***" + speed1 + " "+reportData.powerData.get(0).timeStamp);
					}

					// TimeSeries ts =rec_archive.getTimeSeries();
					// generateSeries(baseTime, ts, 0);

				//	System.out.println("dada111" + ts);

				//	System.out.println("dada222" + ts.numberOfProperties());

				}

				return StreamHelperNextAction.Continue;
			});

			rec_root1.enumerateChildren(holder.createHelper(DeviceElementRecord.class), true, -1, null, (rec_child) -> {
				System.out.printf("  child: %s%n", rec_child.getSysId() + rec_child.getDisplayName());

				try (DeviceElementRecord.ArchiveDescriptor lazy_archive = rec_child.ensureArchive(helperSample,
						baseTime)) {
					// DeviceElementSampleRecord rec_archive = lazy_archive.getRecord();

					TimeSeries ts = lazy_archive.getTimeSeries();
					System.out.println("rec_child.getDisplayName()" + rec_child.getDisplayName());
					if (rec_child.getDisplayName().equals("Altitude") && reportData.gnssData != null && reportData.gnssData.size() > 0) {

						ts.addSample(TimeSeries.SampleResolution.Max1Hz, reportData.gnssData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 3, reportData.gnssData.get(0).altitude);
						System.out.println("altitude123 ***" + altitude1);
					}
					if (rec_child.getDisplayName().equals("Latitude") && reportData.gnssData != null && reportData.gnssData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz,  reportData.gnssData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 10, reportData.gnssData.get(0).latitude);
						System.out.println("latitude113 ***" + latitude1 + " "+reportData.gnssData.get(0).timeStamp);
					}
					if (rec_child.getDisplayName().equals("Longitude") && reportData.gnssData != null && reportData.gnssData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz,  reportData.gnssData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 10, reportData.gnssData.get(0).longitude);
						System.out.println("Longitude ***" + longitude1 + " "+reportData.gnssData.get(0).timeStamp);
					}

					if (rec_child.getDisplayName().equals("Temprature") && reportData.trhData != null && reportData.trhData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz, reportData.trhData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 3, temprature1);
						System.out.println("temprature1113 ***" + temprature1 + " "+reportData.trhData.get(0).timeStamp);
					}

					if (rec_child.getDisplayName().equals("Main Voltage") && reportData.trhData != null && reportData.trhData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz, reportData.trhData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 3, voltage1);
						System.out.println("voltage11113 ***" + voltage1 + " "+reportData.trhData.get(0).timeStamp);
					}
					if (rec_child.getDisplayName().equals("Humidity") && reportData.trhData != null && reportData.trhData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz, reportData.trhData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 3, humidity1);
						System.out.println("humidity111113 ***" + humidity1 + " "+reportData.trhData.get(0).timeStamp);
						}
					if (rec_child.getDisplayName().equals("Voltage Power") && reportData.powerData != null && reportData.powerData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz, reportData.powerData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 3, voltagePower1);
						System.out.println("voltagePower11113 ***" + voltagePower1 + " "+reportData.powerData.get(0).timeStamp);
					}
					if (rec_child.getDisplayName().equals("Average Current Power") && reportData.powerData != null && reportData.powerData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz, reportData.powerData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 3, averageCurrentPower1);
						System.out.println("averageCurrentPower111113 ***" + averageCurrentPower1 + " "+reportData.powerData.get(0).timeStamp);
					}
					if (rec_child.getDisplayName().equals("Ground Speed") && reportData.powerData != null && reportData.powerData.size() > 0) {

						// ZonedDateTime t = ZonedDateTime.now();
						ts.addSample(TimeSeries.SampleResolution.Max1Hz, reportData.powerData.get(0).timeStamp, "present_value",
								TimeSeries.SampleType.Decimal, 3, speed1);
						System.out.println("speed13 ***" + speed1 + " "+reportData.powerData.get(0).timeStamp);
					}

					// TimeSeries ts =rec_archive.getTimeSeries();
					// generateSeries(baseTime, ts, 0);

					//System.out.println("dada111" + ts);

					//System.out.println("dada222" + ts.numberOfProperties());

				}

				return StreamHelperNextAction.Continue;
			});

			holder.commit();

		} catch (

		Exception e) {
			e.printStackTrace();
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "success";
	}

	@POST
	@Path("simulated-gateway/deviceelement")
	@Consumes(MediaType.APPLICATION_JSON)
	public String createDeviceElement(@QueryParam("identifier") String identifier,
			@QueryParam("latitude") double latitude, @QueryParam("longitude") double longitude,
			@QueryParam("altitude") int altitude, @QueryParam("temperature") double temperature,
			@QueryParam("timestamp") String timestamp, @QueryParam("sysId") String sysId,
			@QueryParam("odometer") double odometer, @QueryParam("main_voltage") double main_voltage,
			@QueryParam("battery_voltage") double battery_voltage, @QueryParam("aux_voltage") double aux_voltage,
			@QueryParam("ign_status") String ign_status, @QueryParam("event_name") String event_name,
			@QueryParam("engine_hours") double engine_hours) throws Exception {

		try (SessionHolder holder = m_sessionProvider.newSessionWithTransaction()) {
			System.out.println("timestamp111" + timestamp);

			Instant instant = Instant.ofEpochSecond(Long.parseLong(timestamp));

			// Specify the time zone (you can change the ZoneId according to your
			// requirement)
			ZoneId zoneId = ZoneId.of("America/New_York");

			// Convert Instant to ZonedDateTime using the specified time zone
			ZonedDateTime t = instant.atZone(zoneId);

			// Print the ZonedDateTime
			System.out.println("ZonedDateTime: " + t);
			// ZonedDateTime t = ZonedDateTime.parse(timestamp);
			RecordHelper<AssetRecord> assetHelper = holder.createHelper(AssetRecord.class);

			RecordHelper<DeviceElementRecord> dHelper = holder.createHelper(DeviceElementRecord.class);
			RecordHelper<DeviceElementSampleRecord> helperSample = holder.createHelper(DeviceElementSampleRecord.class);

			String instanceId = String.format("%s__%d", 10, 10);
			InstanceState state = resolveInstanceId(holder, instanceId);
			m_loc_gateway = holder.createLocator(state.rec_gateway);
			m_loc_network = holder.createLocator(state.rec_network);

			RecordHelper<AssetRecord> helper_asset = holder.createHelper(AssetRecord.class);

			String[] idRootList = identifier.split(",");
			for (String id_root1 : idRootList) {
				System.out.println(" rec_root111 " + id_root1);
				AssetRecord rec_root = assetHelper.get(id_root1);
				if (rec_root == null)

				{
					createDevice(id_root1);
				}

				System.out.println(" rec_root " + rec_root);
				System.out.println(" rec_root " + rec_root.getPhysicalName());

				rec_root.enumerateChildren(holder.createHelper(DeviceElementRecord.class), true, -1, null,
						(rec_child) -> {
							System.out.printf("  child: %s%n", rec_child.getSysId() + rec_child.getDisplayName());

							try (DeviceElementRecord.ArchiveDescriptor lazy_archive = rec_child
									.ensureArchive(helperSample, baseTime)) {
								// DeviceElementSampleRecord rec_archive = lazy_archive.getRecord();

								TimeSeries ts = lazy_archive.getTimeSeries();
								if (rec_child.getDisplayName().equals("Altitude")) {

									// ZonedDateTime t = ZonedDateTime.now();
									ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "present_value",
											TimeSeries.SampleType.Decimal, 3, altitude);
								}
								if (rec_child.getDisplayName().equals("Latitude")) {

									// ZonedDateTime t = ZonedDateTime.now();
									ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "present_value",
											TimeSeries.SampleType.Decimal, 10, latitude);
								}
								if (rec_child.getDisplayName().equals("Longitude")) {

									// ZonedDateTime t = ZonedDateTime.now();
									ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "present_value",
											TimeSeries.SampleType.Decimal, 10, longitude);
								}

								if (rec_child.getDisplayName().equals("Temprature")) {

									// ZonedDateTime t = ZonedDateTime.now();
									ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "present_value",
											TimeSeries.SampleType.Decimal, 3, temperature);
								}

								if (rec_child.getIdentifier().equals("Temprature")) {

									// ZonedDateTime t = ZonedDateTime.now();
									ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "present_value",
											TimeSeries.SampleType.Decimal, 3, temperature);
								}

								if (rec_child.getIdentifier().equals("Temprature")) {

									// ZonedDateTime t = ZonedDateTime.now();
									ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "present_value",
											TimeSeries.SampleType.Decimal, 3, temperature);
								}

								if (rec_child.getIdentifier().equals("main_voltage")) {

									// ZonedDateTime t = ZonedDateTime.now();
									ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "present_value",
											TimeSeries.SampleType.Decimal, 3, main_voltage);
								}

								if (rec_child.getIdentifier().equals("battery_voltage")) {

									// ZonedDateTime t = ZonedDateTime.now();
									ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "present_value",
											TimeSeries.SampleType.Decimal, 3, battery_voltage);
								}

								if (rec_child.getIdentifier().equals("aux_voltage")) {

									// ZonedDateTime t = ZonedDateTime.now();
									ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "present_value",
											TimeSeries.SampleType.Decimal, 3, aux_voltage);
								}

								if (rec_child.getIdentifier().equals("odometer")) {

									// ZonedDateTime t = ZonedDateTime.now();
									ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "present_value",
											TimeSeries.SampleType.Decimal, 3, odometer);
								}

								if (rec_child.getIdentifier().equals("engine_hours")) {

									// ZonedDateTime t = ZonedDateTime.now();
									ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "present_value",
											TimeSeries.SampleType.Decimal, 3, engine_hours);
								}

								// TimeSeries ts =rec_archive.getTimeSeries();
								// generateSeries(baseTime, ts, 0);

								System.out.println("dada111" + ts);

								System.out.println("dada222" + ts.numberOfProperties());

							}

							return StreamHelperNextAction.Continue;
						});
			}

			holder.commit();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "success";
	}

}
