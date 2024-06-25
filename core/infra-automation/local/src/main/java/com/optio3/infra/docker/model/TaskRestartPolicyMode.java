/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Docker Engine API
 * The Engine API is an HTTP API served by Docker Engine. It is the API the Docker client uses to communicate with the Engine, so everything the Docker client can do can be done with the API.
 *
 * OpenAPI spec version: 1.28
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.infra.docker.model;

public enum TaskRestartPolicyMode
{
    NONE(String.valueOf("none")),
    ON_FAILURE(String.valueOf("on-failure")),
    ANY(String.valueOf("any"));

    private String value;

    TaskRestartPolicyMode(String v)
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

    public static TaskRestartPolicyMode fromValue(String v)
    {
        for (TaskRestartPolicyMode b : TaskRestartPolicyMode.values())
        {
            if (String.valueOf(b.value)
                      .equals(v))
            {
                return b;
            }
        }

        return null;
    }
}
