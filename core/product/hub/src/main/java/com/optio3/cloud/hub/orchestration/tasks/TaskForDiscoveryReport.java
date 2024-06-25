/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.optio3.cloud.formatting.TabularField;
import com.optio3.cloud.formatting.TabularReportAsExcel;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.logic.normalizations.EquipmentClass;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.logic.normalizations.PointClass;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.asset.DiscoveryReportProgress;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.asset.RelationshipRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.IBackgroundActivityProgress;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.metadata.normalization.BACnetImportExportData;
import com.optio3.metadata.normalization.ImportExportData;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;

public class TaskForDiscoveryReport extends BaseReportTask implements IBackgroundActivityProgress<DiscoveryReportProgress>
{
    private static final String chunk_Excel = "Excel";

    private static class ExtraPointDetails
    {
        public Double           positiveScore;
        public Double           negativeScore;
        public Double           threshold;
        public EngineeringUnits assignedUnits;
    }

    static class RowForReport_ControlPoint
    {
        @TabularField(order = 0, title = "Point SysId")
        public String col_sysId;

        @TabularField(order = 1, title = "BACnet Network Id")
        public int col_networkId;

        @TabularField(order = 2, title = "BACnet Instance Id")
        public int col_instanceId;

        @TabularField(order = 3, title = "BACnet Object Id")
        public String col_objectId;

        @TabularField(order = 4, title = "Transport")
        public String col_transport;

        @TabularField(order = 5, title = "Point Name")
        public String col_normalizedName;

        @TabularField(order = 6, title = "Point Name From Backup")
        public String col_dashboardName;

        @TabularField(order = 7, title = "Discovered Point Name")
        public String col_deviceName;

        @TabularField(order = 8, title = "Discovered Point Description")
        public String col_deviceDescription;

        @TabularField(order = 9, title = "Data Collection")
        public Boolean col_dataCollection;

        @TabularField(order = 10, title = "Point Class Name")
        public String col_pointClassName;

        @TabularField(order = 11, title = "Point Class Id")
        public String col_pointClassId;

        @TabularField(order = 12, title = "Positive Point Class Score")
        public Double col_positivePointClassScore;

        @TabularField(order = 13, title = "Negative Point Class Score")
        public Double col_negativePointClassScore;

        @TabularField(order = 14, title = "Point Class Threshold")
        public Double col_pointClassThreshold;

        @TabularField(order = 15, title = "Equipment Name")
        public String col_equipName;

        @TabularField(order = 16, title = "Equipment SysId")
        public String col_equipSysId;

        @TabularField(order = 17, title = "Equipment Class Name")
        public String col_equipClassName;

        @TabularField(order = 18, title = "Equipment Class Id")
        public String col_equipClassId;

        @TabularField(order = 19, title = "Location")
        public String col_location;

        @TabularField(order = 20, title = "Location SysId")
        public String col_locationSysId;

        @TabularField(order = 21, title = "Device Structure")
        public String col_deviceStructure;

        @TabularField(order = 22, title = "Original Units")
        public String col_units;

        @TabularField(order = 23, title = "Assigned Units")
        public String col_assignedUnits;

        @TabularField(order = 24, title = "Azure Digital Twin")
        public String col_adt;

        @TabularField(order = 25, title = "Tags")
        public String col_tags;
    }

    static class RowForReport_Equipment
    {
        @TabularField(order = 0, title = "Equipment SysId")
        public String col_sysId;

        @TabularField(order = 1, title = "Equipment Name")
        public String col_name;

        @TabularField(order = 2, title = "Parent Equipment SysId")
        public String col_parentSysId;

        @TabularField(order = 3, title = "Parent Equipment Name")
        public String col_parentName;

        @TabularField(order = 4, title = "Equipment Class Name")
        public String col_equipClassName;

        @TabularField(order = 5, title = "Equipment Class Id")
        public String col_equipClassId;

        @TabularField(order = 6, title = "Location")
        public String col_locationName;

        @TabularField(order = 7, title = "Location SysId")
        public String col_locationSysId;

        @TabularField(order = 8, title = "Azure Digital Twin")
        public String col_adt;

        @TabularField(order = 9, title = "Tags")
        public String col_tags;
    }

    static class RowForReport_Location
    {
        @TabularField(order = 0, title = "Location SysId")
        public String col_sysId;

        @TabularField(order = 1, title = "Location Name")
        public String col_name;

        @TabularField(order = 2, title = "Parent Location SysId")
        public String col_parentSysId;

        @TabularField(order = 3, title = "Parent Location Name")
        public String col_parentName;

        @TabularField(order = 4, title = "Location Type")
        public String col_type;

        @TabularField(order = 5, title = "Azure Digital Twin")
        public String col_adt;

        @TabularField(order = 6, title = "Tags")
        public String col_tags;
    }
    //--//

    private static class EquipmentInfo
    {
        public final String sysId;
        public final String name;
        public final String equipClassId;
        public final String locationSysId;
        public final String adt;

        EquipmentInfo(String sysId,
                      String name,
                      String equipClassId,
                      String locationSysId,
                      String adt)
        {
            this.sysId         = sysId;
            this.name          = name;
            this.equipClassId  = equipClassId;
            this.locationSysId = locationSysId;
            this.adt           = adt;
        }
    }

    public List<RecordLocator<NetworkAssetRecord>> targets;

    public DeviceElementFilterRequest filter;

    public DiscoveryReportProgress results;

    //--//

    @Override
    public DiscoveryReportProgress fetchProgress(SessionHolder sessionHolder,
                                                 boolean detailed)
    {
        if (!detailed)
        {
            results.report = null;
        }

        return results;
    }

    @Override
    public void generateStream() throws
                                 IOException
    {
        // Nothing to do.
    }

    @Override
    public InputStream streamContents() throws
                                        IOException
    {
        return readAsStream(chunk_Excel);
    }

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        List<RecordLocator<NetworkAssetRecord>> locators,
                                                        DeviceElementFilterRequest filter) throws
                                                                                           Exception
    {
        return scheduleActivity(sessionHolder, 0, null, TaskForDiscoveryReport.class, (t) ->
        {
            t.targets = locators;
            t.filter  = filter;

            t.results                   = new DiscoveryReportProgress();
            t.results.networksToProcess = locators.size();
        });
    }

    //--//

    @Override
    public String getTitle()
    {
        return "Report Discovery results";
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return null;
    }

    @BackgroundActivityMethod(needsSession = true)
    public void process(SessionHolder sessionHolder) throws
                                                     Exception
    {
        RecordHelper<DeviceRecord>        helper_device        = sessionHolder.createHelper(DeviceRecord.class);
        RecordHelper<DeviceElementRecord> helper_element       = sessionHolder.createHelper(DeviceElementRecord.class);
        Map<String, ExtraPointDetails>    rec_elementToDetails = Maps.newHashMap();
        List<BACnetImportExportData>      objects              = Lists.newArrayList();

        LocationsEngine          locationsEngine   = sessionHolder.getServiceNonNull(LocationsEngine.class);
        LocationsEngine.Snapshot locationsSnapshot = locationsEngine.acquireSnapshot(true);
        Map<String, String>      locationHierarchy = locationsSnapshot.extractReverseHierarchy();

        for (RecordLocator<NetworkAssetRecord> loc_network : targets)
        {
            NetworkAssetRecord rec_network = sessionHolder.fromLocator(loc_network);

            ReportFlusher state = new ReportFlusher(1000);

            rec_network.enumerateChildren(helper_device, true, -1, null, (rec_device) ->
            {
                // Update filter parent
                this.filter.setParent(rec_device.getSysId());

                // Find child device elements
                DeviceElementRecord.enumerateNoNesting(helper_element, this.filter, (rec_object) ->
                {
                    BACnetImportExportData item = Reflection.as(rec_device.extractImportExportData(locationsSnapshot, rec_object), BACnetImportExportData.class);

                    if (item != null)
                    {
                        objects.add(item);

                        MetadataMap       metadata = rec_object.getMetadata();
                        ExtraPointDetails details  = new ExtraPointDetails();
                        rec_elementToDetails.put(item.sysId, details);

                        details.positiveScore = AssetRecord.WellKnownMetadata.pointClassScore.get(metadata);
                        details.negativeScore = AssetRecord.WellKnownMetadata.negativePointClassScore.get(metadata);
                        details.threshold     = AssetRecord.WellKnownMetadata.pointClassThreshold.get(metadata);
                        details.assignedUnits = AssetRecord.WellKnownMetadata.assignedUnits.get(metadata);
                    }

                    results.elementsProcessed++;

                    return StreamHelperNextAction.Continue_Evict;
                });

                results.devicesProcessed++;

                if (state.shouldReport())
                {
                    flushStateToDatabase(sessionHolder);
                }

                return StreamHelperNextAction.Continue_Evict;
            });
        }

        flushStateToDatabase(sessionHolder);

        ImportExportData.sort(objects);

        Map<String, String> pointClassLookup = Maps.newHashMap();
        Map<String, String> equipClassLookup = Maps.newHashMap();
        Map<String, String> equipTagsLookup  = Maps.newHashMap();

        TagsEngine         tagsEngine = sessionHolder.getService(TagsEngine.class);
        NormalizationRules rules      = tagsEngine.getActiveNormalizationRules(sessionHolder);
        if (rules != null)
        {
            for (PointClass pc : rules.pointClasses)
            {
                pointClassLookup.put(pc.idAsString(), pc.pointClassName);
            }

            for (EquipmentClass ec : rules.equipmentClasses)
            {
                String key = ec.idAsString();

                equipClassLookup.put(key, ec.equipClassName);

                if (ec.tags != null)
                {
                    equipTagsLookup.put(key, String.join(",", ec.tags));
                }
            }
        }

        Multimap<String, String> groups        = RelationshipRecord.fetchAllMatchingRelations(sessionHolder, AssetRelationship.controls);
        Multimap<String, String> groupsInverse = Multimaps.invertFrom(groups, HashMultimap.create());

        RecordHelper<LogicalAssetRecord> helper    = sessionHolder.createHelper(LogicalAssetRecord.class);
        List<LogicalAssetRecord>         allGroups = helper.listAll();
        Map<String, EquipmentInfo>       equipment = Maps.newHashMap();
        for (LogicalAssetRecord rec_group : allGroups)
        {
            if (rec_group.isEquipment())
            {
                String locationSysId = RecordWithCommonFields.getSysIdSafe(rec_group.getLocation());

                EquipmentInfo equipInfo = new EquipmentInfo(rec_group.getSysId(), rec_group.getName(), rec_group.getEquipmentClassId(), locationSysId, rec_group.getAzureDigitalTwinModel());
                equipment.put(rec_group.getSysId(), equipInfo);
            }
        }

        Set<String> locations  = Sets.newHashSet();
        Set<String> equipments = Sets.newHashSet();

        try (var holder = new TabularReportAsExcel.Holder())
        {
            {
                TabularReportAsExcel<RowForReport_ControlPoint> tr = new TabularReportAsExcel<>(RowForReport_ControlPoint.class, "Control Points", holder);

                tr.emit(rowHandler ->
                        {
                            for (BACnetImportExportData item : objects)
                            {
                                RowForReport_ControlPoint row = new RowForReport_ControlPoint();

                                ExtraPointDetails details = rec_elementToDetails.get(item.sysId);

                                row.col_sysId      = item.sysId;
                                row.col_networkId  = item.networkId;
                                row.col_instanceId = item.instanceId;
                                if (item.objectId != null)
                                {
                                    row.col_objectId = item.objectId.toJsonValue();
                                }

                                if (item.transport != null)
                                {
                                    row.col_transport = item.transport.toString();
                                }

                                row.col_deviceName        = item.deviceName;
                                row.col_deviceDescription = item.deviceDescription;
                                row.col_dashboardName     = item.dashboardName;
                                if (item.deviceStructure != null)
                                {
                                    row.col_deviceStructure = String.join("/", item.deviceStructure);
                                }

                                row.col_normalizedName = item.normalizedName;
                                row.col_dataCollection = item.isSampled;
                                row.col_units          = Objects.toString(item.units);
                                row.col_assignedUnits  = Objects.toString(details.assignedUnits);

                                row.col_pointClassId = item.pointClassId;
                                row.col_adt          = item.pointClassAdt;

                                if (CollectionUtils.isNotEmpty(item.pointTags))
                                {
                                    row.col_tags = String.join(",", item.pointTags);
                                }

                                row.col_location      = item.locationName;
                                row.col_locationSysId = item.locationSysId;

                                extractLocations(locationHierarchy, locations, item.locationSysId);

                                String equipId = CollectionUtils.firstElement(groupsInverse.get(row.col_sysId));
                                if (equipId != null)
                                {
                                    EquipmentInfo equipInfo = equipment.get(equipId);
                                    row.col_equipSysId     = equipId;
                                    row.col_equipName      = equipInfo.name;
                                    row.col_equipClassId   = equipInfo.equipClassId;
                                    row.col_equipClassName = equipClassLookup.get(equipInfo.equipClassId);

                                    //
                                    // Mark whole equipment chain as seen.
                                    //
                                    for (; equipId != null; equipId = CollectionUtils.firstElement(groupsInverse.get(equipId)))
                                    {
                                        if (!equipments.add(equipId))
                                        {
                                            // Already processed.
                                            break;
                                        }
                                    }
                                }

                                row.col_positivePointClassScore = details.positiveScore;
                                row.col_negativePointClassScore = details.negativeScore;
                                row.col_pointClassThreshold     = details.threshold;
                                row.col_pointClassName          = pointClassLookup.get(row.col_pointClassId);

                                rowHandler.emitRow(row);
                            }
                        });
            }

            {
                TabularReportAsExcel<RowForReport_Equipment> tr = new TabularReportAsExcel<>(RowForReport_Equipment.class, "Equipment", holder);

                tr.emit(rowHandler ->
                        {
                            for (String sysId : equipments)
                            {
                                EquipmentInfo equipInfo = equipment.get(sysId);
                                if (equipInfo == null)
                                {
                                    continue;
                                }

                                String parentSysId = CollectionUtils.firstElement(groupsInverse.get(sysId));

                                RowForReport_Equipment row = new RowForReport_Equipment();

                                row.col_sysId = sysId;
                                row.col_name  = equipInfo.name;

                                row.col_parentSysId = parentSysId;
                                EquipmentInfo parentEquipInfo = parentSysId != null ? equipment.get(parentSysId) : null;
                                if (parentEquipInfo != null)
                                {
                                    row.col_parentName = parentEquipInfo.name;
                                }

                                row.col_equipClassId   = equipInfo.equipClassId;
                                row.col_equipClassName = equipClassLookup.get(equipInfo.equipClassId);
                                row.col_adt            = equipInfo.adt;
                                row.col_tags           = equipTagsLookup.get(equipInfo.equipClassId);

                                row.col_locationSysId = equipInfo.locationSysId;
                                row.col_locationName  = locationsSnapshot.getName(equipInfo.locationSysId);

                                extractLocations(locationHierarchy, locations, equipInfo.locationSysId);

                                rowHandler.emitRow(row);
                            }
                        });
            }

            {
                TabularReportAsExcel<RowForReport_Location> tr = new TabularReportAsExcel<>(RowForReport_Location.class, "Locations", holder);

                tr.emit(rowHandler ->
                        {
                            for (String sysId : locations)
                            {
                                RowForReport_Location row = new RowForReport_Location();

                                row.col_sysId = sysId;
                                row.col_name  = locationsSnapshot.getName(sysId);
                                row.col_adt   = locationsSnapshot.getAdtModel(sysId);
                                row.col_type  = locationsSnapshot.getType(sysId)
                                                                 .name();

                                Set<String> tags = locationsSnapshot.getTags(sysId);
                                if (CollectionUtils.isNotEmpty(tags))
                                {
                                    row.col_tags = String.join(",", tags);
                                }

                                row.col_parentSysId = locationHierarchy.get(sysId);
                                row.col_parentName  = locationsSnapshot.getName(row.col_parentSysId);

                                rowHandler.emitRow(row);
                            }
                        });
            }

            try (OutputStream outputStream = writeAsStream(chunk_Excel, 0))
            {
                holder.toStream(outputStream);
            }
        }

        markAsCompleted();
    }

    private static void extractLocations(Map<String, String> locationHierarchy,
                                         Set<String> locations,
                                         String locationSysId)
    {
        //
        // Mark whole location chain as seen.
        //
        while (locationSysId != null)
        {
            if (!locations.add(locationSysId))
            {
                // Already processed.
                break;
            }

            locationSysId = locationHierarchy.get(locationSysId);
        }
    }
}
