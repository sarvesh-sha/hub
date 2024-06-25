/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.util.List;

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
import com.optio3.cloud.builder.model.jobs.JobDefinitionStep;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "JobDefinitionSteps" }) // For Swagger
@Optio3RestEndpoint(name = "JobDefinitionSteps") // For Optio3 Shell
@Path("/v1/job-definition-steps")
public class JobDefinitionSteps
{
    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<JobDefinitionStep> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<JobDefinitionStepRecord> helper = sessionHolder.createHelper(JobDefinitionStepRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, JobDefinitionStepRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("create/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JobDefinitionStep create(@PathParam("id") String jobId,
                                    JobDefinitionStep model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<JobDefinitionStepRecord> helper = sessionHolder.createHelper(JobDefinitionStepRecord.class);

            JobDefinitionRecord     rec_job  = sessionHolder.getEntity(JobDefinitionRecord.class, jobId);
            JobDefinitionStepRecord rec_step = model.newRecord(rec_job);

            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, model, rec_step);

            helper.persist(rec_step);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_step);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public JobDefinitionStep get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, JobDefinitionStepRecord.class, id);
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    JobDefinitionStep model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            RecordHelper<JobDefinitionStepRecord> helper = validation.sessionHolder.createHelper(JobDefinitionStepRecord.class);
            JobDefinitionStepRecord               rec    = helper.get(id);

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
            RecordHelper<JobDefinitionStepRecord> helper = validation.sessionHolder.createHelper(JobDefinitionStepRecord.class);
            JobDefinitionStepRecord               rec    = helper.getOrNull(id);
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
}
