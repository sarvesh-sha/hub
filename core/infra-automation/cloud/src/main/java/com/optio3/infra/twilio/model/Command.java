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

package com.optio3.infra.twilio.model;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonCreator;

public class Command
{
    public enum Direction
    {
        FROM_SIM("from_sim"),
        TO_SIM("to_sim");

        private final String value;

        private Direction(final String value)
        {
            this.value = value;
        }

        public String toString()
        {
            return value;
        }

        @JsonCreator
        public static Direction forValue(final String value)
        {
            return enumFromString(value, Direction.values());
        }
    }

    public enum Status
    {
        QUEUED("queued"),
        SENT("sent"),
        DELIVERED("delivered"),
        RECEIVED("received"),
        FAILED("failed");

        private final String value;

        private Status(final String value)
        {
            this.value = value;
        }

        public String toString()
        {
            return value;
        }

        @JsonCreator
        public static Status forValue(final String value)
        {
            return enumFromString(value, Status.values());
        }
    }

    public enum CommandMode
    {
        TEXT("text"),
        BINARY("binary");

        private final String value;

        private CommandMode(final String value)
        {
            this.value = value;
        }

        public String toString()
        {
            return value;
        }

        @JsonCreator
        public static CommandMode forValue(final String value)
        {
            return enumFromString(value, CommandMode.values());
        }
    }

    public enum Transport
    {
        SMS("sms"),
        IP("ip");

        private final String value;

        private Transport(final String value)
        {
            this.value = value;
        }

        public String toString()
        {
            return value;
        }

        @JsonCreator
        public static Transport forValue(final String value)
        {
            return enumFromString(value, Transport.values());
        }
    }

    public String      sid;
    public String      account_sid;
    public String      sim_sid;
    public String      command;
    public CommandMode command_mode;
    public Transport   transport;
    public boolean     delivery_receipt_requested;
    public Status      status;
    public Direction   direction;
    public String      date_created;
    public String      date_updated;
    public URI         url;

    public static <T extends Enum<?>> T enumFromString(final String value,
                                                       final T[] values)
    {
        if (value == null)
        {
            return null;
        }

        for (T v : values)
        {
            if (v.toString()
                 .equalsIgnoreCase(value))
            {
                return v;
            }
        }

        return null;
    }
}
