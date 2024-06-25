/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.sensors;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("Ipn:I2C:Temperature")
public class IpnTemperature extends IpnI2CSensor
{
    @FieldModelDescription(description = "Temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.SensorTemperature, minimumDelta = 0.2, debounceSeconds = 30)
    public float temperature;

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
        return "temperature";
    }
}
