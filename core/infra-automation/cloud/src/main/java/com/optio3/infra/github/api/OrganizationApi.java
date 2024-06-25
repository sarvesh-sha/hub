/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

package com.optio3.infra.github.api;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.optio3.infra.github.model.Organization;
import com.optio3.infra.github.model.Repository;

@Path("/")
public interface OrganizationApi
{
    @GET
    @Path("/user/orgs")
    @Produces({ "application/json" })
    public List<Organization> getOrganization();

    @GET
    @Path("/orgs/{owner}/repos")
    @Produces({ "application/json" })
    public List<Repository> listRepositories(@PathParam("owner") String owner);
}
