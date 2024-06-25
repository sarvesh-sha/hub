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

import java.util.ArrayList;
import java.util.List;

public class ErrorFieldDomainContactsValidate
{

    /**
     * Short identifier for the error, suitable for indicating the specific error within client code
     */
    public String       code        = null;
    /**
     * An array of domain names the error is for. If tlds are specified in the request, `domains` will contain tlds.
     */
    public List<String> domains     = new ArrayList<String>();
    /**
     * Human-readable, English description of the problem with the contents of the field
     */
    public String       message     = null;
    /**
     * 1) JSONPath referring to the field within the data containing an error<br/>or<br/>2) JSONPath referring to an object containing an error
     */
    public String       path        = null;
    /**
     * JSONPath referring to the field on the object referenced by `path` containing an error
     */
    public String       pathRelated = null;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class ErrorFieldDomainContactsValidate {\n");

        sb.append("    code: ")
          .append(toIndentedString(code))
          .append("\n");
        sb.append("    domains: ")
          .append(toIndentedString(domains))
          .append("\n");
        sb.append("    message: ")
          .append(toIndentedString(message))
          .append("\n");
        sb.append("    path: ")
          .append(toIndentedString(path))
          .append("\n");
        sb.append("    pathRelated: ")
          .append(toIndentedString(pathRelated))
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
