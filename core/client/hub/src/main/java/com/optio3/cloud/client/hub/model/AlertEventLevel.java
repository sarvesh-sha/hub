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

public enum AlertEventLevel
{
    failure(String.valueOf("failure")),
    info(String.valueOf("info")),
    recall(String.valueOf("recall")),
    success(String.valueOf("success")),
    certification(String.valueOf("certification"));

    private String value;

    AlertEventLevel(String v)
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
    public static AlertEventLevel fromValue(String v)
    {
        for (AlertEventLevel b : AlertEventLevel.values())
        {
            if (String.valueOf(b.value).equals(v))
                return b;
        }

        throw new IllegalArgumentException(String.format("'%s' is not one of the values accepted for %s class: %s", v, AlertEventLevel.class.getSimpleName(), Arrays.toString(AlertEventLevel.values())));
    }
}
