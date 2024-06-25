/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.api;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.waypoint.WaypointApplication;
import com.optio3.cloud.waypoint.model.NetworkDestinationRequest;
import com.optio3.cloud.waypoint.model.NetworkDestinationResponse;
import com.optio3.cloud.waypoint.model.NetworkStatus;
import com.optio3.infra.NetworkHelper;
import io.swagger.annotations.Api;

@Api(tags = { "Networks" }) // For Swagger
@Optio3RestEndpoint(name = "Networks") // For Optio3 Shell
@Path("/v1/networks")
public class Networks
{
    @Inject
    private WaypointApplication m_app;

    @GET
    @Path("check-status")
    @Produces(MediaType.APPLICATION_JSON)
    public NetworkStatus checkStatus() throws
                                       IOException
    {
        NetworkStatus status = new NetworkStatus();

        for (NetworkHelper.InterfaceAddressDetails itfDetails : NetworkHelper.listNetworkAddresses(false, false, false, true, null))
        {
            status.interfaces.put(itfDetails.networkInterface.getName(), itfDetails.cidr.toString());
        }

        return status;
    }

    @POST
    @Path("check-destination")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public NetworkDestinationResponse checkDestination(NetworkDestinationRequest req)
    {
        NetworkDestinationResponse status = new NetworkDestinationResponse();

        status.checkDestination(req.url);

        return status;
    }
}
