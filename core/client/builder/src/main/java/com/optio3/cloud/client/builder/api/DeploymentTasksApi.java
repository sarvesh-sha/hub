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

import com.optio3.cloud.client.builder.model.DeploymentTask;
import com.optio3.cloud.client.builder.model.LogEntryFilterRequest;
import com.optio3.cloud.client.builder.model.LogLine;
import com.optio3.cloud.client.builder.model.LogRange;
import com.optio3.cloud.client.builder.model.RecordIdentity;
import com.optio3.cloud.client.builder.model.ValidationResults;

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
public interface DeploymentTasksApi
{
    @POST
    @Path("/deployment-tasks/item/{id}/log/filter")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<LogRange> filterLog(@PathParam("id") String id, LogEntryFilterRequest body);

    @GET
    @Path("/deployment-tasks/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public DeploymentTask get(@PathParam("id") String id);

    @GET
    @Path("/deployment-tasks/all/{id}")
    @Produces(
    {
        "application/json"
    })
    public List<RecordIdentity> getAll(@PathParam("id") String id);

    @POST
    @Path("/deployment-tasks/batch")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<DeploymentTask> getBatch(List<String> body);

    @GET
    @Path("/deployment-tasks/item/{id}/log")
    @Produces(
    {
        "application/json"
    })
    public List<LogLine> getLog(@PathParam("id") String id, @QueryParam("fromOffset") Integer fromOffset, @QueryParam("toOffset") Integer toOffset, @QueryParam("limit") Integer limit);

    @DELETE
    @Path("/deployment-tasks/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public ValidationResults remove(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun);

    @GET
    @Path("/deployment-tasks/item/{id}/restart")
    @Produces(
    {
        "application/json"
    })
    public ValidationResults restart(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun);

    @GET
    @Path("/deployment-tasks/item/{id}/terminate")
    @Produces(
    {
        "application/json"
    })
    public ValidationResults terminate(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun);

}
