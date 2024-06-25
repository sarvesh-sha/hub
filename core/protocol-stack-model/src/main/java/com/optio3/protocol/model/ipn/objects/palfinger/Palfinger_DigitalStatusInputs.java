/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.palfinger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldChangeMode;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.can.CanMessageType;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Can:Palfinger_DigitalStatusInputs")
@CanMessageType(sourceAddress = 0x689, littleEndian = false)
public class Palfinger_DigitalStatusInputs extends BasePalfingerModel
{
    @FieldModelDescription(description = "B-16 ILQ MINI, YELLOW 0V", units = EngineeringUnits.activeInactive, debounceSeconds = 30, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 0, width = 1, bitOffset = 0)
    public boolean sensorB16WarnLightsOn;

    @FieldModelDescription(description = "B-16 ILQ MINI, GREEN 12V", units = EngineeringUnits.activeInactive, debounceSeconds = 30, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 0, width = 1, bitOffset = 1)
    public boolean sensorB16PositionHorizontal;

    @FieldModelDescription(description = "Lift Command", units = EngineeringUnits.activeInactive, pointClass = WellKnownPointClass.CommandLift, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 0, width = 1, bitOffset = 2)
    public boolean commandLift;

    @FieldModelDescription(description = "Lower Command", units = EngineeringUnits.activeInactive, pointClass = WellKnownPointClass.CommandLower, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 0, width = 1, bitOffset = 3)
    public boolean commandLower;

    @FieldModelDescription(description = "Open Command", units = EngineeringUnits.activeInactive, pointClass = WellKnownPointClass.CommandOpen, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 0, width = 1, bitOffset = 4)
    public boolean commandOpen;

    @FieldModelDescription(description = "Close Command", units = EngineeringUnits.activeInactive, pointClass = WellKnownPointClass.CommandClose, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 0, width = 1, bitOffset = 5)
    public boolean commandClose;

    @FieldModelDescription(description = "Lift Foot Command", units = EngineeringUnits.activeInactive, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 0, width = 1, bitOffset = 6)
    public boolean commandLiftFoot;

    @FieldModelDescription(description = "Lower Foot Command", units = EngineeringUnits.activeInactive, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 0, width = 1, bitOffset = 7)
    public boolean commandLowerFoot;

    //--//

    @FieldModelDescription(description = "B-13 Tilting Switch", notes = "ILUK only", units = EngineeringUnits.activeInactive, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 1, width = 1, bitOffset = 0)
    public boolean J41_A_B13_TiltingSwitch;

    @FieldModelDescription(description = "Slide In", notes = "ILUK only", units = EngineeringUnits.activeInactive, pointClass = WellKnownPointClass.CommandSlideIn,
                           stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 1, width = 1, bitOffset = 1)
    public boolean J32_80;

    @FieldModelDescription(description = "Slide Out", notes = "ILUK only", units = EngineeringUnits.activeInactive, pointClass = WellKnownPointClass.CommandSlideOut,
                           stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 1, width = 1, bitOffset = 2)
    public boolean J32_81;

    @FieldModelDescription(description = "J-1 E Pin Left", units = EngineeringUnits.activeInactive, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 1, width = 1, bitOffset = 3)
    public boolean J1_E_li;

    @FieldModelDescription(description = "J-1 E Pin Right", units = EngineeringUnits.activeInactive, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 1, width = 1, bitOffset = 4)
    public boolean J1_E_re;

    @FieldModelDescription(description = "J-32 E Pin Left", units = EngineeringUnits.activeInactive, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 1, width = 1, bitOffset = 5)
    public boolean J32_E_li;

    @FieldModelDescription(description = "J-32 E Pin Right", units = EngineeringUnits.activeInactive, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 1, width = 1, bitOffset = 6)
    public boolean J32_E_re;

    @FieldModelDescription(description = "J-2 E Pin Left", units = EngineeringUnits.activeInactive, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 1, width = 1, bitOffset = 7)
    public boolean J2_E_li;

    //--//

    @FieldModelDescription(description = "Liftgate Light On Command", units = EngineeringUnits.activeInactive, debounceSeconds = 30, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 2, width = 1, bitOffset = 0)
    public boolean J11_1_CabinControl_On;

    @FieldModelDescription(description = "J-2 E Pin Right", units = EngineeringUnits.activeInactive, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 2, width = 1, bitOffset = 1)
    public boolean J2_E_re;

    //--//

    @Override
    public String extractBaseId()
    {
        return "Palfinger_DigitalStatusInputs";
    }
}
