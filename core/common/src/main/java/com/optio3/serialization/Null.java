/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.serialization;

import com.fasterxml.jackson.annotation.JsonValue;

public final class Null
{
    public static final Null instance = new Null();

    private Null()
    {
    }

    @JsonValue
    private String toJsonValue()
    {
        return null;
    }
}
