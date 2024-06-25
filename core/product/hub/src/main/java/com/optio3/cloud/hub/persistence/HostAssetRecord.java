/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Tuple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.logic.protocol.IProtocolDecoder;
import com.optio3.cloud.hub.logic.protocol.RestPerfDecoder;
import com.optio3.cloud.hub.model.HostAsset;
import com.optio3.cloud.hub.model.asset.HostFilterRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.persistence.LogHandler;
import com.optio3.cloud.persistence.RecordForWorker;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.RestDescriptor;
import com.optio3.protocol.model.RestPerformanceCounters;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "HOST")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "HostAsset", model = HostAsset.class, metamodel = HostAssetRecord_.class)
public class HostAssetRecord extends AssetRecord implements AssetRecord.IProviderOfPropertyTypeExtractorClass,
                                                            RecordForWorker<HostAssetRecord>,
                                                            LogHandler.ILogHost<HostAssetLogRecord>
{
    public static class TypeExtractor extends PropertyTypeExtractor
    {
        private static final Map<String, TimeSeriesPropertyType> s_fields;

        static
        {
            Map<String, TimeSeriesPropertyType> fields = Maps.newHashMap();
            RestPerformanceCounters             obj    = new RestPerformanceCounters();

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
            if (obj instanceof RestPerformanceCounters)
            {
                map.putAll(s_fields);
            }
        }

        @Override
        public IProtocolDecoder getProtocolDecoder()
        {
            return new RestPerfDecoder();
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
        public RestPerformanceCounters getContentsAsObject(DeviceElementRecord rec,
                                                           boolean desiredState) throws
                                                                                 IOException
        {
            ObjectMapper om = RestPerformanceCounters.getObjectMapper();

            if (desiredState)
            {
                return rec.getTypedDesiredContents(om, RestPerformanceCounters.class);
            }
            else
            {
                return rec.getTypedContents(om, RestPerformanceCounters.class);
            }
        }
    }

    //--//

    @Column(name = "last_output")
    private ZonedDateTime lastOutput;

    @Column(name = "last_offset", nullable = false)
    private int lastOffset;

    @Lob
    @Column(name = "log_ranges")
    private byte[] logRanges;

    //--//

    @Override
    public BaseAssetDescriptor getIdentityDescriptor()
    {
        RestDescriptor desc = new RestDescriptor();
        desc.sysId = getSysId();
        return desc;
    }

    public Class<? extends PropertyTypeExtractor> getPropertyTypeExtractorClass()
    {
        return TypeExtractor.class;
    }

    //--//

    private static class HostJoinHelper<T> extends AssetJoinHelper<T, HostAssetRecord>
    {
        HostJoinHelper(RecordHelper<HostAssetRecord> helper,
                       Class<T> clz)
        {
            super(helper, clz);
        }

        //--//

        void applyFilters(HostFilterRequest filters)
        {
            super.applyFilters(filters);
        }
    }

    //--//

    public static List<RecordIdentity> filterHosts(RecordHelper<HostAssetRecord> helper,
                                                   HostFilterRequest filters)
    {
        HostJoinHelper<Tuple> jh = new HostJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return AssetJoinHelper.returnFilterTuples(helper, jh);
    }

    public static long countHosts(RecordHelper<HostAssetRecord> helper,
                                  HostFilterRequest filters)
    {
        HostJoinHelper<Tuple> jh = new HostJoinHelper<>(helper, Tuple.class);

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
        // Nothing to do.
    }

    @Override
    protected boolean canRemoveChildren()
    {
        return true;
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
    public void refineLogQuery(LogHandler.JoinHelper<?, HostAssetLogRecord> jh)
    {
        jh.addWhereClauseWithEqual(jh.rootLog, HostAssetLogRecord_.owningHost, this);
    }

    @Override
    public HostAssetLogRecord allocateNewLogInstance()
    {
        return HostAssetLogRecord.newInstance(this);
    }

    public static LogHandler<HostAssetRecord, HostAssetLogRecord> allocateLogHandler(RecordLocked<HostAssetRecord> lock)
    {
        return new LogHandler<>(lock, HostAssetLogRecord.class);
    }

    public static LogHandler<HostAssetRecord, HostAssetLogRecord> allocateLogHandler(SessionHolder sessionHolder,
                                                                                     HostAssetRecord rec)
    {
        return new LogHandler<>(sessionHolder, rec, HostAssetLogRecord.class);
    }
}
