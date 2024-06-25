/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueLocation;
import com.optio3.cloud.hub.model.location.LocationType;

public class NormalizationEquipmentLocation
{
    public String name;

    public LocationType type;

    public static NormalizationEquipmentLocation fromEngineLocation(NormalizationEngineValueLocation engineLocation)
    {
        if (engineLocation == null)
        {
            return null;
        }

        NormalizationEquipmentLocation location = new NormalizationEquipmentLocation();
        location.name = engineLocation.name;
        location.type = engineLocation.type;
        return location;
    }
}
