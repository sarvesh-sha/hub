/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

import static com.optio3.util.Exceptions.getAndUnwrapException;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Tuple;
import javax.persistence.criteria.Predicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.model.GatewayStatus;
import com.optio3.cloud.client.gateway.model.prober.ProberNetworkStatus;
import com.optio3.cloud.client.gateway.proxy.GatewayProberControlApi;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.logic.protocol.GatewayPerfDecoder;
import com.optio3.cloud.hub.logic.protocol.IProtocolDecoder;
import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.model.asset.GatewayAsset;
import com.optio3.cloud.hub.model.asset.GatewayDetails;
import com.optio3.cloud.hub.model.asset.GatewayFilterRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.orchestration.tasks.TaskForNetworkRefresh;
import com.optio3.cloud.hub.persistence.prober.GatewayProberOperationRecord;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.cloud.messagebus.channel.RpcConnectionInfo;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogHandler;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.GatewayDescriptor;
import com.optio3.protocol.model.GatewayPerformanceCounters;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.service.IServiceProvider;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "ASSET_GATEWAY")
@DynamicUpdate // Due to HHH-11506
@Indexed()
@Optio3TableInfo(externalId = "GatewayAsset", model = GatewayAsset.class, metamodel = GatewayAssetRecord_.class, metadata = GatewayAssetRecord.WellKnownMetadata.class)
public class GatewayAssetRecord extends AssetRecord implements AssetRecord.IProviderOfPropertyTypeExtractorClass,
                                                               LogHandler.ILogHost<GatewayAssetLogRecord>
{
    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        public static final MetadataField<Boolean>       reportAsNew         = new MetadataField<>("reportAsNew", Boolean.class);
        public static final MetadataField<String>        reportAutodiscovery = new MetadataField<>("reportAutodiscovery", String.class);
        public static final MetadataField<ZonedDateTime> gatewayWarning      = new MetadataField<>("gatewayWarning", ZonedDateTime.class);

        public static final MetadataField<ZonedDateTime> gatewayCpuLoadWarning       = new MetadataField<>("gatewayCpuLoadWarning", ZonedDateTime.class);
        public static final MetadataField<Double>        gatewayCpuLoadWarningLevel  = new MetadataField<>("gatewayCpuLoadWarningLevel", Double.class);
        public static final MetadataField<Boolean>       gatewayNeedsNetworkRefresh  = new MetadataField<>("gatewayNeedsNetworkRefresh", Boolean.class);
        public static final MetadataField<Boolean>       gatewayNeedsSamplingRefresh = new MetadataField<>("gatewayNeedsSamplingRefresh", Boolean.class);
    }

    public static class TypeExtractor extends PropertyTypeExtractor
    {
        private static final Map<String, TimeSeriesPropertyType> s_fields;

        static
        {
            Map<String, TimeSeriesPropertyType> fields = Maps.newHashMap();
            GatewayPerformanceCounters          obj    = new GatewayPerformanceCounters();

            for (FieldModel fieldModel : obj.getDescriptors())
            {
                String fieldName = fieldModel.name;

                TypeDescriptor td = Reflection.getDescriptor(fieldModel.type);
                if (td == null)
                {
                    continue;
                }

                final TimeSeriesPropertyType pt = new TimeSeriesPropertyType();
                pt.name        = fieldName;
                pt.displayName = fieldModel.getDescription(obj);
                pt.type        = td.isFloatingType() ? TimeSeries.SampleType.Decimal : TimeSeries.SampleType.Integer;
                pt.setUnits(fieldModel.getUnits(obj));

                pt.expectedType = fieldModel.type;
                pt.targetField  = fieldName;

                fields.put(fieldName, pt);
            }

            s_fields = Collections.unmodifiableMap(fields);
        }

        public static Map<String, TimeSeriesPropertyType> getFields()
        {
            return s_fields;
        }

        //--//

        @Override
        public Map<String, TimeSeriesPropertyType> classifyRecord(DeviceElementRecord rec,
                                                                  boolean handlePresentationType)
        {
            return s_fields;
        }

        @Override
        protected void classifyInstance(Map<String, TimeSeriesPropertyType> map,
                                        BaseObjectModel obj,
                                        boolean handlePresentationType)
        {
            if (obj instanceof GatewayPerformanceCounters)
            {
                map.putAll(s_fields);
            }
        }

        @Override
        public IProtocolDecoder getProtocolDecoder()
        {
            return new GatewayPerfDecoder();
        }

        @Override
        public EngineeringUnitsFactors getUnitsFactors(DeviceElementRecord rec)
        {
            return null;
        }

        @Override
        public String getIndexedValue(DeviceElementRecord rec)
        {
            return null;
        }

        @Override
        public GatewayPerformanceCounters getContentsAsObject(DeviceElementRecord rec,
                                                              boolean desiredState) throws
                                                                                    IOException
        {
            ObjectMapper om = GatewayPerformanceCounters.getObjectMapper();

            if (desiredState)
            {
                return rec.getTypedDesiredContents(om, GatewayPerformanceCounters.class);
            }
            else
            {
                return rec.getTypedContents(om, GatewayPerformanceCounters.class);
            }
        }
    }

    //--//

    @Column(name = "instance_id", nullable = false)
    private String instanceId;

    @Column(name = "rpc_id")
    private String rpcId;

    //--//

    @Column(name = "warning_threshold", nullable = false)
    private int warningThreshold = 30;

    @Column(name = "alert_threshold", nullable = false)
    private int alertThreshold = 45;

    @Column(name = "cpu_load", nullable = false)
    private int cpuLoad;

    @Column(name = "cpu_load_previous", nullable = false)
    private int cpuLoadPrevious;

    //--//

    @Lob
    @Column(name = "details")
    private String details;

    @Transient
    private final PersistAsJsonHelper<String, GatewayDetails> m_detailsParser = new PersistAsJsonHelper<>(() -> details,
                                                                                                          (val) -> details = val,
                                                                                                          String.class,
                                                                                                          GatewayDetails.class,
                                                                                                          ObjectMappers.SkipNulls);

    @Optio3ControlNotifications(reason = "Report changes", direct = Notify.ON_ASSOCIATION_CHANGES, reverse = Notify.NEVER, getter = "getBoundNetworks")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getBoundNetworks")
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "ASSET_GATEWAY_ASSET_NETWORK", joinColumns = @JoinColumn(name = "GatewayAssetRecord_sys_id"), inverseJoinColumns = @JoinColumn(name = "boundNetworks_sys_id"))
    private List<NetworkAssetRecord> boundNetworks = Lists.newArrayList();

    //--//

    @Column(name = "last_output")
    private ZonedDateTime lastOutput;

    @Column(name = "last_offset", nullable = false)
    private int lastOffset;

    @Lob
    @Column(name = "log_ranges")
    private byte[] logRanges;

    //--//

    /**
     * List of all the operations running on the prober.
     */
    @OneToMany(mappedBy = "gateway", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("sys_created_on DESC")
    private List<GatewayProberOperationRecord> operations;

    //--//

    public GatewayAssetRecord()
    {
    }

    //--//

    public ZonedDateTime getLastOutput()
    {
        return lastOutput;
    }

    public int getLastOffset()
    {
        return lastOffset;
    }

    @Override
    public byte[] getLogRanges()
    {
        return logRanges;
    }

    @Override
    public void setLogRanges(byte[] logRanges,
                             ZonedDateTime lastOutput,
                             int lastOffset)
    {
        if (!Arrays.equals(this.logRanges, logRanges))
        {
            this.logRanges  = logRanges;
            this.lastOutput = lastOutput;
            this.lastOffset = lastOffset;
        }
    }

    @Override
    public void refineLogQuery(LogHandler.JoinHelper<?, GatewayAssetLogRecord> jh)
    {
        jh.addWhereClauseWithEqual(jh.rootLog, GatewayAssetLogRecord_.owningGateway, this);
    }

    @Override
    public GatewayAssetLogRecord allocateNewLogInstance()
    {
        return GatewayAssetLogRecord.newInstance(this);
    }

    public static LogHandler<GatewayAssetRecord, GatewayAssetLogRecord> allocateLogHandler(RecordLocked<GatewayAssetRecord> lock)
    {
        return new LogHandler<>(lock, GatewayAssetLogRecord.class);
    }

    public static LogHandler<GatewayAssetRecord, GatewayAssetLogRecord> allocateLogHandler(SessionHolder sessionHolder,
                                                                                           GatewayAssetRecord rec)
    {
        return new LogHandler<>(sessionHolder, rec, GatewayAssetLogRecord.class);
    }

    //--//

    public String getInstanceId()
    {
        return instanceId;
    }

    public void setInstanceId(String instanceId)
    {
        this.instanceId = instanceId;
    }

    public String getRpcId()
    {
        return rpcId;
    }

    public void setRpcId(String rpcId)
    {
        this.rpcId = rpcId;
    }

    public boolean hasHeartbeat()
    {
        return TimeUtils.wasUpdatedRecently(getLastCheckedDate(), 10, TimeUnit.MINUTES);
    }

    public List<GatewayProberOperationRecord> getOperations()
    {
        return CollectionUtils.asEmptyCollectionIfNull(operations);
    }

    //--//

    public int getWarningThreshold()
    {
        return warningThreshold;
    }

    public void setWarningThreshold(int warningThreshold)
    {
        this.warningThreshold = warningThreshold;
    }

    public int getAlertThreshold()
    {
        return alertThreshold;
    }

    public void setAlertThreshold(int alertThreshold)
    {
        this.alertThreshold = alertThreshold;
    }

    //--//

    public int getCpuLoadLast4Hours()
    {
        return this.cpuLoad;
    }

    public int getCpuLoadPrevious4Hours()
    {
        return this.cpuLoadPrevious;
    }

    public void updateCpuLoad(int cpuLoad,
                              int cpuLoadPrevious)
    {
        this.cpuLoad         = cpuLoad;
        this.cpuLoadPrevious = cpuLoadPrevious;
    }

    //--//

    public GatewayDetails getDetails()
    {
        return m_detailsParser.get();
    }

    public void setDetails(GatewayDetails details)
    {
        m_detailsParser.set(details);
    }

    //--//

    public List<NetworkAssetRecord> getBoundNetworks()
    {
        return boundNetworks;
    }

    public NetworkAssetRecord findNetwork(String sysId)
    {
        for (NetworkAssetRecord rec_network : getBoundNetworks())
        {
            if (StringUtils.equals(rec_network.getSysId(), sysId))
            {
                return rec_network;
            }
        }

        return null;
    }

    //--//

    @Override
    public BaseAssetDescriptor getIdentityDescriptor()
    {
        GatewayDescriptor desc = new GatewayDescriptor();
        desc.sysId = getSysId();
        return desc;
    }

    //--//

    private static class GatewayJoinHelper<T> extends AssetJoinHelper<T, GatewayAssetRecord>
    {
        GatewayJoinHelper(RecordHelper<GatewayAssetRecord> helper,
                          Class<T> clz)
        {
            super(helper, clz);
        }

        //--//

        void applyFilters(GatewayFilterRequest filters)
        {
            super.applyFilters(filters);

            if (filters.sortBy != null)
            {
                for (SortCriteria sort : filters.sortBy)
                {
                    switch (sort.column)
                    {
                        case "instanceId":
                        {
                            addOrderBy(root, GatewayAssetRecord_.instanceId, sort.ascending);
                            break;
                        }

                        case "cpuLoad":
                        {
                            addOrderBy(root, GatewayAssetRecord_.cpuLoad, sort.ascending);
                        }

                        case "cpuLoadPrevious":
                        {
                            addOrderBy(root, GatewayAssetRecord_.cpuLoadPrevious, sort.ascending);
                        }
                    }
                }
            }
        }

        @Override
        protected Predicate predicateForLike(List<ParsedLike> likeFilters)
        {
            return or(super.predicateForLike(likeFilters), predicateForLike(root, GatewayAssetRecord_.instanceId, likeFilters));
        }
    }

    //--//

    public static List<GatewayAssetRecord> getBatch(RecordHelper<GatewayAssetRecord> helper,
                                                    List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    private static Map<String, Number> countGatewaysByField(RecordHelper<GatewayAssetRecord> helper,
                                                            GatewayFilterRequest filters,
                                                            Consumer<GatewayJoinHelper<Tuple>> callback)
    {
        GatewayJoinHelper<Tuple> jh = new GatewayJoinHelper<>(helper, Tuple.class);

        callback.accept(jh);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        Map<String, Number> res = Maps.newHashMap();

        for (Tuple t : jh.list())
        {
            res.put((String) t.get(0), (Number) t.get(1));
        }

        return res;
    }

    public static List<RecordIdentity> filterGateways(RecordHelper<GatewayAssetRecord> helper,
                                                      GatewayFilterRequest filters)
    {
        GatewayJoinHelper<Tuple> jh = new GatewayJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return AssetJoinHelper.returnFilterTuples(helper, jh);
    }

    public static long countGateways(RecordHelper<GatewayAssetRecord> helper,
                                     GatewayFilterRequest filters)
    {
        GatewayJoinHelper<Tuple> jh = new GatewayJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return jh.count();
    }

    public static TypedRecordIdentity<GatewayAssetRecord> findByInstanceId(RecordHelper<GatewayAssetRecord> helper,
                                                                           String instanceId) throws
                                                                                              NoResultException
    {
        return QueryHelperWithCommonFields.single(helper, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, GatewayAssetRecord_.instanceId, instanceId);
        });
    }

    public static GatewayAssetRecord findByRpcId(RecordHelper<GatewayAssetRecord> helper,
                                                 String rpcId) throws
                                                               NoResultException
    {
        RecordIdentity ri = QueryHelperWithCommonFields.single(helper, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, GatewayAssetRecord_.rpcId, rpcId);
        });

        return ri != null ? helper.get(ri.sysId) : null;
    }

    //--//

    public List<GatewayNetwork> collectNetworkConfiguration(boolean onlyActive)
    {
        List<GatewayNetwork> res = Lists.newArrayList();

        if (getState() == AssetState.operational)
        {
            for (NetworkAssetRecord rec_network : getBoundNetworks())
            {
                if (rec_network.getState() == AssetState.operational)
                {
                    GatewayNetwork item = new GatewayNetwork();
                    item.sysId            = rec_network.getSysId();
                    item.name             = rec_network.getName();
                    item.cidr             = rec_network.getCidr();
                    item.staticAddress    = rec_network.getStaticAddress();
                    item.networkInterface = rec_network.getNetworkInterface();
                    item.protocolsConfiguration.addAll(rec_network.getProtocolsConfiguration());

                    if (onlyActive && item.protocolsConfiguration.isEmpty())
                    {
                        continue;
                    }

                    res.add(item);
                }
            }
        }

        int maxLen = 0;
        for (GatewayNetwork item : res)
        {
            maxLen = Math.max(maxLen, item.name.length());
        }

        for (GatewayNetwork item : res)
        {
            item.namePadded = StringUtils.rightPad(item.name, maxLen);
        }

        return res;
    }

    //--//

    public RpcConnectionInfo extractConnectionInfo()
    {
        var ci = new RpcConnectionInfo();
        ci.hostDisplayName = getDisplayName();
        ci.instanceId      = getInstanceId();
        ci.rpcId           = getRpcId();
        return ci;
    }

    public void handleStatusUpdate(RecordHelper<GatewayAssetRecord> helper,
                                   GatewayStatus status)
    {
        final ZonedDateTime now = TimeUtils.now();

        setLastUpdatedDate(now);

        GatewayDetails details = new GatewayDetails();
        details.lastRefresh         = now;
        details.availableProcessors = status.availableProcessors;
        details.maxMemory           = status.maxMemory;
        details.freeMemory          = status.freeMemory;
        details.totalMemory         = status.totalMemory;
        details.hardwareVersion     = status.hardwareVersion;
        details.firmwareVersion     = status.firmwareVersion;
        details.networkInterfaces   = status.networkInterfaces;
        details.queueStatus         = status.queueStatus;

        setDetails(details);
    }

    public Class<? extends PropertyTypeExtractor> getPropertyTypeExtractorClass()
    {
        return TypeExtractor.class;
    }

    //--//

    @Override
    public void assetPostCreate(SessionHolder sessionHolder)
    {
        // Nothing to do.
    }

    @Override
    protected void assetPostUpdateInner(SessionHolder sessionHolder) throws
                                                                     Exception
    {
        if (hasStateChanged())
        {
            TaskForNetworkRefresh.Settings settings = new TaskForNetworkRefresh.Settings();
            settings.dontQueueIfAlreadyActive = true;

            TaskForNetworkRefresh.scheduleTask(sessionHolder, settings, this, null);
        }
    }

    @Override
    public void checkRemoveConditions(ValidationResultsHolder validation,
                                      RecordHelper<AssetRecord> helper) throws
                                                                        Exception
    {
        super.checkRemoveConditions(validation, helper);

        if (!getBoundNetworks().isEmpty())
        {
            validation.addFailure("boundNetworks", "Gateway '%s' is bound to networks", getInstanceId());
        }

        if (hasHeartbeat())
        {
            validation.addFailure("updatedOn", "Gateway '%s' is still communicating", getInstanceId());
        }
    }

    @Override
    protected boolean canRemoveChildren()
    {
        // We store trends under a gateway record, so it's fine to delete the gateway when children are present.
        return true;
    }

    //--//

    public ProberNetworkStatus checkNetwork(IServiceProvider serviceProvider)
    {
        try
        {
            GatewayProberControlApi proxy = getProxy(serviceProvider, GatewayProberControlApi.class);

            return getAndUnwrapException(proxy.checkNetwork());
        }
        catch (Throwable t)
        {
            // In case of connection error, assume no network.
            return null;
        }
    }

    public RpcClient checkIfOnline(IServiceProvider serviceProvider,
                                   int timeout,
                                   TimeUnit unit) throws
                                                  Exception
    {
        HubApplication app = serviceProvider.getServiceNonNull(HubApplication.class);
        return app.checkIfOnline(getRpcId(), timeout, unit);
    }

    public <T> T getProxy(IServiceProvider serviceProvider,
                          Class<T> clz) throws
                                        Exception
    {
        RpcClient client = checkIfOnline(serviceProvider, 3, TimeUnit.SECONDS);
        return client != null ? client.createProxy(rpcId, null, clz, 100, TimeUnit.SECONDS) : null;
    }
}
