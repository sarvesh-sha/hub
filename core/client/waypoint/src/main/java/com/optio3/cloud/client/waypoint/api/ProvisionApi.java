/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Waypoint APIs
 * APIs and Definitions for the Optio3 Waypoint product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.waypoint.api;

import com.optio3.cloud.client.waypoint.model.FlashingProgress;
import com.optio3.cloud.client.waypoint.model.ProvisionReport;
import com.optio3.cloud.client.waypoint.model.ProvisionReportExt;
import java.time.ZonedDateTime;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import org.apache.cxf.jaxrs.ext.multipart.*;
import org.apache.cxf.jaxrs.ext.PATCH;

@Path("/")
public interface ProvisionApi
{
    @GET
    @Path("/provision/card/write/progress")
    @Produces(
    {
        "application/json"
    })
    public FlashingProgress checkCardFlashing();

    @GET
    @Path("/provision/card/detect")
    @Produces(
    {
        "application/json"
    })
    public Long detectCard();

    @GET
    @Path("/provision/firmware/downloading")
    @Produces(
    {
        "application/json"
    })
    public Long downloadingFirmware();

    @GET
    @Path("/provision/checkins")
    @Produces(
    {
        "application/json"
    })
    public List<ProvisionReportExt> getNewCheckins(@QueryParam("when") ZonedDateTime when);

    @POST
    @Path("/provision/checkin/create")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public Boolean performCheckin(ProvisionReport body);

    @GET
    @Path("/provision/checkin/print/{hostId}")
    @Produces(
    {
        "application/json"
    })
    public Boolean printCheckin(@PathParam("hostId") String hostId);

    @GET
    @Path("/provision/card/write/start")
    @Produces(
    {
        "application/json"
    })
    public FlashingProgress startCardFlashing();

    @GET
    @Path("/provision/firmware/trigger")
    @Produces(
    {
        "application/json"
    })
    public Long triggerFirmwareDownload();

}