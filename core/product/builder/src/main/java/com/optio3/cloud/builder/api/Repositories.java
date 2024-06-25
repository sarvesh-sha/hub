/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

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
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.model.jobs.input.Repository;
import com.optio3.cloud.builder.model.jobs.input.RepositoryCommit;
import com.optio3.cloud.builder.model.jobs.input.RepositoryRefresh;
import com.optio3.cloud.builder.orchestration.tasks.bookkeeping.TaskForRepositoryRefresh;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryCommitRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.BoxingUtils;
import io.swagger.annotations.Api;

@Api(tags = { "Repositories" }) // For Swagger
@Optio3RestEndpoint(name = "Repositories") // For Optio3 Shell
@Path("/v1/repositories")
@Optio3RequestLogLevel(Severity.Debug)
public class Repositories
{
    @Inject
    private BuilderApplication m_app;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    //--//

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    public TypedRecordIdentityList<RepositoryRecord> getAll()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<RepositoryRecord> helper = sessionHolder.createHelper(RepositoryRecord.class);

            return RepositoryRecord.list(helper);
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Repository> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<RepositoryRecord> helper = sessionHolder.createHelper(RepositoryRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, RepositoryRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("create")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public Repository create(Repository model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<RepositoryRecord> helper = sessionHolder.createHelper(RepositoryRecord.class);

            RepositoryRecord rec = new RepositoryRecord();

            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, model, rec);

            helper.persist(rec);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Repository get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, RepositoryRecord.class, id);
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    Repository model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            RecordHelper<RepositoryRecord> helper = validation.sessionHolder.createHelper(RepositoryRecord.class);

            RepositoryRecord rec = helper.get(id);

            if (validation.canProceed())
            {
                ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec);
            }

            return validation.getResults();
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
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            RecordHelper<RepositoryRecord> helper = validation.sessionHolder.createHelper(RepositoryRecord.class);
            RepositoryRecord               rec    = helper.getOrNull(id);
            if (rec != null)
            {
                if (validation.canProceed())
                {
                    helper.delete(rec);
                }
            }

            return validation.getResults();
        }
    }

    @GET
    @Path("item/{id}/{hash}")
    @Produces(MediaType.APPLICATION_JSON)
    public RepositoryCommit getCommit(@PathParam("id") String id,
                                      @PathParam("hash") String hash)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RepositoryRecord       rec_repository = sessionHolder.getEntity(RepositoryRecord.class, id);
            RepositoryCommitRecord rec_commit     = rec_repository.findCommitByHash(sessionHolder.createHelper(RepositoryCommitRecord.class), hash);

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_commit);
        }
    }

    //--//

    @GET
    @Path("refresh/start")
    @Produces(MediaType.TEXT_PLAIN)
    public String startRefresh() throws
                                 Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, TaskForRepositoryRefresh::scheduleTask);

        return loc_task.getIdRaw();
    }

    @GET
    @Path("refresh/check/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public RepositoryRefresh checkRefresh(@PathParam("id") String id,
                                          @QueryParam("detailed") Boolean detailed)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.getProgress(helper, id, BoxingUtils.get(detailed), TaskForRepositoryRefresh.class);
        }
    }
}
