/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digineous.model;

import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;

public class DigineousVibrationPayload
{
    @FieldModelDescription(description = "Total Acceleration", units = EngineeringUnits.meters_per_second_per_second, pointClass = WellKnownPointClass.Acceleration)
    public Double totalAcceleration;

    @FieldModelDescription(description = "Velocity X", units = EngineeringUnits.meters_per_second, pointClass = WellKnownPointClass.VelocityX)
    public Double velocityX;

    @FieldModelDescription(description = "Velocity Y", units = EngineeringUnits.meters_per_second, pointClass = WellKnownPointClass.VelocityY)
    public Double velocityY;

    @FieldModelDescription(description = "Velocity Z", units = EngineeringUnits.meters_per_second, pointClass = WellKnownPointClass.VelocityZ)
    public Double velocityZ;

    @FieldModelDescription(description = "Temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.SensorTemperature)
    public Double temperature;

    @FieldModelDescription(description = "Audio Noise", units = EngineeringUnits.decibels, pointClass = WellKnownPointClass.SensorNoise)
    public Double audio;

    //--//

    public static FieldModel[] getDescriptors()
    {
        return BaseObjectModel.collectDescriptors(DigineousVibrationPayload.class);
    }
}
