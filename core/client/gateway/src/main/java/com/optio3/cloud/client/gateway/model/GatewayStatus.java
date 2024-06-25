/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class GatewayStatus
{
    // Use String instead of GatewayFeature, to make it backward compatible, in case we remove features.
    public Set<String> supportedFeatures;

    /**
     * A unique id assigned to the Gateway, to distinguish it from other gateways running on the same hub.
     */
    public String instanceId;

    //--//

    public final List<GatewayNetwork> networks = Lists.newArrayList();

    //--//

    public int  availableProcessors;
    public long maxMemory;
    public long freeMemory;
    public long totalMemory;

    public int hardwareVersion;
    public int firmwareVersion;

    public Map<String, String> networkInterfaces = Maps.newHashMap();

    public GatewayQueueStatus queueStatus;

    // TODO: UPGRADE PATCH: Legacy fixup to remove unused variables field
    public void setPerfCounters(Object val)
    {
    }

    //--//

    @JsonIgnore
    public List<GatewayFeature> getSupportedKnownFeatures()
    {
        List<GatewayFeature> lst = Lists.newArrayList();

        if (supportedFeatures != null)
        {
            for (String supportedFeature : supportedFeatures)
            {
                try
                {
                    lst.add(GatewayFeature.valueOf(supportedFeature));
                }
                catch (IllegalArgumentException e)
                {
                    // Old feature, ignore.
                }
            }
        }

        return lst;
    }

    //--//

    public boolean canSupport(GatewayFeature... features)
    {
        for (GatewayFeature feature : features)
        {
            if (supportedFeatures == null || !supportedFeatures.contains(feature.name()))
            {
                return false;
            }
        }

        return true;
    }
}
