/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3NoAuthenticationNeeded;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.jwt.CookieAuthRequestFilter;
import com.optio3.cloud.hub.DataConnectionSite;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.model.ControllerMetadataAggregation;
import com.optio3.cloud.hub.model.EquipmentAggregation;
import com.optio3.cloud.hub.model.EquipmentHierarchy;
import com.optio3.cloud.hub.model.MetadataAggregation;
import com.optio3.cloud.hub.model.MetadataAggregationPoint;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.model.normalization.DeviceElementClassificationMetadata;
import com.optio3.cloud.hub.model.normalization.NormalizationEquipmentLocation;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.asset.RelationshipRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceTypedValue;
import com.optio3.cloud.hub.persistence.dataconnector.DataConnectionRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.hub.persistence.normalization.NormalizationRecord;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.MetadataTagsMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.logging.Severity;
import com.optio3.metadata.normalization.ImportExportData;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.function.ConsumerWithException;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;

@Api(tags = { "DataConnection" }) // For Swagger
@Optio3RestEndpoint(name = "DataConnection") // For Optio3 Shell
@Path("/v1/data-connection")
public class DataConnection
{
    private static final String c_unknownBuildingName = "Unknown";

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @Inject
    private CookieAuthRequestFilter m_filter;

    @GET
    @Path("metadata-aggregation")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public MetadataAggregation metadataAggregation(@QueryParam("unclassified") boolean unclassified) throws
                                                                                                     Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            MetadataAggregation      result             = new MetadataAggregation();
            Multimap<String, String> buildingEquipments = HashMultimap.create();

            Multimap<String, String> groups = RelationshipRecord.fetchAllMatchingRelations(sessionHolder, AssetRelationship.controls);

            // Add default lookup for missing buildings for Tableau
            buildingEquipments.put(c_unknownBuildingName, null);

            for (String groupId : groups.keySet())
            {
                AssetRecord    rec_group    = sessionHolder.getEntity(AssetRecord.class, groupId);
                LocationRecord rec_location = rec_group.getLocation();

                MetadataMap metadata     = rec_group.getMetadata();
                String      equipClassId = AssetRecord.WellKnownMetadata.equipmentClassID.get(metadata);

                String building = rec_location != null ? rec_location.getName() : null;
                if (StringUtils.isEmpty(building))
                {
                    building = c_unknownBuildingName;
                }

                buildingEquipments.put(building, groupId);
                result.equipmentNames.put(groupId, rec_group.getName());
                result.equipmentClassIds.put(groupId, equipClassId);
            }

            enumerateDevices(sessionHolder, (rec_device) ->
            {
                result.controllerNames.putIfAbsent(rec_device.getSysId(), rec_device.getName());
            });

            result.buildingEquipments = buildingEquipments.asMap();
            return result;
        }
    }

    @GET
    @Path("metadata-aggregation/controller/{controllerId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public ControllerMetadataAggregation controllerMetadataAggregation(@PathParam("controllerId") String controllerId,
                                                                       @QueryParam("unclassified") boolean unclassified,
                                                                       @QueryParam("belowThresholdId") Integer belowThresholdId) throws
                                                                                                                                 Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeviceRecord                  rec_controller = sessionHolder.getEntity(DeviceRecord.class, controllerId);
            ControllerMetadataAggregation result         = buildControllerResult(rec_controller);
            Map<String, String>           pointToEquip   = Maps.newHashMap();

            LocationsEngine          locationsEngine   = sessionHolder.getServiceNonNull(LocationsEngine.class);
            LocationsEngine.Snapshot locationsSnapshot = locationsEngine.acquireSnapshot(false);

            RelationshipRecord.fetchAllMatchingRelations(sessionHolder, AssetRelationship.controls)
                              .forEach((equipSys, childSysId) ->
                                       {
                                           pointToEquip.put(childSysId, equipSys);
                                       });

            DeviceElementFilterRequest filters = getDeviceElementFilters(rec_controller, unclassified);

            enumeratePoints(sessionHolder, filters, (rec_object) ->
            {
                MetadataAggregationPoint point = new MetadataAggregationPoint();
                ImportExportData         item  = rec_controller.extractImportExportData(locationsSnapshot, rec_object);

                point.pointId      = rec_object.getSysId();
                point.pointNameRaw = BACnetObjectModel.extractName(item.deviceName, item.deviceDescription, true);
                point.identifier   = rec_object.getIdentifier();

                MetadataMap     metadata = rec_object.getMetadata();
                MetadataTagsMap tags     = rec_object.accessTags();

                LocationRecord rec_location = rec_object.getLocation();

                DeviceElementClassificationMetadata lastClassification = DeviceElementClassificationMetadata.fromMetadata(metadata);
                String                              building           = getBuildingName(lastClassification.locations);
                String                              pointClassId       = rec_object.getPointClassId();

                String pointNameBackup = AssetRecord.WellKnownMetadata.nameFromLegacyImport.get(metadata);

                if (!shouldReturnPoint(unclassified, pointClassId, lastClassification))
                {
                    // We're only concerned with points that have both at the moment.
                    return;
                }

                point.pointName       = BoxingUtils.get(lastClassification.pointName, rec_object.getName());
                point.buildingId      = building;
                point.equipmentId     = pointToEquip.get(point.pointId);
                point.pointClassId    = resolvePointClass(belowThresholdId, pointClassId, lastClassification);
                point.pointNameBackup = pointNameBackup;
                point.locationSysId   = RecordWithCommonFields.getSysIdSafe(rec_location);
                point.tags            = AssetRecord.WellKnownTags.getTags(tags, false, true, true);

                result.points.add(point);
            });

            return result;
        }
    }

    @GET
    @Path("equipment-aggregation")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public EquipmentAggregation equipmentAggregation() throws
                                                       Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            EquipmentAggregation result = new EquipmentAggregation();

            LocationsEngine locationsEngine = sessionHolder.getServiceNonNull(LocationsEngine.class);

            result.locationHierarchy = locationsEngine.acquireSnapshot(false)
                                                      .extractHierarchy();

            NormalizationRecord rec_norm = NormalizationRecord.findActive(sessionHolder.createHelper(NormalizationRecord.class));
            NormalizationRules  rules    = rec_norm != null ? rec_norm.getRules() : null;
            if (rules != null)
            {
                result.pointClasses     = rules.pointClasses;
                result.equipmentClasses = rules.equipmentClasses;
                result.locationClasses  = rules.locationClasses;
            }

            Map<String, EquipmentHierarchy> equipments = Maps.newHashMap();
            Multimap<String, String>        groups     = RelationshipRecord.fetchAllMatchingRelations(sessionHolder, AssetRelationship.controls);
            for (String groupId : groups.keySet())
            {
                AssetRecord     rec_group    = sessionHolder.getEntity(AssetRecord.class, groupId);
                LocationRecord  rec_location = rec_group.getLocation();
                MetadataMap     metadata     = rec_group.getMetadata();
                MetadataTagsMap tags         = rec_group.accessTags();
                String          equipClassId = AssetRecord.WellKnownMetadata.equipmentClassID.get(metadata);

                EquipmentHierarchy equip = new EquipmentHierarchy();
                equip.sysId            = groupId;
                equip.name             = rec_group.getName();
                equip.equipmentClassId = equipClassId;
                equip.locationSysId    = RecordWithCommonFields.getSysIdSafe(rec_location);
                equip.tags             = AssetRecord.WellKnownTags.getTags(tags, false, true, true);

                equipments.put(groupId, equip);
            }

            Map<String, EquipmentHierarchy> topEquipments = Maps.newHashMap(equipments);

            for (String groupId : groups.keySet())
            {
                EquipmentHierarchy parent = equipments.get(groupId);
                for (String childId : groups.get(groupId))
                {
                    EquipmentHierarchy child = equipments.get(childId);
                    if (child != null)
                    {
                        parent.children.add(child);

                        topEquipments.remove(childId);
                    }
                }
            }

            result.equipments.addAll(topEquipments.values());

            enumerateDevices(sessionHolder, (rec_device) ->
            {
                result.controllers.add(rec_device.getSysId());
            });

            return result;
        }
    }

    @GET
    @Path("connection/{connectionId}/last-sample/{controlPointId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public ZonedDateTime getLastSample(@PathParam("connectionId") String connectionId,
                                       @PathParam("controlPointId") String controlPointId)
    {
        return DataConnectionRecord.getLastSample(m_sessionProvider, connectionId, controlPointId);
    }

    @POST
    @Path("connection/{connectionId}/last-sample/{controlPointId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public ZonedDateTime setLastSample(@PathParam("connectionId") String connectionId,
                                       @PathParam("controlPointId") String controlPointId,
                                       ZonedDateTime lastSampleTime)
    {
        DataConnectionRecord.setLastSample(m_sessionProvider, connectionId, controlPointId, lastSampleTime);

        return lastSampleTime;
    }

    @GET
    @Path("site")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DataConnectionSite getSite() throws
                                        Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            return SystemPreferenceRecord.getTypedValue(sessionHolder, SystemPreferenceTypedValue.DataConnectionSite, DataConnectionSite.class);
        }
    }

    @POST
    @Path("site")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DataConnectionSite setSite(DataConnectionSite site) throws
                                                               Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DataConnectionSite siteRoundtrip = SystemPreferenceRecord.setTypedValue(sessionHolder, SystemPreferenceTypedValue.DataConnectionSite, site);

            sessionHolder.commit();

            return siteRoundtrip;
        }
    }

    //--//

    @POST
    @Path("endpoint/{endpointId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @Optio3NoAuthenticationNeeded
    public JsonNode receiveRaw(@Context ContainerRequestContext requestContext,
                               @PathParam("endpointId") String endpointId,
                               @QueryParam("arg") String endpointArg,
                               JsonNode payload) throws
                                                 Throwable
    {
        m_filter.process(requestContext, Optional.empty(), Optional.empty());

        InstanceConfiguration cfg = m_sessionProvider.getServiceNonNull(InstanceConfiguration.class);
        return cfg.handleEndpoint(m_sessionProvider, requestContext, endpointId, endpointArg, payload, null);
    }

    @POST
    @Path("endpoint/{endpointId}/stream")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @Optio3NoAuthenticationNeeded
    public JsonNode receiveStream(@Context ContainerRequestContext requestContext,
                                  @PathParam("endpointId") String endpointId,
                                  @QueryParam("arg") String endpointArg,
                                  InputStream stream) throws
                                                      Throwable
    {
        m_filter.process(requestContext, Optional.empty(), Optional.empty());

        InstanceConfiguration cfg = m_sessionProvider.getServiceNonNull(InstanceConfiguration.class);
        return cfg.handleEndpoint(m_sessionProvider, requestContext, endpointId, endpointArg, null, stream);
    }

    //--//

    private static DeviceElementFilterRequest getDeviceElementFilters(DeviceRecord rec_device,
                                                                      boolean unclassified)
    {
        DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(rec_device);
        if (unclassified)
        {
            filters.hasAnySampling = true;
        }
        else
        {
            filters.hasMetadata = true;
        }

        filters.isNotHidden = true;

        return filters;
    }

    private static ControllerMetadataAggregation buildControllerResult(DeviceRecord rec_controller)
    {
        ControllerMetadataAggregation result = new ControllerMetadataAggregation();

        result.sysId = rec_controller.getSysId();
        result.name  = rec_controller.getName();

        BACnetDeviceDescriptor desc = rec_controller.getIdentityDescriptor(BACnetDeviceDescriptor.class);
        if (desc != null)
        {
            result.networkNumber  = desc.address.networkNumber;
            result.instanceNumber = desc.address.instanceNumber;
        }

        return result;
    }

    private static void enumerateDevices(SessionHolder sessionHolder,
                                         ConsumerWithException<DeviceRecord> callback) throws
                                                                                       Exception
    {
        RecordHelper<NetworkAssetRecord> helper_network = sessionHolder.createHelper(NetworkAssetRecord.class);
        RecordHelper<DeviceRecord>       helper_device  = sessionHolder.createHelper(DeviceRecord.class);

        for (NetworkAssetRecord rec_network : helper_network.listAll())
        {
            rec_network.enumerateChildren(helper_device, true, -1, (filters) -> filters.addState(AssetState.operational), (rec_device) ->
            {
                if (callback != null)
                {
                    callback.accept(rec_device);
                }

                return StreamHelperNextAction.Continue_Evict;
            });
        }
    }

    private static void enumeratePoints(SessionHolder sessionHolder,
                                        DeviceElementFilterRequest filters,
                                        ConsumerWithException<DeviceElementRecord> callback) throws
                                                                                             Exception
    {
        DeviceElementRecord.enumerateNoNesting(sessionHolder.createHelper(DeviceElementRecord.class), filters, (rec_object) ->
        {
            callback.accept(rec_object);

            return StreamHelperNextAction.Continue_Evict;
        });
    }

    private static String getBuildingName(List<NormalizationEquipmentLocation> locations)
    {
        NormalizationEquipmentLocation building = CollectionUtils.firstElement(locations);

        return building != null ? building.name : null;
    }

    private static boolean shouldReturnPoint(boolean unclassified,
                                             String pointClassId,
                                             DeviceElementClassificationMetadata classification)
    {
        if (unclassified)
        {
            return true;
        }

        if (classification == null || pointClassId == null)
        {
            return false;
        }

        if (classification.positiveScore == null || classification.pointClassThreshold == null)
        {
            // Legacy point does not have stored score (assume above threshold)
            return true;
        }

        double score = classification.positiveScore + classification.negativeScore;

        return score >= classification.pointClassThreshold;
    }

    private static String resolvePointClass(Integer belowThresholdId,
                                            String pointClassId,
                                            DeviceElementClassificationMetadata classification)
    {
        if (classification == null || pointClassId == null)
        {
            return null;
        }

        if (belowThresholdId != null)
        {
            double score = classification.positiveScore + classification.negativeScore;
            if (classification.pointClassThreshold != null && score < classification.pointClassThreshold)
            {
                return Integer.toString(belowThresholdId);
            }
        }

        return pointClassId;
    }
}
