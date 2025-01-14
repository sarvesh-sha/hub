/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * APN Log
 * <p>Retrieve a subscriber's APN session log or APN connection details for a specified time frame.</p><p>You can use these endpoints to retrieve the APN information, and sort and filter the returned results.</p>      <p>The <em>APN log</em> is a historical record of the subscriber-generated IP traffic. The log includes the following information&#58;</p>      <ul>        <li>Subscriber credentials used to access the APN</li>        <li>Start and stop dates and times for each session</li>        <li>Mobile codes that identify the network accessed during each session (if provided by the network operator)</li>        <li>Subscriber's IP address, either fixed or dynamic</li>        <li>Total amount of data the subscriber sent during the session (MO data)</li>        <li>Total amount of data the subscriber received during the session (MT data)</li>        <li>Reason for the termination of each session</li>      </ul>      <p>The <em>APN details</em> include the following information&#58;</p>      <ul>        <li>Name of the APN</li>        <li>Subscriber credentials used to access the APN</li>        <li>Internet access status</li>        <li>Subscriber's private IP address</li>        </ul>
 *
 * OpenAPI spec version: 0.1.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.infra.pelion.api;

import java.math.BigDecimal;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.optio3.infra.pelion.model.ApiResults;
import com.optio3.infra.pelion.model.ApnDetail;
import com.optio3.infra.pelion.model.ApnLog;

@Path("/")
public interface APNInformationApi
{
    @GET
    @Path("/{physicalId}/logs")
    @Produces({ "application/json" })
    public ApiResults<List<ApnLog>> getAll(@PathParam("physicalId") String physicalId,
                                           @QueryParam("orderBy") String orderBy,
                                           @QueryParam("offset") BigDecimal offset,
                                           @QueryParam("limit") BigDecimal limit,
                                           @QueryParam("filter") String filter);

    @GET
    @Path("/{physicalId}/apn-details")
    @Produces({ "application/json" })
    public ApiResults<List<ApnDetail>> apnDetails(@PathParam("physicalId") String physicalId);
}
