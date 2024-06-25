/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.hub.model.timeseries.EngineeringUnitsConversionRequest;
import com.optio3.cloud.hub.model.timeseries.EngineeringUnitsConversionResponse;
import com.optio3.cloud.hub.model.timeseries.EngineeringUnitsDescriptor;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.util.CollectionUtils;
import io.swagger.annotations.Api;

@Api(tags = { "Units" }) // For Swagger
@Optio3RestEndpoint(name = "Units") // For Optio3 Shell
@Path("/v1/units")
@Optio3RequestLogLevel(Severity.Debug)
public class Units
{
    @GET
    @Path("describe")
    @Produces(MediaType.APPLICATION_JSON)
    public List<EngineeringUnitsDescriptor> describe()
    {
        return CollectionUtils.transformToList(Lists.newArrayList(EngineeringUnits.values()), EngineeringUnitsDescriptor::new);
    }

    @POST
    @Path("convert")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public EngineeringUnitsConversionResponse convert(EngineeringUnitsConversionRequest request)
    {
        return request.convert();
    }

    @POST
    @Path("compact")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public EngineeringUnitsFactors compact(EngineeringUnitsFactors unitsFactors)
    {
        if (unitsFactors == null)
        {
            return null;
        }

        return unitsFactors.compact();
    }

    @POST
    @Path("simplify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public EngineeringUnitsFactors simplify(EngineeringUnitsFactors unitsFactors)
    {
        if (unitsFactors == null)
        {
            return null;
        }

        return unitsFactors.simplify();
    }
}
