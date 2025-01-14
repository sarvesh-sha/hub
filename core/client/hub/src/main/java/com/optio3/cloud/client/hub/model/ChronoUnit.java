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

public enum ChronoUnit
{
    NANOS(String.valueOf("NANOS")),
    MICROS(String.valueOf("MICROS")),
    MILLIS(String.valueOf("MILLIS")),
    SECONDS(String.valueOf("SECONDS")),
    MINUTES(String.valueOf("MINUTES")),
    HOURS(String.valueOf("HOURS")),
    HALF_DAYS(String.valueOf("HALF_DAYS")),
    DAYS(String.valueOf("DAYS")),
    WEEKS(String.valueOf("WEEKS")),
    MONTHS(String.valueOf("MONTHS")),
    YEARS(String.valueOf("YEARS")),
    DECADES(String.valueOf("DECADES")),
    CENTURIES(String.valueOf("CENTURIES")),
    MILLENNIA(String.valueOf("MILLENNIA")),
    ERAS(String.valueOf("ERAS")),
    FOREVER(String.valueOf("FOREVER"));

    private String value;

    ChronoUnit(String v)
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
    public static ChronoUnit fromValue(String v)
    {
        for (ChronoUnit b : ChronoUnit.values())
        {
            if (String.valueOf(b.value).equals(v))
                return b;
        }

        throw new IllegalArgumentException(String.format("'%s' is not one of the values accepted for %s class: %s", v, ChronoUnit.class.getSimpleName(), Arrays.toString(ChronoUnit.values())));
    }
}
