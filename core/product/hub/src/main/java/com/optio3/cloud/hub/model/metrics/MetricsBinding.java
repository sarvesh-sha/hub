/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.metrics;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.engine.EngineExecutionProgram;
import com.optio3.cloud.hub.engine.metrics.MetricsDefinitionDetails;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphResponse;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.MetricsDeviceElementRecord;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionRecord;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.Encryption;
import org.apache.commons.lang3.StringUtils;

public class MetricsBinding
{
    public static class DetailsSummary
    {
        public TreeMap<String, MetricsDefinitionDetails> dependencies = new TreeMap<>();

        public void add(String sysId,
                        EngineExecutionProgram<MetricsDefinitionDetails> program)
        {
            dependencies.put(sysId, program.definition);
        }

        public String extractHash() throws
                                    JsonProcessingException
        {
            return Encryption.computeSha1AsText(ObjectMappers.SkipNulls.writeValueAsString(this));
        }
    }

    public       String                                    detailsHash;
    public       String                                    namedOutput;
    public       TimeSeriesPropertyType                    schema;
    public final Map<String, MetricsBindingForSeries>      bindingForSeries      = Maps.newHashMap();
    public final Map<String, MetricsBindingForSetOfSeries> bindingForSetOfSeries = Maps.newHashMap();

    @JsonIgnore
    public AssetGraphResponse.Resolved graphSource;

    @JsonIgnore
    private Set<String> m_cachedIds;

    //--//

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        MetricsBinding that = Reflection.as(o, MetricsBinding.class);
        if (that == null)
        {
            return false;
        }

        if (!StringUtils.equals(namedOutput, that.namedOutput))
        {
            return false;
        }

        if (!bindingForSeries.equals(that.bindingForSeries))
        {
            return false;
        }

        if (!bindingForSetOfSeries.equals(that.bindingForSetOfSeries))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(collectSysIds());
    }

    //--//

    public Set<String> collectSysIds()
    {
        if (m_cachedIds == null)
        {
            Set<String> ids = Sets.newTreeSet();

            for (MetricsBindingForSeries binding : bindingForSeries.values())
            {
                ids.add(binding.record.sysId);
            }

            for (MetricsBindingForSetOfSeries binding : bindingForSetOfSeries.values())
            {
                if (binding.records != null)
                {
                    binding.records.collectSysIds(ids);
                }
            }

            m_cachedIds = Collections.unmodifiableSet(ids);
        }

        return m_cachedIds;
    }

    public MetricsBinding copy()
    {
        MetricsBinding copy = ObjectMappers.cloneThroughJson(null, this);
        copy.graphSource = graphSource;
        return copy;
    }

    //--//

    private static final String s_separatorDefinition  = "__";
    private static final String s_separatorNamedOutput = "#";

    public static String extractMetricsDefinition(SessionProvider sessionProvider,
                                                  String sysId_metricsElements)
    {
        int posStart = StringUtils.indexOf(sysId_metricsElements, s_separatorDefinition);
        if (posStart > 0)
        {
            int posEnd = StringUtils.indexOf(sysId_metricsElements, s_separatorNamedOutput, posStart);
            if (posEnd > 0)
            {
                return sysId_metricsElements.substring(posStart + s_separatorDefinition.length(), posEnd);
            }

            return sysId_metricsElements.substring(posStart + s_separatorDefinition.length());
        }

        try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
        {
            MetricsDeviceElementRecord rec = sessionHolder.getEntityOrNull(MetricsDeviceElementRecord.class, sysId_metricsElements);
            return rec != null ? RecordWithCommonFields.getSysIdSafe(rec.getMetricsDefinition()) : null;
        }
    }

    public String generateId(AssetRecord rec_parent,
                             MetricsDefinitionRecord rec_definition)
    {
        // Use a sysId derived from the Parent and the Metrics definition, so it's stable.
        StringBuilder sb = new StringBuilder();
        sb.append(rec_parent.getSysId());
        sb.append(s_separatorDefinition);
        sb.append(rec_definition.getSysId());

        if (namedOutput != null)
        {
            sb.append(s_separatorNamedOutput);
            sb.append(namedOutput);
        }

        return sb.toString();
    }

    public String generateTitle(String title)
    {
        if (namedOutput != null)
        {
            title = String.format("%s - %s", title, namedOutput);
        }

        return title;
    }
}
