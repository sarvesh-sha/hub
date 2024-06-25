/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.montage;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownPointClass;

@JsonTypeName("Ipn:BluetoothGateway:TemperatureHumiditySensor")
public class BluetoothGateway_TemperatureHumiditySensor extends BaseBluetoothGatewayObjectModel
{
    @FieldModelDescription(description = "Battery Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.BatteryVoltage, minimumDelta = 0.1, debounceSeconds = 30)
    public float batteryVoltage;

    @FieldModelDescription(description = "Temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.SensorTemperature, minimumDelta = 0.2, debounceSeconds = 30)
    public float temperature;

    @FieldModelDescription(description = "Humidity", units = EngineeringUnits.percent_relative_humidity, pointClass = WellKnownPointClass.SensorHumidity, minimumDelta = 0.5, debounceSeconds = 30)
    public float humidity;

    //--//

    @Override
    public String extractBaseId()
    {
        return "temperatureHumiditySensor";
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentName    = unitId;
        detailsForParent.equipmentClass   = WellKnownEquipmentClass.Sensor.asWrapped();
        detailsForParent.instanceSelector = unitId;
    }
}
