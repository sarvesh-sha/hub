/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.serialization;

public abstract class TypeDescriptorFactory
{
    public abstract TypeDescriptor create(Class<?> clz);
}
