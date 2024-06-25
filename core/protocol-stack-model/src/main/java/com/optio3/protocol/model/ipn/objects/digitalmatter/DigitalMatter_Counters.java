/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.digitalmatter;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;

@JsonTypeName("Ipn:DigitalMatter:Counters")
public class DigitalMatter_Counters extends BaseDigitalMatterObjectModel
{
    @FieldModelDescription(description = "Successful Uploads", units = EngineeringUnits.counts)
    public int successfulUploads;

    @FieldModelDescription(description = "Successful Uploads Time", units = EngineeringUnits.seconds)
    public int successfulUploadsTime;

    @FieldModelDescription(description = "Failed Uploads", units = EngineeringUnits.counts)
    public int failedUploads;

    @FieldModelDescription(description = "Failed Uploads Time", units = EngineeringUnits.seconds)
    public int failedUploadsTime;

    @FieldModelDescription(description = "Successful GPS Fixes", units = EngineeringUnits.counts)
    public int successfulGpsFixes;

    @FieldModelDescription(description = "Successful GPS Fix Time", units = EngineeringUnits.seconds)
    public int successfulGpsFixTime;

    @FieldModelDescription(description = "Failed GPS Fixes", units = EngineeringUnits.counts)
    public int failedGpsFixes;

    @FieldModelDescription(description = "Failed GPS Fix Time", units = EngineeringUnits.seconds)
    public int failedGpsFixTime;

    @FieldModelDescription(description = "GPS Freshen Attempts", units = EngineeringUnits.counts)
    public int gpsFreshenAttempts;

    @FieldModelDescription(description = "GPS Freshen Time", units = EngineeringUnits.seconds)
    public int gpsFreshenTime;

    @FieldModelDescription(description = "Accelerometer Wakeups", units = EngineeringUnits.counts)
    public int accelerometerWakeups;

    @FieldModelDescription(description = "Trips", units = EngineeringUnits.counts)
    public int trips;

    //--//

    @Override
    public String extractBaseId()
    {
        return "counters";
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        // Not classified.
    }
}
