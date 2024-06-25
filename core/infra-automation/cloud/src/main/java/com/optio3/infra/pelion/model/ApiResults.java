/*
 * Copyright (C) 2017-2020, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.pelion.model;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiResults<T>
{
    public JsonNode   content;
    public Pagination pagination;

    public T decode(ObjectMapper mapper,
                    TypeReference<T> type) throws
                                           IOException
    {
        return content != null ? mapper.readValue(mapper.treeAsTokens(content), type) : null;
    }
}
