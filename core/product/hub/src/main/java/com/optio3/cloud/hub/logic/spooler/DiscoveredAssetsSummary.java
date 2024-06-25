/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.spooler;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.persistence.HostAssetRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord_;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord_;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord_;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.collection.Memoizer;
import com.optio3.logging.ILogger;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.GatewayDescriptor;
import com.optio3.protocol.model.NetworkDescriptor;
import com.optio3.protocol.model.RestDescriptor;
import com.optio3.util.BoxingUtils;

public class DiscoveredAssetsSummary
{
    public static class ForObject
    {
        public final ForAsset parentAsset;
        public final String   identifier;
        private      String   m_sysId;

        private ForObject(ForAsset parentAsset,
                          String identifier)
        {
            this.parentAsset = parentAsset;
            this.identifier  = identifier;
        }

        public String setSysId(DiscoveredAssetsSummary ctx,
                               String sysId)
        {
            m_sysId = ctx.m_cache.intern(sysId);

            ctx.m_objectLookup.putIfAbsent(m_sysId, this);

            return m_sysId;
        }

        public String getSysId()
        {
            return m_sysId;
        }
    }

    public static class ForAsset
    {
        public final  ForRoot                          parentRoot;
        public final  Class<?>                         clz;
        public final  Object                           identifier;
        private final ConcurrentMap<String, ForObject> m_objects = Maps.newConcurrentMap();
        private       String                           m_sysId;

        private ForAsset(ForRoot parentRoot,
                         Class<?> clz,
                         Object identifier)
        {
            this.parentRoot = parentRoot;
            this.clz        = clz;
            this.identifier = identifier;
        }

        public String setSysId(DiscoveredAssetsSummary ctx,
                               String sysId)
        {
            m_sysId = ctx.m_cache.intern(sysId);

            ctx.m_deviceLookup.putIfAbsent(m_sysId, this);

            return m_sysId;
        }

        public String getSysId()
        {
            return m_sysId;
        }

        public ForObject registerObject(DiscoveredAssetsSummary ctx,
                                        String id,
                                        boolean createIfMissing)
        {
            ForObject result = m_objects.get(id);
            if (result == null && createIfMissing)
            {
                result = new ForObject(this, ctx.m_cache.intern(id));

                ForObject oldResult = m_objects.putIfAbsent(result.identifier, result);
                if (oldResult != null)
                {
                    result = oldResult;
                }
            }

            return result;
        }
    }

    public static class ForRoot
    {
        public final  String                          sysId;
        public final  RootKind                        rootKind;
        private final ConcurrentMap<Object, ForAsset> m_assets = Maps.newConcurrentMap();

        // Special asset used to track performance counter, since they miss the Device record.
        ForAsset forPerf;

        private ForRoot(String sysId,
                        RootKind rootKind)
        {
            this.sysId    = sysId;
            this.rootKind = rootKind;
        }

        public ForAsset registerAsset(DiscoveredAssetsSummary ctx,
                                      Class<?> clz,
                                      BaseAssetDescriptor id,
                                      boolean createIfMissing)
        {
            ForAsset result = m_assets.get(id);
            if (result == null && createIfMissing)
            {
                result = new ForAsset(this, clz, ctx.m_cache.intern(id, Object.class));

                ForAsset oldResult = m_assets.putIfAbsent(result.identifier, result);
                if (oldResult != null)
                {
                    result = oldResult;
                }
            }

            return result;
        }
    }

    public enum RootKind
    {
        Gateway,
        Network,
        Device,
        Host
    }

    //--//

    private final Memoizer                         m_cache;
    private final ConcurrentMap<String, ForRoot>   m_roots        = Maps.newConcurrentMap();
    private final ConcurrentMap<String, ForAsset>  m_deviceLookup = Maps.newConcurrentMap();
    private final ConcurrentMap<String, ForObject> m_objectLookup = Maps.newConcurrentMap();

    public DiscoveredAssetsSummary(Memoizer cache)
    {
        m_cache = cache;
    }

    public void analyze(SessionHolder holder,
                        ILogger logger)
    {
        class RawAssetModel
        {
            public String              sysId;
            public String              parentAsset;
            public String              identifier;
            public BaseAssetDescriptor identityDescriptor;
        }

        // Reuse the same instance, since we don't store the individual models.
        final var singletonModel = new RawAssetModel();

        final Set<String> logicalAssets = Sets.newHashSet();
        boolean           debugLevel    = logger.isEnabled(Severity.Debug) || logger.isEnabled(Severity.DebugVerbose);

        if (debugLevel)
        {
            RawQueryHelper<LogicalAssetRecord, RawAssetModel> qh = new RawQueryHelper<>(holder, LogicalAssetRecord.class);
            qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
            qh.stream(() -> singletonModel, (model) -> logicalAssets.add(model.sysId));
        }

        {
            RawQueryHelper<NetworkAssetRecord, RawAssetModel> qh = new RawQueryHelper<>(holder, NetworkAssetRecord.class);
            qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
            qh.stream(() -> singletonModel, (model) -> registerRoot(model.sysId, RootKind.Network, true));
        }

        {
            RawQueryHelper<GatewayAssetRecord, RawAssetModel> qh = new RawQueryHelper<>(holder, GatewayAssetRecord.class);
            qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
            qh.stream(() -> singletonModel, (model) -> registerRoot(model.sysId, RootKind.Gateway, true));
        }

        {
            RawQueryHelper<HostAssetRecord, RawAssetModel> qh = new RawQueryHelper<>(holder, HostAssetRecord.class);
            qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
            qh.stream(() -> singletonModel, (model) -> registerRoot(model.sysId, RootKind.Host, true));
        }

        {
            RawQueryHelper<DeviceRecord, RawAssetModel> qh = new RawQueryHelper<>(holder, DeviceRecord.class);
            qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
            qh.addReferenceRaw(AssetRecord_.parentAsset, (obj, val) -> obj.parentAsset = val);
            qh.addStringDeserializer(DeviceRecord_.identityDescriptor, BaseAssetDescriptor.class, (obj, val) -> obj.identityDescriptor = val);

            qh.stream(() -> singletonModel, (model) ->
            {
                String parentSysId = model.parentAsset;
                if (parentSysId != null)
                {
                    DiscoveredAssetsSummary.ForRoot forRoot = lookupRoot(parentSysId);
                    if (forRoot != null)
                    {
                        DiscoveredAssetsSummary.ForAsset forAsset = forRoot.registerAsset(this, DeviceRecord.class, model.identityDescriptor, true);
                        forAsset.setSysId(this, model.sysId);
                        logger.debugVerbose("Registered device '%s / %s': '%s'", model.sysId, model.identityDescriptor, parentSysId);
                    }
                    else if (debugLevel)
                    {
                        logger.debug("Failed to find parent for device '%s / %s': '%s'", model.sysId, model.identityDescriptor, parentSysId);
                    }
                }
            });
        }

        {
            RawQueryHelper<DeviceElementRecord, RawAssetModel> qh = new RawQueryHelper<>(holder, DeviceElementRecord.class);
            qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
            qh.addReferenceRaw(AssetRecord_.parentAsset, (obj, val) -> obj.parentAsset = val);
            qh.addString(DeviceElementRecord_.identifier, (obj, val) -> obj.identifier = val);

            qh.stream(() -> singletonModel, (model) ->
            {
                String parentSysId = model.parentAsset;
                if (parentSysId != null)
                {
                    DiscoveredAssetsSummary.ForAsset forAsset = lookupAsset(parentSysId);
                    if (forAsset != null)
                    {
                        DiscoveredAssetsSummary.ForObject forObject = forAsset.registerObject(this, model.identifier, true);
                        forObject.setSysId(this, model.sysId);
                        logger.debugVerbose("Registered object '%s / %s': '%s'", model.sysId, model.identifier, parentSysId);
                    }
                    else if (debugLevel)
                    {
                        if (logicalAssets.contains(parentSysId))
                        {
                            // Parent is a logical asset, not a gateway or network, ignore.
                            logger.debugVerbose("Skipped synthetic object '%s / %s': '%s'", model.sysId, model.identifier, parentSysId);
                        }
                        else
                        {
                            logger.debug("Failed to find parent for object '%s / %s': '%s'", model.sysId, model.identifier, parentSysId);
                        }
                    }
                }
            });
        }

        logger.debug("Found %,d roots, %,d devices, %,d objects", m_roots.size(), m_deviceLookup.size(), m_objectLookup.size());
    }

    //--//

    public boolean hasAsset(String sysId)
    {
        return m_deviceLookup.containsKey(sysId) || m_objectLookup.containsKey(sysId);
    }

    public ForObject lookupObject(String sysId)
    {
        return sysId != null ? m_objectLookup.get(sysId) : null;
    }

    public ForAsset lookupAsset(String sysId)
    {
        return sysId != null ? m_deviceLookup.get(sysId) : null;
    }

    public ForRoot lookupRoot(String sysId)
    {
        return sysId != null ? m_roots.get(sysId) : null;
    }

    public ForRoot registerRoot(String sysId,
                                RootKind rootKind,
                                boolean createIfMissing)
    {
        rootKind = BoxingUtils.get(rootKind,RootKind.Device);

        ForRoot result = m_roots.get(sysId);
        if (result == null && createIfMissing)
        {
            result = new ForRoot(m_cache.intern(sysId), rootKind);

            ForRoot oldResult = m_roots.putIfAbsent(result.sysId, result);
            if (oldResult != null)
            {
                result = oldResult;
            }

            switch (rootKind)
            {
                case Network:
                {
                    //
                    // For Performance counters, we register the Network asset as well.
                    //
                    NetworkDescriptor desc = new NetworkDescriptor();
                    desc.sysId = sysId;
                    ForAsset result2 = result.registerAsset(this, NetworkAssetRecord.class, desc, true);
                    result2.setSysId(this, sysId);

                    result.forPerf = result2;
                }
                break;

                case Gateway:
                {
                    //
                    // For Performance counters, we register the Gateway asset as well.
                    //
                    GatewayDescriptor desc = new GatewayDescriptor();
                    desc.sysId = sysId;
                    ForAsset result2 = result.registerAsset(this, GatewayAssetRecord.class, desc, true);
                    result2.setSysId(this, sysId);

                    result.forPerf = result2;
                }
                break;

                case Host:
                {
                    //
                    // For Performance counters, we register the Gateway asset as well.
                    //
                    RestDescriptor desc = new RestDescriptor();
                    desc.sysId = sysId;
                    ForAsset result2 = result.registerAsset(this, HostAssetRecord.class, desc, true);
                    result2.setSysId(this, sysId);

                    result.forPerf = result2;
                }
                break;
            }
        }

        return result;
    }
}
