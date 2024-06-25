/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.EngineeringUnitsFamily;

public class EngineeringUnitsDescriptor
{
    public final EngineeringUnits        units;
    public final String                  displayName;
    public final String                  description;
    public final EngineeringUnitsFamily  family;
    public final EngineeringUnitsFactors factors;

    @JsonCreator
    public EngineeringUnitsDescriptor(@JsonProperty("units") EngineeringUnits units)
    {
        this.units       = units;
        this.displayName = units.getDisplayName();
        this.description = units.getDescription();
        this.family      = units.getFamily();
        this.factors     = units.getConversionFactors();
    }
}
