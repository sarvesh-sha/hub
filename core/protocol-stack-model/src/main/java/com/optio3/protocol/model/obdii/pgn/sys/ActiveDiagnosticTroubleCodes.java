/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.sys;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.ipn.IpnObjectPostProcess;
import com.optio3.protocol.model.ipn.IpnObjectsState;
import com.optio3.protocol.model.obdii.PgnMessageType;
import com.optio3.protocol.model.obdii.pgn.enums.PgnLampState;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:ActiveDiagnosticTroubleCodes")
@PgnMessageType(pgn = 65226, littleEndian = true)
public class ActiveDiagnosticTroubleCodes extends BaseSysPgnObjectModel
{
    @SerializationTag(number = 1, width = 2, bitOffset = 6, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public PgnLampState malfunction_indicator;

    @SerializationTag(number = 1, width = 2, bitOffset = 4, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public PgnLampState red_stop;

    @SerializationTag(number = 1, width = 2, bitOffset = 2, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public PgnLampState amber_warning;

    @SerializationTag(number = 1, width = 2, bitOffset = 0, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public PgnLampState protect;

    @SerializationTag(number = 2, width = 2, bitOffset = 6, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public PgnLampState flash_malfunction_indicator;

    @SerializationTag(number = 2, width = 2, bitOffset = 4, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public PgnLampState flash_red_stop;

    @SerializationTag(number = 2, width = 2, bitOffset = 2, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public PgnLampState flash_amber_warning;

    @SerializationTag(number = 2, width = 2, bitOffset = 0, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public PgnLampState flash_protect;

    @JsonIgnore
    @SerializationTag(number = 3, width = 8, bitOffset = 0, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int spnHi;

    @JsonIgnore
    @SerializationTag(number = 3, width = 8, bitOffset = 8, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int spnMid;

    @JsonIgnore
    @SerializationTag(number = 3, width = 3, bitOffset = 5+16, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int spnLo;

    public int spn;

    @SerializationTag(number = 3, width = 5, bitOffset = 16, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int fmi;

    @SerializationTag(number = 3, width = 7, bitOffset = 24, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int occurrence_count;

    @SerializationTag(number = 3, width = 1, bitOffset = 24+7, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public boolean spn_conversion_method;

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "ActiveDiagnosticTroubleCodes";
    }

    public void postDecodeFixup()
    {
        spn = spnLo | (spnMid << 3) | (spnHi << (3+8));
    }
}
