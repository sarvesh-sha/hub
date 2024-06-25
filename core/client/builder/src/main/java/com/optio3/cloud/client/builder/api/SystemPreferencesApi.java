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

import com.optio3.cloud.client.builder.model.SystemPreference;

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
public interface SystemPreferencesApi
{
    @POST
    @Path("/system-preferences/value/check")
    @Consumes(
    {
        "application/x-www-form-urlencoded"
    })
    @Produces(
    {
        "text/plain"
    })
    public Boolean checkValueFormat(@FormParam(value = "path") String path, @FormParam(value = "name") String name, @FormParam(value = "value") String value);

    @POST
    @Path("/system-preferences/value/get")
    @Consumes(
    {
        "application/x-www-form-urlencoded"
    })
    @Produces(
    {
        "application/json"
    })
    public SystemPreference getValue(@FormParam(value = "path") String path, @FormParam(value = "name") String name);

    @POST
    @Path("/system-preferences/subkey/list")
    @Consumes(
    {
        "application/x-www-form-urlencoded"
    })
    @Produces(
    {
        "application/json"
    })
    public List<String> listSubKeys(@FormParam(value = "path") String path);

    @POST
    @Path("/system-preferences/value/list")
    @Consumes(
    {
        "application/x-www-form-urlencoded"
    })
    @Produces(
    {
        "application/json"
    })
    public List<String> listValues(@FormParam(value = "path") String path);

    @POST
    @Path("/system-preferences/subkey/remove")
    @Consumes(
    {
        "application/x-www-form-urlencoded"
    })
    @Produces(
    {
        "text/plain"
    })
    public Boolean removeSubKeys(@FormParam(value = "path") String path);

    @POST
    @Path("/system-preferences/value/remove")
    @Consumes(
    {
        "application/x-www-form-urlencoded"
    })
    @Produces(
    {
        "text/plain"
    })
    public Boolean removeValue(@FormParam(value = "path") String path, @FormParam(value = "name") String name);

    @POST
    @Path("/system-preferences/value/set")
    @Consumes(
    {
        "application/x-www-form-urlencoded"
    })
    @Produces(
    {
        "text/plain"
    })
    public String setValue(@FormParam(value = "path") String path, @FormParam(value = "name") String name, @FormParam(value = "value") String value);

}
