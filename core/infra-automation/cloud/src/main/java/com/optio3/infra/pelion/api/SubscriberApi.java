/*
 * Copyright (C) 2017-2020, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.pelion.api;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.optio3.infra.pelion.model.ApiResults;
import com.optio3.infra.pelion.model.SubscriberResponse;

@Path("/")
public interface SubscriberApi
{
    @GET
    @Path("/subscribers/{physicalId}")
    @Produces({ "application/json" })
    public ApiResults<SubscriberResponse> getSubscriber(@PathParam("physicalId") String physicalId);

    @GET
    @Path("/subscribers")
    @Produces({ "application/json" })
    public ApiResults<List<SubscriberResponse>> getSubscribers(@QueryParam("limit") Integer limit,
                                                               @QueryParam("offset") Integer offset,
                                                               @QueryParam("filter") String filter,
                                                               @QueryParam("orderBy") String orderBy);
}
