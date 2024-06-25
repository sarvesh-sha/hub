/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.bluesky;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.ipn.enums.IpnAuxiliaryFET;
import com.optio3.protocol.model.ipn.enums.IpnAuxiliaryMode;
import com.optio3.protocol.model.ipn.enums.IpnChargeState;
import com.optio3.protocol.model.ipn.enums.IpnCompletedCycles;
import com.optio3.protocol.model.ipn.enums.IpnCycleKind;
import com.optio3.protocol.model.ipn.enums.IpnEqualizeHistory;
import com.optio3.protocol.model.ipn.enums.IpnPowerSource;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:MasterRequest")
public class BlueSky_MasterRequest extends BaseBlueSkyObjectModel
{
    @Override
    public String extractBaseId()
    {
        return "masterRequest";
    }

    @Override
    public boolean postDecodingValidation()
    {
        return true;
    }
}
