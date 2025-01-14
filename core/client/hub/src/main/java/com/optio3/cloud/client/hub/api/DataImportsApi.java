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

import com.optio3.cloud.client.hub.model.DataImportProgress;
import com.optio3.cloud.client.hub.model.DataImportRun;
import com.optio3.cloud.client.hub.model.ImportedMetadata;
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
public interface DataImportsApi
{
    @GET
    @Path("/data-imports/check/{id}")
    @Produces(
    {
        "application/json"
    })
    public DataImportProgress checkImport(@PathParam("id") String id, @QueryParam("detailed") Boolean detailed);

    @POST
    @Path("/data-imports/create")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public ImportedMetadata create(ImportedMetadata body);

    @GET
    @Path("/data-imports/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public ImportedMetadata get(@PathParam("id") String id, @QueryParam("detailed") Boolean detailed);

    @GET
    @Path("/data-imports/all")
    @Produces(
    {
        "application/json"
    })
    public List<RecordIdentity> getAll();

    @POST
    @Path("/data-imports/batch")
    @Produces(
    {
        "application/json"
    })
    public List<ImportedMetadata> getBatch(List<String> body);

    @GET
    @Path("/data-imports/item/{id}/activate")
    @Produces(
    {
        "application/json"
    })
    public ImportedMetadata makeActive(@PathParam("id") String id);

    @POST
    @Path("/data-imports/parse-import")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public ImportedMetadata parseImport(RawImport body);

    @DELETE
    @Path("/data-imports/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public ValidationResults remove(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun);

    @POST
    @Path("/data-imports/start")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "text/plain"
    })
    public String startImport(DataImportRun body);

}
