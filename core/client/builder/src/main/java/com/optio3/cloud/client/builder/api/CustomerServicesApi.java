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

import com.optio3.cloud.client.builder.model.BackgroundActivity;
import com.optio3.cloud.client.builder.model.CheckUsagesProgress;
import com.optio3.cloud.client.builder.model.CustomerService;
import com.optio3.cloud.client.builder.model.CustomerServiceDesiredState;
import com.optio3.cloud.client.builder.model.DeployerShutdownConfiguration;
import com.optio3.cloud.client.builder.model.DeploymentCellularChargesSummary;
import com.optio3.cloud.client.builder.model.DeploymentHost;
import com.optio3.cloud.client.builder.model.DeploymentHostConfig;
import com.optio3.cloud.client.builder.model.HubHeapAndThreads;
import com.optio3.cloud.client.builder.model.LogEntryFilterRequest;
import com.optio3.cloud.client.builder.model.LogLine;
import com.optio3.cloud.client.builder.model.LogRange;
import com.optio3.cloud.client.builder.model.RecordIdentity;
import com.optio3.cloud.client.builder.model.RoleAndArchitectureWithImage;
import com.optio3.cloud.client.builder.model.UsageFilterRequest;
import com.optio3.cloud.client.builder.model.ValidationResults;
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
public interface CustomerServicesApi
{
    @POST
    @Path("/customer-services/item/{id}/add-image")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public Boolean addImage(@PathParam("id") String id, RoleAndArchitectureWithImage body);

    @GET
    @Path("/customer-services/item/{id}/backup")
    @Produces(
    {
        "application/json"
    })
    public BackgroundActivity backup(@PathParam("id") String id);

    @POST
    @Path("/customer-services/item/{id}/check-usages")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "text/plain"
    })
    public String checkUsages(@PathParam("id") String id, UsageFilterRequest body);

    @GET
    @Path("/customer-services/check-usages/progress/{id}")
    @Produces(
    {
        "application/json"
    })
    public CheckUsagesProgress checkUsagesProgress(@PathParam("id") String id, @QueryParam("detailed") Boolean detailed);

    @GET
    @Path("/customer-services/item/{id}/compact-time-series")
    @Produces(
    {
        "application/json"
    })
    public BackgroundActivity compactTimeSeries(@PathParam("id") String id);

    @POST
    @Path("/customer-services/create/{customerId}")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public CustomerService create(@PathParam("customerId") String customerId, CustomerService body);

    @DELETE
    @Path("/customer-services/item/{id}/log")
    @Produces(
    {
        "application/json"
    })
    public Integer deleteLog(@PathParam("id") String id, @QueryParam("olderThanXMinutes") Integer olderThanXMinutes);

    @POST
    @Path("/customer-services/item/{id}/deploy")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public DeploymentHost deploy(@PathParam("id") String id, DeploymentHostConfig body);

    @POST
    @Path("/customer-services/item/{id}/desired-state")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public BackgroundActivity desiredState(@PathParam("id") String id, CustomerServiceDesiredState body);

    @POST
    @Path("/customer-services/item/{id}/log/filter")
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
    @Path("/customer-services/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public CustomerService get(@PathParam("id") String id);

    @GET
    @Path("/customer-services/all/{customerId}")
    @Produces(
    {
        "application/json"
    })
    public List<RecordIdentity> getAll(@PathParam("customerId") String customerId);

    @GET
    @Path("/customer-services/accounts/{instanceType}")
    @Produces(
    {
        "application/json"
    })
    public List<String> getAvailableAccounts(@PathParam("instanceType") String instanceType);

    @GET
    @Path("/customer-services/regions/{instanceType}")
    @Produces(
    {
        "application/json"
    })
    public List<String> getAvailableRegions(@PathParam("instanceType") String instanceType);

    @POST
    @Path("/customer-services/batch")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public List<CustomerService> getBatch(List<String> body);

    @GET
    @Path("/customer-services/item/{id}/charges")
    @Produces(
    {
        "application/json"
    })
    public DeploymentCellularChargesSummary getCharges(@PathParam("id") String id, @QueryParam("maxTopHosts") Integer maxTopHosts);

    @GET
    @Path("/customer-services/item/{id}/charges-report/{fileName}")
    @Produces(
    {
        "application/csv"
    })
    public String getChargesReport(@PathParam("id") String id, @PathParam("fileName") String fileName);

    @GET
    @Path("/customer-services/item/{id}/heap-status-history")
    @Produces(
    {
        "application/json"
    })
    public List<HubHeapAndThreads> getHeapStatusHistory(@PathParam("id") String id);

    @GET
    @Path("/customer-services/item/{id}/log")
    @Produces(
    {
        "application/json"
    })
    public List<LogLine> getLog(@PathParam("id") String id, @QueryParam("fromOffset") Integer fromOffset, @QueryParam("toOffset") Integer toOffset, @QueryParam("limit") Integer limit);

    @GET
    @Path("/customer-services/item/{id}/upgrade-blocker")
    @Produces(
    {
        "application/json"
    })
    public Boolean manageUpgradeBlocker(@PathParam("id") String id, @QueryParam("until") ZonedDateTime until);

    @PATCH
    @Path("/customer-services/item/{id}/migrate")
    @Produces(
    {
        "application/json"
    })
    public BackgroundActivity migrate(@PathParam("id") String id);

    @GET
    @Path("/customer-services/item/{id}/refresh-accounts")
    @Produces(
    {
        "application/json"
    })
    public BackgroundActivity refreshAccounts(@PathParam("id") String id);

    @PATCH
    @Path("/customer-services/item/{id}/refresh-certificate")
    @Produces(
    {
        "application/json"
    })
    public BackgroundActivity refreshCertificate(@PathParam("id") String id);

    @GET
    @Path("/customer-services/item/{id}/refresh-secrets")
    @Produces(
    {
        "application/json"
    })
    public BackgroundActivity refreshSecrets(@PathParam("id") String id);

    @DELETE
    @Path("/customer-services/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public ValidationResults remove(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun);

    @GET
    @Path("/customer-services/item/{id}/alert-thresholds")
    @Produces(
    {
        "application/json"
    })
    public Boolean setAlertThresholds(@PathParam("id") String id, @QueryParam("role") String role, @QueryParam("warningThreshold") Integer warningThreshold, @QueryParam("alertThreshold") Integer alertThreshold);

    @POST
    @Path("/customer-services/item/{id}/battery-thresholds")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public Boolean setBatteryThresholds(@PathParam("id") String id, DeployerShutdownConfiguration body);

    @POST
    @Path("/customer-services/item/{id}")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public ValidationResults update(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun, CustomerService body);

}