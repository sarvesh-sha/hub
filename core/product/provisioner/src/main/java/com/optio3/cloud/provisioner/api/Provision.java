/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.provisioner.api;

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
import com.optio3.cloud.provisioner.ProvisionerApplication;
import com.optio3.cloud.provisioner.ProvisionerConfiguration;
import com.optio3.cloud.provisioner.model.FlashingProgress;
import com.optio3.cloud.provisioner.model.ProvisionReportExt;
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
    private ProvisionerApplication m_app;

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
    @Produces(MediaType.TEXT_PLAIN)
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
        ProvisionerConfiguration cfg = m_app.getServiceNonNull(ProvisionerConfiguration.class);
        report.manufacturingLocation = cfg.hostId;
        report.timestamp             = TimeUtils.now();

        return m_app.recordCheckin(report);
    }

    //--//

    @GET
    @Path("firmware/trigger")
    @Produces(MediaType.TEXT_PLAIN)
    @Optio3RequestLogLevel(Severity.Debug)
    public long triggerFirmwareDownload()
    {
        m_app.queueFirmwareCheck(0, false);

        return m_app.downloadingFirmware();
    }

    @GET
    @Path("firmware/downloading")
    @Produces(MediaType.TEXT_PLAIN)
    @Optio3RequestLogLevel(Severity.Debug)
    public long downloadingFirmware()
    {
        return m_app.downloadingFirmware();
    }

    @GET
    @Path("board/power-switch-supported")
    @Produces(MediaType.TEXT_PLAIN)
    @Optio3RequestLogLevel(Severity.Debug)
    public String powerSwitchSupported()
    {
        return m_app.isPowerSupported() ? "Supported" : null;
    }

    @GET
    @Path("board/power-on")
    @Produces(MediaType.TEXT_PLAIN)
    @Optio3RequestLogLevel(Severity.Debug)
    public String boardPowerOn()
    {
        return m_app.powerBoard(true);
    }

    @GET
    @Path("board/power-off")
    @Produces(MediaType.TEXT_PLAIN)
    @Optio3RequestLogLevel(Severity.Debug)
    public String boardPowerOff()
    {
        return m_app.powerBoard(false);
    }

    @GET
    @Path("board/detect")
    @Produces(MediaType.TEXT_PLAIN)
    @Optio3RequestLogLevel(Severity.Debug)
    public String detectBoard()
    {
        return m_app.detectBoard();
    }

    @GET
    @Path("board/write/start/{serialNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public FlashingProgress startBoardFlashing(@PathParam("serialNumber") String serialNumber)
    {
        return m_app.startBoardFlashing(serialNumber);
    }

    @GET
    @Path("board/write/progress")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public FlashingProgress checkBoardFlashing()
    {
        return m_app.checkBoardFlashing();
    }
}
