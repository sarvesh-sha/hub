/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.remoting;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.serialization.Reflection;

public class RemoteArgument implements Comparable<RemoteArgument>
{
    public String   typeId;
    public JsonNode value;

    public String callbackId;

    //--//

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        RemoteArgument that = Reflection.as(o, RemoteArgument.class);
        if (that == null)
        {
            return false;
        }

        return Objects.equals(typeId, that.typeId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(typeId);
    }

    @Override
    public int compareTo(RemoteArgument o)
    {
        return typeId.compareTo(o.typeId);
    }

    public RemoteArgument cloneForSignatureMatching()
    {
        RemoteArgument res = new RemoteArgument();

        res.typeId = typeId;

        return res;
    }

    //--//

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        toString(sb, true);

        return sb.toString();
    }

    public void toString(StringBuilder sb,
                         boolean includeValues)
    {
        sb.append(typeId);

        if (includeValues)
        {
            if (value != null)
            {
                sb.append("=");
                sb.append(value);
            }
            else
            {
                sb.append("Callback:");
                sb.append(callbackId);
            }
        }
    }
}
