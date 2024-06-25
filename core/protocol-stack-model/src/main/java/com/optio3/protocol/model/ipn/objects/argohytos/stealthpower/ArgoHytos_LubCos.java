/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.argohytos.stealthpower;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;

@JsonTypeName("Ipn:ArgoHytos::LubCos")
public class ArgoHytos_LubCos extends BaseArgoHytosModel
{
    // @formatter:off
    @FieldModelDescription(description = "Fluid Temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.SensorTemperature, debounceSeconds = 30, minimumDelta = 0.5)
    public float temperature;

    @FieldModelDescription(description = "Relative Humidity", units = EngineeringUnits.percent_relative_humidity, pointClass = WellKnownPointClass.SensorHumidity, debounceSeconds = 30, minimumDelta = 1)
    public float relative_humidity;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "ArgoHytos_LubCos";
    }
}
