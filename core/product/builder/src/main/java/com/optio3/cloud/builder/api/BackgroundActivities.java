/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.util.List;

import javax.annotation.security.RolesAllowed;
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
import com.optio3.cloud.builder.model.scheduler.BackgroundActivity;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.model.scheduler.BackgroundActivityDetails;
import com.optio3.cloud.model.scheduler.BackgroundActivityFilterRequest;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "BackgroundActivities" }) // For Swagger
@Optio3RestEndpoint(name = "BackgroundActivities") // For Optio3 Shell
@Path("/v1/background-activities")
public class BackgroundActivities
{
    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("filtered")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<BackgroundActivityRecord> getFiltered(BackgroundActivityFilterRequest filters)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return BackgroundActivityRecord.list(helper, filters, null);
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<BackgroundActivity> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, BackgroundActivityRecord.getBatch(helper, ids));
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public BackgroundActivity get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, BackgroundActivityRecord.class, id);
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    BackgroundActivity model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            BackgroundActivityRecord rec = validation.sessionHolder.getEntity(BackgroundActivityRecord.class, id);

            if (validation.canProceed())
            {
                ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec);
            }

            return validation.getResults();
        }
    }

    @GET
    @Path("item/{id}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    public BackgroundActivity cancel(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);
            BackgroundActivityRecord               rec    = BackgroundActivityRecord.cancelActivity(helper, id);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{id}/details")
    @Produces(MediaType.APPLICATION_JSON)
    public BackgroundActivityDetails details(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            BackgroundActivityRecord rec = sessionHolder.getEntityOrNull(BackgroundActivityRecord.class, id);
            if (rec != null)
            {
                BackgroundActivityDetails res = new BackgroundActivityDetails();
                res.handlerState = rec.getHandlerStateAsJson();
                return res;
            }

            return null;
        }
    }

    @DELETE
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults remove(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkAnyRoles(m_principalAccessor, WellKnownRoleIds.Administrator);

            RecordHelper<BackgroundActivityRecord> helper = validation.sessionHolder.createHelper(BackgroundActivityRecord.class);
            BackgroundActivityRecord               rec    = helper.getOrNull(id);
            if (rec != null)
            {
                rec.remove(validation, helper);
            }

            return validation.getResults();
        }
    }
}
