/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

public enum WellKnownDestination
{
    Broadcast("<<BROADCAST>>"),
    // For a broadcast message.
    Service("<<SERVICE>>"),
    // For a service message.
    Service_Broadcast("<<SERVICE BROADCAST>>"); // For a broadcast only to services.

    private final String m_id;

    WellKnownDestination(String id)
    {
        m_id = id;
    }

    public static WellKnownDestination parse(String id)
    {
        for (WellKnownDestination dst : values())
        {
            if (dst.getId()
                   .equals(id))
            {
                return dst;
            }
        }

        return null;
    }

    public String getId()
    {
        return m_id;
    }
}
