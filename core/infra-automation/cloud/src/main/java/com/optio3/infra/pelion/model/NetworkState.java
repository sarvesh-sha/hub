/*
 * Copyright (C) 2017-2020, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.pelion.model;

public class NetworkState
{

    /**
     * Indicates whether the subscriber is currently online.
     */
    public Boolean isOnline       = null;
    /**
     * Indicates whether the subscriber is currently transferring data.
     */
    public Boolean isTransferring = null;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class NetworkState {\n");

        sb.append("    isOnline: ")
          .append(toIndentedString(isOnline))
          .append("\n");
        sb.append("    isTransferring: ")
          .append(toIndentedString(isTransferring))
          .append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private static String toIndentedString(java.lang.Object o)
    {
        if (o == null)
        {
            return "null";
        }
        return o.toString()
                .replace("\n", "\n    ");
    }
}
