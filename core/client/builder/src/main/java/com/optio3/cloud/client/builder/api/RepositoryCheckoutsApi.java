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

import com.optio3.cloud.client.builder.model.RecordIdentity;
import com.optio3.cloud.client.builder.model.RepositoryCheckout;
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
public interface RepositoryCheckoutsApi
{
    @GET
    @Path("/repository-checkouts/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public RepositoryCheckout get(@PathParam("id") String id);

    @GET
    @Path("/repository-checkouts/all/{id}")
    @Produces(
    {
        "application/json"
    })
    public List<RecordIdentity> getAll(@PathParam("id") String id);

    @POST
    @Path("/repository-checkouts/batch")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<RepositoryCheckout> getBatch(List<String> body);

    @DELETE
    @Path("/repository-checkouts/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public ValidationResults remove(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun);

}