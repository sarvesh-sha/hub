/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.docker;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonStreamReader implements AutoCloseable
{
    private static final ObjectMapper s_mapper;

    static
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(Feature.ALLOW_MISSING_VALUES, true);
        s_mapper = mapper;
    }

    //--//

    private final JsonParser m_parser;

    public JsonStreamReader(Response response) throws
                                               JsonParseException,
                                               IOException
    {
        this((InputStream) response.getEntity());
    }

    public JsonStreamReader(InputStream in) throws
                                            JsonParseException,
                                            IOException
    {
        m_parser = s_mapper.getFactory()
                           .createParser(in);
    }

    @Override
    public void close() throws
                        Exception
    {
        m_parser.close();
    }

    public <T> T readAs(Class<T> clz)
    {
        try
        {
            if (!m_parser.hasCurrentToken())
            {
                if (m_parser.nextToken() == null)
                {
                    return null;
                }
            }

            return m_parser.readValueAs(clz);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public <T extends TreeNode> T readAsTree()
    {
        try
        {
            if (!m_parser.hasCurrentToken())
            {
                if (m_parser.nextToken() == null)
                {
                    return null;
                }
            }

            return m_parser.readValueAsTree();
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
