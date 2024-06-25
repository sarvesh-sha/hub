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

@JsonTypeName("Ipn:BluetoothGateway:PixelTag")
public class BluetoothGateway_PixelTag extends BaseBluetoothGatewayObjectModel
{
    @FieldModelDescription(description = "Temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.SensorTemperature)
    public float temperature;

    @FieldModelDescription(description = "RSSI", units = EngineeringUnits.no_units, pointClass = WellKnownPointClass.SensorRSSI, minimumDelta = 0.2, debounceSeconds = 30)
    public int rssi;

    //--//

    @Override
    public String extractBaseId()
    {
        return "pixelTag";
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentName    = unitId;
        detailsForParent.equipmentClass   = WellKnownEquipmentClass.AssetTag.asWrapped();
        detailsForParent.instanceSelector = unitId;
    }
}
