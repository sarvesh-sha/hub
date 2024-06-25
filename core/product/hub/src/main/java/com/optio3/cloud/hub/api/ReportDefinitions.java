/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.model.report.ReportDefinition;
import com.optio3.cloud.hub.model.report.ReportDefinitionFilterRequest;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionRecord;
import com.optio3.cloud.model.RawImport;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "ReportDefinitions" }) // For Swagger
@Optio3RestEndpoint(name = "ReportDefinitions") // For Optio3 Shell
@Path("/v1/report-definitions")
public class ReportDefinitions
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
    @Path("filter")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<ReportDefinitionRecord> getFiltered(ReportDefinitionFilterRequest filters)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<ReportDefinitionRecord> helper   = sessionHolder.createHelper(ReportDefinitionRecord.class);
            UserRecord                           rec_user = m_cfg.getUserFromAccessor(sessionHolder, m_principalAccessor);

            return ReportDefinitionRecord.filter(helper, rec_user, filters);
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<ReportDefinition> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<ReportDefinitionRecord> helper = sessionHolder.createHelper(ReportDefinitionRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, ReportDefinitionRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ReportDefinition create(ReportDefinition model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<ReportDefinitionRecord> helper = sessionHolder.createHelper(ReportDefinitionRecord.class);

            ReportDefinitionRecord rec_reportDefinition = new ReportDefinitionRecord();

            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, model, rec_reportDefinition);

            UserRecord rec_user = m_cfg.getUserFromAccessor(sessionHolder, m_principalAccessor);
            rec_reportDefinition.setUser(rec_user);
            rec_reportDefinition.refreshNextActivation();

            helper.persist(rec_reportDefinition);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_reportDefinition);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public ReportDefinition get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, ReportDefinitionRecord.class, id);
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    ReportDefinition model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            RecordHelper<ReportDefinitionRecord> helper               = validation.sessionHolder.createHelper(ReportDefinitionRecord.class);
            ReportDefinitionRecord               rec_reportDefinition = helper.get(id);

            if (validation.canProceed())
            {
                ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec_reportDefinition);
                rec_reportDefinition.refreshNextActivation();
            }

            return validation.getResults();
        }
    }

    @DELETE
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults remove(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun) throws
                                                                          Exception
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            RecordHelper<ReportDefinitionRecord> helper               = validation.sessionHolder.createHelper(ReportDefinitionRecord.class);
            ReportDefinitionRecord               rec_reportDefinition = helper.getOrNull(id);
            if (rec_reportDefinition != null)
            {
                if (validation.canProceed())
                {
                    rec_reportDefinition.remove(validation, helper);
                }
            }

            return validation.getResults();
        }
    }

    @POST
    @Path("parse-import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ReportDefinition parseImport(RawImport rawImport)
    {
        return rawImport.validate(ReportDefinition.class);
    }
}
