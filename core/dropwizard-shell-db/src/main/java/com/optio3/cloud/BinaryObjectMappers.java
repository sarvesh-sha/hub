/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.optio3.serialization.ObjectMappers;

public final class BinaryObjectMappers
{
    public static final ObjectMapper SkipNulls;
    public static final ObjectMapper SkipNullsAllowEmptyBeans;
    public static final ObjectMapper SkipNullsCaseInsensitive;
    public static final ObjectMapper SkipDefaults;
    public static       ObjectMapper RestDefaults;

    static
    {
        {
            ObjectMapper objectMapper = new ObjectMapper(new CBORFactory());

            objectMapper.findAndRegisterModules();

            ObjectMappers.configureToSkipNulls(objectMapper);

            SkipNulls = objectMapper;
        }

        {
            ObjectMapper objectMapper = SkipNulls.copy();
            ObjectMappers.configureCaseInsensitive(objectMapper);
            SkipNullsCaseInsensitive = objectMapper;
        }

        {
            ObjectMapper objectMapper = SkipNulls.copy();
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            SkipNullsAllowEmptyBeans = objectMapper;
        }

        {
            ObjectMapper objectMapper = SkipNulls.copy();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
            SkipDefaults = objectMapper;
        }

        RestDefaults = SkipNulls.copy();
        RestDefaults.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
}
