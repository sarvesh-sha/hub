package com.optio3.cloud.hub.model.dashboard;

import com.optio3.cloud.hub.model.dashboard.enums.AggregationTypeId;
import com.optio3.cloud.hub.model.visualization.ColorConfiguration;
import com.optio3.cloud.hub.model.visualization.ToggleableNumericRange;
import com.optio3.protocol.model.EngineeringUnitsFactors;

public class AggregationNodeBinding
{
    public String                  name;
    public String                  nodeId;
    public AggregationTypeId       aggregationType;
    public EngineeringUnitsFactors units;
    public String                  unitsDisplay;
    public ToggleableNumericRange  barRange;
    public ColorConfiguration      color;
}
