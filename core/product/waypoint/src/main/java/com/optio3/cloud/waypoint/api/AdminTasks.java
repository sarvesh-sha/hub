/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.api;

import java.io.IOException;
import java.lang.management.ThreadInfo;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.exception.DetailedApplicationException;
import com.optio3.cloud.waypoint.WaypointApplication;
import com.optio3.cloud.waypoint.WaypointConfiguration;
import com.optio3.concurrency.Executors;
import com.optio3.infra.waypoint.BootConfig;
import com.optio3.logging.LoggerConfiguration;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.Severity;
import com.optio3.util.StackTraceAnalyzer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(tags = { "AdminTasks" }) // For Swagger
@Optio3RestEndpoint(name = "AdminTasks") // For Optio3 Shell
@Path("/v1/admin-tasks")
public class AdminTasks
{
    @Inject
    private WaypointApplication m_app;

    //--//

    @GET
    @Path("app-version")
    @Produces(MediaType.TEXT_PLAIN)
    @Optio3RequestLogLevel(Severity.Debug)
    public String getAppVersion()
    {
        return m_app.getAppVersion();
    }

    @GET
    @Path("production-mode")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public boolean isProductionMode()
    {
        WaypointConfiguration cfg = m_app.getServiceNonNull(WaypointConfiguration.class);

        return cfg.productionMode;
    }

    //--//

    @POST
    @Path("shutdown")
    @Produces(MediaType.TEXT_PLAIN)
    public String shutdown()
    {
        Executors.scheduleOnDefaultPool(() ->
                                        {
                                            Runtime.getRuntime()
                                                   .exit(0);
                                        }, 1, TimeUnit.SECONDS);

        return "Bye";
    }

    @POST
    @Path("reboot")
    @Produces(MediaType.TEXT_PLAIN)
    public String reboot()
    {
        Executors.scheduleOnDefaultPool(() ->
                                        {
                                            Runtime.getRuntime()
                                                   .exit(10);
                                        }, 1, TimeUnit.SECONDS);

        return "Bye";
    }

    @GET
    @Path("threads")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiResponses({ @ApiResponse(code = 412, message = "Error", response = DetailedApplicationException.ErrorDetails.class) })
    public String dumpThreads()
    {
        Map<StackTraceAnalyzer, List<ThreadInfo>> uniqueStackTraces = StackTraceAnalyzer.allThreadInfos();
        List<String>                              lines             = StackTraceAnalyzer.printThreadInfos(true, uniqueStackTraces);

        return String.join("\n", lines);
    }

    //--//

    @GET
    @Path("loggers/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<LoggerConfiguration> getLoggers()
    {
        return LoggerFactory.getLoggersConfiguration();
    }

    @POST
    @Path("loggers/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LoggerConfiguration configLogger(LoggerConfiguration cfg)
    {
        return LoggerFactory.setLoggerConfiguration(cfg);
    }

    //--//

    @GET
    @Path("options/{option}")
    @Produces(MediaType.APPLICATION_JSON)
    public BootConfig.Line getOption(@PathParam("option") BootConfig.Options option)
    {
        BootConfig bc = parseBootConfig();
        return bc != null ? bc.get(option, null) : null;
    }

    @POST
    @Path("options/{option}/{value}")
    @Produces(MediaType.APPLICATION_JSON)
    public BootConfig.Line setOption(@PathParam("option") BootConfig.Options option,
                                     @PathParam("value") String value) throws
                                                                       IOException
    {
        BootConfig bc = parseBootConfig();
        if (bc == null)
        {
            return null;
        }

        BootConfig.Line res = bc.set(option, null, value);

        saveBootConfig(bc);

        return res;
    }

    @DELETE
    @Path("options/{option}")
    @Produces(MediaType.APPLICATION_JSON)
    public BootConfig.Line unsetOption(@PathParam("option") BootConfig.Options option) throws
                                                                                       IOException
    {
        BootConfig bc = parseBootConfig();
        if (bc == null)
        {
            return null;
        }

        BootConfig.Line res = bc.unset(option, null);

        saveBootConfig(bc);

        return res;
    }

    //--//

    private BootConfig parseBootConfig()
    {
        WaypointConfiguration cfg = m_app.getServiceNonNull(WaypointConfiguration.class);
        return BootConfig.parse(cfg.bootConfig);
    }

    private void saveBootConfig(BootConfig bc) throws
                                               IOException
    {
        WaypointConfiguration cfg = m_app.getServiceNonNull(WaypointConfiguration.class);
        bc.save(cfg.bootConfig);
    }
}
