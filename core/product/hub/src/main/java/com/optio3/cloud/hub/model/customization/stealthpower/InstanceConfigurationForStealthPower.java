/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.stealthpower;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.customization.InstanceConfigurationForTransportation;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.protocol.model.WellKnownPointClassOrCustom;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.protocol.model.obdii.ObdiiObjectModel;
import com.optio3.serialization.Reflection;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("InstanceConfigurationForStealthPower")
public class InstanceConfigurationForStealthPower extends InstanceConfigurationForTransportation
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
        if (isLocationRecord(rec))
        {
            // Ignore all location warnings.
            return false;
        }

        // Don't notify if it's a problem that lasted less than a day.
        return !TimeUtils.wasUpdatedRecently(unresponsiveSince, 1, TimeUnit.DAYS);
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

        if (StringUtils.isEmpty(cfg.obdiiPort))
        {
            cfg.obdiiPort      = "/optio3-dev/optio3_obdii";
            cfg.obdiiFrequency = 115200;
            modified           = true;
        }

        if (StringUtils.isEmpty(cfg.stealthpowerPort))
        {
            cfg.stealthpowerPort = "/optio3-dev/optio3_RS232";
            modified             = true;
        }

        return modified;
    }

    @Override
    protected boolean shouldIncludeObjectInClassification(IpnObjectModel contents)
    {
        ObdiiObjectModel obd = Reflection.as(contents, ObdiiObjectModel.class);
        if (obd != null)
        {
            // Only include items from this address.
            return obd.sourceAddress == 8;
        }

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
