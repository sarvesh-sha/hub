/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Provisioner APIs
 * APIs and Definitions for the Optio3 Provisioner product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.provisioner.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ProvisionTestResult
{
    Skipped(String.valueOf("Skipped")),
    Passed(String.valueOf("Passed")),
    Failed(String.valueOf("Failed"));

    private String value;

    ProvisionTestResult(String v)
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
    public static ProvisionTestResult fromValue(String v)
    {
        for (ProvisionTestResult b : ProvisionTestResult.values())
        {
            if (String.valueOf(b.value).equals(v))
                return b;
        }

        throw new IllegalArgumentException(String.format("'%s' is not one of the values accepted for %s class: %s", v, ProvisionTestResult.class.getSimpleName(), Arrays.toString(ProvisionTestResult.values())));
    }
}
