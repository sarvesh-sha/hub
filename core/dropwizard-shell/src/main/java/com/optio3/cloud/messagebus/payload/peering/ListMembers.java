/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload.peering;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ListMembers")
public class ListMembers extends Peering
{
    public List<String> brokersPath; // Used to control fan-out of the request

    public String channel;

    //--//

    public boolean alreadyVisited(String brokerId)
    {
        return brokersPath != null && brokersPath.contains(brokerId);
    }
}
