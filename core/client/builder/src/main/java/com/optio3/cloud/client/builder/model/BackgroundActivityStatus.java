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

public enum BackgroundActivityStatus
{
    ACTIVE(String.valueOf("ACTIVE")),
    ACTIVE_BUT_CANCELLING(String.valueOf("ACTIVE_BUT_CANCELLING")),
    PAUSED(String.valueOf("PAUSED")),
    PAUSED_BUT_CANCELLING(String.valueOf("PAUSED_BUT_CANCELLING")),
    WAITING(String.valueOf("WAITING")),
    WAITING_BUT_CANCELLING(String.valueOf("WAITING_BUT_CANCELLING")),
    SLEEPING(String.valueOf("SLEEPING")),
    SLEEPING_BUT_CANCELLIN(String.valueOf("SLEEPING_BUT_CANCELLIN")),
    EXECUTING(String.valueOf("EXECUTING")),
    EXECUTING_BUT_CANCELLING(String.valueOf("EXECUTING_BUT_CANCELLING")),
    CANCELLED(String.valueOf("CANCELLED")),
    COMPLETED(String.valueOf("COMPLETED")),
    FAILED(String.valueOf("FAILED"));

    private String value;

    BackgroundActivityStatus(String v)
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
    public static BackgroundActivityStatus fromValue(String v)
    {
        for (BackgroundActivityStatus b : BackgroundActivityStatus.values())
        {
            if (String.valueOf(b.value).equals(v))
                return b;
        }

        throw new IllegalArgumentException(String.format("'%s' is not one of the values accepted for %s class: %s", v, BackgroundActivityStatus.class.getSimpleName(), Arrays.toString(BackgroundActivityStatus.values())));
    }
}
