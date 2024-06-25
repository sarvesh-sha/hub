/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("Ipn:Satellite")
public class IpnSatellite extends IpnObjectModel
{
    public int satelliteId;

    public double elevation;
    public double azimuth;
    public double snr;

    public int totalInView;

    @JsonIgnore
    public boolean isTracked()
    {
        return Double.isFinite(elevation) && Double.isFinite(azimuth) && snr > 0;
    }

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
    protected BaseObjectModel createEmptyCopy()
    {
        IpnSatellite copy = (IpnSatellite) super.createEmptyCopy();
        copy.satelliteId = satelliteId;
        return copy;
    }

    @Override
    public String extractBaseId()
    {
        return "satellite";
    }

    @Override
    public String extractUnitId()
    {
        return Integer.toString(satelliteId);
    }

    @Override
    public boolean parseId(String id)
    {
        final String baseId = extractBaseId();
        if (StringUtils.startsWith(id, baseId))
        {
            String[] parts = StringUtils.split(id, '/');
            if (parts.length == 2 && StringUtils.equals(baseId, parts[0]))
            {
                try
                {
                    satelliteId = Integer.parseInt(parts[1]);
                    return true;
                }
                catch (NumberFormatException e)
                {
                    // Not a valid id.
                }
            }
        }

        return false;
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
