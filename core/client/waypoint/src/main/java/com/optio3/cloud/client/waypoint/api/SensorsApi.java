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

import com.optio3.cloud.client.waypoint.model.SensorConfig;
import com.optio3.cloud.client.waypoint.model.SensorResult;
import com.optio3.cloud.client.waypoint.model.SensorResultToken;

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
public interface SensorsApi
{
    @POST
    @Path("/sensors/check-status")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public SensorResult checkStatus(SensorResultToken body);

    @POST
    @Path("/sensors/start-check-status")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public SensorResultToken startCheckStatus(SensorConfig body);

}
