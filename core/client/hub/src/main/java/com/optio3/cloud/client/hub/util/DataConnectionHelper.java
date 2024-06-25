/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.hub.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.cloud.client.hub.api.DataConnectionApi;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;

public class DataConnectionHelper
{
    public static <I, O> O callEndpoint(DataConnectionApi proxy,
                                        String endpointId,
                                        String endpointArg,
                                        I body,
                                        Class<O> clzOutput)
    {
        JsonNode res = proxy.receiveRaw(endpointId, endpointArg, ObjectMappers.SkipNulls.valueToTree(body));

        try
        {
            return ObjectMappers.SkipNulls.treeToValue(res, clzOutput);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static <I, O> O callEndpoint(DataConnectionApi proxy,
                                        String endpointId,
                                        String endpointArg,
                                        I body,
                                        TypeReference<O> typeOutput)
    {
        return callEndpoint(proxy, endpointId, endpointArg, body, Reflection.getRawType(typeOutput.getType()));
    }
}
