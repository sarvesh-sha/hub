/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.merlinsolar;

import java.time.ZonedDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.customization.InstanceConfigurationForTransportation;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.protocol.model.WellKnownPointClassOrCustom;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.IpnObjectModel;

@JsonTypeName("InstanceConfigurationForMerlinSolar")
public class InstanceConfigurationForMerlinSolar extends InstanceConfigurationForTransportation
{
    @Override
    public boolean hasRoamingAssets()
    {
        return true;
    }

    @Override
    public boolean shouldReportWhenUnreachable(DeviceRecord rec,
                                               ZonedDateTime unresponsiveSince)
    {
        return shouldReportWhenUnreachableImpl(rec, unresponsiveSince);
    }

    @Override
    public boolean fixupAutoConfig(ProtocolConfigForIpn cfg)
    {
        // No fixup needed.
        return false;
    }

    @Override
    protected boolean shouldIncludeObjectInClassification(IpnObjectModel contents)
    {
        // Include everything
        return true;
    }

    @Override
    protected boolean shouldBeSingletonInClassification(WellKnownPointClassOrCustom pointClass,
                                                        Set<String> pointTags)
    {
        // Only one entry per point class.
        return true;
    }
}
