/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.sys;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.obdii.pgn.BasePgnObjectModel;

@JsonSubTypes({ @JsonSubTypes.Type(value = ActiveDiagnosticTroubleCodes.class),
                @JsonSubTypes.Type(value = Proprietary.class),
                @JsonSubTypes.Type(value = SoftwareIdentification.class),
                @JsonSubTypes.Type(value = SysAcknowledgmentMessage.class),
                @JsonSubTypes.Type(value = SysAddressClaimed.class),
                @JsonSubTypes.Type(value = SysIsoTransportLayer.class),
                @JsonSubTypes.Type(value = SysIsoTransportLayerRequest.class),
                @JsonSubTypes.Type(value = SysRequest.class),
                @JsonSubTypes.Type(value = SysTransportProtocol.class),
                @JsonSubTypes.Type(value = SysTransportProtocolDataTransfer.class) })
public abstract class BaseSysPgnObjectModel extends BasePgnObjectModel
{
    public boolean shouldIncludeObject()
    {
        // Ignore all system messages.
        return false;
    }

    public boolean shouldIncludeProperty(String prop)
    {
        // Ignore all properties.
        return false;
    }
}
