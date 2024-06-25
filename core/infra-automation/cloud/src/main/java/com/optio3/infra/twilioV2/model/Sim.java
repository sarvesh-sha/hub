/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * wireless.twilio.com
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 2.4.9
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.infra.twilioV2.model;

import java.time.ZonedDateTime;
import java.util.Map;

public class Sim
{
    public enum Status
    {
        NEW("new"),
        READY("ready"),
        ACTIVE("active"),
        INACTIVE("inactive"),
        SCHEDULED("scheduled");

        private final String value;

        Status(final String value)
        {
            this.value = value;
        }

        public String toString()
        {
            return value;
        }
    }

    public String sid;
    public String unique_name;
    public String account_sid;
    public String iccid;
    public Status status;
    public String fleet_sid;
    public ZonedDateTime       date_created;
    public ZonedDateTime       date_updated;
    public String              url;
}