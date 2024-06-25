/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.time.Duration;
import java.util.List;

import javax.annotation.security.RolesAllowed;
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
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.model.dashboard.DashboardConfiguration;
import com.optio3.cloud.hub.model.dashboard.DashboardDefinition;
import com.optio3.cloud.hub.model.dashboard.DashboardDefinitionVersion;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.dashboard.DashboardDefinitionRecord;
import com.optio3.cloud.hub.persistence.dashboard.DashboardDefinitionVersionRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.BoxingUtils;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;

@Api(tags = { "DashboardDefinitions" }) // For Swagger
@Optio3RestEndpoint(name = "DashboardDefinitions") // For Optio3 Shell
@Path("/v1/dashboard-definitions")
public class DashboardDefinitions
{
    @Inject
    private HubApplication m_app;

    @Inject
    private HubConfiguration m_cfg;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<DashboardDefinitionRecord> getAll()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<DashboardDefinitionRecord> helper = sessionHolder.createHelper(DashboardDefinitionRecord.class);

            UserRecord rec_user = m_cfg.getUserFromAccessor(sessionHolder, m_principalAccessor);
            return DashboardDefinitionRecord.getAllByUser(helper, rec_user);
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<DashboardDefinition> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<DashboardDefinitionRecord> helper = sessionHolder.createHelper(DashboardDefinitionRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, DashboardDefinitionRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DashboardDefinition create(DashboardDefinition model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<DashboardDefinitionRecord> helper = sessionHolder.createHelper(DashboardDefinitionRecord.class);

            DashboardDefinitionRecord rec_dashboardDefinition = new DashboardDefinitionRecord();

            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, model, rec_dashboardDefinition);

            UserRecord rec_user = m_cfg.getUserFromAccessor(sessionHolder, m_principalAccessor);
            rec_dashboardDefinition.setUser(rec_user);

            helper.persist(rec_dashboardDefinition);

            var res = ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_dashboardDefinition);
            sessionHolder.commit();
            return res;
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DashboardDefinition get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, DashboardDefinitionRecord.class, id);
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    DashboardDefinition model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            RecordHelper<DashboardDefinitionRecord> helper                  = validation.sessionHolder.createHelper(DashboardDefinitionRecord.class);
            DashboardDefinitionRecord               rec_dashboardDefinition = helper.get(id);

            if (validation.canProceed())
            {
                ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec_dashboardDefinition);
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
            RecordHelper<DashboardDefinitionRecord> helper                  = validation.sessionHolder.createHelper(DashboardDefinitionRecord.class);
            DashboardDefinitionRecord               rec_dashboardDefinition = helper.getOrNull(id);
            if (rec_dashboardDefinition != null)
            {
                if (validation.canProceed())
                {
                    rec_dashboardDefinition.remove(validation, helper);
                }
            }

            return validation.getResults();
        }
    }

    @GET
    @Path("item/{id}/history")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<DashboardDefinitionVersion> getHistory(@PathParam("id") String sysId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<DashboardDefinitionRecord> helper = sessionHolder.createHelper(DashboardDefinitionRecord.class);

            DashboardDefinitionRecord        rec_def = helper.get(sysId);
            List<DashboardDefinitionVersion> lst     = rec_def.getHistory(sessionHolder);
            if (rec_def.pruneHistory(sessionHolder, lst, 20, Duration.ofMinutes(15)))
            {
                sessionHolder.commit();

                lst = rec_def.getHistory(sessionHolder);
            }

            return lst;
        }
    }

    @POST
    @Path("admin/push")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Publisher })
    public ValidationResults push(@QueryParam("dryRun") Boolean dryRun,
                                  @QueryParam("create") Boolean create,
                                  DashboardConfiguration model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            UserRecord                                     rec_currentUser   = m_cfg.getUserFromAccessor(validation.sessionHolder, m_principalAccessor);
            RecordHelper<UserRecord>                       helper_user       = validation.sessionHolder.createHelper(UserRecord.class);
            RecordHelper<DashboardDefinitionRecord>        helper_definition = validation.sessionHolder.createHelper(DashboardDefinitionRecord.class);
            RecordHelper<DashboardDefinitionVersionRecord> helper_version    = validation.sessionHolder.createHelper(DashboardDefinitionVersionRecord.class);

            DashboardDefinitionVersion version = new DashboardDefinitionVersion();
            version.details = model;

            for (UserRecord rec_user : helper_user.listAll())
            {
                try
                {
                    if (rec_user.isFromLdap() || rec_currentUser == rec_user)
                    {
                        continue;
                    }

                    boolean                         found       = false;
                    List<DashboardDefinitionRecord> definitions = QueryHelperWithCommonFields.getBatch(helper_definition, DashboardDefinitionRecord.getAllByUser(helper_definition, rec_user));
                    for (DashboardDefinitionRecord rec_dashboard : definitions)
                    {
                        DashboardDefinitionVersionRecord rec_head    = rec_dashboard.getHeadVersion();
                        DashboardConfiguration           headDetails = rec_head.getDetails();
                        if (StringUtils.equals(headDetails.title, model.title))
                        {
                            found = true;

                            if (validation.canProceed())
                            {
                                DashboardDefinitionVersionRecord.newInstance(helper_version, rec_dashboard, rec_head, version, null, false);
                            }
                        }
                    }

                    if (!found && BoxingUtils.get(create))
                    {
                        if (validation.canProceed())
                        {
                            DashboardDefinitionRecord rec_dashboardNew = new DashboardDefinitionRecord();
                            rec_dashboardNew.setUser(rec_user);
                            helper_definition.persist(rec_dashboardNew);

                            DashboardDefinitionVersionRecord.newInstance(helper_version, rec_dashboardNew, null, version, null, false);
                        }
                    }
                }
                catch (Throwable t)
                {
                }
            }

            return validation.getResults();
        }
    }
}
