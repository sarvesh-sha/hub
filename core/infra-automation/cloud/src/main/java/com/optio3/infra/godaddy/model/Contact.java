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

public class Contact
{

    public Address addressMailing = null;
    public String  email          = null;
    public String  fax            = null;
    public String  jobTitle       = null;
    public String  nameFirst      = null;
    public String  nameLast       = null;
    public String  nameMiddle     = null;
    public String  organization   = null;
    public String  phone          = null;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class Contact {\n");

        sb.append("    addressMailing: ")
          .append(toIndentedString(addressMailing))
          .append("\n");
        sb.append("    email: ")
          .append(toIndentedString(email))
          .append("\n");
        sb.append("    fax: ")
          .append(toIndentedString(fax))
          .append("\n");
        sb.append("    jobTitle: ")
          .append(toIndentedString(jobTitle))
          .append("\n");
        sb.append("    nameFirst: ")
          .append(toIndentedString(nameFirst))
          .append("\n");
        sb.append("    nameLast: ")
          .append(toIndentedString(nameLast))
          .append("\n");
        sb.append("    nameMiddle: ")
          .append(toIndentedString(nameMiddle))
          .append("\n");
        sb.append("    organization: ")
          .append(toIndentedString(organization))
          .append("\n");
        sb.append("    phone: ")
          .append(toIndentedString(phone))
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