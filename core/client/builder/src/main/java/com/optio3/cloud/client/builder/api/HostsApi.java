/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Builder APIs
 * APIs and Definitions for the Optio3 Builder product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.builder.api;

import com.optio3.cloud.client.builder.model.Host;
import com.optio3.cloud.client.builder.model.LogEntryFilterRequest;
import com.optio3.cloud.client.builder.model.LogLine;
import com.optio3.cloud.client.builder.model.LogRange;
import com.optio3.cloud.client.builder.model.RecordIdentity;

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
public interface HostsApi
{
    @DELETE
    @Path("/hosts/item/{hostId}/log")
    @Produces(
    {
        "application/json"
    })
    public Integer deleteLog(@PathParam("hostId") String hostId, @QueryParam("olderThanXMinutes") Integer olderThanXMinutes);

    @POST
    @Path("/hosts/item/{hostId}/log/filter")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<LogRange> filterLog(@PathParam("hostId") String hostId, LogEntryFilterRequest body);

    @GET
    @Path("/hosts/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public Host get(@PathParam("id") String id);

    @GET
    @Path("/hosts/all")
    @Produces(
    {
        "application/json"
    })
    public List<RecordIdentity> getAll();

    @POST
    @Path("/hosts/batch")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<Host> getBatch(List<String> body);

    @GET
    @Path("/hosts/item/{hostId}/log")
    @Produces(
    {
        "application/json"
    })
    public List<LogLine> getLog(@PathParam("hostId") String hostId, @QueryParam("fromOffset") Integer fromOffset, @QueryParam("toOffset") Integer toOffset, @QueryParam("limit") Integer limit);

}
