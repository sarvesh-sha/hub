/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.optio3.cloud.exception.NotImplementedException;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueEquipment;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.logic.normalizations.EquipmentClass;
import com.optio3.cloud.hub.logic.normalizations.NormalizationEngine;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.logic.normalizations.NormalizationScore;
import com.optio3.cloud.hub.logic.normalizations.NormalizationState;
import com.optio3.cloud.hub.logic.normalizations.NormalizationTerm;
import com.optio3.cloud.hub.logic.normalizations.PointClass;
import com.optio3.cloud.hub.logic.spooler.ResultStagingSpooler;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.logic.tags.TagsStreamNextAction;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.model.location.LocationHierarchy;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.model.normalization.ClassificationPointInput;
import com.optio3.cloud.hub.model.normalization.ClassificationPointOutput;
import com.optio3.cloud.hub.model.normalization.ClassificationPointOutputDetails;
import com.optio3.cloud.hub.model.normalization.ClassificationReason;
import com.optio3.cloud.hub.model.normalization.DeviceElementClassificationMetadata;
import com.optio3.cloud.hub.model.normalization.DeviceElementClassificationOverrides;
import com.optio3.cloud.hub.model.normalization.DeviceElementNormalizationProgress;
import com.optio3.cloud.hub.model.normalization.EquipmentClassificationMetadata;
import com.optio3.cloud.hub.model.normalization.NormalizationEquipment;
import com.optio3.cloud.hub.model.normalization.NormalizationEquipmentLocation;
import com.optio3.cloud.hub.model.tags.TagsConditionLocation;
import com.optio3.cloud.hub.model.workflow.WorkflowOverrides;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.hub.persistence.asset.MetricsDeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.asset.RelationshipRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.IBackgroundActivityProgress;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.cloud.search.HibernateSearch;
import com.optio3.metadata.normalization.BACnetImportExportData;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class TaskForDeviceElementNormalization extends BaseTaskForNormalization implements IBackgroundActivityProgress<DeviceElementNormalizationProgress>
{
    private static final String chunk_DEVICE_DETAILS = "DeviceDetails";
    private static final String chunk_EQUIPMENT      = "Equipment";

    enum State
    {
        Normalize,
        EquipmentRelationships,
        Cleanup
    }

    public static class DeviceDetails
    {
        public List<ClassificationPointOutput> results = Lists.newArrayList();
    }

    public static class ChunkForLocations
    {
        public final Map<String, String> assetToLocation = Maps.newHashMap();
    }

    public static class ChunkForEquipment
    {
        public final Map<String, String> keyToSysId = Maps.newHashMap();

        @JsonIgnore
        public boolean dirty;
    }

    public static class ChunkForDeviceObjects
    {
        public final Map<String, String> deviceToObject = Maps.newHashMap();
    }

    public static class ChunkForGroups
    {
        public Multimap<String, String> groupsInverse;
    }

    public static void extractEquipmentFromNormalizationOutput(Map<String, NormalizationEquipment> equipments,
                                                               Map<String, Set<String>> equipmentRelationships,
                                                               String networkSysId,
                                                               List<NormalizationEquipment> overrides,
                                                               List<NormalizationEquipmentLocation> locations,
                                                               NormalizationState normalizationState,
                                                               NormalizationEngine engine,
                                                               WorkflowOverrides workflowOverrides)
    {

        if (overrides != null && !overrides.isEmpty())
        {
            NormalizationEquipment parent = null;
            for (NormalizationEquipment eq : overrides)
            {
                parent = addEquipment(equipments, equipmentRelationships, parent, getLocationsOrDefault(eq.locations, locations), networkSysId, eq.name, eq.equipmentClassId, workflowOverrides);
            }
        }

        if (equipments.isEmpty())
        {
            if (normalizationState.equipments.isEmpty())
            {
                if (!engine.hasAnyLogic())
                {
                    String equipmentName = normalizationState.extractEquipmentThroughLegacyHeuristics();
                    if (StringUtils.isNotBlank(equipmentName))
                    {
                        addEquipment(equipments, equipmentRelationships, null, locations, networkSysId, equipmentName, null, workflowOverrides);
                    }
                }
            }
            else
            {
                for (NormalizationEngineValueEquipment nEq : normalizationState.equipments)
                {
                    collectHierarchy(equipments, equipmentRelationships, networkSysId, engine, nEq, null, locations, workflowOverrides);
                }
            }
        }
    }

    private static List<NormalizationEquipmentLocation> getLocationsOrDefault(List<NormalizationEquipmentLocation> locations,
                                                                              List<NormalizationEquipmentLocation> defaultLocations)
    {
        return CollectionUtils.isNotEmpty(locations) ? locations : defaultLocations;
    }

    private static void collectHierarchy(Map<String, NormalizationEquipment> equipments,
                                         Map<String, Set<String>> equipmentRelationships,
                                         String networkSysId,
                                         NormalizationEngine engine,
                                         NormalizationEngineValueEquipment nEq,
                                         NormalizationEquipment parent,
                                         List<NormalizationEquipmentLocation> locations,
                                         WorkflowOverrides workflowOverrides)

    {
        String equipmentName    = nEq.name;
        String equipmentClassId = nEq.equipmentClassId;
        locations = getLocationsOrDefault(CollectionUtils.transformToList(nEq.locations, NormalizationEquipmentLocation::fromEngineLocation), locations);

        if (StringUtils.isBlank(equipmentClassId) && StringUtils.isNotEmpty(equipmentName) && !nEq.setUnclassified)
        {
            String nameForClassification = equipmentName;
            if (StringUtils.isNotBlank(nEq.equipmentClassHint))
            {
                nameForClassification = nEq.equipmentClassHint;
            }

            String equipmentOutput = engine.normalizeSimple(nameForClassification);
            if (equipmentOutput != null)
            {
                NormalizationScore.Context<EquipmentClass> classificationCtx = engine.scoreTopEquipmentClass(equipmentOutput, null, null);
                if (classificationCtx != null)
                {
                    EquipmentClass ec = classificationCtx.context;
                    equipmentClassId = ec.idAsString();

                    LoggerInstance.debug("##### Matched equipment class: %s: %d (%s ## %s)", nameForClassification, ec.id, ec.equipClassName, ec.description);
                }
            }
            else
            {
                LoggerInstance.debug("##### Failed search for equipment class: %s", nameForClassification);
            }
        }

        if (StringUtils.isNotBlank(equipmentName))
        {
            NormalizationEquipment equip = addEquipment(equipments, equipmentRelationships, parent, locations, networkSysId, equipmentName, equipmentClassId, workflowOverrides);

            if (nEq.childEquipment != null)
            {
                for (NormalizationEngineValueEquipment nEq2 : nEq.childEquipment)
                {
                    collectHierarchy(equipments, equipmentRelationships, networkSysId, engine, nEq2, equip, locations, workflowOverrides);
                }
            }
        }
    }

    private static NormalizationEquipment addEquipment(Map<String, NormalizationEquipment> equipments,
                                                       Map<String, Set<String>> equipmentRelationships,
                                                       NormalizationEquipment parent,
                                                       List<NormalizationEquipmentLocation> locations,
                                                       String networkSysId,
                                                       String name,
                                                       String equipClassId,
                                                       WorkflowOverrides workflowOverrides)
    {
        if (equipments == null)
        {
            return null;
        }

        NormalizationEquipment eq = new NormalizationEquipment();
        eq.name             = name;
        eq.equipmentClassId = equipClassId;
        eq.locations        = locations;
        eq.key              = getEquipmentKey(eq.name, eq.locations, networkSysId, parent != null ? parent.key : null);

        if (eq.key != null)
        {
            eq.name             = workflowOverrides.equipmentNames.getOrDefault(eq.key, eq.name);
            eq.equipmentClassId = workflowOverrides.equipmentClasses.getOrDefault(eq.key, eq.equipmentClassId);

            equipments.putIfAbsent(eq.key, eq);
            if (parent != null)
            {
                Set<String> relationships = equipmentRelationships.computeIfAbsent(parent.key, (key) -> Sets.newHashSet());
                relationships.add(eq.key);
            }
        }

        return eq;
    }

    //--//

    public State step;

    public boolean dryRun;

    public RecordLocator<UserRecord> user;

    public List<RecordLocator<DeviceRecord>> targets;

    public int devicesToProcess;
    public int devicesProcessed;
    public int elementsProcessed;

    public TreeMap<String, Integer> allWords        = new TreeMap<>();
    public TreeMap<String, Integer> allUnknownWords = new TreeMap<>();

    public Map<String, NormalizationEquipment> equipments             = Maps.newHashMap();
    public Map<String, Set<String>>            equipmentRelationships = Maps.newHashMap();

    //--//

    private ChunkForLocations     m_locationsChunk;
    private ChunkForEquipment     m_equipmentChunk;
    private ChunkForDeviceObjects m_deviceObjectsChunk;
    private ChunkForGroups        m_groupsChunk;

    private final Map<String, LogicalAssetRecord> m_equipmentCache = Maps.newHashMap();
    private final Map<String, LocationRecord>     m_locationCache  = Maps.newHashMap();

    //--//

    @Override
    public DeviceElementNormalizationProgress fetchProgress(SessionHolder sessionHolder,
                                                            boolean detailed)
    {
        DeviceElementNormalizationProgress results = new DeviceElementNormalizationProgress();

        results.devicesToProcess  = devicesToProcess;
        results.devicesProcessed  = devicesProcessed;
        results.elementsProcessed = elementsProcessed;

        if (detailed)
        {
            results.allWords        = allWords;
            results.allUnknownWords = allUnknownWords;

            results.equipments             = equipments;
            results.equipmentRelationships = equipmentRelationships;

            forEachChunkInSequence(chunk_DEVICE_DETAILS, DeviceDetails.class, (seq, deviceDetails) ->
            {
                results.details.addAll(deviceDetails.results);
            });

            results.workflowOverrides = ensureWorkflowOverrides(sessionHolder);
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
    public InputStream streamContents()
    {
        throw new NotImplementedException("Not supported");
    }

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        List<RecordLocator<DeviceRecord>> locators,
                                                        NormalizationRules rules,
                                                        RecordLocator<UserRecord> loc_userForNotification,
                                                        boolean dryRun) throws
                                                                        Exception
    {
        return scheduleActivity(sessionHolder, 0, null, TaskForDeviceElementNormalization.class, (t) ->
        {
            t.rules  = rules;
            t.dryRun = dryRun;
            t.step   = State.Normalize;

            t.user             = loc_userForNotification;
            t.targets          = locators;
            t.devicesToProcess = locators.size();
        });
    }

    //--//

    @Override
    public String getTitle()
    {
        return "Normalizing Control Point Names";
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
        if (dryRun)
        {
            // Don't disable indexing if we are not going to touch the database.
            processInner(sessionHolder);
        }
        else
        {
            try (var searchGate = app.closeGate(HibernateSearch.Gate.class))
            {
                try (var tagsGate = app.closeGate(TagsEngine.Gate.class))
                {
                    try (var spoolerGate = app.closeGate(ResultStagingSpooler.Gate.class))
                    {
                        processInner(sessionHolder);
                    }
                }
            }
        }
    }

    private void processInner(SessionHolder sessionHolder) throws
                                                           Exception
    {
        if (startTime == null)
        {
            startTime = TimeUtils.now();
        }

        MonotonousTime batchTimeout = TimeUtils.computeTimeoutExpiration(5, TimeUnit.SECONDS);

        NormalizationEngine engine            = ensureEngine(() -> new NormalizationEngine(getSessionProvider(), rules, true));
        WorkflowOverrides   workflowOverrides = ensureWorkflowOverrides(sessionHolder);

        switch (step)
        {
            case Normalize:
            {
                while (!targets.isEmpty())
                {
                    if (TimeUtils.isTimeoutExpired(batchTimeout))
                    {
                        flushStateToDatabase(sessionHolder);
                    }

                    RecordLocator<DeviceRecord> loc_target = targets.remove(targets.size() - 1);

                    DeviceRecord rec_target = sessionHolder.fromLocatorOrNull(loc_target);
                    if (rec_target != null)
                    {
                        devicesProcessed++;

                        LoggerInstance.info("Processing %d/%d: %s %s", devicesProcessed, devicesToProcess, rec_target.getSysId(), rec_target.getName());

                        BACnetDeviceRecord rec_device_bacnet = Reflection.as(rec_target, BACnetDeviceRecord.class);
                        if (rec_device_bacnet != null)
                        {
                            BACnetImportExportData device_info;

                            DeviceElementRecord rec_object_device = findDeviceObject(sessionHolder, rec_device_bacnet);
                            if (rec_object_device != null)
                            {
                                device_info = rec_device_bacnet.extractImportExportData(null, rec_object_device);
                            }
                            else
                            {
                                device_info = null;
                            }

                            NetworkAssetRecord rec_network      = rec_device_bacnet.findParentAssetRecursively(NetworkAssetRecord.class);
                            LocationRecord     rec_locationRoot = rec_network.getLocation();

                            final DeviceDetails              details = new DeviceDetails();
                            final DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(rec_target);
                            DeviceElementRecord.enumerateNoNesting(sessionHolder.createHelper(DeviceElementRecord.class), filters, (rec_object) ->
                            {
                                boolean modified = processObjects(sessionHolder, engine, rec_object, rec_device_bacnet, device_info, rec_network, rec_locationRoot, details);

                                elementsProcessed++;

                                return modified ? StreamHelperNextAction.Continue_Flush_Evict : StreamHelperNextAction.Continue_Evict;
                            });

                            updateLocationFromOverride(sessionHolder, rec_device_bacnet.getSysId(), rec_device_bacnet, workflowOverrides.deviceLocations);

                            addChunkToSequence(chunk_DEVICE_DETAILS, details);
                        }
                    }
                }

                step = State.EquipmentRelationships;

                rescheduleDelayed(0, null);
                return;
            }

            case EquipmentRelationships:
            {
                LoggerInstance.info("Creating Equipment Relationships");
                createEquipmentRelationships(sessionHolder, engine);
                if (!dryRun)
                {
                    persistEquipmentRelationships(sessionHolder);
                }

                step = State.Cleanup;

                rescheduleDelayed(0, null);
                return;
            }

            case Cleanup:
                for (String removedEquipment : workflowOverrides.removedEquipment)
                {
                    equipments.remove(removedEquipment);
                    removeParentRelationships(removedEquipment);
                    equipmentRelationships.remove(removedEquipment);
                }

                for (String mergedEquipment : workflowOverrides.equipmentMerge.keySet())
                {
                    equipments.remove(mergedEquipment);
                    removeParentRelationships(mergedEquipment);
                    equipmentRelationships.remove(mergedEquipment);
                }

                if (!dryRun)
                {
                    Map<String, String> equipmentKeyToSysId = ensureExistingEquipment(sessionHolder);
                    Set<String>         equipmentToDelete   = Sets.newHashSet(equipmentKeyToSysId.values());
                    List<String>        referencedIds;

                    referencedIds = CollectionUtils.transformToList(equipments.keySet(), equipmentKeyToSysId::get);
                    referencedIds.addAll(CollectionUtils.transformToList(workflowOverrides.createdEquipment, equipmentKeyToSysId::get));
                    equipmentToDelete.removeAll(referencedIds);

                    LoggerInstance.info("Deleting %d stale equipment", equipmentToDelete.size());

                    for (String id : equipmentToDelete)
                    {
                        try (SessionHolder holder = sessionHolder.spawnNewSessionWithTransaction())
                        {
                            RecordHelper<LogicalAssetRecord>         helperInner   = holder.createHelper(LogicalAssetRecord.class);
                            RecordHelper<MetricsDeviceElementRecord> helperMetrics = holder.createHelper(MetricsDeviceElementRecord.class);

                            LogicalAssetRecord rec = helperInner.get(id);
                            for (MetricsDeviceElementRecord rec_child : rec.getChildren(helperMetrics))
                            {
                                helperMetrics.delete(rec_child);
                            }

                            helperInner.delete(rec);

                            holder.commit();
                        }
                    }

                    workflowOverrides.resolveWorkflows(sessionHolder, user);
                }
                break;
        }

        LoggerInstance.info("Processed normalization for %d devices and %d control points in %s", devicesProcessed, elementsProcessed, TimeUtils.toText(Duration.between(startTime, TimeUtils.now())));

        markAsCompleted();
    }

    private boolean processObjects(SessionHolder sessionHolder,
                                   NormalizationEngine engine,
                                   DeviceElementRecord rec_object,
                                   BACnetDeviceRecord rec_device,
                                   BACnetImportExportData device_info,
                                   NetworkAssetRecord rec_network,
                                   LocationRecord rec_locationRoot,
                                   DeviceDetails details)
    {
        boolean modified = false;

        BACnetImportExportData               item              = rec_device.extractImportExportData(null, rec_object);
        MetadataMap                          metadata          = rec_object.getMetadata();
        DeviceElementClassificationOverrides overrides         = rules.pointOverrides.get(rec_object.getSysId());
        WorkflowOverrides                    workflowOverrides = ensureWorkflowOverrides(sessionHolder);

        ClassificationPointInput input = new ClassificationPointInput();
        input.sysId        = rec_object.getSysId();
        input.parentSysId  = rec_device.getSysId();
        input.networkSysId = rec_network.getSysId();
        input.objectType   = item.objectId.object_type.value;
        input.objectUnits  = item.units;

        // Bad point, skip it
        if (input.objectType == null || input.objectUnits == null)
        {
            return false;
        }

        input.details.objectName       = item.deviceName;
        input.details.objectBackupName = AssetRecord.WellKnownMetadata.nameFromLegacyImport.get(metadata);

        input.equipmentOverrides = overrides != null ? overrides.equipments : null;
        input.pointClassOverride = workflowOverrides.pointClasses.getOrDefault(input.sysId, overrides != null ? overrides.pointClassId : null);

        input.details.objectIdentifier           = rec_object.getIdentifier();
        input.details.objectName                 = item.deviceName;
        input.details.objectWorkflowOverrideName = workflowOverrides.pointNames.get(input.sysId);
        input.details.objectDescription          = item.deviceDescription;
        input.details.objectLocation             = item.deviceLocation;
        input.details.objectBackupName           = AssetRecord.WellKnownMetadata.nameFromLegacyImport.get(metadata);
        input.details.objectBackupEquipmentName  = AssetRecord.WellKnownMetadata.equipmentNameFromLegacyImport.get(metadata);
        input.details.objectUnits                = item.units.toString();
        input.details.objectType                 = input.objectType.toString();

        if (item.dashboardStructure != null)
        {
            input.details.objectBackupStructure = Lists.newArrayList(item.dashboardStructure);
        }

        if (device_info != null)
        {
            input.details.controllerIdentifier       = device_info.objectId.toJsonValue();
            input.details.controllerName             = device_info.deviceName;
            input.details.controllerBackupName       = device_info.dashboardName;
            input.details.controllerDescription      = device_info.deviceDescription;
            input.details.controllerLocation         = device_info.deviceLocation;
            input.details.controllerVendorName       = device_info.deviceVendor;
            input.details.controllerModelName        = device_info.deviceModel;
            input.details.controllerTransportAddress = device_info.transport;
        }

        NormalizationState normalizationStateIn = NormalizationState.fromClassificationInput(input);

        //--//

        NormalizationState normalizationState = engine.normalizeWithHistory(normalizationStateIn, true);
        String             output             = normalizationState.controlPointName;

        if (output != null)
        {
            String oldValue = rec_object.getNormalizedName();

            DeviceElementClassificationMetadata lastClassification = DeviceElementClassificationMetadata.fromMetadata(metadata);

            ClassificationPointOutput mr = input.asResult();
            mr.sysId             = rec_object.getSysId();
            mr.parentSysId       = rec_device.getSysId();
            mr.oldNormalizedName = oldValue;

            mr.locations = CollectionUtils.transformToList(normalizationState.locations, NormalizationEquipmentLocation::fromEngineLocation);

            if (overrides != null)
            {
                if (overrides.locationsWithType != null && overrides.locationsWithType.isEmpty())
                {
                    mr.locations = overrides.locationsWithType;
                }

                if (StringUtils.isNotBlank(overrides.pointName))
                {
                    output = overrides.pointName;
                }
            }

            if (workflowOverrides.pointNames.containsKey(mr.sysId))
            {
                output = workflowOverrides.pointNames.get(mr.sysId);
            }

            mr.normalizedName = output;

            String outputWithAcronyms = engine.compressAcronym(output);
            for (String word : NormalizationEngine.split(outputWithAcronyms))
            {
                String wordLowercase = word.toLowerCase();
                increment(allWords, wordLowercase);

                if (!engine.isKnownTerm(wordLowercase))
                {
                    increment(allUnknownWords, wordLowercase);
                }
            }

            extractEquipmentFromNormalizationOutput(mr.equipments,
                                                    mr.equipmentRelationships,
                                                    rec_network.getSysId(),
                                                    overrides != null ? overrides.equipments : null,
                                                    mr.locations,
                                                    normalizationState,
                                                    engine,
                                                    workflowOverrides);

            for (String equipmentKey : mr.equipments.keySet())
            {
                equipments.putIfAbsent(equipmentKey, mr.equipments.get(equipmentKey));
                Set<String> relations = equipmentRelationships.computeIfAbsent(equipmentKey, (key) -> Sets.newHashSet());
                if (mr.equipmentRelationships.containsKey(equipmentKey))
                {
                    relations.addAll(mr.equipmentRelationships.get(equipmentKey));
                }
            }

            ClassificationPointOutputDetails currentResult = new ClassificationPointOutputDetails();
            mr.currentResult = currentResult;

            currentResult.samplingPeriod = normalizationState.samplingPeriod;
            currentResult.noSampling     = normalizationState.noSampling;

            if (StringUtils.isNotBlank(input.pointClassOverride))
            {
                currentResult.id     = input.pointClassOverride;
                currentResult.reason = ClassificationReason.Override;
            }
            else if (StringUtils.isNotBlank(normalizationState.pointClassId) || normalizationState.setUnclassified)
            {
                currentResult.id            = normalizationState.pointClassId;
                currentResult.reason        = normalizationState.classificationReason;
                currentResult.positiveScore = normalizationState.positiveScore;
                currentResult.negativeScore = normalizationState.negativeScore;
                if (normalizationState.classificationReason == ClassificationReason.TermScoring)
                {
                    currentResult.threshold = rules.scoreThreshold;
                }
            }

            if (StringUtils.isNotEmpty(currentResult.id))
            {
                PointClass pointClass = CollectionUtils.findFirst(engine.pointClasses, (pc) -> StringUtils.equals(pc.idAsString(), currentResult.id));
                if (pointClass != null)
                {
                    currentResult.ignored = pointClass.ignorePointIfMatched;
                }
            }

            if (normalizationState.controlPointUnits != normalizationStateIn.controlPointUnits)
            {
                currentResult.assignedUnits = normalizationState.controlPointUnits;
            }

            Set<String> tags = getTags(engine, normalizationState.tagsSet, normalizationState.tags, currentResult.id, output);

            mr.normalizationTags = tags;

            details.results.add(mr);

            //--//

            if (!dryRun)
            {
                String deviceNormalizedName = rec_device.getNormalizedName();

                BACnetObjectIdentifier objId = new BACnetObjectIdentifier(rec_object.getIdentifier());
                if (objId.object_type.equals(BACnetObjectType.device) && !StringUtils.equals(deviceNormalizedName, output))
                {
                    deviceNormalizedName = output;
                }

                String deviceName = workflowOverrides.deviceNames.get(rec_device.getSysId());
                if (deviceName != null && !StringUtils.equals(deviceNormalizedName, deviceName))
                {
                    deviceNormalizedName = deviceName;
                }

                rec_device.setNormalizedName(deviceNormalizedName);

                //--//

                if (!StringUtils.equals(oldValue, output))
                {
                    rec_object.setNormalizedName(output);
                    LoggerInstance.debug("Changed normalized name on %s (%s -> %s)", rec_object.getSysId(), oldValue, output);
                    modified = true;

                    // Refresh metadata, the previous reference has been invalidated.
                    metadata = rec_object.getMetadata();
                }

                if (mr.locations.isEmpty())
                {
                    lastClassification.locations = null;
                }
                else
                {
                    lastClassification.locations = mr.locations;
                }

                LocationRecord rec_location = getOrCreateLocation(sessionHolder, rec_locationRoot, mr.locations);
                if (updateLocation(rec_object, rec_location))
                {
                    LoggerInstance.debug("Changed location on %s", rec_object.getSysId());
                    modified = true;
                }

                if (handleEquipment(sessionHolder, engine, mr, rec_object, rec_locationRoot, metadata))
                {
                    LoggerInstance.debug("Changed equipment on %s", rec_object.getSysId());
                    modified = true;
                }

                addTags(tags, metadata);

                lastClassification.saveToMetadata(metadata);
            }

            modified |= handleClassification(sessionHolder, mr, rec_network, rec_device, rec_object, metadata, workflowOverrides, engine);

            if (!dryRun && rec_object.setMetadata(metadata))
            {
                LoggerInstance.debug("Changed metadata on %s", rec_object.getSysId());
                modified = true;
            }
        }

        return modified;
    }

    private void increment(TreeMap<String, Integer> map,
                           String key)
    {
        Integer count = map.get(key);
        map.put(key, count != null ? count + 1 : 1);
    }

    private boolean handleClassification(SessionHolder sessionHolder,
                                         ClassificationPointOutput output,
                                         NetworkAssetRecord rec_network,
                                         DeviceRecord rec_device,
                                         DeviceElementRecord rec_object,
                                         MetadataMap metadata,
                                         WorkflowOverrides workflowOverrides,
                                         NormalizationEngine engine)
    {
        String pointClassId = output.currentResult.id;

        PointClass pc = engine.lookupPointClass.get(pointClassId);
        if (pc != null && StringUtils.isNotBlank(pc.aliasPointClassId))
        {
            pointClassId = pc.aliasPointClassId;

            pc = engine.lookupPointClass.get(pc.aliasPointClassId);
        }

        DeviceElementClassificationMetadata previousClassification = DeviceElementClassificationMetadata.fromMetadata(metadata);

        String  previousPointClassId            = previousClassification.pointClassId;
        Double  previousPositivePointClassScore = previousClassification.positiveScore;
        Double  previousNegativePointClassScore = previousClassification.negativeScore;
        Double  previousPointClassThreshold     = previousClassification.pointClassThreshold;
        Boolean previousPointClassIgnored       = previousClassification.pointIgnore;

        output.lastResult = new ClassificationPointOutputDetails();

        if (previousPointClassId != null)
        {
            output.lastResult.id = previousPointClassId;
            if (previousPositivePointClassScore != null)
            {
                output.lastResult.positiveScore = previousPositivePointClassScore;
            }

            if (previousNegativePointClassScore != null)
            {
                output.lastResult.negativeScore = previousNegativePointClassScore;
            }

            if (previousPointClassThreshold != null)
            {
                output.lastResult.threshold = previousPointClassThreshold;
            }

            if (previousPointClassIgnored != null)
            {
                output.lastResult.ignored = previousPointClassIgnored;
            }
        }

        output.lastResult.assignedUnits = previousClassification.assignedUnits;

        output.lastResult.azureDigitalTwinModel = AssetRecord.WellKnownMetadata.azureDigitalTwinModel.get(metadata);

        List<DeviceElementSampling> existingConfig = rec_object.getSamplingSettings();

        if (CollectionUtils.isNotEmpty(existingConfig))
        {
            output.lastResult.samplingPeriod = CollectionUtils.firstElement(existingConfig).samplingPeriod;
        }
        else
        {
            output.lastResult.noSampling = true;
        }

        int     samplingPeriod         = workflowOverrides.pointSamplingPeriods.getOrDefault(output.sysId, output.currentResult.samplingPeriod);
        boolean samplingEnabled        = workflowOverrides.pointSampling.getOrDefault(output.sysId, !output.currentResult.noSampling);
        boolean explicitSamplingPeriod = samplingPeriod > 0;
        boolean isUnclassified         = pointClassId == null || output.currentResult.getTotalScore() < output.currentResult.threshold;
        boolean useDefaultSettings     = !explicitSamplingPeriod && isUnclassified;

        List<DeviceElementSampling> config;

        try
        {
            config = rec_device.prepareSamplingConfiguration(sessionHolder, rec_object, useDefaultSettings);
            if (CollectionUtils.isEmpty(config))
            {
                samplingEnabled = false;
            }
        }
        catch (Exception e)
        {
            LoggerInstance.warn("Failed to analyze object '%s': %s", rec_object.getIdentifier(), e);
            config = null;
        }

        if (samplingEnabled)
        {
            if (explicitSamplingPeriod)
            {
                output.currentResult.samplingPeriod = samplingPeriod;
            }
            else if (output.lastResult.samplingPeriod > 0)
            {
                output.currentResult.samplingPeriod = output.lastResult.samplingPeriod;
            }
            else
            {
                output.currentResult.samplingPeriod = rec_network.getSamplingPeriod();
            }
        }
        else
        {
            output.currentResult.noSampling = true;
        }

        Map<String, String> map        = m_lookupPointAzureModel.get();
        boolean             classified = pointClassId != null && !output.currentResult.ignored && output.currentResult.isAboveThreshold();

        output.currentResult.azureDigitalTwinModel = classified ? map.get(pointClassId) : null;

        //--//

        boolean modified = false;

        if (!dryRun)
        {
            previousClassification.pointClassId        = pointClassId;
            previousClassification.pointIgnore         = output.currentResult.ignored;
            previousClassification.positiveScore       = output.currentResult.positiveScore;
            previousClassification.negativeScore       = output.currentResult.negativeScore;
            previousClassification.pointClassThreshold = output.currentResult.threshold;
            previousClassification.pointClassTags      = pc != null ? pc.tags : null;
            previousClassification.assignedUnits       = output.currentResult.assignedUnits;

            AssetRecord.WellKnownMetadata.azureDigitalTwinModel.put(metadata, output.currentResult.azureDigitalTwinModel);

            previousClassification.saveToMetadata(metadata);

            if (!samplingEnabled)
            {
                if (rec_object.setSamplingSettings(null))
                {
                    LoggerInstance.debug("Changed sampling settings on %s", rec_object.getSysId());
                    modified = true;
                }
            }
            else if (explicitSamplingPeriod && config != null)
            {
                for (DeviceElementSampling sampling : config)
                {
                    sampling.samplingPeriod = samplingPeriod;
                }

                if (rec_object.setSamplingSettings(config))
                {
                    LoggerInstance.debug("Changed sampling settings on %s", rec_object.getSysId());
                    modified = true;
                }
            }
            else if (!rec_object.hasSamplingSettings())
            {
                // Set default sampling settings
                if (rec_object.setSamplingSettings(config))
                {
                    LoggerInstance.debug("Changed sampling settings on %s", rec_object.getSysId());
                    modified = true;
                }
            }
        }

        return modified;
    }

    private boolean handleEquipment(SessionHolder sessionHolder,
                                    NormalizationEngine engine,
                                    ClassificationPointOutput mr,
                                    DeviceElementRecord rec_object,
                                    LocationRecord rec_locationRoot,
                                    MetadataMap metadata)
    {

        AtomicBoolean       modified            = new AtomicBoolean();
        Set<String>         leaves              = Sets.newHashSet();
        Map<String, String> equipmentKeyToSysId = ensureExistingEquipment(sessionHolder);

        for (NormalizationEquipment equipment : mr.equipments.values())
        {
            String equipmentName = StringUtils.defaultString(equipment.name);

            if (equipment.key != null)
            {
                LogicalAssetRecord rec_equip;
                LocationRecord     rec_location = getOrCreateLocation(sessionHolder, rec_locationRoot, equipment.locations);
                if (!equipmentKeyToSysId.containsKey(equipment.key))
                {
                    rec_equip = createNewEquipment(sessionHolder, equipment.key, equipmentName, rec_location);
                }
                else
                {
                    rec_equip = getGroup(sessionHolder, equipmentKeyToSysId.get(equipment.key));
                }

                equipment.sysId = rec_equip.getSysId();

                updateEquipmentMetadata(sessionHolder, rec_equip, engine, equipment, rec_location);
            }

            if (!mr.equipmentRelationships.containsKey(equipment.key))
            {
                leaves.add(equipment.key);
            }
        }

        flushDirtyEquipment();

        WorkflowOverrides workflowOverrides = ensureWorkflowOverrides(sessionHolder);
        if (workflowOverrides.pointParents.containsKey(mr.sysId))
        {
            String parentKey = workflowOverrides.pointParents.get(mr.sysId);
            leaves.clear();
            if (equipmentKeyToSysId.containsKey(parentKey))
            {
                leaves.add(parentKey);
            }
        }

        List<String> mergedLeaves = CollectionUtils.transformToList(leaves, (key) ->
        {
            String mergedKey = key;
            while (workflowOverrides.equipmentMerge.containsKey(mergedKey))
            {
                mergedKey = workflowOverrides.equipmentMerge.get(mergedKey);
            }
            return mergedKey;
        });

        leaves = Sets.newHashSet(mergedLeaves);

        Multimap<String, String> groupsInverse = ensureGroupsInverse(sessionHolder);

        for (String groupId : groupsInverse.get(mr.sysId))
        {
            String equipmentKey = CollectionUtils.findFirst(leaves, (key) -> key != null && equipmentKeyToSysId.containsKey(key) && StringUtils.equals(equipmentKeyToSysId.get(key), groupId));

            if (equipmentKey == null)
            {
                // Point no longer part of this equipment
                LogicalAssetRecord rec_group = getGroup(sessionHolder, groupId);
                if (rec_group != null)
                {
                    if (RelationshipRecord.removeRelation(sessionHolder, rec_group, rec_object, AssetRelationship.controls))
                    {
                        modified.set(true);
                    }
                }
            }
            else
            {
                // Point already part of this equipment
                leaves.remove(equipmentKey);
            }
        }

        List<String> parentEquipNames = Lists.newArrayList();
        for (String finalEquipmentKey : mergedLeaves)
        {
            boolean inGroup = !leaves.contains(finalEquipmentKey);

            if (finalEquipmentKey != null && equipmentKeyToSysId.containsKey(finalEquipmentKey))
            {
                LogicalAssetRecord rec_equipNew = getGroup(sessionHolder, equipmentKeyToSysId.get(finalEquipmentKey));
                if (rec_equipNew != null)
                {
                    parentEquipNames.add(rec_equipNew.getName());

                    if (!inGroup)
                    {
                        RelationshipRecord.addRelation(sessionHolder, rec_equipNew, rec_object, AssetRelationship.controls, modified);
                    }
                }
            }
        }

        AssetRecord.WellKnownMetadata.parentEquipmentName.put(metadata, parentEquipNames.isEmpty() ? null : parentEquipNames);

        return modified.get();
    }

    private LogicalAssetRecord createNewEquipment(SessionHolder sessionHolder,
                                                  String equipmentKey,
                                                  String equipmentName,
                                                  LocationRecord rec_location)
    {
        LogicalAssetRecord rec_equipment = new LogicalAssetRecord();
        rec_equipment.setPhysicalName(equipmentName);
        rec_equipment.setLocation(rec_location);

        sessionHolder.persistEntity(rec_equipment);

        recordNewEquipment(sessionHolder, equipmentKey, rec_equipment);

        return rec_equipment;
    }

    private void updateEquipmentMetadata(SessionHolder sessionHolder,
                                         LogicalAssetRecord rec_equipment,
                                         NormalizationEngine engine,
                                         NormalizationEquipment equipment,
                                         LocationRecord rec_location)
    {
        if (equipment == null)
        {
            return;
        }

        MetadataMap                     metadata       = rec_equipment.getMetadata();
        EquipmentClassificationMetadata classification = EquipmentClassificationMetadata.fromMetadata(metadata);
        classification.equipmentClassId = equipment.equipmentClassId;
        classification.equipmentKey     = equipment.key;

        boolean addedTags = false;

        if (equipment.equipmentClassId != null)
        {
            var lookupAdt  = m_lookupEquipmentAzureModel.get();
            var lookupTags = m_lookupEquipmentTags.get();

            var tags = lookupTags.get(equipment.equipmentClassId);
            classification.equipmentClassTags = tags;
            if (CollectionUtils.isNotEmpty(tags))
            {
                metadata.modifyTags(AssetRecord.WellKnownMetadata.tags, (map) ->
                {
                    AssetRecord.WellKnownTags.assignTags(map, false, true, false, tags);
                });
                addedTags = true;
            }

            AssetRecord.WellKnownMetadata.azureDigitalTwinModel.put(metadata, lookupAdt.get(equipment.equipmentClassId));
        }
        else
        {
            AssetRecord.WellKnownMetadata.azureDigitalTwinModel.put(metadata, null);
        }

        classification.saveToMetadata(metadata);

        if (!addedTags)
        {
            Set<String> tags = getTagsFromName(engine, equipment.name);
            addTags(tags, metadata);
        }

        rec_equipment.setMetadata(metadata);
        rec_equipment.setPhysicalName(equipment.name);

        updateLocation(rec_equipment, rec_location);
        updateLocationFromOverride(sessionHolder, equipment.key, rec_equipment, ensureWorkflowOverrides(sessionHolder).equipmentLocations);
    }

    private LocationRecord getOrCreateLocation(SessionHolder sessionHolder,
                                               LocationRecord rec_locationRoot,
                                               List<NormalizationEquipmentLocation> locations)
    {
        if (locations == null || locations.isEmpty())
        {
            return rec_locationRoot;
        }

        String key = String.join(" - ", CollectionUtils.transformToList(locations, loc -> loc.name));
        key = String.join(" - ", rec_locationRoot.getSysId(), key);

        LocationRecord rec_loc = m_locationCache.get(key);
        if (rec_loc == null)
        {
            RecordHelper<LocationRecord> helper = sessionHolder.createHelper(LocationRecord.class);

            rec_loc = rec_locationRoot;

            for (NormalizationEquipmentLocation location : locations)
            {
                LocationRecord rec_sub = CollectionUtils.findFirst(rec_loc.getChildren(helper), rec -> StringUtils.equals(location.name, rec.getPhysicalName()));
                if (rec_sub == null)
                {
                    rec_sub = new LocationRecord();
                    rec_sub.setPhysicalName(location.name);
                    rec_sub.setType(location.type);

                    helper.persist(rec_sub);

                    rec_sub.linkToParent(helper, rec_loc);
                }

                rec_loc = rec_sub;

                if (location.type != null)
                {
                    Map<LocationType, String>       lookupAdt  = m_lookupLocationAzureModel.get();
                    Map<LocationType, List<String>> lookupTags = m_lookupLocationTags.get();

                    List<String> tags = lookupTags.get(location.type);
                    if (CollectionUtils.isNotEmpty(tags))
                    {
                        rec_loc.assignTags(tags, false, true, false);
                    }

                    rec_loc.setAzureDigitalTwinModel(lookupAdt.get(location.type));
                }
                else
                {
                    rec_loc.setAzureDigitalTwinModel(null);
                }
            }

            m_locationCache.put(key, rec_loc);
        }

        return rec_loc;
    }

    private boolean shouldUpdateLocation(AssetRecord rec_asset,
                                         LocationRecord rec_location)
    {
        return shouldUpdateLocation(rec_asset, RecordWithCommonFields.getSysIdSafe(rec_location));
    }

    private boolean shouldUpdateLocation(AssetRecord rec_asset,
                                         String locationSysId)
    {
        String currentSysId = ensureCurrentLocations().get(rec_asset.getSysId());
        return !StringUtils.equals(locationSysId, currentSysId);
    }

    private boolean updateLocation(AssetRecord rec_asset,
                                   LocationRecord rec_location)
    {
        if (shouldUpdateLocation(rec_asset, rec_location))
        {
            rec_asset.setLocation(rec_location);
            return true;
        }

        return false;
    }

    private boolean updateLocationFromOverride(SessionHolder sessionHolder,
                                               String id,
                                               AssetRecord rec_asset,
                                               Map<String, String> overrides)
    {
        if (!overrides.containsKey(id))
        {
            return false;
        }

        String locationSysId = overrides.get(id);
        if (shouldUpdateLocation(rec_asset, locationSysId))
        {
            if (locationSysId != null)
            {
                RecordHelper<LocationRecord> helper = sessionHolder.createHelper(LocationRecord.class);

                LocationRecord rec_location = helper.get(locationSysId);
                rec_asset.setLocation(rec_location);
            }
            else
            {
                rec_asset.setLocation(null);
            }

            return true;
        }

        return false;
    }

    private Map<String, String> ensureCurrentLocations()
    {
        if (m_locationsChunk == null)
        {
            m_locationsChunk = ensureChunkNoThrow("ChunkForLocations", ChunkForLocations.class, () ->
            {
                ChunkForLocations chunk = new ChunkForLocations();

                TagsEngine.Snapshot snapshot = getService(TagsEngine.class).acquireSnapshot(false);

                LocationsEngine          locationsEngine   = getService(LocationsEngine.class);
                LocationsEngine.Snapshot locationsSnapshot = locationsEngine.acquireSnapshot(true);
                List<LocationHierarchy>  locations         = locationsSnapshot.extractHierarchy();

                processLocationList(chunk, snapshot, locations);

                return chunk;
            });
        }

        return m_locationsChunk.assetToLocation;
    }

    private static void processLocationList(ChunkForLocations chunk,
                                            TagsEngine.Snapshot snapshot,
                                            List<LocationHierarchy> locations)
    {
        for (LocationHierarchy hierarchy : locations)
        {
            TagsConditionLocation condition = new TagsConditionLocation();
            condition.locationSysId = hierarchy.ri.sysId;
            TagsEngine.Snapshot.AssetSet assets = snapshot.evaluateCondition(condition);

            assets.streamResolved((ri) ->
                                  {
                                      chunk.assetToLocation.put(ri.sysId, condition.locationSysId);
                                      return TagsStreamNextAction.Continue;
                                  });

            processLocationList(chunk, snapshot, hierarchy.subLocations);
        }
    }

    //--//

    private Map<String, String> ensureExistingEquipment(SessionHolder sessionHolder)
    {
        if (m_equipmentChunk == null)
        {
            m_equipmentChunk = ensureChunkNoThrow(chunk_EQUIPMENT, ChunkForEquipment.class, () ->
            {
                ChunkForEquipment chunk = new ChunkForEquipment();

                RecordHelper<LogicalAssetRecord> helper = sessionHolder.createHelper(LogicalAssetRecord.class);
                for (LogicalAssetRecord rec_group : helper.listAll())
                {
                    if (rec_group != null && rec_group.isEquipment())
                    {
                        m_equipmentCache.put(rec_group.getSysId(), rec_group);

                        MetadataMap metadata = rec_group.getMetadata();

                        EquipmentClassificationMetadata equipmentMetadata = EquipmentClassificationMetadata.fromMetadata(metadata);

                        String equipmentKey = equipmentMetadata.equipmentKey;
                        if (equipmentKey != null)
                        {
                            chunk.keyToSysId.putIfAbsent(equipmentKey, rec_group.getSysId());
                        }
                    }
                }

                return chunk;
            });
        }

        return m_equipmentChunk.keyToSysId;
    }

    private void recordNewEquipment(SessionHolder sessionHolder,
                                    String equipmentKey,
                                    LogicalAssetRecord rec_equipment)
    {
        String sysId = rec_equipment.getSysId();
        m_equipmentCache.putIfAbsent(sysId, rec_equipment);

        ensureExistingEquipment(sessionHolder);

        m_equipmentChunk.keyToSysId.putIfAbsent(equipmentKey, sysId);
        m_equipmentChunk.dirty = true;
    }

    private void flushDirtyEquipment()
    {
        if (m_equipmentChunk != null && m_equipmentChunk.dirty)
        {
            putChunk(chunk_EQUIPMENT, m_equipmentChunk);
            m_equipmentChunk.dirty = false;
        }
    }

    //--//

    private Multimap<String, String> ensureGroupsInverse(SessionHolder sessionHolder)
    {
        if (m_groupsChunk == null)
        {
            m_groupsChunk = ensureChunkNoThrow("ChunkForGroups", ChunkForGroups.class, () ->
            {
                ChunkForGroups chunk = new ChunkForGroups();

                Multimap<String, String> groups = RelationshipRecord.fetchAllMatchingRelations(sessionHolder, AssetRelationship.controls);

                chunk.groupsInverse = Multimaps.invertFrom(groups, ArrayListMultimap.create());

                return chunk;
            });
        }

        return m_groupsChunk.groupsInverse;
    }

    //--//

    private DeviceElementRecord findDeviceObject(SessionHolder sessionHolder,
                                                 BACnetDeviceRecord rec_device)
    {
        if (m_deviceObjectsChunk == null)
        {
            m_deviceObjectsChunk = ensureChunkNoThrow("ChunkForDeviceObjects", ChunkForDeviceObjects.class, () ->
            {
                ChunkForDeviceObjects chunk = new ChunkForDeviceObjects();

                BACnetDeviceRecord.findDeviceObjects(sessionHolder)
                                  .forEach((k, v) -> chunk.deviceToObject.put(v, k));

                return chunk;
            });
        }

        return sessionHolder.getEntityOrNull(DeviceElementRecord.class, m_deviceObjectsChunk.deviceToObject.get(rec_device.getSysId()));
    }

    //--//

    private static void addTags(Set<String> tagSet,
                                MetadataMap metadata)
    {
        metadata.modifyTags(AssetRecord.WellKnownMetadata.tags, (tags) ->
        {
            // Reset classification tags.
            AssetRecord.WellKnownTags.clearTags(tags, false, true, false);

            tags.addTags(tagSet, true);
        });
    }

    public static Set<String> getTags(NormalizationEngine engine,
                                      boolean hasNormalizationTags,
                                      Set<String> normalizationTags,
                                      String pointClassId,
                                      String name)
    {
        Set<String> tags = Sets.newHashSet();

        PointClass pc = engine.lookupPointClass.get(pointClassId);
        if (pc != null && StringUtils.isNotBlank(pc.aliasPointClassId))
        {
            pc = engine.lookupPointClass.get(pc.aliasPointClassId);
        }

        Collection<String> pointClassTags = pc != null ? pc.tags : null;

        boolean hasPointClassTags = CollectionUtils.isNotEmpty(pointClassTags);

        if (hasPointClassTags)
        {
            tags.addAll(pointClassTags);
        }

        if (hasNormalizationTags)
        {
            tags.addAll(normalizationTags);
        }

        if (!hasPointClassTags && !hasNormalizationTags)
        {
            tags.addAll(getTagsFromName(engine, name));
        }

        return tags;
    }

    private static Set<String> getTagsFromName(NormalizationEngine engine,
                                               String name)
    {
        Set<String>            tags  = Sets.newHashSet();
        Set<NormalizationTerm> terms = engine.extractDimensions(name);

        for (NormalizationTerm term : terms)
        {
            if (term.acronym != null)
            {
                tags.add(term.acronym);
            }

            tags.addAll(term.nameWords);
        }

        return tags;
    }

    private static String getEquipmentKey(String equipmentName,
                                          List<NormalizationEquipmentLocation> locations,
                                          String networkSysId,
                                          String parentKey)
    {
        if (StringUtils.isBlank(equipmentName))
        {
            return null;
        }

        String locationKey = "";
        if (locations != null && !locations.isEmpty())
        {
            locationKey = String.join("|", CollectionUtils.transformToList(locations, loc -> loc.name));
        }

        String key = String.format("%s|%s", equipmentName, locationKey);

        if (parentKey != null)
        {
            return String.format("%s::%s", parentKey, key);
        }
        else
        {
            return String.format("%s::%s", networkSysId, key);
        }
    }

    private LogicalAssetRecord getGroup(SessionHolder sessionHolder,
                                        String sysId)
    {
        LogicalAssetRecord rec = m_equipmentCache.get(sysId);
        if (rec == null)
        {
            rec = sessionHolder.getEntity(LogicalAssetRecord.class, sysId);
            m_equipmentCache.put(sysId, rec);
        }

        return rec;
    }

    private void createEquipmentRelationships(SessionHolder sessionHolder,
                                              NormalizationEngine engine)
    {
        // Lookup to map client keys to server keys
        Map<String, String> lookup              = Maps.newHashMap();
        Map<String, String> equipmentKeyToSysId = ensureExistingEquipment(sessionHolder);

        for (Map.Entry<String, NormalizationEquipment> entry : rules.equipments.entrySet())
        {
            // Don't trust key given by client, create our own copy
            NormalizationEquipment equipment    = entry.getValue();
            String                 equipmentKey = getEquipmentKey(equipment.name, equipment.locations, null, null);
            lookup.put(entry.getKey(), equipmentKey);

            if (!equipments.containsKey(equipmentKey))
            {
                equipments.put(equipmentKey, equipment);
                equipmentRelationships.put(equipmentKey, Sets.newHashSet());

                if (!dryRun && !equipmentKeyToSysId.containsKey(equipmentKey))
                {
                    LogicalAssetRecord rec_equipment = createNewEquipment(sessionHolder, equipmentKey, equipment.name, null);
                    updateEquipmentMetadata(sessionHolder, rec_equipment, engine, equipment, null);
                }
            }
        }

        flushDirtyEquipment();

        for (String key : rules.equipmentRelationships.keySet())
        {
            String equipKey = lookup.getOrDefault(key, key);

            // Don't add relationships if the equipment has gone away
            if (equipmentRelationships.containsKey(equipKey))
            {
                for (String childKey : CollectionUtils.transformToList(rules.equipmentRelationships.get(key), k -> lookup.getOrDefault(k, k)))
                {
                    removeParentRelationships(childKey);
                    equipmentRelationships.get(equipKey)
                                          .add(childKey);
                }
            }
        }

        WorkflowOverrides workflowOverrides = ensureWorkflowOverrides(sessionHolder);
        for (String childKey : workflowOverrides.equipmentParents.keySet())
        {
            if (equipments.containsKey(childKey))
            {
                removeParentRelationships(childKey);

                String parentKey = workflowOverrides.equipmentParents.get(childKey);
                if (parentKey != null && equipments.containsKey(parentKey))
                {
                    equipmentRelationships.get(parentKey)
                                          .add(childKey);
                }
            }
        }
    }

    private void removeParentRelationships(String key)
    {
        for (Set<String> children : equipmentRelationships.values())
        {
            children.remove(key);
        }
    }

    private void persistEquipmentRelationships(SessionHolder sessionHolder)
    {
        // Create requested relationships
        for (String equipKey : equipmentRelationships.keySet())
        {
            Map<String, String> equipmentKeyToSysId = ensureExistingEquipment(sessionHolder);

            try (SessionHolder holder = sessionHolder.spawnNewSessionWithTransaction())
            {
                RecordHelper<LogicalAssetRecord> helper = holder.createHelper(LogicalAssetRecord.class);

                LogicalAssetRecord rec_parent = helper.getOrNull(equipmentKeyToSysId.get(equipKey));
                if (rec_parent != null)
                {
                    Set<String> currentChildren   = Sets.newHashSet(RelationshipRecord.getChildren(holder, rec_parent.getSysId(), AssetRelationship.controls));
                    Set<String> requestedChildren = Sets.newHashSet();
                    for (String subEquipKey : equipmentRelationships.get(equipKey))
                    {
                        String sysId = equipmentKeyToSysId.get(subEquipKey);
                        requestedChildren.add(sysId);
                        if (!currentChildren.contains(sysId))
                        {
                            LogicalAssetRecord rec_child = helper.getOrNull(sysId);
                            if (rec_child != null)
                            {
                                RelationshipRecord.addRelation(holder, rec_parent, rec_child, AssetRelationship.controls);
                            }
                        }
                    }

                    for (String childSysId : currentChildren)
                    {
                        if (m_equipmentCache.containsKey(childSysId) && !requestedChildren.contains(childSysId))
                        {
                            LogicalAssetRecord rec_child = holder.getEntityOrNull(LogicalAssetRecord.class, childSysId);
                            if (rec_child != null)
                            {
                                RelationshipRecord.removeRelation(holder, rec_parent, rec_child, AssetRelationship.controls);
                            }
                        }
                    }
                }

                holder.commit();
            }
        }
    }
}
