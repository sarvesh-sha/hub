/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.visualization;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.EngineeringUnitsFamily;
import com.google.common.collect.Lists;

public class TimeSeriesAxisConfiguration
{
    public        String                                 label;
    public        String                                 color;
    public        EngineeringUnitsFactors                displayFactors;
    private final List<TimeSeriesAxisGroupConfiguration> m_groupedFactors = Lists.newArrayList();
    private       ToggleableNumericRange                 m_override;

    //--//

    // TODO: UPGRADE PATCH: Legacy fixup for updated field
    public List<TimeSeriesAxisGroupConfiguration> getGroupedFactors()
    {
        groupedFactorsFixup();

        return m_groupedFactors;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for updated field
    public ToggleableNumericRange getOverride()
    {
        groupedFactorsFixup();

        return m_override;
    }

    public void setOverride(ToggleableNumericRange override)
    {
        m_override = override;
    }

    private void groupedFactorsFixup()
    {
        if (m_groupedFactors.size() > 0 && m_override != null && displayFactors != null)
        {
            for (TimeSeriesAxisGroupConfiguration group : m_groupedFactors)
            {
                if (displayFactors.isEquivalent(group.keyFactors))
                {
                    group.override = m_override;
                    break;
                }
            }

            m_override = null;
        }
    }

    //--//

    // TODO: UPGRADE PATCH: Legacy fixup for removed field - behavior is always 'GROUPED' mode now
    public void setMode(JsonNode node)
    {
        HubApplication.reportPatchCall(node);
    }

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setDisplay(EngineeringUnitsFamily family)
    {
        HubApplication.reportPatchCall(family);

        for (EngineeringUnits value : EngineeringUnits.values())
        {
            if (value.getFamily() == family)
            {
                this.displayFactors = value.getConversionFactors();
                break;
            }
        }
    }

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setFamilies(JsonNode node)
    {
        HubApplication.reportPatchCall(node);
    }
}
