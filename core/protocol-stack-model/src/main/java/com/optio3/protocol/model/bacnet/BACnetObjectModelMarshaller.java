/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet;

import com.optio3.logging.ILogger;
import com.optio3.serialization.ObjectMappers;

public interface BACnetObjectModelMarshaller
{
    default BACnetObjectModel allocateObject()
    {
        return allocateObject(BACnetObjectModel.class);
    }

    default <T extends BACnetObjectModel> T toObject(Class<T> clz)
    {
        T obj = allocateObject(clz);

        updateObjectNoLog(obj);

        return obj;
    }

    <T extends BACnetObjectModel> T allocateObject(Class<T> clz);

    void updateObjectNoLog(BACnetObjectModel target);

    default void updateObject(ILogger logger,
                              BACnetObjectModel target)
    {
        if (logger != null)
        {
            try
            {
                updateObjectNoLog(target);
            }
            catch (Throwable t)
            {
                logger.error("updateObject: failed with %s", t.getMessage());
                logger.error("%s", ObjectMappers.prettyPrintAsJson(this));

                throw t;
            }
        }
        else
        {
            updateObjectNoLog(target);
        }
    }
}
