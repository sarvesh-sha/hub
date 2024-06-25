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
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("Ipn:I2C:Voltage")
public class IpnVoltage extends IpnI2CSensor
{
    @FieldModelDescription(description = "Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.SensorVoltage, minimumDelta = 0.05, debounceSeconds = 10)
    public float voltage;

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
        return "voltage";
    }
}
