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

import com.optio3.cloud.client.hub.model.AggregationRequest;
import com.optio3.cloud.client.hub.model.AggregationResponse;
import com.optio3.cloud.client.hub.model.TimeSeriesLastValueRequest;
import com.optio3.cloud.client.hub.model.TimeSeriesLastValueResponse;
import com.optio3.cloud.client.hub.model.TimeSeriesMultiPropertyRequest;
import com.optio3.cloud.client.hub.model.TimeSeriesMultiPropertyResponse;
import com.optio3.cloud.client.hub.model.TimeSeriesNewValueRequest;
import com.optio3.cloud.client.hub.model.TimeSeriesNewValueResponse;
import com.optio3.cloud.client.hub.model.TimeSeriesRangeRequest;
import com.optio3.cloud.client.hub.model.TimeSeriesRangeResponse;
import com.optio3.cloud.client.hub.model.TimeSeriesSchemaRequest;
import com.optio3.cloud.client.hub.model.TimeSeriesSchemaResponse;
import com.optio3.cloud.client.hub.model.TimeSeriesSinglePropertyRequest;
import com.optio3.cloud.client.hub.model.TimeSeriesSinglePropertyResponse;

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
public interface AssetTimeSeriesApi
{
    @POST
    @Path("/assets-timeseries/aggregation")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json",
        "application/cbor"
    })
    public AggregationResponse aggregation(AggregationRequest body);

    @GET
    @Path("/assets-timeseries/compact/{id}")
    @Produces(
    {
        "text/plain"
    })
    public String compactSamples(@PathParam("id") String id, @QueryParam("force") Boolean force);

    @POST
    @Path("/assets-timeseries/new-values")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public TimeSeriesNewValueResponse emitNewValues(TimeSeriesNewValueRequest body);

    @POST
    @Path("/assets-timeseries/last-value")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json",
        "application/cbor"
    })
    public TimeSeriesLastValueResponse getLastValue(TimeSeriesLastValueRequest body);

    @POST
    @Path("/assets-timeseries/last-value-batch")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json",
        "application/cbor"
    })
    public List<TimeSeriesLastValueResponse> getLastValueBatch(List<TimeSeriesLastValueRequest> body);

    @POST
    @Path("/assets-timeseries/request-range")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public TimeSeriesRangeResponse getRange(TimeSeriesRangeRequest body);

    @POST
    @Path("/assets-timeseries/request-range-batch")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<TimeSeriesRangeResponse> getRangeBatch(List<TimeSeriesRangeRequest> body);

    @POST
    @Path("/assets-timeseries/schema")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json",
        "application/cbor"
    })
    public TimeSeriesSchemaResponse getSchema(TimeSeriesSchemaRequest body);

    @POST
    @Path("/assets-timeseries/schema-batch")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json",
        "application/cbor"
    })
    public List<TimeSeriesSchemaResponse> getSchemaBatch(List<TimeSeriesSchemaRequest> body);

    @POST
    @Path("/assets-timeseries/request-multiple")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json",
        "application/cbor"
    })
    public TimeSeriesMultiPropertyResponse getValuesMultiple(TimeSeriesMultiPropertyRequest body);

    @POST
    @Path("/assets-timeseries/request-single")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json",
        "application/cbor"
    })
    public TimeSeriesSinglePropertyResponse getValuesSingle(TimeSeriesSinglePropertyRequest body);

    @POST
    @Path("/assets-timeseries/request-single-batch")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json",
        "application/cbor"
    })
    public List<TimeSeriesSinglePropertyResponse> getValuesSingleBatch(List<TimeSeriesSinglePropertyRequest> body);

    @GET
    @Path("/assets-timeseries/trim/{id}/{days}")
    @Produces(
    {
        "application/json"
    })
    public Boolean trimSamples(@PathParam("id") String id, @PathParam("days") Integer days);

}