/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.jobs.Job;
import com.optio3.cloud.builder.model.jobs.JobDefinition;
import com.optio3.cloud.builder.orchestration.tasks.build.BaseBuildTask;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionRecord;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "JobDefinitions" }) // For Swagger
@Optio3RestEndpoint(name = "JobDefinitions") // For Optio3 Shell
@Path("/v1/job-definitions")
public class JobDefinitions
{
    @Inject
    private BuilderApplication m_app;

    @Inject
    private BuilderConfiguration m_config;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<JobDefinitionRecord> getAll()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<JobDefinitionRecord> helper = sessionHolder.createHelper(JobDefinitionRecord.class);

            return JobDefinitionRecord.list(helper);
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<JobDefinition> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<JobDefinitionRecord> helper = sessionHolder.createHelper(JobDefinitionRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, JobDefinitionRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JobDefinition create(JobDefinition model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<JobDefinitionRecord> helper = sessionHolder.createHelper(JobDefinitionRecord.class);
            JobDefinitionRecord               rec    = new JobDefinitionRecord();

            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, model, rec);

            helper.persist(rec);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public JobDefinition get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, JobDefinitionRecord.class, id);
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    JobDefinition model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            RecordHelper<JobDefinitionRecord> helper = validation.sessionHolder.createHelper(JobDefinitionRecord.class);
            JobDefinitionRecord               rec    = helper.get(id);

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
            RecordHelper<JobDefinitionRecord> helper = validation.sessionHolder.createHelper(JobDefinitionRecord.class);

            JobDefinitionRecord rec = helper.getOrNull(id);
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

    //--//

    @GET
    @Path("trigger/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Job trigger(@PathParam("id") String id,
                       @QueryParam("branch") String branch,
                       @QueryParam("commit") String commit) throws
                                                            Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            CookiePrincipal principal = CookiePrincipalAccessor.get(m_principalAccessor);

            UserRecord user = m_config.userLogic.findUser(sessionHolder, principal, true);

            //
            // Lock definition to avoid races in assigning a unique ID to the Job.
            //
            RecordLocked<JobDefinitionRecord> lock_jobDef = sessionHolder.getEntityWithLock(JobDefinitionRecord.class, id, 2, TimeUnit.MINUTES);
            JobDefinitionRecord               rec_jobDef  = lock_jobDef.get();

            if (branch == null)
            {
                branch = "master";
            }

            HostRecord rec_host = m_app.getCurrentHost(sessionHolder);

            JobRecord rec_job = BaseBuildTask.schedule(lock_jobDef, rec_jobDef.getName(), rec_host, branch, commit, user);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_job);
        }
    }
}
