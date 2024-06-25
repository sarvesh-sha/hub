/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.Resources;

public class ValidationRules
{
    public List<ValidationEquipmentRule> equipmentRules = Lists.newArrayList();

    public List<ValidationPointClassRule> pointClassRules = Lists.newArrayList();

    public static ValidationRules load() throws
                                         Exception
    {
        try (InputStream stream = Resources.openResourceAsStream(EquipmentClass.class, "normalization/McKinstryValidationRules.json"))
        {
            return ObjectMappers.SkipNullsCaseInsensitive.readValue(stream, new TypeReference<ValidationRules>()
            {
            });
        }
    }
}
