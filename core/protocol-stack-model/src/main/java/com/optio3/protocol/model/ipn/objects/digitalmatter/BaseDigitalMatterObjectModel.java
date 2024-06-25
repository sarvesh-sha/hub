/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.digitalmatter;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.ipn.IpnObjectModel;

@JsonSubTypes({ @JsonSubTypes.Type(value = DigitalMatter_AnalogState.class), @JsonSubTypes.Type(value = DigitalMatter_Counters.class), @JsonSubTypes.Type(value = DigitalMatter_DigitalState.class) })
public abstract class BaseDigitalMatterObjectModel extends IpnObjectModel
{
    @Override
    public boolean shouldCommitReachabilityChange(boolean isReachable,
                                                  ZonedDateTime lastReachable)
    {
        return false;
    }
}
