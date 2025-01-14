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

public enum LogSeverity
{
    Error(String.valueOf("Error")),
    Warn(String.valueOf("Warn")),
    Info(String.valueOf("Info")),
    Debug(String.valueOf("Debug")),
    DebugVerbose(String.valueOf("DebugVerbose")),
    DebugObnoxious(String.valueOf("DebugObnoxious"));

    private String value;

    LogSeverity(String v)
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
    public static LogSeverity fromValue(String v)
    {
        for (LogSeverity b : LogSeverity.values())
        {
            if (String.valueOf(b.value).equals(v))
                return b;
        }

        throw new IllegalArgumentException(String.format("'%s' is not one of the values accepted for %s class: %s", v, LogSeverity.class.getSimpleName(), Arrays.toString(LogSeverity.values())));
    }
}
