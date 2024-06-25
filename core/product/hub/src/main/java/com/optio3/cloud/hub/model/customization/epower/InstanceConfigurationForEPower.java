/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.epower;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.model.customization.InstanceConfigurationForTransportation;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.protocol.model.WellKnownPointClassOrCustom;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.util.TimeUtils;

@JsonSubTypes({ @JsonSubTypes.Type(value = InstanceConfigurationForEPower_Amazon.class) })
public abstract class InstanceConfigurationForEPower extends InstanceConfigurationForTransportation
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
            // Don't notify if it's a problem that lasted less than a day.
            return !TimeUtils.wasUpdatedRecently(unresponsiveSince, 1, TimeUnit.DAYS);
        }

        return true;
    }

    @Override
    protected boolean shouldIncludeObjectInClassification(IpnObjectModel contents)
    {
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
