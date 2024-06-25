/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Provisioner APIs
 * APIs and Definitions for the Optio3 Provisioner product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.provisioner.api;

import com.optio3.cloud.client.provisioner.model.FlashingProgress;
import com.optio3.cloud.client.provisioner.model.ProvisionReport;
import com.optio3.cloud.client.provisioner.model.ProvisionReportExt;
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
    @Path("/provision/board/power-off")
    @Produces(
    {
        "text/plain"
    })
    public String boardPowerOff();

    @GET
    @Path("/provision/board/power-on")
    @Produces(
    {
        "text/plain"
    })
    public String boardPowerOn();

    @GET
    @Path("/provision/board/write/progress")
    @Produces(
    {
        "application/json"
    })
    public FlashingProgress checkBoardFlashing();

    @GET
    @Path("/provision/board/detect")
    @Produces(
    {
        "text/plain"
    })
    public String detectBoard();

    @GET
    @Path("/provision/firmware/downloading")
    @Produces(
    {
        "text/plain"
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
    @Path("/provision/board/power-switch-supported")
    @Produces(
    {
        "text/plain"
    })
    public String powerSwitchSupported();

    @GET
    @Path("/provision/checkin/print/{hostId}")
    @Produces(
    {
        "text/plain"
    })
    public Boolean printCheckin(@PathParam("hostId") String hostId);

    @GET
    @Path("/provision/board/write/start/{serialNumber}")
    @Produces(
    {
        "application/json"
    })
    public FlashingProgress startBoardFlashing(@PathParam("serialNumber") String serialNumber);

    @GET
    @Path("/provision/firmware/trigger")
    @Produces(
    {
        "text/plain"
    })
    public Long triggerFirmwareDownload();

}