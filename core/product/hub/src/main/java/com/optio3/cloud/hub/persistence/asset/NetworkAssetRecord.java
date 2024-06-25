/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Tuple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.logic.protocol.IProtocolDecoder;
import com.optio3.cloud.hub.logic.protocol.NetworkPerfDecoder;
import com.optio3.cloud.hub.model.asset.Asset;
import com.optio3.cloud.hub.model.asset.NetworkAsset;
import com.optio3.cloud.hub.model.asset.NetworkFilterRequest;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousMachineConfig;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.orchestration.tasks.TaskForNetworkRefresh;
import com.optio3.cloud.hub.orchestration.tasks.TaskForSamplingPeriod;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogHandler;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.NetworkDescriptor;
import com.optio3.protocol.model.TransportPerformanceCounters;
import com.optio3.protocol.model.config.ProtocolConfig;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "ASSET_NETWORK")
@DynamicUpdate // Due to HHH-11506
@Indexed()
@Optio3TableInfo(externalId = "NetworkAsset", model = NetworkAsset.class, metamodel = NetworkAssetRecord_.class, metadata = NetworkAssetRecord.WellKnownMetadata.class)
public class NetworkAssetRecord extends AssetRecord implements AssetRecord.IProviderOfPropertyTypeExtractorClass,
                                                               LogHandler.ILogHost<NetworkAssetLogRecord>
{
    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        public static final MetadataField<DigineousMachineConfig> digineous_machineConfig = new MetadataField<>("digineous_machineConfig", DigineousMachineConfig.class);
    }

    //--//

    static class SerializedConfig
    {
        public List<ProtocolConfig> values = Lists.newArrayList();
    }

    public static class TypeExtractor extends PropertyTypeExtractor
    {
        private static final Map<String, TimeSeriesPropertyType> s_fields;

        static
        {
            Map<String, TimeSeriesPropertyType> fields = Maps.newHashMap();
            TransportPerformanceCounters        obj    = new TransportPerformanceCounters();

            for (FieldModel fieldModel : obj.getDescriptors())
            {
                String fieldName = fieldModel.name;

                final TimeSeriesPropertyType pt = new TimeSeriesPropertyType();
                pt.name        = fieldName;
                pt.displayName = fieldModel.getDescription(obj);
                pt.type        = TimeSeries.SampleType.Integer;
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
            if (obj instanceof TransportPerformanceCounters)
            {
                map.putAll(s_fields);
            }
        }

        @Override
        public IProtocolDecoder getProtocolDecoder()
        {
            return new NetworkPerfDecoder();
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
        public TransportPerformanceCounters getContentsAsObject(DeviceElementRecord rec,
                                                                boolean desiredState) throws
                                                                                      IOException
        {
            ObjectMapper om = TransportPerformanceCounters.getObjectMapper();

            if (desiredState)
            {
                return rec.getTypedDesiredContents(om, TransportPerformanceCounters.class);
            }
            else
            {
                return rec.getTypedContents(om, TransportPerformanceCounters.class);
            }
        }
    }

    //--//

    @Column(name = "cidr", nullable = false)
    private String cidr = "0.0.0.0/0";

    @Column(name = "static_address")
    private String staticAddress;

    @Column(name = "network_interface")
    private String networkInterface;

    @Column(name = "sampling_period", nullable = false)
    private int samplingPeriod = 5 * 60;

    @Transient
    private Integer samplingPeriodPrevious;

    @Lob
    @Column(name = "protocols_configuration")
    private String protocolsConfiguration;

    @Transient
    private final PersistAsJsonHelper<String, List<ProtocolConfig>> m_protocolsConfigurationParser = new PersistAsJsonHelper<>(() -> protocolsConfiguration,
                                                                                                                               (val) -> protocolsConfiguration = val,
                                                                                                                               (val) ->
                                                                                                                               {
                                                                                                                                   if (val != null)
                                                                                                                                   {
                                                                                                                                       SerializedConfig wrapper = ObjectMappers.SkipNulls.readValue(val,
                                                                                                                                                                                                    SerializedConfig.class);
                                                                                                                                       return wrapper.values;
                                                                                                                                   }
                                                                                                                                   else
                                                                                                                                   {
                                                                                                                                       return Collections.emptyList();
                                                                                                                                   }
                                                                                                                               },
                                                                                                                               (val) ->
                                                                                                                               {
                                                                                                                                   if (val == null)
                                                                                                                                   {
                                                                                                                                       return null;
                                                                                                                                   }
                                                                                                                                   else
                                                                                                                                   {
                                                                                                                                       SerializedConfig wrapper = new SerializedConfig();
                                                                                                                                       wrapper.values = val;

                                                                                                                                       return ObjectMappers.SkipNulls.writeValueAsString(wrapper);
                                                                                                                                   }
                                                                                                                               },
                                                                                                                               String.class);

    //--//

    @Lob
    @Column(name = "discovery_state")
    private String discoveryState;

    @Transient
    private final PersistAsJsonHelper<String, DiscoveryState> m_discoveryStateHelper = new PersistAsJsonHelper<>(() -> discoveryState,
                                                                                                                 (val) -> discoveryState = val,
                                                                                                                 String.class,
                                                                                                                 DiscoveryState.class,
                                                                                                                 ObjectMappers.SkipNulls);

    //--//

    /**
     * The gateways that are associated with this network.
     */
    @ManyToMany(mappedBy = "boundNetworks", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private Set<GatewayAssetRecord> boundGateways = Sets.newHashSet();

    //--//

    @Column(name = "last_output")
    private ZonedDateTime lastOutput;

    @Column(name = "last_offset", nullable = false)
    private int lastOffset;

    @Lob
    @Column(name = "log_ranges")
    private byte[] logRanges;

    //--//

    public NetworkAssetRecord()
    {
    }

    //--//

    public String getCidr()
    {
        return cidr;
    }

    public void setCidr(String cidr)
    {
        this.cidr = cidr;
    }

    public String getStaticAddress()
    {
        return staticAddress;
    }

    public void setStaticAddress(String staticAddress)
    {
        this.staticAddress = staticAddress;
    }

    public String getNetworkInterface()
    {
        return networkInterface;
    }

    public void setNetworkInterface(String networkInterface)
    {
        this.networkInterface = networkInterface;
    }

    public int getSamplingPeriod()
    {
        return samplingPeriod;
    }

    public void setSamplingPeriod(int samplingPeriod)
    {
        if (this.samplingPeriodPrevious == null)
        {
            this.samplingPeriodPrevious = this.samplingPeriod;
        }

        this.samplingPeriod = samplingPeriod;
    }

    public List<ProtocolConfig> getProtocolsConfiguration()
    {
        return m_protocolsConfigurationParser.getNoCaching();
    }

    public <T extends ProtocolConfig> List<T> getProtocolsConfiguration(Class<T> clz)
    {
        return CollectionUtils.transformToListNoNulls(getProtocolsConfiguration(), (cfg) -> Reflection.as(cfg, clz));
    }

    public boolean setProtocolsConfiguration(List<ProtocolConfig> protocolsConfiguration)
    {
        return m_protocolsConfigurationParser.set(protocolsConfiguration);
    }

    public DiscoveryState getDiscoveryState()
    {
        return m_discoveryStateHelper.get();
    }

    public boolean setDiscoveryState(DiscoveryState val)
    {
        return m_discoveryStateHelper.set(val);
    }

    //--//

    public Set<GatewayAssetRecord> getBoundGateways()
    {
        return boundGateways;
    }

    public GatewayAssetRecord getBoundGateway()
    {
        return CollectionUtils.firstElement(boundGateways);
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
    public void refineLogQuery(LogHandler.JoinHelper<?, NetworkAssetLogRecord> jh)
    {
        jh.addWhereClauseWithEqual(jh.rootLog, NetworkAssetLogRecord_.owningNetwork, this);
    }

    @Override
    public NetworkAssetLogRecord allocateNewLogInstance()
    {
        return NetworkAssetLogRecord.newInstance(this);
    }

    public static LogHandler<NetworkAssetRecord, NetworkAssetLogRecord> allocateLogHandler(RecordLocked<NetworkAssetRecord> lock)
    {
        return new LogHandler<>(lock, NetworkAssetLogRecord.class);
    }

    public static LogHandler<NetworkAssetRecord, NetworkAssetLogRecord> allocateLogHandler(SessionHolder sessionHolder,
                                                                                           NetworkAssetRecord rec)
    {
        return new LogHandler<>(sessionHolder, rec, NetworkAssetLogRecord.class);
    }

    //--//

    @Override
    public BaseAssetDescriptor getIdentityDescriptor()
    {
        NetworkDescriptor desc = new NetworkDescriptor();
        desc.sysId = getSysId();
        return desc;
    }

    public Class<? extends PropertyTypeExtractor> getPropertyTypeExtractorClass()
    {
        return TypeExtractor.class;
    }

    //--//

    private static class NetworkJoinHelper<T> extends AssetJoinHelper<T, NetworkAssetRecord>
    {
        NetworkJoinHelper(RecordHelper<NetworkAssetRecord> helper,
                          Class<T> clz)
        {
            super(helper, clz);
        }

        //--//

        void applyFilters(NetworkFilterRequest filters)
        {
            super.applyFilters(filters);

            if (filters.sortBy != null)
            {
                for (SortCriteria sort : filters.sortBy)
                {
                    switch (sort.column)
                    {
                        case "cidr":
                        {
                            addOrderBy(root, NetworkAssetRecord_.cidr, sort.ascending);
                            break;
                        }
                    }
                }
            }
        }
    }

    //--//

    public static List<RecordIdentity> filterNetworks(RecordHelper<NetworkAssetRecord> helper,
                                                      NetworkFilterRequest filters)
    {
        NetworkJoinHelper<Tuple> jh = new NetworkJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return AssetJoinHelper.returnFilterTuples(helper, jh);
    }

    public static long countNetworks(RecordHelper<NetworkAssetRecord> helper,
                                     NetworkFilterRequest filters)
    {
        NetworkJoinHelper<Tuple> jh = new NetworkJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return jh.count();
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
        if (samplingPeriodPrevious != null)
        {
            TaskForSamplingPeriod.scheduleTask(sessionHolder, this, samplingPeriodPrevious, samplingPeriod);
        }

        TaskForNetworkRefresh.Settings settings = new TaskForNetworkRefresh.Settings();
        settings.dontQueueIfAlreadyActive = true;

        for (GatewayAssetRecord rec_gateway : getBoundGateways())
        {
            TaskForNetworkRefresh.scheduleTask(sessionHolder, settings, rec_gateway, null);
        }
    }

    @Override
    public void checkRemoveConditions(ValidationResultsHolder validation,
                                      RecordHelper<AssetRecord> helper) throws
                                                                        Exception
    {
        super.checkRemoveConditions(validation, helper);

        if (getBoundGateway() != null)
        {
            validation.addFailure("boundGateway", "Network '%s' is bound to gateway", getName());
        }
    }

    @Override
    protected boolean canRemoveChildren()
    {
        // Children have to be deleted individually.
        return false;
    }

    //--//

    @Override
    public void fromModelOverride(SessionHolder sessionHolder,
                                  ModelMapperPolicy policy,
                                  Asset model)
    {
        if (StringUtils.isNotEmpty(model.physicalName))
        {
            setPhysicalName(model.physicalName);
        }

        NetworkAsset modelNetwork = Reflection.as(model, NetworkAsset.class);
        if (modelNetwork != null)
        {
            // Reset all the sampling configuration ID's.
            for (ProtocolConfig protocolConfig : modelNetwork.protocolsConfiguration)
            {
                protocolConfig.samplingConfigurationId = null;
            }
        }
    }
}
