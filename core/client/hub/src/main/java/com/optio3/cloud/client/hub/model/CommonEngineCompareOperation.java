/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Hub APIs
 * APIs and Definitions for the Optio3 Hub product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.hub.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CommonEngineCompareOperation
{
    Equal(String.valueOf("Equal")),
    NotEqual(String.valueOf("NotEqual")),
    LessThan(String.valueOf("LessThan")),
    LessThanOrEqual(String.valueOf("LessThanOrEqual")),
    GreaterThan(String.valueOf("GreaterThan")),
    GreaterThanOrEqual(String.valueOf("GreaterThanOrEqual"));

    private String value;

    CommonEngineCompareOperation(String v)
    {
        value = v;
    }

    public String value()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return String.valueOf(value);
    }

    @JsonCreator
    public static CommonEngineCompareOperation fromValue(String v)
    {
        for (CommonEngineCompareOperation b : CommonEngineCompareOperation.values())
        {
            if (String.valueOf(b.value).equals(v))
                return b;
        }

        throw new IllegalArgumentException(String.format("'%s' is not one of the values accepted for %s class: %s", v, CommonEngineCompareOperation.class.getSimpleName(), Arrays.toString(CommonEngineCompareOperation.values())));
    }
}
