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
import com.optio3.protocol.model.FieldTemporalResolution;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.ipn.IpnObjectModel;

@JsonTypeName("Ipn:Accelerometer")
public class IpnAccelerometer extends IpnObjectModel
{
    @FieldModelDescription(description = "X acceleration", units = EngineeringUnits.millig, temporalResolution = FieldTemporalResolution.Max1000Hz, pointClass=WellKnownPointClass.AccelerationX)
    public int x;

    @FieldModelDescription(description = "Y acceleration", units = EngineeringUnits.millig, temporalResolution = FieldTemporalResolution.Max1000Hz, pointClass = WellKnownPointClass.AccelerationY)
    public int y;

    @FieldModelDescription(description = "Z acceleration", units = EngineeringUnits.millig, temporalResolution = FieldTemporalResolution.Max1000Hz, pointClass = WellKnownPointClass.AccelerationZ)
    public int z;

    //--//

    @Override
    public boolean shouldCommitReachabilityChange(boolean isReachable,
                                                  ZonedDateTime lastReachable)
    {
        return true;
    }

    @Override
    public String extractBaseId()
    {
        return "accelerometer";
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentClass = WellKnownEquipmentClass.Accelerometer.asWrapped();
    }
}
