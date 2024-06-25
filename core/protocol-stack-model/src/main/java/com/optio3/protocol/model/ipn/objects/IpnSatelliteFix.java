/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects;

import java.time.ZonedDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("Ipn:SatelliteFix")
public class IpnSatelliteFix extends IpnObjectModel
{
    public enum FixMode
    {
        Unavailable,
        TwoD,
        ThreeD
    }

    public FixMode      fixMode;
    public Set<Integer> fixSet;
    public double       pdop;
    public double       hdop;
    public double       vdop;

    //--//

    @Override
    public boolean shouldIncludeObject()
    {
        return false;
    }

    @Override
    public boolean shouldIncludeProperty(String prop)
    {
        return false;
    }

    @Override
    public boolean shouldCommitReachabilityChange(boolean isReachable,
                                                  ZonedDateTime lastReachable)
    {
        return false;
    }

    //--//

    @Override
    public String extractBaseId()
    {
        return "satelliteFix";
    }

    //--//

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        // Not sent to server.
    }
}
