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

import com.optio3.cloud.client.hub.model.DetailedApplicationExceptionErrorDetails;
import com.optio3.cloud.client.hub.model.User;
import com.optio3.cloud.client.hub.model.UserCreationRequest;
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
public interface UsersApi
{
    @POST
    @Path("/users/item/{id}/changePwd")
    @Consumes(
    {
        "application/x-www-form-urlencoded"
    })
    @Produces(
    {
        "application/json"
    })
    public User changePassword(@PathParam("id") String id, @FormParam(value = "currentPassword") String currentPassword, @FormParam(value = "newPassword") String newPassword);

    @POST
    @Path("/users/create")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public User create(UserCreationRequest body);

    @POST
    @Path("/users/forgotPwd")
    @Consumes(
    {
        "application/x-www-form-urlencoded"
    })
    @Produces(
    {
        "application/json"
    })
    public User forgotPassword(@FormParam(value = "emailAddress") String emailAddress);

    @GET
    @Path("/users/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public User get(@PathParam("id") String id);

    @GET
    @Path("/users/all")
    @Produces(
    {
        "application/json"
    })
    public List<User> getAll();

    @GET
    @Path("/users/impersonate/{userId}")
    @Produces(
    {
        "application/json"
    })
    public User impersonate(@PathParam("userId") String userId);

    @POST
    @Path("/users/login")
    @Consumes(
    {
        "application/x-www-form-urlencoded"
    })
    @Produces(
    {
        "application/json"
    })
    public User login(@FormParam(value = "username") String username, @FormParam(value = "password") String password);

    @GET
    @Path("/users/logout")
    public String logout();

    @DELETE
    @Path("/users/item/{id}")
    @Produces(
    {
        "application/json"
    })
    public ValidationResults remove(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun);

    @POST
    @Path("/users/resetPwd")
    @Consumes(
    {
        "application/x-www-form-urlencoded"
    })
    @Produces(
    {
        "application/json"
    })
    public User resetPassword(@FormParam(value = "emailAddress") String emailAddress, @FormParam(value = "token") String token, @FormParam(value = "newPassword") String newPassword);

    @POST
    @Path("/users/item/{id}")
    @Consumes(
    {
        "application/json"
    })
    @Produces(
    {
        "application/json"
    })
    public ValidationResults update(@PathParam("id") String id, @QueryParam("dryRun") Boolean dryRun, User body);

}
