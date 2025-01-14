/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * api.godaddy.com
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 2.4.9
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.infra.godaddy.model;

public enum IncludeKind
{
    FAST(String.valueOf("FAST")),
    FULL(String.valueOf("FULL"));

    private String value;

    IncludeKind(String v)
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

    public static IncludeKind fromValue(String v)
    {
        for (IncludeKind b : IncludeKind.values())
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
