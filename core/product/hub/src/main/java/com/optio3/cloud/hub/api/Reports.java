/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3NoAuthenticationNeeded;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.model.report.Report;
import com.optio3.cloud.hub.model.report.ReportFilterRequest;
import com.optio3.cloud.hub.model.report.ReportReason;
import com.optio3.cloud.hub.model.report.ReportStatus;
import com.optio3.cloud.hub.orchestration.tasks.TaskForReportGeneration;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionRecord;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.report.ReportRecord;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.TimeUtils;
import io.swagger.annotations.Api;

@Api(tags = { "Reports" }) // For Swagger
@Optio3RestEndpoint(name = "Reports") // For Optio3 Shell
@Path("/v1/reports")
public class Reports
{
    @Inject
    private HubApplication m_app;

    @Inject
    private HubConfiguration m_cfg;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Report create(Report model) throws
                                       Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<ReportRecord> helper = sessionHolder.createHelper(ReportRecord.class);

            ReportDefinitionVersionRecord rec_reportDefinitionVersion = sessionHolder.getEntity(ReportDefinitionVersionRecord.class, model.reportDefinitionVersion.sysId);
            ReportDefinitionRecord        rec_reportDefinition        = sessionHolder.getEntity(ReportDefinitionRecord.class, model.reportDefinition.sysId);

            ReportRecord rec_report = ReportRecord.newInstance(rec_reportDefinition, rec_reportDefinitionVersion);
            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, model, rec_report);
            rec_report.setReason(ReportReason.OnDemand);
            helper.persist(rec_report);

            TaskForReportGeneration.scheduleTask(sessionHolder, rec_report, rec_report.getCreatedOn());

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_report);
        }
    }

    @POST
    @Path("item/{id}/retry")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Report retry(@PathParam("id") String sysId) throws
                                                       Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            ReportRecord rec_report = sessionHolder.getEntity(ReportRecord.class, sysId);

            if (rec_report.getStatus() == ReportStatus.Failed)
            {
                TaskForReportGeneration.scheduleTask(sessionHolder, rec_report, TimeUtils.now());
            }

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_report);
        }
    }

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<ReportRecord> getAll()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            UserRecord rec_user = m_cfg.getUserFromAccessor(sessionHolder, m_principalAccessor);

            RecordHelper<ReportRecord> helper = sessionHolder.createHelper(ReportRecord.class);

            return ReportRecord.getAllByUser(helper, rec_user);
        }
    }

    @POST
    @Path("filter")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<RecordIdentity> getFiltered(ReportFilterRequest filters)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<ReportRecord> helper = sessionHolder.createHelper(ReportRecord.class);

            return ReportRecord.filterReports(helper, filters);
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<Report> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<ReportRecord> helper = sessionHolder.createHelper(ReportRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, ReportRecord.getBatch(helper, ids));
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public Report get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, ReportRecord.class, id);
    }

    @GET
    @Path("item/{id}/{filename}")
    @Produces("application/pdf")
    @Optio3NoAuthenticationNeeded
    public byte[] downloadReport(@PathParam("id") String id,
                                 @PathParam("filename") String filename)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            ReportRecord rec_report = sessionHolder.getEntity(ReportRecord.class, id);

            return rec_report.getBytes();
        }
    }
}
