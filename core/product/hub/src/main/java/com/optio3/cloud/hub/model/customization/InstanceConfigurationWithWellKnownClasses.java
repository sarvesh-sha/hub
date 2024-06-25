/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.logic.normalizations.EquipmentClass;
import com.optio3.cloud.hub.logic.normalizations.NormalizationEngine;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.logic.normalizations.PointClass;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.model.customization.digineous.InstanceConfigurationForDigineous;
import com.optio3.cloud.hub.model.customization.digitalmatter.InstanceConfigurationForDigitalMatter;
import com.optio3.cloud.hub.model.workflow.IWorkflowHandler;
import com.optio3.cloud.hub.model.workflow.IWorkflowHandlerForTransportation;
import com.optio3.cloud.hub.model.workflow.WorkflowDetails;
import com.optio3.cloud.hub.orchestration.tasks.TaskForAutoNetworkClassification;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.asset.RelationshipRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.hub.persistence.normalization.NormalizationRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowRecord;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ITableLockProvider;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.search.HibernateSearch;
import com.optio3.concurrency.AsyncGate;
import com.optio3.concurrency.Executors;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownEquipmentClassOrCustom;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.WellKnownPointClassOrCustom;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@JsonSubTypes({ @JsonSubTypes.Type(value = InstanceConfigurationForDigineous.class),
                @JsonSubTypes.Type(value = InstanceConfigurationForDigitalMatter.class),
                @JsonSubTypes.Type(value = InstanceConfigurationForTransportation.class) })
public abstract class InstanceConfigurationWithWellKnownClasses extends InstanceConfiguration
{
    public static class FieldAssociation
    {
        public DeviceRecord                rec_device;
        public LogicalAssetRecord          rec_groupForPointClass;
        public DeviceElementRecord         rec_element;
        public IpnObjectModel              contents;
        public FieldModel                  fieldDesc;
        public WellKnownPointClassOrCustom selectedPointClass;
        public boolean                     includeInClassification;

        public BaseObjectModel.ClassificationDetails infoForGroup;
        public BaseObjectModel.ClassificationDetails infoForDevice;
        public BaseObjectModel.ClassificationDetails infoForPoint;
    }

    public class ClassificationContext
    {
        public class PerNetwork
        {
            public final SessionHolder                                                 sessionHolder;
            public final RecordHelper<AssetRecord>                                     helper_asset;
            public final NetworkAssetRecord                                            rec_network;
            public final Set<String>                                                   equipInUse          = Sets.newHashSet();
            public final Multimap<WellKnownEquipmentClassOrCustom, LogicalAssetRecord> lookupEquipRecord   = LinkedListMultimap.create();
            public final Multimap<LogicalAssetRecord, FieldAssociation>                lookupEquipElements = LinkedListMultimap.create();

            private NormalizationRules m_normalizationRules;

            public PerNetwork(SessionHolder sessionHolder,
                              NetworkAssetRecord rec_network)
            {
                this.sessionHolder = sessionHolder;
                this.rec_network   = rec_network;

                helper_asset = sessionHolder.createHelper(AssetRecord.class);

                for (LogicalAssetRecord rec_group : getChildren(rec_network, AssetRelationship.structural, LogicalAssetRecord.class))
                {
                    var ec = WellKnownEquipmentClassOrCustom.parse(rec_group.getEquipmentClassId());
                    if (ec != null)
                    {
                        lookupEquipRecord.put(ec, rec_group);
                    }
                }
            }

            public void ensureClassified() throws
                                           Exception
            {
                Multimap<String, FieldAssociation> map = analyzeObjects();

                for (String deviceId : map.keySet())
                {
                    createEquipmentsFromDevice(map.get(deviceId));
                }

                for (String deviceId : map.keySet())
                {
                    assignPointClassToElements(map.get(deviceId));
                }

                RecordHelper<AssetRecord> helper = sessionHolder.createHelper(AssetRecord.class);
                try (ValidationResultsHolder validation = new ValidationResultsHolder(sessionHolder, false, true))
                {
                    for (LogicalAssetRecord rec : lookupEquipRecord.values())
                    {
                        if (!equipInUse.contains(rec.getSysId()))
                        {
                            rec.remove(validation, helper);
                        }
                    }
                }
            }

            private void addRelation(AssetRecord rec_parent,
                                     AssetRecord rec_child,
                                     AssetRelationship relationship)
            {
                TagsEngine.Snapshot.AssetSet children = tagsSnapshot.resolveRelations(rec_parent.getSysId(), relationship, false);
                if (!children.contains(rec_child))
                {
                    RelationshipRecord.addRelation(sessionHolder, rec_parent, rec_child, relationship);
                }
            }

            private void removeRelation(AssetRecord rec_parent,
                                        AssetRecord rec_child,
                                        AssetRelationship relationship)
            {
                TagsEngine.Snapshot.AssetSet children = tagsSnapshot.resolveRelations(rec_parent.getSysId(), relationship, false);
                if (children.contains(rec_child))
                {
                    RelationshipRecord.removeRelation(sessionHolder, rec_parent, rec_child, relationship);
                }
            }

            private <T extends AssetRecord> List<T> getChildren(AssetRecord rec_parent,
                                                                AssetRelationship relationship,
                                                                Class<T> targetClz)
            {
                TagsEngine.Snapshot.AssetSet children = tagsSnapshot.resolveRelations(rec_parent.getSysId(), relationship, false);

                List<String> ids     = CollectionUtils.transformToListNoNulls(children.resolve(), (ri) -> Reflection.isSubclassOf(targetClz, ri.getEntityClass()) ? ri.sysId : null);
                List<T>      results = QueryHelperWithCommonFields.getBatch(sessionHolder.createHelper(targetClz), ids);
                return CollectionUtils.transformToListNoNulls(results, (r) -> r);
            }

            //--//

            private LogicalAssetRecord ensureEquipment(BaseObjectModel.ClassificationDetails details) throws
                                                                                                      Exception
            {
                String physicalName = details.getPhysicalName();

                for (LogicalAssetRecord rec : lookupEquipRecord.get(details.equipmentClass))
                {
                    if (StringUtils.equals(rec.getPhysicalName(), physicalName))
                    {
                        rec.setPhysicalName(physicalName);
                        rec.setNormalizedName(details.getNormalizedName());
                        setEquipmentClass(rec, details.equipmentClass, details.extraTags);

                        equipInUse.add(rec.getSysId());
                        return rec;
                    }
                }

                LogicalAssetRecord rec = new LogicalAssetRecord();

                rec.setPhysicalName(physicalName);
                rec.setNormalizedName(details.getNormalizedName());
                setEquipmentClass(rec, details.equipmentClass, details.extraTags);

                sessionHolder.persistEntity(rec);

                rec.linkToParent(helper_asset, rec_network);

                lookupEquipRecord.put(details.equipmentClass, rec);

                equipInUse.add(rec.getSysId());
                return rec;
            }

            private Multimap<String, FieldAssociation> analyzeObjects()
            {
                Multimap<String, FieldAssociation> map = HashMultimap.create();

                for (DeviceRecord rec_dev : getChildren(rec_network, AssetRelationship.structural, DeviceRecord.class))
                {
                    BaseAssetDescriptor desc = rec_dev.getIdentityDescriptor();

                    for (DeviceElementRecord rec_element : getChildren(rec_dev, AssetRelationship.structural, DeviceElementRecord.class))
                    {
                        try
                        {
                            IpnObjectModel contents = rec_element.getTypedContents(IpnObjectModel.getObjectMapper(), IpnObjectModel.class);
                            if (contents == null || !contents.parseId(desc.toString()))
                            {
                                continue;
                            }

                            FieldAssociation fa = new FieldAssociation();
                            fa.includeInClassification = shouldIncludeObjectInClassification(contents);
                            fa.rec_device              = rec_dev;
                            fa.rec_element             = rec_element;
                            fa.contents                = contents;
                            fa.fieldDesc               = contents.getDescriptor(rec_element.getIdentifier(), true);

                            map.put(rec_dev.getSysId(), fa);
                        }
                        catch (Exception e)
                        {
                            LoggerInstance.error("Failed to analyze %s:%s, due to %s", rec_dev.getIdentityDescriptor(), rec_element.getIdentifier(), e);
                        }
                    }
                }

                return map;
            }

            private void createEquipmentsFromDevice(Collection<FieldAssociation> elements) throws
                                                                                           Exception
            {
                for (FieldAssociation fa : elements)
                {
                    if (fa.contents != null && fa.includeInClassification)
                    {
                        fa.infoForGroup                               = new BaseObjectModel.ClassificationDetails();
                        fa.infoForGroup.equipmentClass                = WellKnownEquipmentClass.Deployment.asWrapped();
                        fa.infoForGroup.noEquipmentClassInDisplayName = true;

                        LocationRecord rec_location = rec_network.getLocation();
                        fa.infoForGroup.instanceSelector = rec_location != null ? rec_location.getPhysicalName() : rec_network.getPhysicalName();

                        fa.infoForDevice                               = new BaseObjectModel.ClassificationDetails();
                        fa.infoForDevice.noEquipmentClassInDisplayName = false;

                        fa.infoForPoint = new BaseObjectModel.ClassificationDetails();

                        fa.contents.fillClassificationDetails(fa.infoForGroup, fa.infoForDevice, fa.infoForPoint);

                        if (WellKnownEquipmentClassOrCustom.isValid(fa.infoForDevice.equipmentClass))
                        {
                            fa.rec_groupForPointClass = ensureEquipment(fa.infoForDevice);
                            lookupEquipElements.put(fa.rec_groupForPointClass, fa);

                            LogicalAssetRecord rec_parent = ensureEquipment(fa.infoForGroup);

                            //
                            // Link device group to deployment.
                            //
                            addRelation(rec_parent, fa.rec_groupForPointClass, AssetRelationship.controls);
                        }
                    }
                }
            }

            private void assignPointClassToElements(Collection<FieldAssociation> elements) throws
                                                                                           Exception
            {
                class SingletonLookup
                {
                    WellKnownPointClassOrCustom pointClass;
                    Set<String>                 pointTags;

                    @Override
                    public boolean equals(Object o)
                    {
                        SingletonLookup that = Reflection.as(o, SingletonLookup.class);
                        if (that == null)
                        {
                            return false;
                        }

                        return pointClass.equals(that.pointClass) && pointTags.equals(that.pointTags);
                    }

                    @Override
                    public int hashCode()
                    {
                        return Objects.hash(pointClass, pointTags);
                    }
                }

                Map<SingletonLookup, FieldAssociation> lookup = Maps.newHashMap();

                for (FieldAssociation fa : elements)
                {
                    if (fa.includeInClassification)
                    {
                        IpnObjectModel              obj        = fa.contents;
                        WellKnownPointClassOrCustom pointClass = fa.fieldDesc.getPointClass(obj);
                        if (WellKnownPointClassOrCustom.isValid(pointClass))
                        {
                            for (String tag : fa.fieldDesc.getPointTags(obj))
                            {
                                fa.infoForPoint.addExtraTag(tag, false);
                            }

                            Set<String> pointTags = fa.infoForPoint.extraTags.keySet();

                            if (shouldBeSingletonInClassification(pointClass, pointTags))
                            {
                                SingletonLookup sl = new SingletonLookup();
                                sl.pointClass = pointClass;
                                sl.pointTags  = pointTags;

                                FieldAssociation previous = lookup.get(sl);
                                if (previous != null)
                                {
                                    if (previous.fieldDesc.getPointClassPriority(obj) >= fa.fieldDesc.getPointClassPriority(obj))
                                    {
                                        continue;
                                    }

                                    previous.selectedPointClass = null;
                                }

                                lookup.put(sl, fa);
                            }

                            fa.selectedPointClass = pointClass;
                        }
                    }
                }

                for (FieldAssociation fa : elements)
                {
                    if (fa.selectedPointClass != null)
                    {
                        setPointClass(fa.rec_element, fa.selectedPointClass, fa.infoForPoint.extraTags);

                        if (fa.rec_groupForPointClass != null)
                        {
                            addRelation(fa.rec_groupForPointClass, fa.rec_element, AssetRelationship.controls);
                        }
                    }
                    else
                    {
                        resetPointClass(fa.rec_element);

                        if (fa.rec_groupForPointClass != null)
                        {
                            removeRelation(fa.rec_groupForPointClass, fa.rec_element, AssetRelationship.controls);
                        }
                    }
                }
            }

            private void setEquipmentClass(AssetRecord rec,
                                           WellKnownEquipmentClassOrCustom ec,
                                           Map<String, Boolean> extraTags) throws
                                                                           Exception
            {
                EquipmentClass ecObj = resolve(ec);

                final String equipClassId = Integer.toString(ecObj.id);

                rec.putMetadata(AssetRecord.WellKnownMetadata.equipmentClassID, equipClassId);

                rec.modifyTags((tags) ->
                               {
                                   //
                                   // Remove classification tags.
                                   //
                                   AssetRecord.WellKnownTags.clearTags(tags, false, true, false);

                                   tags.addTag(AssetRecord.WellKnownTags.isEquipment, false);

                                   if (CollectionUtils.isNotEmpty(ecObj.tags))
                                   {
                                       tags.addTags(ecObj.tags, true);
                                   }
                                   else if (ecObj.description != null)
                                   {
                                       for (String tag : NormalizationEngine.splitAndLowercase(ecObj.description))
                                       {
                                           tags.addTag(tag, false);
                                       }
                                   }

                                   tags.setValuesForTag(AssetRecord.WellKnownTags.equipmentClassId, Sets.newHashSet(equipClassId));

                                   tags.addTags(extraTags.keySet(), true);
                               });
            }

            private void setPointClass(AssetRecord rec,
                                       WellKnownPointClassOrCustom pc,
                                       Map<String, Boolean> extraTags) throws
                                                                       Exception
            {
                PointClass pcObj = resolve(pc);

                final String pointClassId = Integer.toString(pc.asInteger());

                rec.putMetadata(AssetRecord.WellKnownMetadata.pointClassID, pointClassId);

                rec.modifyTags((tags) ->
                               {
                                   //
                                   // Remove classification tags.
                                   //
                                   AssetRecord.WellKnownTags.clearTags(tags, false, true, false);

                                   if (CollectionUtils.isNotEmpty(pcObj.tags))
                                   {
                                       tags.addTags(pcObj.tags, true);
                                   }
                                   else if (pcObj.pointClassDescription != null)
                                   {
                                       for (String tag : NormalizationEngine.splitAndLowercase(pcObj.pointClassDescription))
                                       {
                                           tags.addTag(tag, false);
                                       }
                                   }

                                   tags.setValuesForTag(AssetRecord.WellKnownTags.pointClassId, Sets.newHashSet(pointClassId));

                                   tags.addTags(extraTags.keySet(), true);
                               });
            }

            private void resetPointClass(AssetRecord rec)
            {
                // Set the point class to Ignored, to distinguish from null.
                final String pointClassId = Integer.toString(WellKnownPointClass.Ignored.getId());

                rec.putMetadata(AssetRecord.WellKnownMetadata.pointClassID, pointClassId);

                rec.modifyTags((tags) ->
                               {
                                   tags.clear();

                                   tags.setValuesForTag(AssetRecord.WellKnownTags.pointClassId, Sets.newHashSet(pointClassId));
                               });
            }

            //--//

            private EquipmentClass resolve(WellKnownEquipmentClassOrCustom ec) throws
                                                                               Exception
            {
                var rules = ensureNormalizationRules();

                return rules.addEquipmentClassIfAbsent(ec.asInteger(), (ecNew) ->
                {
                    ecNew.equipClassName = "Unknown Equipment Class " + ec.custom;
                });
            }

            private PointClass resolve(WellKnownPointClassOrCustom pc) throws
                                                                       Exception
            {
                var rules = ensureNormalizationRules();

                return rules.addPointClassIfAbsent(pc.asInteger(), (pcNew) ->
                {
                    pcNew.pointClassName = "Unknown Point Class " + pc.custom;
                });
            }

            private NormalizationRules ensureNormalizationRules() throws
                                                                  Exception
            {
                if (m_normalizationRules == null)
                {
                    NormalizationRecord rec_normalization = NormalizationRecord.findActive(sessionHolder.createHelper(NormalizationRecord.class));
                    if (rec_normalization != null)
                    {
                        m_normalizationRules = rec_normalization.getRules();
                    }
                    else
                    {
                        m_normalizationRules = new NormalizationRules();
                        m_normalizationRules.populateWithWellKnownClasses();
                    }
                }

                return m_normalizationRules;
            }
        }

        public final TagsEngine.Snapshot tagsSnapshot;

        public ClassificationContext(TagsEngine.Snapshot tagsSnapshot)
        {
            this.tagsSnapshot = tagsSnapshot;
        }

        public final void ensureClassified(SessionHolder sessionHolder,
                                           NetworkAssetRecord rec_network) throws
                                                                           Exception
        {
            LoggerInstance.info("Reclassifying network '%s' (%s)", rec_network.getSysId(), rec_network.getName());

            PerNetwork contextPerNetwork = new PerNetwork(sessionHolder, rec_network);
            contextPerNetwork.ensureClassified();

            LoggerInstance.info("Reclassified network '%s' (%s)", rec_network.getSysId(), rec_network.getName());
        }
    }

    //--//

    @Override
    public NormalizationRules updateNormalizationRules(SessionHolder sessionHolder,
                                                       NormalizationRules rules) throws
                                                                                 Exception
    {
        if (rules == null)
        {
            rules = new NormalizationRules();
        }

        rules.populateWithWellKnownClasses();

        return rules;
    }

    protected void reclassifyAllNetworks()
    {
        Executors.scheduleOnDefaultPool(() ->
                                        {
                                            LoggerInstance.info("Reclassifying objects...");

                                            try (AsyncGate.Holder ignored = m_app.closeGate(HibernateSearch.Gate.class))
                                            {
                                                List<String> networks;

                                                SessionProvider sessionProvider = new SessionProvider(m_app, null, Optio3DbRateLimiter.Normal);

                                                try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
                                                {
                                                    RecordHelper<NetworkAssetRecord> helper = sessionHolder.createHelper(NetworkAssetRecord.class);

                                                    networks = QueryHelperWithCommonFields.listRaw(helper, null);
                                                }

                                                CollectionUtils.transformInParallel(networks, HubApplication.GlobalRateLimiter, (network) ->
                                                {
                                                    var loc_network = new RecordLocator<>(NetworkAssetRecord.class, network);

                                                    executeClassification(sessionProvider, loc_network);

                                                    return null;
                                                });
                                            }

                                            LoggerInstance.info("Reclassified objects");
                                        }, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public BackgroundActivityRecord scheduleClassification(SessionHolder sessionHolder,
                                                           NetworkAssetRecord rec_network) throws
                                                                                           Exception
    {
        return TaskForAutoNetworkClassification.scheduleTaskIfNotRunning(sessionHolder, rec_network);
    }

    @Override
    public void executeClassification(SessionProvider sessionProvider,
                                      RecordLocator<NetworkAssetRecord> loc_network) throws
                                                                                     Exception
    {
        ITableLockProvider provider = sessionProvider.getServiceNonNull(ITableLockProvider.class);

        try (var lock = provider.lockRecord(sessionProvider, NetworkAssetRecord.class, loc_network.getIdRaw(), 30, TimeUnit.MINUTES))
        {
            try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
            {
                NetworkAssetRecord rec_network = sessionHolder.fromLocatorOrNull(loc_network);
                if (rec_network != null)
                {
                    TagsEngine            tagsEngine = sessionHolder.getServiceNonNull(TagsEngine.class);
                    ClassificationContext context    = new ClassificationContext(tagsEngine.acquireSnapshot(true));

                    context.ensureClassified(sessionHolder, rec_network);

                    sessionHolder.commit();
                }
            }
        }
    }

    @Override
    public void handleWorkflowCreated(SessionHolder sessionHolder,
                                      WorkflowRecord rec,
                                      UserRecord rec_user)
    {
        WorkflowDetails details = rec.getDetails();

        boolean                           closed                   = false;
        IWorkflowHandlerForTransportation handlerForTransportation = Reflection.as(details, IWorkflowHandlerForTransportation.class);
        if (handlerForTransportation != null)
        {
            closed = handlerForTransportation.postWorkflowCreationForTransportation(sessionHolder);
        }
        else
        {
            IWorkflowHandler handler = Reflection.as(details, IWorkflowHandler.class);
            if (handler != null)
            {
                closed = handler.postWorkflowCreation(sessionHolder);
            }
        }

        if (closed)
        {
            rec.markAsProcessed(sessionHolder, rec_user);
        }
    }

    @Override
    public void handleWorkflowUpdated(SessionHolder sessionHolder,
                                      WorkflowRecord rec,
                                      UserRecord rec_user)
    {
    }

    protected abstract boolean shouldIncludeObjectInClassification(IpnObjectModel contents);

    protected abstract boolean shouldBeSingletonInClassification(WellKnownPointClassOrCustom pointClass,
                                                                 Set<String> pointTags);
}
