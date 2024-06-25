/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Hub APIs
 * APIs and Definitions for the Optio3 Hub product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.hub.api;

import com.optio3.cloud.client.hub.model.AlertDefinition;
import com.optio3.cloud.client.hub.model.AlertDefinitionFilterRequest;
import com.optio3.cloud.client.hub.model.LogEntryFilterRequest;
import com.optio3.cloud.client.hub.model.LogLine;
import com.optio3.cloud.client.hub.model.LogRange;
import com.optio3.cloud.client.hub.model.RawImport;
import com.optio3.cloud.client.hub.model.RecordIdentity;
import com.optio3.cloud.client.hub.model.ValidationResults;

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
public interface AlertDefinitionsApi
{
    @POST
    @Path("/alert-definitions/create")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public AlertDefinition create(AlertDefinition body);

    @DELETE
    @Path("/alert-definitions/item/{id}/log")
    @Produces(
    {
        "application/json"
    })
    public Integer deleteLog(@PathParam("id") String id, @QueryParam("olderThanXMinutes") Integer olderThanXMinutes);

    @POST
    @Path("/alert-definitions/item/{id}/log/filter")
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
    @Path("/alert-definitions/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public AlertDefinition get(@PathParam("id") String id);

    @POST
    @Path("/alert-definitions/batch")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<AlertDefinition> getBatch(List<String> body);

    @POST
    @Path("/alert-definitions/filter")
    @Produces(
    {
        "application/json"
    })
    public List<RecordIdentity> getFiltered(AlertDefinitionFilterRequest body);

    @GET
    @Path("/alert-definitions/item/{id}/log")
    @Produces(
    {
        "application/json"
    })
    public List<LogLine> getLog(@PathParam("id") String id, @QueryParam("fromOffset") Integer fromOffset, @QueryParam("toOffset") Integer toOffset, @QueryParam("limit") Integer limit);

    @POST
    @Path("/alert-definitions/parse-import")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public AlertDefinition parseImport(RawImport body);

    @DELETE
    @Path("/alert-definitions/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public ValidationResults remove(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun);

    @POST
    @Path("/alert-definitions/item/{id}")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public ValidationResults update(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun, AlertDefinition body);

}
