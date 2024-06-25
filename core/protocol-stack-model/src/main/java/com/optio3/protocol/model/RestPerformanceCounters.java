/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optio3.serialization.ObjectMappers;

public class RestPerformanceCounters extends BaseObjectModel
{
    @FieldModelDescription(description = "Requests", units = EngineeringUnits.counts)
    public int requests;

    @FieldModelDescription(description = "Execution Time", units = EngineeringUnits.milliseconds)
    public long executionTime;

    @FieldModelDescription(description = "Bytes Read", units = EngineeringUnits.bytes)
    public long bytesRead;

    @FieldModelDescription(description = "Bytes Written", units = EngineeringUnits.bytes)
    public long bytesWritten;

    @FieldModelDescription(description = "Success", units = EngineeringUnits.counts)
    public long statusSuccess;

    @FieldModelDescription(description = "Client Errors", units = EngineeringUnits.counts)
    public long statusClientError;

    @FieldModelDescription(description = "Server Errors", units = EngineeringUnits.counts)
    public long statusServerError;

    //--//

    public static RestPerformanceCounters deserializeFromJson(String json) throws
                                                                           IOException
    {
        return deserializeInner(getObjectMapper(), RestPerformanceCounters.class, json);
    }

    @Override
    public ObjectMapper getObjectMapperForInstance()
    {
        return getObjectMapper();
    }

    public static ObjectMapper getObjectMapper()
    {
        return ObjectMappers.SkipDefaults;
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        // Not classified.
    }
}
