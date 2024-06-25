/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.digitalmatter;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownPointClass;

@JsonTypeName("Ipn:DigitalMatter:AnalogState")
public class DigitalMatter_AnalogState extends BaseDigitalMatterObjectModel
{
    @FieldModelDescription(description = "Battery Voltage", units = EngineeringUnits.millivolts, pointClass = WellKnownPointClass.BatteryVoltage)
    public float batteryVoltage;

    @FieldModelDescription(description = "Estimated Battery Capacity", units = EngineeringUnits.percent, pointClass = WellKnownPointClass.BatteryStateOfCharge)
    public float batteryCapacity;

    @FieldModelDescription(description = "Internal Temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.BatteryTemperature)
    public float internalTemperature;

    @FieldModelDescription(description = "Signal Strength", units = EngineeringUnits.decibels_milliwatts, pointClass = WellKnownPointClass.SensorSignalQuality)
    public int signalStrength;

    //--//

    @Override
    public String extractBaseId()
    {
        return "analogState";
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentClass = WellKnownEquipmentClass.SensorDiagnostics.asWrapped();
    }
}
