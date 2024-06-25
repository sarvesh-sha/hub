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
public interface AdminTasksApi
{
    @GET
    @Path("/admin-tasks/app-version")
    @Produces(
    {
        "text/plain"
    })
    public String getAppVersion();

    @GET
    @Path("/admin-tasks/factor-floor-mode")
    @Produces(
    {
        "application/json"
    })
    public Boolean isFactoryFloorMode();

    @GET
    @Path("/admin-tasks/production-mode")
    @Produces(
    {
        "application/json"
    })
    public Boolean isProductionMode();

    @POST
    @Path("/admin-tasks/log")
    @Consumes(
    {
        "application/x-www-form-urlencoded"
    })
    @Produces(
    {
        "application/json"
    })
    public ZonedDateTime log(@FormParam(value = "level") String level, @FormParam(value = "text") String text);

}
