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
import com.optio3.cloud.client.builder.model.JobFilterRequest;
import com.optio3.cloud.client.builder.model.JobUsage;
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
public interface JobsApi
{
    @GET
    @Path("/jobs/item/{id}/cancel")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public Job cancel(@PathParam("id") String id);

    @GET
    @Path("/jobs/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public Job get(@PathParam("id") String id);

    @GET
    @Path("/jobs/all")
    @Produces(
    {
        "application/json"
    })
    public List<RecordIdentity> getAll();

    @POST
    @Path("/jobs/batch")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<Job> getBatch(List<String> body);

    @POST
    @Path("/jobs/filter")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<RecordIdentity> getFiltered(JobFilterRequest body);

    @GET
    @Path("/jobs/item/{id}/usage")
    @Produces(
    {
        "application/json"
    })
    public JobUsage getUsage(@PathParam("id") String id);

    @DELETE
    @Path("/jobs/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public ValidationResults remove(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun);

    @POST
    @Path("/jobs/item/{id}")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public ValidationResults update(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun, Job body);

}
