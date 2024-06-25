/*
 * Copyright (C) 2017-2020, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.pelion.model;

public class ApnDetail
{

    /**
     * The name of the APN to which the subscriber connected.
     */
    public String apnName              = null;
    /**
     * The subscriber's outbound internet access, either enabled or disabled.
     */
    public String internetAccessStatus = null;
    /**
     * The subscriber's APN password.
     */
    public String password             = null;
    /**
     * The subscriber's private IP address.
     */
    public String privateIpAddress     = null;
    /**
     * The subscriber's APN username.
     */
    public String username             = null;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class ApnDetail {\n");

        sb.append("    apnName: ")
          .append(toIndentedString(apnName))
          .append("\n");
        sb.append("    internetAccessStatus: ")
          .append(toIndentedString(internetAccessStatus))
          .append("\n");
        sb.append("    password: ")
          .append(toIndentedString(password))
          .append("\n");
        sb.append("    privateIpAddress: ")
          .append(toIndentedString(privateIpAddress))
          .append("\n");
        sb.append("    username: ")
          .append(toIndentedString(username))
          .append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private static String toIndentedString(Object o)
    {
        if (o == null)
        {
            return "null";
        }
        return o.toString()
                .replace("\n", "\n    ");
    }
}
