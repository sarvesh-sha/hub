/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.serialization;

import java.util.Optional;

public abstract class SerializationValueProcessor
{
    public abstract Optional<Object> handle(SerializablePiece piece,
                                            Object value);
}