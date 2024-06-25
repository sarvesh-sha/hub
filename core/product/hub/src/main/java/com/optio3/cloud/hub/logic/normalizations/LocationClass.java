/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.hub.model.location.LocationType;
import org.apache.commons.lang3.StringUtils;

public class LocationClass
{
    public LocationType id;
    public String       description;
    public String       azureDigitalTwin;
    public List<String> tags;

    @JsonIgnore
    public String idAsString()
    {
        return id.name();
    }

    //--//

    public static int compare(LocationClass a,
                              LocationClass b,
                              boolean ascending)
    {
        int diff = 0;

        if (a != null && b != null)
        {
            diff = StringUtils.compareIgnoreCase(a.id.name(), b.id.name());
        }
        else if (a != null)
        {
            diff = -1;
        }
        else if (b != null)
        {
            diff = 1;
        }

        return ascending ? diff : -diff;
    }
}
