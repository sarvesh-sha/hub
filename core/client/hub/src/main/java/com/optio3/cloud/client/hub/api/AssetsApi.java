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

import com.optio3.cloud.client.hub.model.Asset;
import com.optio3.cloud.client.hub.model.AssetFilterRequest;
import com.optio3.cloud.client.hub.model.AssetFilterResponse;
import com.optio3.cloud.client.hub.model.AssetGraphRequest;
import com.optio3.cloud.client.hub.model.AssetGraphResponse;
import com.optio3.cloud.client.hub.model.AssetTravelLog;
import com.optio3.cloud.client.hub.model.DeviceElementReportProgress;
import java.io.InputStream;
import com.optio3.cloud.client.hub.model.MetricsDefinition;
import com.optio3.cloud.client.hub.model.RawImport;
import com.optio3.cloud.client.hub.model.RecordIdentity;
import com.optio3.cloud.client.hub.model.SummaryResult;
import com.optio3.cloud.client.hub.model.TagsJoinQuery;
import com.optio3.cloud.client.hub.model.TagsSummary;
import com.optio3.cloud.client.hub.model.ValidationResults;
import java.time.ZonedDateTime;

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
public interface AssetsApi
{
    @GET
    @Path("/assets/device-elements/report/check/{id}")
    @Produces(
    {
        "application/json"
    })
    public DeviceElementReportProgress checkDeviceElementReport(@PathParam("id") String id, @QueryParam("detailed") Boolean detailed);

    @POST
    @Path("/assets/create")
    @Produces(
    {
        "application/json"
    })
    public Asset create(Asset body);

    @POST
    @Path("/assets/asset-graph/evaluate")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public AssetGraphResponse evaluateAssetGraph(AssetGraphRequest body);

    @GET
    @Path("/assets/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public Asset get(@PathParam("id") String id);

    @GET
    @Path("/assets/active-workflows/{id}")
    @Produces(
    {
        "application/json"
    })
    public List<RecordIdentity> getActiveWorkflows(@PathParam("id") String id);

    @POST
    @Path("/assets/active-workflows-batch")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<List<RecordIdentity>> getActiveWorkflowsBatch(List<String> body);

    @POST
    @Path("/assets/batch")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<Asset> getBatch(List<String> body);

    @POST
    @Path("/assets/filter")
    @Produces(
    {
        "application/json"
    })
    public AssetFilterResponse getFiltered(AssetFilterRequest body);

    @POST
    @Path("/assets/count")
    @Produces(
    {
        "application/json"
    })
    public Long getFilteredCount(AssetFilterRequest body);

    @GET
    @Path("/assets/alert-history/{id}")
    @Produces(
    {
        "application/json"
    })
    public List<RecordIdentity> getHistoryByID(@PathParam("id") String id, @QueryParam("rangeStart") ZonedDateTime rangeStart, @QueryParam("rangeEnd") ZonedDateTime rangeEnd);

    @POST
    @Path("/assets/summary")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<SummaryResult> getSummary(@QueryParam("groupBy") String groupBy, AssetFilterRequest body);

    @GET
    @Path("/assets/tags/{id}/{tag}/get")
    @Produces(
    {
        "application/json"
    })
    public List<String> getTag(@PathParam("id") String id, @PathParam("tag") String tag);

    @GET
    @Path("/assets/travel-log/{id}")
    @Produces(
    {
        "application/json"
    })
    public AssetTravelLog getTravelLog(@PathParam("id") String id, @QueryParam("maxGapForSegmentInMeters") Integer maxGapForSegmentInMeters, @QueryParam("maxDurationPerSegmentInSeconds") Integer maxDurationPerSegmentInSeconds, @QueryParam("rangeStart") ZonedDateTime rangeStart, @QueryParam("rangeEnd") ZonedDateTime rangeEnd);

    @GET
    @Path("/assets/lookup-metrics/{id}")
    @Produces(
    {
        "application/json"
    })
    public MetricsDefinition lookupMetrics(@PathParam("id") String id);

    @POST
    @Path("/assets/parse-import")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public Asset parseImport(RawImport body);

    @GET
    @Path("/assets/item/{id}/reassign-to/{parentId}")
    @Produces(
    {
        "application/json"
    })
    public ValidationResults reassignParent(@PathParam("id") String id, @PathParam("parentId") String parentId, @QueryParam("dryRun") Boolean dryRun);

    @DELETE
    @Path("/assets/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public ValidationResults remove(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun);

    @GET
    @Path("/assets/tags/{id}/{tag}/remove")
    @Produces(
    {
        "application/json"
    })
    public Boolean removeTag(@PathParam("id") String id, @PathParam("tag") String tag);

    @POST
    @Path("/assets/tags/{id}/{tag}/set")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<String> setTag(@PathParam("id") String id, @PathParam("tag") String tag, List<String> body);

    @POST
    @Path("/assets/device-elements/report/start/{deviceSysId}")
    @Produces(
    {
        "text/plain"
    })
    public String startDeviceElementsReport(@PathParam("deviceSysId") String deviceSysId);

    @GET
    @Path("/assets/device-elements/report/excel/{id}/{fileName}")
    @Produces(
    {
        "application/octet-stream"
    })
    public InputStream streamDeviceElementReport(@PathParam("id") String id, @PathParam("fileName") String fileName);

    @POST
    @Path("/assets/tags-query")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<List<String>> tagsQuery(TagsJoinQuery body);

    @POST
    @Path("/assets/tags-query-distinct")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<RecordIdentity> tagsQueryDistinct(TagsJoinQuery body, @QueryParam("level") Integer level);

    @GET
    @Path("/assets/tags-summary")
    @Produces(
    {
        "application/json"
    })
    public TagsSummary tagsSummary(@QueryParam("recomputeIfChanged") Integer recomputeIfChanged);

    @POST
    @Path("/assets/item/{id}")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public ValidationResults update(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun, Asset body);

}
