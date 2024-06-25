/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.builder.model.common.LogLine;
import com.optio3.cloud.builder.model.jobs.JobStep;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogEntryFilterRequest;
import com.optio3.cloud.persistence.LogRange;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "JobSteps" }) // For Swagger
@Optio3RestEndpoint(name = "JobSteps") // For Optio3 Shell
@Path("/v1/job-steps")
public class JobSteps
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<JobStep> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<JobStepRecord> helper = sessionHolder.createHelper(JobStepRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, JobStepRecord.getBatch(helper, ids));
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public JobStep get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, JobStepRecord.class, id);
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    JobStep model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            RecordHelper<JobStepRecord> helper = validation.sessionHolder.createHelper(JobStepRecord.class);

            JobStepRecord rec = helper.get(id);

            if (validation.canProceed())
            {
                ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec);
            }

            return validation.getResults();
        }
    }

    //--//

    @POST
    @Path("log/{id}/filter")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LogRange> filterLog(@PathParam("id") String id,
                                    LogEntryFilterRequest filters) throws
                                                                   Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            JobStepRecord rec = sessionHolder.getEntity(JobStepRecord.class, id);

            try (var logHandler = JobStepRecord.allocateLogHandler(sessionHolder, rec))
            {
                return logHandler.filter(filters);
            }
        }
    }

    @GET
    @Path("log/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LogLine> getLog(@PathParam("id") String id,
                                @QueryParam("fromOffset") Integer fromOffset,
                                @QueryParam("toOffset") Integer toOffset,
                                @QueryParam("limit") Integer limit) throws
                                                                    Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            JobStepRecord rec = sessionHolder.getEntity(JobStepRecord.class, id);

            if (limit == null)
            {
                limit = 500;
            }

            List<LogLine> lines = Lists.newArrayList();

            try (var logHandler = JobStepRecord.allocateLogHandler(sessionHolder, rec))
            {
                logHandler.extract(fromOffset, toOffset, limit, (item, offset) ->
                {
                    LogLine newLine = new LogLine();
                    newLine.lineNumber = offset;
                    newLine.copyFrom(item);
                    lines.add(newLine);
                });
            }

            return lines;
        }
    }
}
