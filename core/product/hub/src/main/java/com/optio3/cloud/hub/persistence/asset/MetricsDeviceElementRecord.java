/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.metrics.MetricsEngineExecutionContext;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.logic.protocol.IProtocolDecoder;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.model.asset.MetricsDeviceElement;
import com.optio3.cloud.hub.model.metrics.MetricsBinding;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.persistence.FixupProcessingRecord;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "ASSET_METRICS_DEVICE_ELEMENT")
@DynamicUpdate // Due to HHH-11506
@Indexed()
@Optio3TableInfo(externalId = "MetricsDeviceElement", model = MetricsDeviceElement.class, metamodel = MetricsDeviceElementRecord_.class)
public class MetricsDeviceElementRecord extends DeviceElementRecord
{
    public static class FixupForForcingSampling extends FixupProcessingRecord.Handler
    {
        @Override
        public Result process(Logger logger,
                              SessionHolder sessionHolder) throws
                                                           Exception
        {
            RecordHelper<MetricsDeviceElementRecord> helper = sessionHolder.createHelper(MetricsDeviceElementRecord.class);

            AssetRecord.enumerateNoNesting(helper, -1, null, (rec_asset) ->
            {
                boolean modified = rec_asset.setSamplingSettings(null);

                rec_asset.dontRefreshUpdatedOn();

                return modified ? StreamHelperNextAction.Continue_Flush_Evict : StreamHelperNextAction.Continue_Evict;
            });

            return Result.Done;
        }
    }

    //--//

    public static class TypeExtractor extends PropertyTypeExtractor
    {
        @Override
        public Map<String, TimeSeriesPropertyType> classifyRecord(DeviceElementRecord rec,
                                                                  boolean handlePresentationType)
        {
            Map<String, TimeSeriesPropertyType> res = Maps.newHashMap();

            MetricsDeviceElementRecord rec2 = Reflection.as(rec, MetricsDeviceElementRecord.class);
            if (rec2 != null)
            {
                MetricsBinding         bindings = rec2.getBindings();
                TimeSeriesPropertyType pt       = bindings.schema;
                if (pt == null)
                {
                    pt              = new TimeSeriesPropertyType();
                    pt.name         = DeviceElementRecord.DEFAULT_PROP_NAME;
                    pt.type         = TimeSeries.SampleType.Decimal;
                    pt.unitsFactors = EngineeringUnitsFactors.Dimensionless;

                    pt.targetField = pt.name;
                }

                if (pt.type == TimeSeries.SampleType.Enumerated)
                {
                    pt.expectedType = String.class;
                }
                else
                {
                    pt.expectedType = double.class;
                    pt.values       = null;
                }

                res.put(pt.name, pt);
            }

            return res;
        }

        @Override
        protected void classifyInstance(Map<String, TimeSeriesPropertyType> map,
                                        BaseObjectModel obj,
                                        boolean handlePresentationType)
        {
            // Everything happens at runtime.
        }

        @Override
        public IProtocolDecoder getProtocolDecoder()
        {
            return null;
        }

        @Override
        public EngineeringUnitsFactors getUnitsFactors(DeviceElementRecord rec)
        {
            Map<String, TimeSeriesPropertyType> map = classifyRecord(rec, false);
            TimeSeriesPropertyType              pt  = lookupPropertyType(map, DeviceElementRecord.DEFAULT_PROP_NAME);
            return pt != null ? pt.unitsFactors : null;
        }

        @Override
        public String getIndexedValue(DeviceElementRecord rec)
        {
            return null;
        }

        @Override
        public BaseObjectModel getContentsAsObject(DeviceElementRecord rec,
                                                   boolean desiredState)
        {
            return null;
        }
    }

    //--//

    @Optio3ControlNotifications(reason = "No need to notify metrics when an asset changes", direct = Optio3ControlNotifications.Notify.ON_ASSOCIATION_CHANGES,
                                reverse = Optio3ControlNotifications.Notify.NEVER, getter = "getMetricsDefinition")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getMetricsDefinition")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "metrics_definition", nullable = false, foreignKey = @ForeignKey(name = "ASSET_METRICS_DEVICE_ELEMENT__METRICS_DEFINITION__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private MetricsDefinitionRecord metricsDefinition;

    //--//

    public MetricsDeviceElementRecord()
    {
    }

    public static String newInstance(SessionHolder sessionHolder,
                                     AssetRecord rec_parent,
                                     MetricsDefinitionRecord rec_definition,
                                     String metricsTitle,
                                     MetricsBinding metricsBindings)
    {
        String sysId = metricsBindings.generateId(rec_parent, rec_definition);
        String title = metricsBindings.generateTitle(metricsTitle);

        MetricsDeviceElementRecord rec;
        AssetRecord                rec_existing = sessionHolder.getEntityOrNull(AssetRecord.class, sysId);
        if (rec_existing != null)
        {
            rec = Reflection.as(rec_existing, MetricsDeviceElementRecord.class);
            if (rec != null && rec.getMetricsDefinition() == rec_definition)
            {
                rec.putMetadata(AssetRecord.WellKnownMetadata.metricsBindings, metricsBindings);
                return null;
            }

            throw Exceptions.newIllegalArgumentException("Internal Error: duplicate metrics record for '%s' on '%s': %s", title, rec_parent.getSysId(), rec_existing.getSysId());
        }

        rec = new MetricsDeviceElementRecord();
        rec.setSysId(sysId);

        rec.metricsDefinition = rec_definition;
        rec.setIdentifier(String.format("Metrics %s", rec_definition.getSysId()));
        rec.setPhysicalName(title);
        rec.setSamplingSettings(null);

        rec.updateBindings(metricsBindings);

        rec.modifyTags((tags) ->
                       {
                           tags.addValueToTag(WellKnownTags.sysMetrics, rec_definition.getSysId());

                           if (metricsBindings.namedOutput != null)
                           {
                               tags.addValueToTag(WellKnownTags.sysMetricsOutput, metricsBindings.namedOutput);
                           }
                           else
                           {
                               tags.removeTag(WellKnownTags.sysMetricsOutput);
                           }
                       });

        rec.setLocation(rec_parent.getLocation());

        sessionHolder.persistEntity(rec);

        rec.linkToParent(sessionHolder.createHelper(AssetRecord.class), rec_parent);

        if (SessionHolder.isEntityOfClass(rec_parent, LogicalAssetRecord.class))
        {
            RelationshipRecord.addRelation(sessionHolder, rec_parent, rec, AssetRelationship.controls);
        }

        return sysId;
    }

    public MetricsDefinitionRecord getMetricsDefinition()
    {
        return metricsDefinition;
    }

    public void updateBindings(MetricsBinding metricsBinding)
    {
        putMetadata(AssetRecord.WellKnownMetadata.metricsBindings, metricsBinding);
    }

    public MetricsBinding getBindings()
    {
        return getMetadata(AssetRecord.WellKnownMetadata.metricsBindings);
    }

    //--//

    public static MetricsEngineValueSeries evaluate(ZonedDateTime rangeStart,
                                                    ZonedDateTime rangeEnd,
                                                    MetricsEngineExecutionContext ctx,
                                                    MetricsBinding bindings,
                                                    EngineExecutionContext.LogEntry callback)
    {
        ctx.bindings   = bindings;
        ctx.rangeStart = rangeStart;
        ctx.rangeEnd   = rangeEnd;

        ctx.traceExecution = true;
        ctx.reset(null);

        ctx.evaluate(1000, callback);

        if (bindings.namedOutput != null)
        {
            return ctx.outputForNamedSeries.get(bindings.namedOutput);
        }

        return ctx.outputForSeries;
    }

    //--//

    @Override
    public GatewayDiscoveryEntity createRequest(GatewayDiscoveryEntity en_device,
                                                boolean forUpdate)
    {
        return null;
    }

    @Override
    public PropertyTypeExtractor getPropertyTypeExtractor()
    {
        return new TypeExtractor();
    }

    @Override
    public boolean isSampled()
    {
        return true;
    }

    @Override
    public boolean isClassified()
    {
        return true;
    }

    @Override
    public String getParentProtocolIdentifierForSearch()
    {
        return "Metrics";
    }

    @Override
    protected List<DeviceElementSampling> sanitizeSamplingSettings(List<DeviceElementSampling> list)
    {
        DeviceElementSampling sampling = new DeviceElementSampling();
        sampling.propertyName   = DEFAULT_PROP_NAME;
        sampling.samplingPeriod = 1;

        return Lists.newArrayList(sampling);
    }
}
