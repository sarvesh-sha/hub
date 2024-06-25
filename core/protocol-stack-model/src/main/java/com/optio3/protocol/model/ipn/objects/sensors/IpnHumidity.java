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

@JsonTypeName("Ipn:I2C:Humidity")
public class IpnHumidity extends IpnI2CSensor
{
    @FieldModelDescription(description = "Humidity", units = EngineeringUnits.percent_relative_humidity, pointClass = WellKnownPointClass.SensorHumidity, minimumDelta = 0.5, debounceSeconds = 30)
    public float humidity;

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
        return "humidity";
    }
}
