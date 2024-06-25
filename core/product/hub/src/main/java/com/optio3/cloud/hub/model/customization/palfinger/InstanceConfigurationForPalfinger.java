/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.palfinger;

import java.time.ZonedDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.customization.InstanceConfigurationForTransportation;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.protocol.model.WellKnownPointClassOrCustom;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("InstanceConfigurationForPalfinger")
public class InstanceConfigurationForPalfinger extends InstanceConfigurationForTransportation
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
        boolean modified = false;

        if (StringUtils.isEmpty(cfg.gpsPort))
        {
            cfg.gpsPort = "/optio3-dev/optio3_gps";
            modified    = true;
        }

        if (StringUtils.isEmpty(cfg.canPort))
        {
            cfg.canPort = "can0";
            modified    = true;
        }

        return modified;
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
