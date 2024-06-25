/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Api documentation
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.client.reporter.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReportStatus
{
    UNKNOWN(String.valueOf("UNKNOWN")),
    QUEUED(String.valueOf("QUEUED")),
    PROCESSING(String.valueOf("PROCESSING")),
    SUCCESS(String.valueOf("SUCCESS")),
    FAILURE(String.valueOf("FAILURE"));

    private String value;

    ReportStatus(String v)
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
    public static ReportStatus fromValue(String v)
    {
        for (ReportStatus b : ReportStatus.values())
        {
            if (String.valueOf(b.value).equals(v))
                return b;
        }

        throw new IllegalArgumentException(String.format("'%s' is not one of the values accepted for %s class: %s", v, ReportStatus.class.getSimpleName(), Arrays.toString(ReportStatus.values())));
    }
}