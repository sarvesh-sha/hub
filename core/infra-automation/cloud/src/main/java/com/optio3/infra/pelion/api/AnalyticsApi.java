/*
 * Copyright (C) 2017-2020, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.pelion.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.optio3.infra.pelion.model.ApiResults;
import com.optio3.infra.pelion.model.DataUsage;
import com.optio3.infra.pelion.model.DataUsageByIpAddress;
import com.optio3.infra.pelion.model.PelionDateTime;

@Path("/")
public interface AnalyticsApi
{
    @GET
    @Path("/analytics/data-usage/{physicalId}")
    @Produces({ "application/json" })
    public ApiResults<DataUsage> getDataUsage(@QueryParam("startDate") PelionDateTime startDate,
                                              @QueryParam("endDate") PelionDateTime endDate,
                                              @PathParam("physicalId") String physicalId);

    @GET
    @Path("/analytics/data-usage-ip-address/{physicalId}")
    @Produces({ "application/json" })
    public ApiResults<DataUsageByIpAddress> getDataUsageByIpAddress(@QueryParam("startDate") PelionDateTime startDate,
                                                                    @QueryParam("endDate") PelionDateTime endDate,
                                                                    @PathParam("physicalId") String physicalId);
}
