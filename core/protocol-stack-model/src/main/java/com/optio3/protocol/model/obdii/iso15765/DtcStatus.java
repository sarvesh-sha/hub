/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.iso15765;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.obdii.Iso15765MessageType;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:ISO15765:DtcStatus")
@Iso15765MessageType(service = 3, pdu = -1, hasMultipleFrames = true)
public class DtcStatus extends BaseIso15765ObjectModel
{
    private static final String[] c_lookup     = { "P0", "P1", "P2", "P3", "C0", "C1", "C2", "C3", "B0", "B1", "B2", "B3", "U0", "U1", "U2", "U3" };
    private final static String[] s_emptyCodes = new String[0];

    @JsonIgnore
    @SerializationTag(number = 0)
    public Unsigned8 count;

    @JsonIgnore
    @SerializationTag(number = 1)
    public Unsigned16[] dtc;

    @FieldModelDescription(description = "Fault Codes", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.ObdiiFaultCodes, debounceSeconds = 5, indexed = true)
    public String[] fault_codes = s_emptyCodes;

    //--//

    @Override
    public String extractBaseId()
    {
        return "ISO15765_DtcStatus";
    }

    @Override
    public void postDecodeFixup()
    {
        if (count != null && dtc != null)
        {
            List<String> codes = Lists.newArrayList();

            for (int i = 0; i < count.unbox() && i < dtc.length; i++)
            {
                int val = dtc[i].unboxUnsigned();
                if (val != 0)
                {
                    codes.add(String.format("%s%03X", c_lookup[(val >> 12) & 0xF], val & 0xFFF));
                }
            }

            codes.sort(String::compareTo);
            fault_codes = new String[codes.size()];
            codes.toArray(fault_codes);
        }
    }
}
