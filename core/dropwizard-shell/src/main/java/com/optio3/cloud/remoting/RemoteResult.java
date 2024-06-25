/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.remoting;

import com.fasterxml.jackson.databind.JsonNode;

public class RemoteResult
{
    public String   typeId;
    public JsonNode value;

    public RemoteExceptionResult exception;

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
        if (exception != null)
        {
            sb.append("Exception: ");
            exception.toString(sb, includeValues);
        }
        else
        {
            sb.append(typeId);

            if (includeValues)
            {
                sb.append("=");
                sb.append(value);
            }
        }
    }
}
