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

public enum DeploymentOperationalStatus
{
    factoryFloor(String.valueOf("factoryFloor")),
    provisioned(String.valueOf("provisioned")),
    installationPending(String.valueOf("installationPending")),
    offline(String.valueOf("offline")),
    idle(String.valueOf("idle")),
    operational(String.valueOf("operational")),
    maintenance(String.valueOf("maintenance")),
    lostConnectivity(String.valueOf("lostConnectivity")),
    storageCorruption(String.valueOf("storageCorruption")),
    RMA_warranty(String.valueOf("RMA_warranty")),
    RMA_nowarranty(String.valueOf("RMA_nowarranty")),
    retired(String.valueOf("retired"));

    private String value;

    DeploymentOperationalStatus(String v)
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
    public static DeploymentOperationalStatus fromValue(String v)
    {
        for (DeploymentOperationalStatus b : DeploymentOperationalStatus.values())
        {
            if (String.valueOf(b.value).equals(v))
                return b;
        }

        throw new IllegalArgumentException(String.format("'%s' is not one of the values accepted for %s class: %s", v, DeploymentOperationalStatus.class.getSimpleName(), Arrays.toString(DeploymentOperationalStatus.values())));
    }
}
