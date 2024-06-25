/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

public interface ModelMapperTarget<T, TMeta>
{
    default T toModelOverride(SessionHolder sessionHolder,
                              ModelMapperPolicy policy,
                              T model)
    {
        return model;
    }

    default void fromModelOverride(SessionHolder sessionHolder,
                                   ModelMapperPolicy policy,
                                   T model)
    {
    }
}
