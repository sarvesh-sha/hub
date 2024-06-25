/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.util.function.Consumer;
import java.util.function.Function;

public interface IRecordWithMetadata
{
    MetadataMap getMetadata();

    boolean setMetadata(MetadataMap metadata);

    default <T> T modifyMetadata(Function<MetadataMap, T> callback)
    {
        MetadataMap metadata = getMetadata();

        T res = callback.apply(metadata);

        setMetadata(metadata);

        return res;
    }

    default boolean modifyMetadata(Consumer<MetadataMap> callback)
    {
        MetadataMap metadata = getMetadata();

        callback.accept(metadata);

        return setMetadata(metadata);
    }

    //--//

    default boolean hasMetadata(MetadataField<?> field)
    {
        return field.isPresent(getMetadata());
    }

    default <T> T getMetadata(MetadataField<T> field)
    {
        return field.get(getMetadata());
    }

    default <T> boolean putMetadata(MetadataField<T> field,
                                    T val)
    {
        return modifyMetadata(map ->
                              {
                                  field.put(map, val);
                              });
    }
}
