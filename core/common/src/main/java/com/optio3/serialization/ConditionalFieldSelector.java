/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.serialization;

import java.util.Optional;

import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public interface ConditionalFieldSelector
{
    boolean shouldEncode(String fieldName);

    boolean shouldDecode(String fieldName);

    default boolean encodeValue(String fieldName,
                                OutputBuffer buffer,
                                Object value)
    {
        return false;
    }

    default Optional<Object> provideValue(String fieldName,
                                          InputBuffer buffer)
    {
        return Optional.empty();
    }
}
