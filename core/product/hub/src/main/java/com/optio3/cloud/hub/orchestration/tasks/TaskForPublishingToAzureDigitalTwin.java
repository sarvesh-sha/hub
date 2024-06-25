/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.azure.digitaltwins.core.BasicDigitalTwin;
import com.azure.digitaltwins.core.BasicDigitalTwinMetadata;
import com.azure.digitaltwins.core.BasicRelationship;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.optio3.cloud.exception.NotImplementedException;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.model.asset.AzureDigitalTwinSyncProgress;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.model.customization.InstanceConfigurationForCRE;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.asset.RelationshipRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.IBackgroundActivityProgress;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.infra.integrations.azuredigitaltwins.AzureDigitalTwinsHelper;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class TaskForPublishingToAzureDigitalTwin extends BaseReportTask implements IBackgroundActivityProgress<AzureDigitalTwinSyncProgress>
{
    private static final String c_IdPrefix    = "Optio3_";
    private static final String c_IdPrefixInv = "Optio3Inv_";

    private static class LocationInfo
    {
        public final String sysId;
        public final String name;
        public final String parentSysId;
        public final String adt;

        LocationInfo(String sysId,
                     String name,
                     String parentSysId,
                     String adt)
        {
            this.sysId       = sysId;
            this.name        = name;
            this.parentSysId = parentSysId;
            this.adt         = adt;
        }

        String getId()
        {
            return c_IdPrefix + sysId;
        }

        String getParentRelId(boolean active)
        {
            return parentSysId != null ? (active ? c_IdPrefix : c_IdPrefixInv) + parentSysId + "__" + sysId : null;
        }
    }

    private static class EquipmentInfo
    {
        public final String  sysId;
        public final String  name;
        public final String  parentSysId;
        public final String  locationSysId;
        public final String  adt;
        public       boolean inUse;

        EquipmentInfo(String sysId,
                      String name,
                      String parentSysId,
                      String locationSysId,
                      String adt)
        {
            this.sysId         = sysId;
            this.name          = name;
            this.parentSysId   = parentSysId;
            this.locationSysId = locationSysId;
            this.adt           = adt;
        }

        String getId()
        {
            return c_IdPrefix + sysId;
        }

        String getParentRelId(boolean active)
        {
            return parentSysId != null ? (active ? c_IdPrefix : c_IdPrefixInv) + parentSysId + "__" + sysId : null;
        }

        String getLocationRelId(boolean active)
        {
            return locationSysId != null ? (active ? c_IdPrefix : c_IdPrefixInv) + locationSysId + "__" + sysId : null;
        }
    }

    private static class PointInfo
    {
        public final String sysId;
        public final String bacnetExternalId;
        public final String name;
        public final String equipmentSysId;
        public final String locationSysId;
        public final String adt;

        PointInfo(String sysId,
                  String bacnetExternalId,
                  String name,
                  String equipmentSysId,
                  String locationSysId,
                  String adt)
        {
            this.sysId            = sysId;
            this.bacnetExternalId = bacnetExternalId;
            this.name             = name;
            this.equipmentSysId   = equipmentSysId;
            this.locationSysId    = locationSysId;
            this.adt              = adt;
        }

        String getId()
        {
            return c_IdPrefix + sysId;
        }

        String getLocationRelId(boolean active)
        {
            return locationSysId != null ? (active ? c_IdPrefix : c_IdPrefixInv) + locationSysId + "__" + sysId : null;
        }

        String getEquipmentRelId(boolean active)
        {
            return equipmentSysId != null ? (active ? c_IdPrefix : c_IdPrefixInv) + equipmentSysId + "__" + sysId : null;
        }
    }

    private class LocalState
    {
        private final SessionHolder m_sessionHolder;
        private final ReportFlusher m_state;

        public final Map<String, PointInfo>     points    = Maps.newHashMap();
        public final Map<String, EquipmentInfo> equipment = Maps.newHashMap();
        public final Map<String, LocationInfo>  locations = Maps.newHashMap();

        public LocalState(SessionHolder sessionHolder,
                          ReportFlusher state)
        {
            m_sessionHolder = sessionHolder;
            m_state         = state;
        }

        public void analyze() throws
                              Exception
        {
            RecordHelper<LogicalAssetRecord>  helper_group   = m_sessionHolder.createHelper(LogicalAssetRecord.class);
            RecordHelper<DeviceRecord>        helper_device  = m_sessionHolder.createHelper(DeviceRecord.class);
            RecordHelper<DeviceElementRecord> helper_element = m_sessionHolder.createHelper(DeviceElementRecord.class);

            //
            // 1) Build equipment hierarchy.
            //
            Multimap<String, String> groups        = RelationshipRecord.fetchAllMatchingRelations(m_sessionHolder, AssetRelationship.controls);
            Multimap<String, String> groupsInverse = Multimaps.invertFrom(groups, HashMultimap.create());

            for (LogicalAssetRecord rec_group : helper_group.listAll())
            {
                if (rec_group.isEquipment())
                {
                    String equipADT = rec_group.getAzureDigitalTwinModel();
                    if (equipADT != null)
                    {
                        String parentSysId   = CollectionUtils.firstElement(groupsInverse.get(rec_group.getSysId()));
                        String locationSysId = RecordWithCommonFields.getSysIdSafe(rec_group.getLocation());

                        EquipmentInfo equipInfo = new EquipmentInfo(rec_group.getSysId(), rec_group.getName(), parentSysId, locationSysId, equipADT);
                        equipment.put(equipInfo.sysId, equipInfo);
                    }
                }
            }

            //
            // 2) Analyze all controllers and points
            //
            for (RecordLocator<NetworkAssetRecord> loc_network : targets)
            {
                NetworkAssetRecord rec_network = m_sessionHolder.fromLocator(loc_network);

                ReportFlusher state = new ReportFlusher(1000);

                rec_network.enumerateChildren(helper_device, true, -1, null, (rec_device) ->
                {
                    BACnetDeviceDescriptor desc = Reflection.as(rec_device.getIdentityDescriptor(), BACnetDeviceDescriptor.class);
                    if (desc != null)
                    {
                        // Update filter parent
                        DeviceElementFilterRequest filter = DeviceElementFilterRequest.createFilterForParent(rec_device.getSysId());

                        // Find child device elements
                        DeviceElementRecord.enumerateNoNesting(helper_element, filter, (rec_object) ->
                        {
                            String pointClassAdt = rec_object.getAzureDigitalTwinModel();
                            if (pointClassAdt != null)
                            {
                                String sysId      = rec_object.getSysId();
                                String equipSysId = CollectionUtils.firstElement(groupsInverse.get(sysId));

                                String                 controllerIP = StringUtils.replace(desc.transport.toString(), "UDP::", "");
                                BACnetObjectIdentifier objectId     = new BACnetObjectIdentifier(rec_object.getIdentifier());

                                String bacnetExternalId;
                                if (desc.bacnetAddress != null)
                                {
                                    bacnetExternalId = String.format("%s#%s %s %s", controllerIP, desc.bacnetAddress.getMacAddress(), desc.address, objectId.toJsonValue());
                                }
                                else
                                {
                                    bacnetExternalId = String.format("%s %s %s", controllerIP, desc.address, objectId.toJsonValue());
                                }

                                var pi = new PointInfo(sysId, bacnetExternalId, rec_object.getName(), equipSysId, RecordWithCommonFields.getSysIdSafe(rec_object.getLocation()), pointClassAdt);

                                points.put(pi.sysId, pi);
                            }

                            results.elementsProcessed++;
                            return StreamHelperNextAction.Continue_Evict;
                        });
                    }

                    results.devicesProcessed++;

                    if (state.shouldReport())
                    {
                        flushStateToDatabase(m_sessionHolder);
                    }

                    return StreamHelperNextAction.Continue_Evict;
                });
            }

            flushStateToDatabase(m_sessionHolder);

            //
            // 3) Build location hierarchy.
            //
            LocationsEngine          locationsEngine   = m_sessionHolder.getServiceNonNull(LocationsEngine.class);
            LocationsEngine.Snapshot locationsSnapshot = locationsEngine.acquireSnapshot(true);
            Map<String, String>      locationHierarchy = locationsSnapshot.extractReverseHierarchy();

            for (PointInfo pointInfo : points.values())
            {
                markLocations(locationsSnapshot, locationHierarchy, locations, pointInfo.locationSysId);

                //
                // Mark whole equipment chain as in use.
                //
                String equipSysId = pointInfo.equipmentSysId;
                while (equipSysId != null)
                {
                    EquipmentInfo equipInfo = equipment.get(equipSysId);
                    if (equipInfo == null)
                    {
                        break;
                    }

                    if (equipInfo.inUse)
                    {
                        // Already marked.
                        break;
                    }

                    equipInfo.inUse = true;
                    markLocations(locationsSnapshot, locationHierarchy, locations, equipInfo.locationSysId);

                    equipSysId = equipInfo.parentSysId;
                }
            }

            flushStateToDatabase(m_sessionHolder);
        }

        private void markLocations(LocationsEngine.Snapshot locationsSnapshot,
                                   Map<String, String> locationHierarchy,
                                   Map<String, LocationInfo> locations,
                                   String sysId)
        {
            //
            // Mark whole location chain as seen.
            //
            while (sysId != null)
            {
                if (locations.containsKey(sysId))
                {
                    // Already processed.
                    break;
                }

                String name = locationsSnapshot.getName(sysId);
                String adt  = locationsSnapshot.getAdtModel(sysId);
                if (name == null || adt == null)
                {
                    break;
                }

                String parentSysId = locationHierarchy.get(sysId);

                // BUGBUG: REC ontology is broken, everything needs to be a space.
                adt = "dtmi:digitaltwins:rec_3_3:core:Space;1";
                var li = new LocationInfo(sysId, name, parentSysId, adt);
                locations.put(sysId, li);

                sysId = parentSysId;
            }
        }
    }

    private class Progress
    {
        private final String         m_context;
        private final int            m_total;
        private       int            m_counter    = 0;
        private       MonotonousTime m_nextReport = TimeUtils.computeTimeoutExpiration(30, TimeUnit.SECONDS);

        Progress(String context,
                 int total)
        {
            m_context = context;
            m_total   = total;
        }

        void progress()
        {
            m_counter++;
            if (TimeUtils.isTimeoutExpired(m_nextReport))
            {
                loggerInstance.info("Processing %d %s...", m_counter, m_context);
                m_nextReport = TimeUtils.computeTimeoutExpiration(30, TimeUnit.SECONDS);
            }
        }

        <T> void done()
        {
            loggerInstance.info("Processed all %d %s!", m_total, m_context);
        }
    }

    private class RemoteState
    {
        private final SessionHolder           m_sessionHolder;
        private final ReportFlusher           m_state;
        private final AzureDigitalTwinsHelper m_adtHelper;

        public final Map<String, BasicDigitalTwin>       sysIdToTwin           = Maps.newHashMap();
        public final Multimap<String, BasicRelationship> inboundRelationships  = LinkedListMultimap.create();
        public final Multimap<String, BasicRelationship> outboundRelationships = LinkedListMultimap.create();

        public RemoteState(SessionHolder sessionHolder,
                           ReportFlusher state,
                           AzureDigitalTwinsHelper.Credentials cred)
        {
            m_sessionHolder = sessionHolder;
            m_state         = state;
            m_adtHelper     = new AzureDigitalTwinsHelper(cred);
        }

        public void analyze() throws
                              Exception
        {
            Map<String, AtomicInteger> countersTwins        = Maps.newHashMap();
            Map<String, AtomicInteger> countersRelationship = Maps.newHashMap();

            Map<String, BasicDigitalTwin> twins = Maps.newHashMap();

            for (BasicDigitalTwin twin : m_adtHelper.queryTwins("SELECT * FROM DIGITALTWINS T WHERE IS_DEFINED(T.externalIds.Optio3)"))
            {
                m_adtHelper.sanitizeTwin(twin);

                countersTwins.computeIfAbsent(twin.getMetadata()
                                                  .getModelId(), (key) -> new AtomicInteger(0))
                             .incrementAndGet();

                String sysId = m_adtHelper.getTwinProperty(twin, "externalIds", "Optio3");
                if (sysId != null)
                {
                    sysIdToTwin.put(sysId, twin);
                    twins.put(twin.getId(), twin);

                    results.twinsFound++;

                    if (m_state.shouldReport())
                    {
                        flushStateToDatabase(m_sessionHolder);
                    }
                }
            }
            loggerInstance.info("Found %d twins...", results.twinsFound);
            countersTwins.forEach((key, value) -> loggerInstance.info("  %d %s", value.get(), key));

            flushStateToDatabase(m_sessionHolder);

            //
            // Only keep relationships involving Optio3 twins.
            //
            for (BasicRelationship relationship : m_adtHelper.queryRelationships("SELECT * FROM RELATIONSHIPS"))
            {
                countersRelationship.computeIfAbsent(relationship.getName(), (key) -> new AtomicInteger(0))
                                    .incrementAndGet();

                boolean included = false;
                if (twins.containsKey(relationship.getSourceId()))
                {
                    outboundRelationships.put(relationship.getSourceId(), relationship);
                    included = true;
                }

                if (twins.containsKey(relationship.getTargetId()))
                {
                    inboundRelationships.put(relationship.getTargetId(), relationship);
                    included = true;
                }

                if (included)
                {
                    results.relationshipsFound++;

                    if (m_state.shouldReport())
                    {
                        flushStateToDatabase(m_sessionHolder);
                    }
                }
            }
            loggerInstance.info("Found %d relationships...", results.relationshipsFound);
            countersRelationship.forEach((key, value) -> loggerInstance.info("  %d %s", value.get(), key));

            flushStateToDatabase(m_sessionHolder);
        }

        public void synchronize(LocalState localState) throws
                                                       Exception
        {
            Set<String> twinsToKeep         = Sets.newHashSet();
            Set<String> relationshipsToKeep = Sets.newHashSet();

            loggerInstance.info("Synchronizing %d locations, %d equipment, %d points...", localState.locations.size(), localState.equipment.size(), localState.points.size());

            loggerInstance.info("Synchronizing twins...");

            synchronizeTwins(twinsToKeep, localState);
            flushStateToDatabase(m_sessionHolder);

            loggerInstance.info("Deleting stale twins...");

            deleteStaleTwins(twinsToKeep);
            flushStateToDatabase(m_sessionHolder);

            loggerInstance.info("Synchronizing relationships...");

            synchronizeRelationships(relationshipsToKeep, localState);
            flushStateToDatabase(m_sessionHolder);

            loggerInstance.info("Deleting stale relationships...");

            deleteStaleRelationships(relationshipsToKeep);
            flushStateToDatabase(m_sessionHolder);
        }

        private void synchronizeTwins(Set<String> twinsToKeep,
                                      LocalState localState) throws
                                                             Exception
        {
            {
                Progress progress = new Progress("locations", localState.locations.size());

                for (LocationInfo li : localState.locations.values())
                {
                    BasicDigitalTwin twin = new BasicDigitalTwin(li.getId()).setMetadata(new BasicDigitalTwinMetadata().setModelId(li.adt));
                    m_adtHelper.setTwinProperty(twin, li.name, "name");
                    m_adtHelper.setTwinProperty(twin, li.sysId, "externalIds", "Optio3");

                    ensureTwin(twinsToKeep, li.sysId, twin);

                    progress.progress();
                }

                progress.done();
            }

            {
                Progress progress = new Progress("equipment", localState.equipment.size());

                for (EquipmentInfo ei : localState.equipment.values())
                {
                    BasicDigitalTwin twin = new BasicDigitalTwin(ei.getId()).setMetadata(new BasicDigitalTwinMetadata().setModelId(ei.adt));
                    m_adtHelper.setTwinProperty(twin, ei.name, "name");
                    m_adtHelper.setTwinProperty(twin, ei.sysId, "externalIds", "Optio3");

                    ensureTwin(twinsToKeep, ei.sysId, twin);

                    progress.progress();
                }

                progress.done();
            }

            {
                Progress progress = new Progress("points", localState.points.size());

                for (PointInfo pi : localState.points.values())
                {
                    BasicDigitalTwin twin = new BasicDigitalTwin(pi.getId()).setMetadata(new BasicDigitalTwinMetadata().setModelId(pi.adt));
                    m_adtHelper.setTwinProperty(twin, pi.name, "name");
                    m_adtHelper.setTwinProperty(twin, pi.sysId, "externalIds", "Optio3");
                    m_adtHelper.setTwinProperty(twin, pi.bacnetExternalId, "externalIds", "BACnet");

                    ensureTwin(twinsToKeep, pi.sysId, twin);

                    progress.progress();
                }

                progress.done();
            }
        }

        private void synchronizeRelationships(Set<String> relationshipsToKeep,
                                              LocalState localState)
        {
            {
                Progress progress = new Progress("location relationships", localState.locations.size());

                for (LocationInfo li : localState.locations.values())
                {
                    if (li.parentSysId != null)
                    {
                        BasicDigitalTwin twinSource = sysIdToTwin.get(li.parentSysId);
                        BasicDigitalTwin twinTarget = sysIdToTwin.get(li.sysId);

                        if (locations_useActive)
                        {
                            ensureRelationship(relationshipsToKeep, li.getParentRelId(true), twinSource, twinTarget, "hasPart");
                        }

                        if (locations_usePassive)
                        {
                            ensureRelationship(relationshipsToKeep, li.getParentRelId(false), twinTarget, twinSource, "isPartOf");
                        }

                        progress.progress();
                    }
                }

                progress.done();
            }

            {
                Progress progress = new Progress("equipment relationships", localState.equipment.size());

                for (EquipmentInfo ei : localState.equipment.values())
                {
                    if (ei.parentSysId != null)
                    {
                        BasicDigitalTwin twinSource = sysIdToTwin.get(ei.parentSysId);
                        BasicDigitalTwin twinTarget = sysIdToTwin.get(ei.sysId);

                        if (equipment_useActive)
                        {
                            ensureRelationship(relationshipsToKeep, ei.getParentRelId(true), twinSource, twinTarget, "hasPart");

                            progress.progress();
                        }

                        if (equipment_usePassive)
                        {
                            ensureRelationship(relationshipsToKeep, ei.getParentRelId(false), twinTarget, twinSource, "isPartOf");

                            progress.progress();
                        }
                    }

                    if (ei.locationSysId != null)
                    {
                        BasicDigitalTwin twinSource = sysIdToTwin.get(ei.locationSysId);
                        BasicDigitalTwin twinTarget = sysIdToTwin.get(ei.sysId);

                        if (equipment_useActive)
                        {
                            ensureRelationship(relationshipsToKeep, ei.getLocationRelId(true), twinSource, twinTarget, "isLocationOf");

                            progress.progress();
                        }

                        if (equipment_usePassive)
                        {
                            ensureRelationship(relationshipsToKeep, ei.getLocationRelId(false), twinTarget, twinSource, "locatedIn");

                            progress.progress();
                        }
                    }
                }

                progress.done();
            }

            {
                Progress progress = new Progress("point relationships", localState.points.size());

                for (PointInfo pi : localState.points.values())
                {
                    if (pi.locationSysId != null)
                    {
                        BasicDigitalTwin twinSource = sysIdToTwin.get(pi.locationSysId);
                        BasicDigitalTwin twinTarget = sysIdToTwin.get(pi.sysId);

                        if (points_useActive)
                        {
                            ensureRelationship(relationshipsToKeep, pi.getLocationRelId(true), twinSource, twinTarget, "hasCapability");

                            progress.progress();
                        }

                        if (points_usePassive)
                        {
                            ensureRelationship(relationshipsToKeep, pi.getLocationRelId(false), twinTarget, twinSource, "isCapabilityOf");

                            progress.progress();
                        }
                    }

                    if (pi.equipmentSysId != null)
                    {
                        BasicDigitalTwin twinSource = sysIdToTwin.get(pi.equipmentSysId);
                        BasicDigitalTwin twinTarget = sysIdToTwin.get(pi.sysId);

                        if (points_useActive)
                        {
                            ensureRelationship(relationshipsToKeep, pi.getEquipmentRelId(true), twinSource, twinTarget, "hasCapability");

                            progress.progress();
                        }

                        if (points_usePassive)
                        {
                            ensureRelationship(relationshipsToKeep, pi.getEquipmentRelId(false), twinTarget, twinSource, "isCapabilityOf");

                            progress.progress();
                        }
                    }
                }

                progress.done();
            }
        }

        private void deleteStaleTwins(Set<String> twinsToKeep)
        {
            int total = 0;

            for (BasicDigitalTwin twin : sysIdToTwin.values())
            {
                String id = twin.getId();

                if (!twinsToKeep.contains(id))
                {
                    for (BasicRelationship rel : inboundRelationships.get(id))
                    {
                        m_adtHelper.deleteRelationship(rel.getSourceId(), rel.getId());
                    }

                    inboundRelationships.removeAll(id);

                    for (BasicRelationship rel : outboundRelationships.get(id))
                    {
                        m_adtHelper.deleteRelationship(rel.getSourceId(), rel.getId());
                        results.relationshipsProcessed++;
                    }

                    outboundRelationships.removeAll(id);

                    m_adtHelper.deleteTwin(id);
                    results.twinsProcessed++;
                    total++;

                    if (m_state.shouldReport())
                    {
                        flushStateToDatabase(m_sessionHolder);
                    }
                }
            }

            if (total > 0)
            {
                loggerInstance.info("Deleted %d stale twins!", total);
            }
        }

        private void deleteStaleRelationships(Set<String> relationshipsToKeep)
        {
            int total = 0;

            for (BasicRelationship rel : inboundRelationships.values())
            {
                String id = rel.getId();

                if (!relationshipsToKeep.contains(id))
                {
                    m_adtHelper.deleteRelationship(rel.getSourceId(), id);
                    results.relationshipsProcessed++;
                    total++;

                    if (m_state.shouldReport())
                    {
                        flushStateToDatabase(m_sessionHolder);
                    }
                }
            }

            if (total > 0)
            {
                loggerInstance.info("Deleted %d stale relationships!", total);
            }
        }

        private void ensureTwin(Set<String> twinsToKeep,
                                String sysId,
                                BasicDigitalTwin twin) throws
                                                       Exception
        {
            m_adtHelper.sanitizeTwin(twin);

            BasicDigitalTwin twinExisting = sysIdToTwin.get(sysId);
            if (m_adtHelper.areTwinsEquivalent(twin, twinExisting))
            {
                twinsToKeep.add(twinExisting.getId());
            }
            else
            {
                try
                {
                    twin = m_adtHelper.createOrReplaceTwin(twin);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(String.format("Failed to create twin %s(%s)",
                                                             twin.getMetadata()
                                                                 .getModelId(),
                                                             twin.getId()), e);
                }

                twinsToKeep.add(twin.getId());
                sysIdToTwin.put(sysId, twin);
            }

            results.twinsProcessed++;

            if (m_state.shouldReport())
            {
                flushStateToDatabase(m_sessionHolder);
            }
        }

        private void ensureRelationship(Set<String> relationshipsToKeep,
                                        String id,
                                        BasicDigitalTwin twinSource,
                                        BasicDigitalTwin twinTarget,
                                        String relationshipName)
        {
            if (twinSource != null && twinTarget != null)
            {
                id += "/" + relationshipName;

                boolean           found = false;
                BasicRelationship rel   = new BasicRelationship(id, twinSource.getId(), twinTarget.getId(), relationshipName);
                for (BasicRelationship relExisting : outboundRelationships.get(twinSource.getId()))
                {
                    if (m_adtHelper.areRelationshipsEquivalent(rel, relExisting))
                    {
                        relationshipsToKeep.add(relExisting.getId());
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    try
                    {
                        rel = m_adtHelper.createOrReplaceRelationship(rel);
                    }
                    catch (Exception e)
                    {
                        // BUGBUG: Due to inconsistencies in REC ontology, don't quit on relationship failures.
                        loggerInstance.error("Failed to create relationship '%s' between %s(%s) and %s(%s), due to %s",
                                             relationshipName,
                                             twinSource.getMetadata()
                                                       .getModelId(),
                                             twinSource.getId(),
                                             twinTarget.getMetadata()
                                                       .getModelId(),
                                             twinTarget.getId(),
                                             e);
//                        throw new RuntimeException(String.format("Failed to create relationship '%s' between %s(%s) and %s(%s)",
//                                                                 relationshipName,
//                                                                 twinSource.getMetadata()
//                                                                           .getModelId(),
//                                                                 twinSource.getId(),
//                                                                 twinTarget.getMetadata()
//                                                                           .getModelId(),
//                                                                 twinTarget.getId()), e);
                    }

                    relationshipsToKeep.add(rel.getId());

                    outboundRelationships.put(twinSource.getId(), rel);
                    inboundRelationships.put(twinTarget.getId(), rel);
                }

                results.relationshipsProcessed++;

                if (m_state.shouldReport())
                {
                    flushStateToDatabase(m_sessionHolder);
                }
            }
        }
    }

    //--//

    public List<RecordLocator<NetworkAssetRecord>> targets;

    public AzureDigitalTwinSyncProgress results;

    // Configure which side of the relationships to create.
    public boolean locations_useActive  = true;
    public boolean locations_usePassive = false;
    public boolean equipment_useActive  = false;
    public boolean equipment_usePassive = true;
    public boolean points_useActive     = false;
    public boolean points_usePassive    = true;

    //--//

    @Override
    public AzureDigitalTwinSyncProgress fetchProgress(SessionHolder sessionHolder,
                                                      boolean detailed)
    {
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
        throw new NotImplementedException("Not supported");
    }

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        List<RecordLocator<NetworkAssetRecord>> locators) throws
                                                                                                          Exception
    {
        return scheduleActivity(sessionHolder, 0, null, TaskForPublishingToAzureDigitalTwin.class, (t) ->
        {
            t.targets = locators;

            t.results                   = new AzureDigitalTwinSyncProgress();
            t.results.networksToProcess = locators.size();
        });
    }

    //--//

    @Override
    public String getTitle()
    {
        return "Push graph to Azure Digital Twin";
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
        InstanceConfiguration       cfg    = getServiceNonNull(InstanceConfiguration.class);
        InstanceConfigurationForCRE cfgCRE = Reflection.as(cfg, InstanceConfigurationForCRE.class);
        if (cfgCRE != null)
        {
            AzureDigitalTwinsHelper.Credentials cred = cfgCRE.getAzureDigitalTwinCredentials();
            if (cred != null)
            {
                ReportFlusher state = new ReportFlusher(1000);

                LocalState localState = new LocalState(sessionHolder, state);
                localState.analyze();

                RemoteState remoteState = new RemoteState(sessionHolder, state, cred);
                remoteState.analyze();

                if (false)
                {
                    loggerInstance.info("##########################################");
                    loggerInstance.info(ObjectMappers.prettyPrintAsJson(localState.points.values()));
                    loggerInstance.info("##########################################");
                    loggerInstance.info(ObjectMappers.prettyPrintAsJson(localState.equipment.values()));
                    loggerInstance.info("##########################################");
                    loggerInstance.info(ObjectMappers.prettyPrintAsJson(localState.locations.values()));
                    loggerInstance.info("##########################################");
                }

                remoteState.synchronize(localState);

                markAsCompleted();
            }
        }

        markAsFailed("No Azure credentials");
    }
}
