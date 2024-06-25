/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Builder APIs
 * APIs and Definitions for the Optio3 Builder product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.builder.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DetailedApplicationExceptionCode
{
    ALREADY_EXISTS(String.valueOf("ALREADY_EXISTS")),
    NOT_AUTHENTICATED(String.valueOf("NOT_AUTHENTICATED")),
    NOT_AUTHORIZED(String.valueOf("NOT_AUTHORIZED")),
    NOT_FOUND(String.valueOf("NOT_FOUND")),
    NOT_IMPLEMENTED(String.valueOf("NOT_IMPLEMENTED")),
    INVALID_ARGUMENT(String.valueOf("INVALID_ARGUMENT")),
    INVALID_STATE(String.valueOf("INVALID_STATE"));

    private String value;

    DetailedApplicationExceptionCode(String v)
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
    public static DetailedApplicationExceptionCode fromValue(String v)
    {
        for (DetailedApplicationExceptionCode b : DetailedApplicationExceptionCode.values())
        {
            if (String.valueOf(b.value).equals(v))
                return b;
        }

        throw new IllegalArgumentException(String.format("'%s' is not one of the values accepted for %s class: %s", v, DetailedApplicationExceptionCode.class.getSimpleName(), Arrays.toString(DetailedApplicationExceptionCode.values())));
    }
}
