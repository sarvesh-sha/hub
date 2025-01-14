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

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.infra.docker.model.ContainerChangeResponseItem;
import com.optio3.infra.docker.model.ContainerCreateCreatedBody;
import com.optio3.infra.docker.model.ContainerInspection;
import com.optio3.infra.docker.model.ContainerSummary;
import com.optio3.infra.docker.model.ContainerTopOKBody;
import com.optio3.infra.docker.model.ContainerUpdateOKBody;
import com.optio3.infra.docker.model.ContainerWaitOKBody;
import com.optio3.infra.docker.model.ContainersPruneReport;
import com.optio3.infra.docker.model.ExtendedConfig;
import com.optio3.infra.docker.model.ExtendedResources;

@Path("/")
public interface ContainerApi
{
    @GET
    @Path("/containers/{id}/archive")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/x-tar" })
    public Response containerArchive(@PathParam("id") String id,
                                     @QueryParam("path") String path,
                                     @QueryParam("copyUIDGID") Boolean copyUIDGID);

    @HEAD
    @Path("/containers/{id}/archive")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json", "text/plain" })
    public void containerArchiveInfo(@PathParam("id") String id,
                                     @QueryParam("path") String path);

    @POST
    @Path("/containers/{id}/attach")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/vnd.docker.raw-stream" })
    public void containerAttach(@PathParam("id") String id,
                                @QueryParam("detachKeys") String detachKeys,
                                @QueryParam("logs") Boolean logs,
                                @QueryParam("stream") Boolean stream,
                                @QueryParam("stdin") Boolean stdin,
                                @QueryParam("stdout") Boolean stdout,
                                @QueryParam("stderr") Boolean stderr);

    @GET
    @Path("/containers/{id}/attach/ws")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json", "text/plain" })
    public void containerAttachWebsocket(@PathParam("id") String id,
                                         @QueryParam("detachKeys") String detachKeys,
                                         @QueryParam("logs") Boolean logs,
                                         @QueryParam("stream") Boolean stream,
                                         @QueryParam("stdin") Boolean stdin,
                                         @QueryParam("stdout") Boolean stdout,
                                         @QueryParam("stderr") Boolean stderr);

    @GET
    @Path("/containers/{id}/changes")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json" })
    public List<ContainerChangeResponseItem> containerChanges(@PathParam("id") String id);

    @POST
    @Path("/containers/create")
    @Consumes({ "application/json", "application/octet-stream" })
    @Produces({ "application/json" })
    public ContainerCreateCreatedBody containerCreate(ExtendedConfig body,
                                                      @QueryParam("name") String name);

    @DELETE
    @Path("/containers/{id}")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json", "text/plain" })
    public void containerDelete(@PathParam("id") String id,
                                @QueryParam("v") Boolean v,
                                @QueryParam("force") Boolean force,
                                @QueryParam("link") Boolean link);

    @GET
    @Path("/containers/{id}/export")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/octet-stream" })
    public void containerExport(@PathParam("id") String id);

    @GET
    @Path("/containers/{id}/json")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json" })
    public ContainerInspection containerInspect(@PathParam("id") String id,
                                                @QueryParam("size") Boolean size);

    @POST
    @Path("/containers/{id}/kill")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json", "text/plain" })
    public void containerKill(@PathParam("id") String id,
                              @QueryParam("signal") String signal);

    @GET
    @Path("/containers/json")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json" })
    public List<ContainerSummary> containerList(@QueryParam("all") Boolean all,
                                                @QueryParam("limit") Integer limit,
                                                @QueryParam("size") Boolean size,
                                                @QueryParam("filters") String filters);

    @GET
    @Path("/containers/{id}/logs")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json", "text/plain" })
    public Response containerLogs(@PathParam("id") String id,
                                  @QueryParam("follow") Boolean follow,
                                  @QueryParam("stdout") Boolean stdout,
                                  @QueryParam("stderr") Boolean stderr,
                                  @QueryParam("since") Integer since,
                                  @QueryParam("timestamps") Boolean timestamps,
                                  @QueryParam("tail") String tail);

    @POST
    @Path("/containers/{id}/pause")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json", "text/plain" })
    public void containerPause(@PathParam("id") String id);

    @POST
    @Path("/containers/prune")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json" })
    public ContainersPruneReport containerPrune(@QueryParam("filters") String filters);

    @POST
    @Path("/containers/{id}/rename")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json", "text/plain" })
    public void containerRename(@PathParam("id") String id,
                                @QueryParam("name") String name);

    @POST
    @Path("/containers/{id}/resize")
    @Consumes({ "application/octet-stream" })
    @Produces({ "text/plain" })
    public void containerResize(@PathParam("id") String id,
                                @QueryParam("h") Integer h,
                                @QueryParam("w") Integer w);

    @POST
    @Path("/containers/{id}/restart")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json", "text/plain" })
    public void containerRestart(@PathParam("id") String id,
                                 @QueryParam("t") Integer t);

    @POST
    @Path("/containers/{id}/start")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json", "text/plain" })
    public void containerStart(@PathParam("id") String id,
                               @QueryParam("detachKeys") String detachKeys);

    @GET
    @Path("/containers/{id}/stats")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json" })
    public JsonNode containerStats(@PathParam("id") String id,
                                   @QueryParam("stream") Boolean stream);

    @POST
    @Path("/containers/{id}/stop")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json", "text/plain" })
    public void containerStop(@PathParam("id") String id,
                              @QueryParam("t") Integer t);

    @GET
    @Path("/containers/{id}/top")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json", "text/plain" })
    public ContainerTopOKBody containerTop(@PathParam("id") String id,
                                           @QueryParam("ps_args") String ps_args);

    @POST
    @Path("/containers/{id}/unpause")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json", "text/plain" })
    public void containerUnpause(@PathParam("id") String id);

    @POST
    @Path("/containers/{id}/update")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public ContainerUpdateOKBody containerUpdate(@PathParam("id") String id,
                                                 ExtendedResources update);

    @POST
    @Path("/containers/{id}/wait")
    @Consumes({ "application/json", "text/plain" })
    @Produces({ "application/json" })
    public ContainerWaitOKBody containerWait(@PathParam("id") String id);

    @PUT
    @Path("/containers/{id}/archive")
    @Consumes({ "application/x-tar", "application/octet-stream" })
    @Produces({ "application/json", "text/plain" })
    public void putContainerArchive(@PathParam("id") String id,
                                    @QueryParam("path") String path,
                                    InputStream inputStream,
                                    @QueryParam("noOverwriteDirNonDir") String noOverwriteDirNonDir);
}
