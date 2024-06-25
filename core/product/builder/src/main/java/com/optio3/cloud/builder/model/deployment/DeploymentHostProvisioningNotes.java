/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;

import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

public class DeploymentHostProvisioningNotes
{
    public String        sysId;
    public ZonedDateTime timestamp;
    public String        customerInfo;
    public String        text;
    public boolean       readyForProduction;
    public boolean       readyForShipping;
    public boolean       deployed;

    @Override
    public boolean equals(Object o)
    {
        DeploymentHostProvisioningNotes other = Reflection.as(o, DeploymentHostProvisioningNotes.class);
        if (other != null)
        {
            if (readyForProduction == other.readyForProduction && readyForShipping == other.readyForShipping && deployed == other.deployed)
            {
                return StringUtils.equalsIgnoreCase(customerInfo, other.customerInfo) && StringUtils.equalsIgnoreCase(text, other.text);
            }
        }

        return false;
    }
}
