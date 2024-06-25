/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@Provider
@Consumes(CborProvider.APPLICATION_CBOR)
@Produces(CborProvider.APPLICATION_CBOR)
public final class CborProvider implements MessageBodyReader<Object>,
                                           MessageBodyWriter<Object>
{
    public static final String    APPLICATION_CBOR      = "application/cbor";
    public static final MediaType APPLICATION_CBOR_TYPE = MediaType.valueOf(APPLICATION_CBOR);

    @Override
    public boolean isReadable(Class<?> aClass,
                              Type type,
                              Annotation[] annotations,
                              MediaType mediaType)
    {
        return APPLICATION_CBOR_TYPE.equals(mediaType);
    }

    @Override
    public Object readFrom(Class<Object> aClass,
                           Type type,
                           Annotation[] annotations,
                           MediaType mediaType,
                           MultivaluedMap<String, String> multivaluedMap,
                           InputStream inputStream) throws
                                                    IOException,
                                                    WebApplicationException
    {
        return BinaryObjectMappers.RestDefaults.readValue(inputStream, aClass);
    }

    @Override
    public boolean isWriteable(Class<?> aClass,
                               Type type,
                               Annotation[] annotations,
                               MediaType mediaType)
    {
        return APPLICATION_CBOR_TYPE.equals(mediaType);
    }

    @Override
    public void writeTo(Object o,
                        Class<?> aClass,
                        Type type,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> multivaluedMap,
                        OutputStream outputStream) throws
                                                   IOException,
                                                   WebApplicationException
    {
        BinaryObjectMappers.RestDefaults.writeValue(outputStream, o);
    }
}
