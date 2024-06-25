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

@JsonTypeName("Ipn:ArgoHytos::OPComII")
public class ArgoHytos_OPComII extends BaseArgoHytosModel
{
    // @formatter:off
    @FieldModelDescription(description = "ISO 4um Count", units = EngineeringUnits.counts, pointClass = WellKnownPointClass.SensorParticleMonitor, pointTags = "ISO4um", debounceSeconds = 30, minimumDelta = 1)
    public int ISO4um;

    @FieldModelDescription(description = "ISO 6um Count", units = EngineeringUnits.counts, pointClass = WellKnownPointClass.SensorParticleMonitor, pointTags = "ISO6um", debounceSeconds = 30, minimumDelta = 1)
    public int ISO6um;

    @FieldModelDescription(description = "ISO 14um Count", units = EngineeringUnits.counts, pointClass = WellKnownPointClass.SensorParticleMonitor, pointTags = "ISO14um", debounceSeconds = 30, minimumDelta = 1)
    public int ISO14um;

    @FieldModelDescription(description = "ISO 21um Count", units = EngineeringUnits.counts, pointClass = WellKnownPointClass.SensorParticleMonitor, pointTags = "ISO21um", debounceSeconds = 30, minimumDelta = 1)
    public int ISO21um;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "ArgoHytos_OPComII";
    }
}
