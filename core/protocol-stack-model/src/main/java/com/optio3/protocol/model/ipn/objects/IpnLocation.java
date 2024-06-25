/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.util.TimeUtils;

@JsonTypeName("Ipn:Location")
public class IpnLocation extends IpnObjectModel
{
    // Use 5 decimal digits for coordinates, to get about 1 meter accuracy.
    @FieldModelDescription(description = "Latitude", units = EngineeringUnits.latitude, digitsOfPrecision = 5, pointClass = WellKnownPointClass.LocationLatitude, minimumDelta = 0.0001,
                           debounceSeconds = 10)
    public double latitude;

    // Use 5 decimal digits for coordinates, to get about 1 meter accuracy.
    @FieldModelDescription(description = "Longitude", units = EngineeringUnits.longitude, digitsOfPrecision = 5, pointClass = WellKnownPointClass.LocationLongitude, minimumDelta = 0.0001,
                           debounceSeconds = 10)
    public double longitude;

    @FieldModelDescription(description = "Altitude", units = EngineeringUnits.meters, pointClass = WellKnownPointClass.LocationAltitude, minimumDelta = 10, debounceSeconds = 10)
    public int altitude;

    @FieldModelDescription(description = "Speed", units = EngineeringUnits.kilometers_per_hour, pointClass = WellKnownPointClass.LocationSpeed, debounceSeconds = 10)
    public int speed;

    @FieldModelDescription(description = "Heading", units = EngineeringUnits.degrees_angular, pointClass = WellKnownPointClass.LocationHeading, debounceSeconds = 10)
    public int heading;

    //--//

    @Override
    public boolean shouldCommitReachabilityChange(boolean isReachable,
                                                  ZonedDateTime lastReachable)
    {
        if (!isReachable)
        {
            if (TimeUtils.wasUpdatedRecently(lastReachable, 8, TimeUnit.HOURS))
            {
                // Wait a few hours before notifying about lost GPS.
                return false;
            }
        }

        return true;
    }

    @Override
    public String extractBaseId()
    {
        return "location";
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentClass = WellKnownEquipmentClass.GPS.asWrapped();
    }
}
