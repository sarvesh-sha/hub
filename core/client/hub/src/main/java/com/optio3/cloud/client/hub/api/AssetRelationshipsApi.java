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

import com.optio3.cloud.client.hub.model.AssetRelationshipRequest;
import com.optio3.cloud.client.hub.model.AssetRelationshipResponse;
import com.optio3.cloud.client.hub.model.EquipmentReportProgress;
import java.io.InputStream;
import com.optio3.cloud.client.hub.model.RecordIdentity;

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
public interface AssetRelationshipsApi
{
    @GET
    @Path("/assets-relationships/equipment/report/check/{id}")
    @Produces(
    {
        "application/json"
    })
    public EquipmentReportProgress checkEquipmentReport(@PathParam("id") String id, @QueryParam("detailed") Boolean detailed);

    @GET
    @Path("/assets-relationships/top-equipments")
    @Produces(
    {
        "application/json"
    })
    public List<RecordIdentity> getTopEquipments();

    @POST
    @Path("/assets-relationships/lookup")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<AssetRelationshipResponse> lookupBatch(List<AssetRelationshipRequest> body);

    @POST
    @Path("/assets-relationships/equipment/report")
    @Produces(
    {
        "text/plain"
    })
    public String startEquipmentReport(@QueryParam("id") String id);

    @GET
    @Path("/assets-relationships/equipment/report/excel/{id}/{fileName}")
    @Produces(
    {
        "application/octet-stream"
    })
    public InputStream streamEquipmentReport(@PathParam("id") String id, @PathParam("fileName") String fileName);

}