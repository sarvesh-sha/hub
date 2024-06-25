/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import java.util.List;

public class NormalizationEquipmentLocations
{
    public List<NormalizationEquipmentLocation> locations;

    public static NormalizationEquipmentLocations wrap(List<NormalizationEquipmentLocation> locations)
    {
        if (locations == null || locations.isEmpty())
        {
            return null;
        }

        NormalizationEquipmentLocations result = new NormalizationEquipmentLocations();
        result.locations = locations;
        return result;
    }

    public static List<NormalizationEquipmentLocation> unwrap(NormalizationEquipmentLocations locationWrapper)
    {
        return locationWrapper != null ? locationWrapper.locations : null;
    }
}
