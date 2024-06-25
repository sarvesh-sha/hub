/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Docker Engine API
 * The Engine API is an HTTP API served by Docker Engine. It is the API the Docker client uses to communicate with the Engine, so everything the Docker client can do can be done with the API.
 *
 * OpenAPI spec version: 1.28
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.infra.docker.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.optio3.infra.docker.model.ExtendedSecretSpec;
import com.optio3.infra.docker.model.Secret;
import com.optio3.infra.docker.model.SecretCreateResponse;
import com.optio3.infra.docker.model.SecretSpec;

@Path("/")
public interface SecretApi
{
    @POST
    @Path("/secrets/create")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public SecretCreateResponse secretCreate(ExtendedSecretSpec body);

    @DELETE
    @Path("/secrets/{id}")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json" })
    public void secretDelete(@PathParam("id") String id);

    @GET
    @Path("/secrets/{id}")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json" })
    public Secret secretInspect(@PathParam("id") String id);

    @GET
    @Path("/secrets")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json" })
    public List<Secret> secretList(@QueryParam("filters") String filters);

    @POST
    @Path("/secrets/{id}/update")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json", "text/plain" })
    public void secretUpdate(@PathParam("id") String id,
                             @QueryParam("version") Long version,
                             SecretSpec body);
}
