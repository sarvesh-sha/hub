/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.engine.EngineBlock;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionProgram;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineInputParameterSeries;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineInputParameterSeriesWithTimeOffset;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineInputParameterSetOfSeries;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineStatementSetOutputToSeriesWithName;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueScalar;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSetOfSeries;
import com.optio3.cloud.hub.model.asset.graph.AssetGraph;
import com.optio3.cloud.hub.model.metrics.MetricsBinding;
import com.optio3.cloud.hub.model.metrics.MetricsBindingForSeries;
import com.optio3.cloud.hub.model.metrics.MetricsBindingForSetOfSeries;
import com.optio3.cloud.hub.model.tags.TagsConditionMetrics;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesBaseRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionVersionRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.ModelSanitizerContext;
import com.optio3.cloud.persistence.ModelSanitizerHandler;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.ILogger;
import com.optio3.serialization.Reflection;
import com.optio3.util.TimeUtils;

public class MetricsEngineExecutionContext extends EngineExecutionContext<MetricsDefinitionDetails, MetricsEngineExecutionStep>
{
    public static class CachedSeries
    {
        public MetricsEngineValueSeries series;
        public double                   timeShift;
    }

    public MetricsBinding bindings;
    public ZonedDateTime  rangeStart;
    public ZonedDateTime  rangeEnd;

    public AssetGraph.Analyzed graphAnalyzed;
    public Set<String>         seriesInGraph      = Sets.newHashSet();
    public Set<String>         setOfSeriesInGraph = Sets.newHashSet();

    public       MetricsEngineValueScalar                  outputForScalar;
    public       MetricsEngineValueSeries                  outputForSeries;
    public final TreeMap<String, MetricsEngineValueSeries> outputForNamedSeries = new TreeMap<>();

    public double maxInterpolationGap;

    private final Multimap<String, CachedSeries> m_lookupSeries = ArrayListMultimap.create();

    //--//

    public MetricsEngineExecutionContext(ILogger logger,
                                         SessionProvider sessionProvider,
                                         EngineExecutionProgram<MetricsDefinitionDetails> program)
    {
        super(logger, sessionProvider, program);

        if (program.definition.graph != null)
        {
            graphAnalyzed = program.definition.graph.analyze();
        }

        try (var ctx = new ModelSanitizerContext.SimpleLazy(sessionProvider::newReadOnlySession)
        {
            @Override
            protected ModelSanitizerHandler.Target processInner(Object obj,
                                                                ModelSanitizerHandler handler)
            {
                EngineBlock block = Reflection.as(obj, EngineBlock.class);
                if (block != null)
                {
                    if (block instanceof MetricsEngineInputParameterSeries)
                    {
                        MetricsEngineInputParameterSeries input = (MetricsEngineInputParameterSeries) block;
                        seriesInGraph.add(input.nodeId);
                    }

                    if (block instanceof MetricsEngineInputParameterSeriesWithTimeOffset)
                    {
                        MetricsEngineInputParameterSeriesWithTimeOffset input = (MetricsEngineInputParameterSeriesWithTimeOffset) block;
                        seriesInGraph.add(input.nodeId);
                    }

                    if (block instanceof MetricsEngineInputParameterSetOfSeries)
                    {
                        MetricsEngineInputParameterSetOfSeries input = (MetricsEngineInputParameterSetOfSeries) block;
                        setOfSeriesInGraph.add(input.nodeId);
                    }

                    if (block instanceof MetricsEngineStatementSetOutputToSeriesWithName)
                    {
                        MetricsEngineStatementSetOutputToSeriesWithName input = (MetricsEngineStatementSetOutputToSeriesWithName) block;
                        outputForNamedSeries.put(input.name, null);
                    }
                }

                return super.processInner(obj, handler);
            }
        })
        {
            ctx.processTyped(program.definition);
        }
    }

    //--//

    public static void validate(SessionHolder sessionHolder,
                                MetricsDefinitionVersionRecord rec)
    {
        EngineExecutionProgram<MetricsDefinitionDetails> program = rec.prepareProgram(sessionHolder);

        rec.setDetails(program.definition);
    }

    public Set<String> extractDependencies()
    {
        Set<String> seen = Sets.newHashSet();

        ModelSanitizerContext ctx = new ModelSanitizerContext.Simple(null)
        {
            @Override
            protected ModelSanitizerHandler.Target processInner(Object obj,
                                                                ModelSanitizerHandler handler)
            {
                TagsConditionMetrics condition = Reflection.as(obj, TagsConditionMetrics.class);
                if (condition != null)
                {
                    seen.add(condition.metricsSysId);
                }

                return super.processInner(obj, handler);
            }
        };

        ctx.process(graphAnalyzed.graph);

        return seen;
    }

    //--//

    public MetricsEngineValueSeries generateEngineValue(TypedRecordIdentity<DeviceElementRecord> record,
                                                        long timeShift,
                                                        ChronoUnit timeShiftUnit)
    {
        if (record == null)
        {
            return null;
        }

        double raw = TimeUtils.computeSafeDuration(timeShift, timeShiftUnit)
                              .toMillis() / 1000.0;

        for (CachedSeries cachedSeries : m_lookupSeries.get(record.sysId))
        {
            if (cachedSeries.timeShift == raw)
            {
                return cachedSeries.series;
            }
        }

        CachedSeries newCachedSeries = new CachedSeries();
        newCachedSeries.timeShift = raw;
        m_lookupSeries.put(record.sysId, newCachedSeries);

        TimeSeriesPropertyRequest spec = new TimeSeriesPropertyRequest();
        spec.sysId                 = record.sysId;
        spec.prop                  = DeviceElementRecord.DEFAULT_PROP_NAME;
        spec.offsetInSeconds       = (long) raw;
        spec.treatWideGapAsMissing = true;

        TimeSeriesBaseRequest cfg = new TimeSeriesBaseRequest();
        cfg.rangeStart = rangeStart;
        cfg.rangeEnd   = rangeEnd;

        TimeSeriesPropertyResponse specResults = spec.fetch(getSamplesCache(), cfg, Duration.of(100, ChronoUnit.MILLIS));
        if (specResults != null)
        {
            newCachedSeries.series = new MetricsEngineValueSeries(specResults);
        }

        return newCachedSeries.series;
    }

    public MetricsEngineValueSeries getSeries(String nodeId,
                                              int timeShift,
                                              ChronoUnit timeShiftUnit)
    {
        MetricsBindingForSeries binding = bindings.bindingForSeries.get(nodeId);
        if (binding == null)
        {
            return null;
        }

        return generateEngineValue(binding.record, timeShift, timeShiftUnit);
    }

    public MetricsEngineValueSetOfSeries getSetOfSeries(String nodeId,
                                                        int timeShift,
                                                        ChronoUnit timeShiftUnit)
    {
        MetricsBindingForSetOfSeries binding = bindings.bindingForSetOfSeries.get(nodeId);
        if (binding == null)
        {
            return null;
        }

        MetricsEngineValueSetOfSeries val = new MetricsEngineValueSetOfSeries();

        for (TypedRecordIdentity<DeviceElementRecord> record : binding.records)
        {
            MetricsEngineValueSeries value = generateEngineValue(record, timeShift, timeShiftUnit);
            if (value != null)
            {
                val.elements.add(value);
            }
        }

        return val;
    }

    //--//

    @Override
    public void reset(ZonedDateTime when)
    {
        m_lookupSeries.clear();
        outputForScalar = null;
        outputForSeries = null;

        super.reset(when);
    }

    @Override
    protected MetricsEngineExecutionStep allocateStep()
    {
        return new MetricsEngineExecutionStep();
    }
}
