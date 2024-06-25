/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.bluesky;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;
import org.apache.commons.lang3.StringUtils;

//
// Byte 7: power unit’s status
//      Bits 7-1: reserved
//      Bit 0: 0 = charge off, 1 = charging
// Byte 8: LSB of input volts X10
// Byte 9: MSB of input volts X10
// Byte 10: LSB of input amps X10
// Byte 11: MSB of input amps X10
// Byte 12: LSB of output amp hours
// Byte 13: MSB of output amp hours
// Byte 14: LSB of output amps
// Byte 15: MSB of output amps
// Byte 16: Signed heat sink temperature (in 2’s complement form)
// Byte 17: reserved
//

@JsonTypeName("Ipn:UnitValues")
public class BlueSky_UnitValues extends BaseBlueSkyObjectModel
{
    @FieldModelDescription(description = "Charging", units = EngineeringUnits.activeInactive)
    @SerializationTag(number = 0, width = 1, bitOffset = 0)
    public boolean charging; // 0 = charge off, 1 = charging

    @FieldModelDescription(description = "Input Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.ArrayVoltage, minimumDelta = 0.11, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float inputVoltage;

    @FieldModelDescription(description = "Input Current", units = EngineeringUnits.amperes, pointClass = WellKnownPointClass.ArrayCurrent, minimumDelta = 0.11, debounceSeconds = 5)
    @SerializationTag(number = 10, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float inputCurrent;

    @FieldModelDescription(description = "Output Charge", units = EngineeringUnits.ampere_seconds, pointClass = WellKnownPointClass.TotalCharge)
    @SerializationTag(number = 12, width = 16, scaling = { @SerializationScaling(scalingFactor = 3600) })
    public float outputAH;

    @FieldModelDescription(description = "Output Current", units = EngineeringUnits.amperes, pointClass = WellKnownPointClass.BatteryCurrent, minimumDelta = 0.11, debounceSeconds = 5)
    @SerializationTag(number = 14, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float outputCurrent;

    @FieldModelDescription(description = "Heat Sink Temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.HeatsinkTemperature, debounceSeconds = 30)
    @SerializationTag(number = 16, width = 8, scaling = { @SerializationScaling(scalingFactor = 1.0) })
    public float heatSinkTemperature;

    @JsonIgnore
    @SerializationTag(number = 17)
    public byte reserved1;

    //--//

    @JsonIgnore
    public int unitId;

    //--//

    @Override
    protected BaseObjectModel createEmptyCopy()
    {
        BlueSky_UnitValues copy = (BlueSky_UnitValues) super.createEmptyCopy();
        copy.unitId = unitId;
        return copy;
    }

    @Override
    public String extractBaseId()
    {
        return "unit";
    }

    @Override
    public String extractUnitId()
    {
        return Integer.toString(unitId);
    }

    @Override
    public boolean parseId(String id)
    {
        final String baseId = extractBaseId();
        if (StringUtils.startsWith(id, baseId))
        {
            String[] parts = StringUtils.split(id, '/');
            if (parts.length == 2 && StringUtils.equals(baseId, parts[0]))
            {
                try
                {
                    unitId = Integer.parseInt(parts[1]);
                    return true;
                }
                catch (NumberFormatException e)
                {
                    // Not a valid id.
                }
            }
        }

        return false;
    }

    @Override
    public boolean postDecodingValidation()
    {
        boolean ok = true;

        ok &= isAcceptableRange(unitId, 0, 7);
        ok &= isAcceptableRange(inputVoltage, 0, 50);
        ok &= isAcceptableRange(inputCurrent, 0, 200);
        ok &= isAcceptableRange(outputCurrent, 0, 200);
        ok &= isAcceptableRange(outputAH, 0, 3600 * 65535);

        return ok;
    }
}
