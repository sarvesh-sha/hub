/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import java.util.List;
import java.util.Objects;

import com.optio3.serialization.Reflection;

public class NormalizationTerm
{
    public String dimensionId;

    public String       name;
    public List<String> nameWords;

    public String acronym;
    public double positiveWeight;
    public double negativeWeight;

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        NormalizationTerm that = Reflection.as(o, NormalizationTerm.class);
        if (that == null)
        {
            return false;
        }

        return Objects.equals(dimensionId, that.dimensionId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(dimensionId);
    }
}
