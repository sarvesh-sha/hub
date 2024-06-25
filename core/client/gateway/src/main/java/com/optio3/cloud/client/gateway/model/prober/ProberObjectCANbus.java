/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model.prober;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("ProberObjectCANbus")
public class ProberObjectCANbus extends ProberObject
{
    public ZonedDateTime timestamp;

    @Override
    protected int compareObjectId(ProberObject o)
    {
        return StringUtils.compare(this.objectId, o.objectId);
    }
}