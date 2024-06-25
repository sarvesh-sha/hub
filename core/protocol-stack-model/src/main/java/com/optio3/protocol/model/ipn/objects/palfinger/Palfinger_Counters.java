/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.palfinger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.can.CanMessageType;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Can:Palfinger_Counters")
@CanMessageType(sourceAddress = 0x68C, littleEndian = false)
public class Palfinger_Counters extends BasePalfingerModel
{
    @FieldModelDescription(description = "Cycle Count", units = EngineeringUnits.counts, pointClass = WellKnownPointClass.CounterNonResettable)
    @SerializationTag(number = 0, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int counterNonResettable;

    @FieldModelDescription(description = "Service Count", units = EngineeringUnits.counts, pointClass = WellKnownPointClass.CounterResettable)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int counterService;

    //--//

    @Override
    public String extractBaseId()
    {
        return "Palfinger_Counters";
    }
}
