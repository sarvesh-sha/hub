/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * api.godaddy.com
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 2.4.9
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.infra.godaddy.model;

public class LegalAgreement
{

    /**
     * Unique identifier for the legal agreement
     */
    public String agreementKey = null;
    /**
     * Contents of the legal agreement, suitable for embedding
     */
    public String content      = null;
    /**
     * Title of the legal agreement
     */
    public String title        = null;
    /**
     * URL to a page containing the legal agreement
     */
    public String url          = null;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class LegalAgreement {\n");

        sb.append("    agreementKey: ")
          .append(toIndentedString(agreementKey))
          .append("\n");
        sb.append("    content: ")
          .append(toIndentedString(content))
          .append("\n");
        sb.append("    title: ")
          .append(toIndentedString(title))
          .append("\n");
        sb.append("    url: ")
          .append(toIndentedString(url))
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
