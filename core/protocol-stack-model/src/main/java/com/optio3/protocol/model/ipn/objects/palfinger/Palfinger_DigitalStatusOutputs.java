/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.palfinger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldChangeMode;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.can.CanMessageType;
import com.optio3.protocol.model.ipn.IpnObjectPostProcess;
import com.optio3.protocol.model.ipn.IpnObjectsState;
import com.optio3.protocol.model.ipn.enums.Ipn_PalFinger_DisplayCode;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Can:Palfinger_DigitalStatusOutputs")
@CanMessageType(sourceAddress = 0x68B, littleEndian = false)
public class Palfinger_DigitalStatusOutputs extends BasePalfingerModel implements IpnObjectPostProcess<Palfinger_DigitalStatusOutputs>
{
    @FieldModelDescription(description = "Tilting Up", notes = "ILUK only", units = EngineeringUnits.activeInactive)
    @SerializationTag(number = 0, width = 1, bitOffset = 0)
    public boolean valve_S5;

    @FieldModelDescription(description = "Motor Solenoid", units = EngineeringUnits.activeInactive, pointClass = WellKnownPointClass.MotorSolenoid)
    @SerializationTag(number = 0, width = 1, bitOffset = 1)
    public boolean relay_Aggregat;

    @FieldModelDescription(description = "Up And Down", notes = "ILUK only", units = EngineeringUnits.activeInactive)
    @SerializationTag(number = 0, width = 1, bitOffset = 2)
    public boolean valve_Liftzyl;

    @FieldModelDescription(description = "Tilting Down", notes = "ILUK only", units = EngineeringUnits.activeInactive)
    @SerializationTag(number = 0, width = 1, bitOffset = 3)
    public boolean valve_Tiltzyl;

    @FieldModelDescription(description = "Sliding Out", notes = "ILUK only", units = EngineeringUnits.activeInactive)
    @SerializationTag(number = 0, width = 1, bitOffset = 4)
    public boolean valve_J42_81;

    @FieldModelDescription(description = "Sliding In", notes = "ILUK only", units = EngineeringUnits.activeInactive)
    @SerializationTag(number = 0, width = 1, bitOffset = 5)
    public boolean valve_J42_80;

    @FieldModelDescription(description = "Opening Platform", notes = "ILD+ only", units = EngineeringUnits.activeInactive)
    @SerializationTag(number = 0, width = 1, bitOffset = 6)
    public boolean valve_J43_90;

    @FieldModelDescription(description = "Closing Platform", notes = "ILD+ only", units = EngineeringUnits.activeInactive)
    @SerializationTag(number = 0, width = 1, bitOffset = 7)
    public boolean valve_J43_91;

    //--//

    @FieldModelDescription(description = "Valve J43_93", units = EngineeringUnits.activeInactive)
    @SerializationTag(number = 1, width = 1, bitOffset = 0)
    public boolean valve_J43_93;

    @FieldModelDescription(description = "Valve J43_94", units = EngineeringUnits.activeInactive)
    @SerializationTag(number = 1, width = 1, bitOffset = 1)
    public boolean valve_J43_94;

    @FieldModelDescription(description = "Indicator Light", units = EngineeringUnits.activeInactive, debounceSeconds = 30, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 1, width = 1, bitOffset = 2)
    public boolean light;

    @FieldModelDescription(description = "Warning Lights", notes = "ILUK only", units = EngineeringUnits.activeInactive, debounceSeconds = 30, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 1, width = 1, bitOffset = 3)
    public boolean warningLights;

    @FieldModelDescription(description = "Liftgate Light On", units = EngineeringUnits.activeInactive, debounceSeconds = 30, stickyMode = FieldChangeMode.StickyActive)
    @SerializationTag(number = 1, width = 1, bitOffset = 4)
    public boolean control_LED_DriversCabin;

    @FieldModelDescription(description = "Battery Low Voltage", units = EngineeringUnits.activeInactive)
    @SerializationTag(number = 1, width = 1, bitOffset = 5)
    public boolean battery_LowVoltage;

    //--//

    @JsonIgnore
    @SerializationTag(number = 2, width = 8)
    public byte byte2;

    @JsonIgnore
    @SerializationTag(number = 3, width = 8)
    public byte byte3;

    @JsonIgnore
    @SerializationTag(number = 4, width = 8)
    public byte byte4;

    @JsonIgnore
    @SerializationTag(number = 5, width = 8)
    public byte byte5;

    @JsonIgnore
    @SerializationTag(number = 6, width = 8)
    public byte byte6;

    @FieldModelDescription(description = "Fault Code", units = EngineeringUnits.ticks, pointClass = WellKnownPointClass.FaultCode, flushOnChange = true, debounceSeconds = 20,
                           stickyMode = FieldChangeMode.StickyNonZero, desiredTypeForSamples = Ipn_PalFinger_DisplayCode.class, indexed = true)
    @SerializationTag(number = 7, width = 8)
    public byte segmentDisplay;

    //--//

    @Override
    public String extractBaseId()
    {
        return "Palfinger_DigitalStatusOutputs";
    }

    @Override
    public void postProcess(IpnObjectsState state,
                            Palfinger_DigitalStatusOutputs previousValue)
    {
        // Only track the 7 segments.
        segmentDisplay = (byte) (0x7F & segmentDisplay);
    }
}
