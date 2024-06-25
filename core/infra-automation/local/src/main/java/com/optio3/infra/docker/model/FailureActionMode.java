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

public enum FailureActionMode
{
    CONTINUE(String.valueOf("continue")),
    PAUSE(String.valueOf("pause")),
    ROLLBACK(String.valueOf("rollback"));

    private String value;

    FailureActionMode(String v)
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

    public static FailureActionMode fromValue(String v)
    {
        for (FailureActionMode b : FailureActionMode.values())
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
