/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.api;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.waypoint.WaypointApplication;
import com.optio3.cloud.waypoint.model.SensorConfig;
import com.optio3.cloud.waypoint.model.SensorResult;
import com.optio3.cloud.waypoint.model.SensorResultToken;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.IdGenerator;
import io.swagger.annotations.Api;

@Api(tags = { "Sensors" }) // For Swagger
@Optio3RestEndpoint(name = "Sensors") // For Optio3 Shell
@Path("/v1/sensors")
public class Sensors
{
    private static final Map<String, SensorResult> s_pending = Maps.newHashMap();

    @Inject
    private WaypointApplication m_app;

    @POST
    @Path("start-check-status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public SensorResultToken startCheckStatus(SensorConfig config) throws
                                                                   Exception
    {
        SensorResultToken token = new SensorResultToken();
        token.id = IdGenerator.newGuid();

        synchronized (SensorConfig.class) // Just to print in one shot.
        {
            WaypointApplication.LoggerInstance.info("[%s] Checking %s", token.id, ObjectMappers.prettyPrintAsJson(config));
        }

        CompletableFuture<? extends SensorResult> execution = config.exec(m_app);
        execution.whenComplete((val, err) ->
                               {
                                   synchronized (SensorConfig.class) // Just to print in one shot.
                                   {
                                       WaypointApplication.LoggerInstance.info("[%s] Result %s", token.id, ObjectMappers.prettyPrintAsJson(val));
                                   }

                                   synchronized (s_pending)
                                   {
                                       s_pending.put(token.id, val);
                                   }
                               });

        return token;
    }

    @POST
    @Path("check-status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public SensorResult checkStatus(SensorResultToken token) throws
                                                             Exception
    {
        synchronized (s_pending)
        {
            return s_pending.remove(token.id);
        }
    }
}
