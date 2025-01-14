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

import java.time.ZonedDateTime;

public class DataSession
{
    public static class Location
    {
        public double lon;
        public double lat;
    }

    public String        sid;                    // The unique id of the Data Session resource that this Data Record is for.
    public String        sim_sid;                // The unique id of the SIM resource that this Data Session is for.
    public String        account_sid;            // The unique id of the Account that the SIM belongs to.
    public ZonedDateTime start;                  // The date that this Data Session started, given as GMT in ISO 8601 format.
    public ZonedDateTime end;                    // The date that this record ended, given as GMT in ISO 8601 format.
    public ZonedDateTime last_updated;           // The date that this resource was last updated, given as GMT in ISO 8601 format.
    public String        operator_mcc;           // The 'mobile country code' is the unique id of the home country where the Data Session took place. See: MCC/MNC lookup.
    public String        operator_country;       // The three letter country code representing where the device's Data Session took place. This is derived from a lookup of the operator_mcc.
    public String        operator_mnc;           // The 'mobile network code' is the unique id specific to the mobile operator network where the Data Session took place.
    public String        operator_name;          // The friendly name of the mobile operator network that the SIM-connected device is attached to. This is derived from a lookup of the operator_mnc.
    public String        cell_id;                // The unique id of the cellular tower that the device was attached to at the moment when the Data Session was last updated.
    public Location      cell_location_estimate; // An object representing the estimated location where the device's Data Session took place. See Cell Location Estimate Object below. This is derived from the cell_id and reflects only the moment when the Data Session was last updated. If the location could not be estimated, this object will be null.
    public String        radio_link;             //	The generation of wireless technology that the device was attached to the cellular tower using.
    public int           packets_downloaded;     //	The number of packets downloaded by the device between the start time and when the Data Session was last updated.
    public int           packets_uploaded;       //	The number of packets uploaded by the device between the start time and when the Data Session was last updated.
    public String        imei;                   // The 'international mobile equipment identity' is the unique id of the device using the SIM to connect. 15-digit string: 14 digits for the device identifier plus a check digit calculated using the Luhn formula.
}
