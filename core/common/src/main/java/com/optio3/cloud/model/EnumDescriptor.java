/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.util.CollectionUtils;

public class EnumDescriptor
{
    public String id;
    public String displayName;
    public String description;

    //--//

    public static <T extends IEnumDescription> List<EnumDescriptor> describe(List<T> values)
    {
        List<EnumDescriptor> res = Lists.newArrayList();

        if (values == null)
        {
            return Collections.emptyList();
        }

        return CollectionUtils.transformToListNoNulls(values, (t) ->
        {
            EnumDescriptor obj = new EnumDescriptor();
            obj.id = t.name();
            obj.displayName = t.getDisplayName();
            obj.description = t.getDescription();
            return obj.displayName != null ? obj : null;
        });
    }
}
