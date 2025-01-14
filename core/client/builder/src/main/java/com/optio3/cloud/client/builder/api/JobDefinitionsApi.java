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

import com.optio3.cloud.client.builder.model.Job;
import com.optio3.cloud.client.builder.model.JobDefinition;
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
public interface JobDefinitionsApi
{
    @POST
    @Path("/job-definitions/create")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public JobDefinition create(JobDefinition body);

    @GET
    @Path("/job-definitions/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public JobDefinition get(@PathParam("id") String id);

    @GET
    @Path("/job-definitions/all")
    @Produces(
    {
        "application/json"
    })
    public List<RecordIdentity> getAll();

    @POST
    @Path("/job-definitions/batch")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<JobDefinition> getBatch(List<String> body);

    @DELETE
    @Path("/job-definitions/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public ValidationResults remove(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun);

    @GET
    @Path("/job-definitions/trigger/{id}")
    @Produces(
    {
        "application/json"
    })
    public Job trigger(@PathParam("id") String id, @QueryParam("branch") String branch, @QueryParam("commit") String commit);

    @POST
    @Path("/job-definitions/item/{id}")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public ValidationResults update(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun, JobDefinition body);

}
