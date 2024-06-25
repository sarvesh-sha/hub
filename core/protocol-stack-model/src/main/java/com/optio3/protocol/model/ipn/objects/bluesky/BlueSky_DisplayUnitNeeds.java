/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.bluesky;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.serialization.SerializationTag;

//
// Byte 7: display unit’s needs/requests
//      0x10: start equalize now
//      0x20: stop equalize now
//      0x30: clear total charge amp-hours
//      0x40: request master to transmit its setpoints
//      0x50: reserved
//      0x60: display unit has new setpoints to send
//      0x70: master is to restore default setpoints
//      0x80: display unit has new post-dusk and/or pre-dawn hours
//      (Alternative 0x80 DC Mode only: display unit has new input current limit to send)
//      0x90: disable MPPT for DC power source
//      0xA0: enable normal MPPT operation
// Byte 8: LSB of battery amp-hours from full (used for load control if amp-hour mode selected)
// Byte 9: MSB of battery amp-hours from full (used for load control if amp-hour mode selected)
// Byte 10: flags
//      Bits 7,6,5 & 4: reserved (set to zero’s)
//      Bit 3: 1=display has seen valid charge/discharge cycle, is displaying/sending amp-hours, and load control can use amp-hours
//      Bit 2: 1=battery amp-hours from full has gone to zero
//      Bit 1: 1=net battery current as seen by the display unit is negative
//      Bit 0: 1=net battery current is low enough to enter float

@JsonTypeName("Ipn:DisplayUnitNeeds")
public class BlueSky_DisplayUnitNeeds extends BaseBlueSkyObjectModel
{
    @SerializationTag(number = 7)
    public byte requests;

    @SerializationTag(number = 8)
    public short batteryAmpHoursFromFull;

    @SerializationTag(number = 10, width = 1, bitOffset = 3)
    public boolean displayCycle; // 1=display has seen valid charge/discharge cycle, is displaying/sending amp-hours, and load control can use amp-hours

    @SerializationTag(number = 10, width = 1, bitOffset = 2)
    public boolean batteryFull; // 1=battery amp-hours from full has gone to zero

    @SerializationTag(number = 10, width = 1, bitOffset = 1)
    public boolean negativeNetCurrent; // 1=net battery current as seen by the display unit is negative

    @SerializationTag(number = 10, width = 1, bitOffset = 0)
    public boolean lowNetCurrent; // 1=net battery current is low enough to enter float

    //--//

    @Override
    public String extractBaseId()
    {
        return "displayUnitNeeds";
    }

    @Override
    public boolean postDecodingValidation()
    {
        boolean ok = true;

        ok &= isAcceptableRange(batteryAmpHoursFromFull, 0, 32768);

        return ok;
    }
}
