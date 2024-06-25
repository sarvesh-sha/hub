/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.can;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.protocol.model.ipn.objects.hendrickson.BaseHendricksonModel;
import com.optio3.protocol.model.ipn.objects.nitephoenix.BaseNitePhoenixModel;
import com.optio3.protocol.model.ipn.objects.palfinger.BasePalfingerModel;
import com.optio3.protocol.model.obdii.ObdiiObjectModel;

@JsonSubTypes({ @JsonSubTypes.Type(value = ObdiiObjectModel.class),
                @JsonSubTypes.Type(value = BaseHendricksonModel.class),
                @JsonSubTypes.Type(value = BaseNitePhoenixModel.class),
                @JsonSubTypes.Type(value = BasePalfingerModel.class) })
public abstract class CanObjectModel extends IpnObjectModel
{
    public void initializeFromAnnotation(CanMessageType anno)
    {
        // Nothing to do.
    }

    public void initializeFromAnnotation(CanExtendedMessageType annoExt)
    {
        // Nothing to do.
    }
}
