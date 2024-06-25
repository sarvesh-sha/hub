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

public enum IdentityDocumentVerificationFlavor
{
    APPROVED(String.valueOf("APPROVED")),
    REJECTED(String.valueOf("REJECTED")),
    PENDING(String.valueOf("PENDING"));

    private String value;

    IdentityDocumentVerificationFlavor(String v)
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

    public static IdentityDocumentVerificationFlavor fromValue(String v)
    {
        for (IdentityDocumentVerificationFlavor b : IdentityDocumentVerificationFlavor.values())
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