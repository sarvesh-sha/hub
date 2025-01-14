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

import com.optio3.cloud.client.waypoint.model.BootConfigLine;
import com.optio3.cloud.client.waypoint.model.DetailedApplicationExceptionErrorDetails;
import com.optio3.cloud.client.waypoint.model.LoggerConfiguration;

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
public interface AdminTasksApi
{
    @POST
    @Path("/admin-tasks/loggers/config")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public LoggerConfiguration configLogger(LoggerConfiguration body);

    @GET
    @Path("/admin-tasks/threads")
    @Produces(
    {
        "text/plain"
    })
    public String dumpThreads();

    @GET
    @Path("/admin-tasks/app-version")
    @Produces(
    {
        "text/plain"
    })
    public String getAppVersion();

    @GET
    @Path("/admin-tasks/loggers/list")
    @Produces(
    {
        "application/json"
    })
    public List<LoggerConfiguration> getLoggers();

    @GET
    @Path("/admin-tasks/options/{option}")
    @Produces(
    {
        "application/json"
    })
    public BootConfigLine getOption(@PathParam("option") String option);

    @GET
    @Path("/admin-tasks/production-mode")
    @Produces(
    {
        "application/json"
    })
    public Boolean isProductionMode();

    @POST
    @Path("/admin-tasks/reboot")
    @Produces(
    {
        "text/plain"
    })
    public String reboot();

    @POST
    @Path("/admin-tasks/options/{option}/{value}")
    @Produces(
    {
        "application/json"
    })
    public BootConfigLine setOption(@PathParam("option") String option, @PathParam("value") String value);

    @POST
    @Path("/admin-tasks/shutdown")
    @Produces(
    {
        "text/plain"
    })
    public String shutdown();

    @DELETE
    @Path("/admin-tasks/options/{option}")
    @Produces(
    {
        "application/json"
    })
    public BootConfigLine unsetOption(@PathParam("option") String option);

}
