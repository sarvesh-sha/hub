/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.serialization.ObjectMappers;

public class RawImport
{
    public String contentsAsJSON;

    public <T> T validate(Class<T> clz)
    {
        try
        {
            return ObjectMappers.SkipNulls.readValue(contentsAsJSON, clz);
        }
        catch (Throwable t)
        {
            throw new InvalidArgumentException("Invalid import format");
        }
    }

    public <T> T validate(TypeReference<T> typeRef)
    {
        try
        {
            return ObjectMappers.SkipNulls.readValue(contentsAsJSON, typeRef);
        }
        catch (Throwable t)
        {
            throw new InvalidArgumentException("Invalid import format");
        }
    }
}
