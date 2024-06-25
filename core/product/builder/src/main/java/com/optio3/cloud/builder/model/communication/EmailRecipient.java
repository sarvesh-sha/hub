/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.communication;

public class EmailRecipient
{
    public String name;
    public String address;

    public void substituteDomain(String newDomain)
    {
        int pos = address.indexOf("@");
        if (pos > 0)
        {
            address = address.substring(0, pos + 1) + newDomain;
        }
    }
}
