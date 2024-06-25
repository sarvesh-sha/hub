/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.api;

import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.client.builder.model.ProvisionReport;
import com.optio3.cloud.waypoint.WaypointApplication;
import com.optio3.cloud.waypoint.WaypointConfiguration;
import com.optio3.cloud.waypoint.model.FlashingProgress;
import com.optio3.cloud.waypoint.model.ProvisionReportExt;
import com.optio3.logging.Severity;
import com.optio3.util.TimeUtils;
import io.dropwizard.jersey.jsr310.ZonedDateTimeParam;
import io.swagger.annotations.Api;

@Api(tags = { "Provision" }) // For Swagger
@Optio3RestEndpoint(name = "Provision") // For Optio3 Shell
@Path("/v1/provision")
public class Provision
{
    @Inject
    private WaypointApplication m_app;

    @GET
    @Path("checkins")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<ProvisionReportExt> getNewCheckins(@QueryParam("when") ZonedDateTimeParam fromParam)
    {
        ZonedDateTime from = fromParam != null ? fromParam.get() : null;

        return m_app.getNewCheckins(from);
    }

    @GET
    @Path("checkin/print/{hostId}")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean printCheckin(@PathParam("hostId") String hostId)
    {
        return m_app.printCheckin(hostId);
    }

    @POST
    @Path("checkin/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public boolean performCheckin(ProvisionReport report)
    {
        WaypointConfiguration cfg = m_app.getServiceNonNull(WaypointConfiguration.class);
        report.manufacturingLocation = cfg.hostId;
        report.timestamp = TimeUtils.now();

        return m_app.recordCheckin(report);
    }

    //--//

    @GET
    @Path("firmware/trigger")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public long triggerFirmwareDownload()
    {
        m_app.queueFirmwareCheck(0, false);

        return m_app.downloadingFirmware();
    }

    @GET
    @Path("firmware/downloading")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public long downloadingFirmware()
    {
        return m_app.downloadingFirmware();
    }

    @GET
    @Path("card/detect")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public long detectCard()
    {
        return m_app.detectCard();
    }

    @GET
    @Path("card/write/start")
    @Produces(MediaType.APPLICATION_JSON)
    public FlashingProgress startCardFlashing()
    {
        return m_app.startCardFlashing();
    }

    @GET
    @Path("card/write/progress")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public FlashingProgress checkCardFlashing()
    {
        return m_app.checkCardFlashing();
    }
}
