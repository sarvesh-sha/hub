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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.optio3.infra.docker.model.ExecConfig;
import com.optio3.infra.docker.model.ExecInspection;
import com.optio3.infra.docker.model.ExecStartCheck;
import com.optio3.infra.docker.model.IdResponse;

@Path("/")
public interface ExecApi
{
    @POST
    @Path("/containers/{id}/exec")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public IdResponse containerExec(ExecConfig execConfig,
                                    @PathParam("id") String id);

    @GET
    @Path("/exec/{id}/json")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json" })
    public ExecInspection execInspect(@PathParam("id") String id);

    @POST
    @Path("/exec/{id}/resize")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json", "text/plain" })
    public void execResize(@PathParam("id") String id,
                           @QueryParam("h") Integer h,
                           @QueryParam("w") Integer w);

    @POST
    @Path("/exec/{id}/start")
    @Consumes({ "application/json" })
    @Produces({ "application/vnd.docker.raw-stream" })
    public void execStart(@PathParam("id") String id,
                          ExecStartCheck execStartConfig);
}