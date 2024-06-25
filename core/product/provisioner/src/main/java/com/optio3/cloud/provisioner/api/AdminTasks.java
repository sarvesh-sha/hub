/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.provisioner.api;

import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.provisioner.ProvisionerApplication;
import com.optio3.cloud.provisioner.ProvisionerConfiguration;
import com.optio3.logging.Severity;
import com.optio3.util.TimeUtils;
import io.swagger.annotations.Api;

@Api(tags = { "AdminTasks" }) // For Swagger
@Optio3RestEndpoint(name = "AdminTasks") // For Optio3 Shell
@Path("/v1/admin-tasks")
public class AdminTasks
{
    @Inject
    private ProvisionerApplication m_app;

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
        ProvisionerConfiguration cfg = m_app.getServiceNonNull(ProvisionerConfiguration.class);

        return cfg.productionMode;
    }

    @GET
    @Path("factor-floor-mode")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public boolean isFactoryFloorMode()
    {
        ProvisionerConfiguration cfg = m_app.getServiceNonNull(ProvisionerConfiguration.class);

        return cfg.factoryFloorMode;
    }

    @POST
    @Path("log")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public ZonedDateTime log(@FormParam("level") Severity level,
                             @FormParam("text") String text)
    {
        ProvisionerApplication.LoggerInstance.log(null, level, null, text);

        return TimeUtils.now();
    }
}
