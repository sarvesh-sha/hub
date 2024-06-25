/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

package com.optio3.infra.github.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.optio3.infra.github.model.WebHook;

@Path("/")
public interface RepoApi
{
    @GET
    @Path("/repos/{owner}/{repo}/hooks")
    @Produces({ "application/json" })
    public List<WebHook> listHooks(@PathParam("owner") String owner,
                                   @PathParam("repo") String repo);

    @POST
    @Path("/repos/{owner}/{repo}/hooks")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public WebHook createHook(@PathParam("owner") String owner,
                              @PathParam("repo") String repo,
                              WebHook hook);

    @GET
    @Path("/repos/{owner}/{repo}/hooks/{id}")
    @Produces({ "application/json" })
    public WebHook getHook(@PathParam("owner") String owner,
                           @PathParam("repo") String repo,
                           @PathParam("id") int id);

    @DELETE
    @Path("/repos/{owner}/{repo}/hooks/{id}")
    @Produces({ "application/json" })
    public void deleteHook(@PathParam("owner") String owner,
                           @PathParam("repo") String repo,
                           @PathParam("id") int id);

    @POST
    @Path("/repos/{owner}/{repo}/hooks/{id}/tests")
    @Produces({ "application/json" })
    public void testHook(@PathParam("owner") String owner,
                         @PathParam("repo") String repo,
                         @PathParam("id") int id);
}
