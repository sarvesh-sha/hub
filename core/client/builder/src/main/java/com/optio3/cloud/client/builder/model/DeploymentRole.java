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

public enum DeploymentRole
{
    test(String.valueOf("test")),
    builder(String.valueOf("builder")),
    deployer(String.valueOf("deployer")),
    database(String.valueOf("database")),
    database_backup(String.valueOf("database_backup")),
    hub(String.valueOf("hub")),
    gateway(String.valueOf("gateway")),
    prober(String.valueOf("prober")),
    waypoint(String.valueOf("waypoint")),
    tracker(String.valueOf("tracker")),
    reporter(String.valueOf("reporter")),
    provisioner(String.valueOf("provisioner")),
    bridge_BACnetP2(String.valueOf("bridge_BACnetP2"));

    private String value;

    DeploymentRole(String v)
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
    public static DeploymentRole fromValue(String v)
    {
        for (DeploymentRole b : DeploymentRole.values())
        {
            if (String.valueOf(b.value).equals(v))
                return b;
        }

        throw new IllegalArgumentException(String.format("'%s' is not one of the values accepted for %s class: %s", v, DeploymentRole.class.getSimpleName(), Arrays.toString(DeploymentRole.values())));
    }
}
