/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.serialization.ObjectMappers;
import org.apache.commons.lang3.StringUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = ProtocolConfigForBACnet.class), @JsonSubTypes.Type(value = ProtocolConfigForIpn.class) })
public abstract class ProtocolConfig
{
    // Since it's transient, don't use it to compare two configurations.
    public String samplingConfigurationId;

    public abstract boolean equals(ProtocolConfig other);

    protected boolean equalsThroughJson(ProtocolConfig other)
    {
        if (other == null)
        {
            return false;
        }

        // Volatile fields, temporarily reset them, so they get ignored.
        String aId = this.samplingConfigurationId;
        String bId = other.samplingConfigurationId;
        this.samplingConfigurationId = null;
        other.samplingConfigurationId = null;

        String jsonThis = ObjectMappers.prettyPrintAsJson(this);
        String jsonThat = ObjectMappers.prettyPrintAsJson(other);

        boolean res = StringUtils.equals(jsonThis, jsonThat);

        this.samplingConfigurationId = aId;
        other.samplingConfigurationId = bId;

        return res;
    }
}
