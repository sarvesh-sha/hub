/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes.configuration;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.visualization.TimeSeriesChartConfiguration;
import com.optio3.cloud.hub.model.visualization.TimeSeriesSourceConfiguration;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("PaneFieldConfigurationChart")
public class PaneFieldConfigurationChart extends PaneFieldConfiguration
{
    private List<PaneChartSource> m_sources;

    public TimeSeriesChartConfiguration config;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setChartConfig(TimeSeriesChartConfiguration chartConfig)
    {
        config = chartConfig;
        if (m_sources != null)
        {
            fixupSources();
        }
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setSources(List<PaneChartSource> sources)
    {
        m_sources = sources;
        if (config != null)
        {
            fixupSources();
        }
    }

    private void fixupSources()
    {
        if (CollectionUtils.isNotEmpty(m_sources))
        {
            for (PaneChartSource source : m_sources)
            {
                TimeSeriesSourceConfiguration chartSource = CollectionUtils.findFirst(config.getDataSources(), (s) -> StringUtils.equals(s.id, source.id));
                if (chartSource != null)
                {
                    chartSource.pointBinding = source.pointInput;
                }
            }
        }
    }
}
