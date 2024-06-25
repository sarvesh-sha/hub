/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.sensors;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("Ipn:I2C:Current")
public class IpnCurrent extends IpnI2CSensor
{
    @FieldModelDescription(description = "Current", units = EngineeringUnits.amperes, pointClass = WellKnownPointClass.SensorCurrent, minimumDelta = 0.05, debounceSeconds = 10)
    public float current;

    //--//

    @Override
    public boolean shouldCommitReachabilityChange(boolean isReachable,
                                                  ZonedDateTime lastReachable)
    {
        return true;
    }

    //--//

    @Override
    public String extractBaseId()
    {
        return "current";
    }
}
