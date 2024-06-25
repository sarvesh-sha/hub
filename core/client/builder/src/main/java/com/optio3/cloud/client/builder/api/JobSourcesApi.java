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

import com.optio3.cloud.client.builder.model.JobSource;

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
public interface JobSourcesApi
{
    @GET
    @Path("/job-sources/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public JobSource get(@PathParam("id") String id);

    @POST
    @Path("/job-sources/batch")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<JobSource> getBatch(List<String> body);

}
