/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.hub.model.alert.AlertDefinition;
import com.optio3.cloud.hub.model.alert.AlertDefinitionFilterRequest;
import com.optio3.cloud.hub.model.common.LogLine;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionRecord;
import com.optio3.cloud.model.RawImport;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogEntryFilterRequest;
import com.optio3.cloud.persistence.LogRange;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.TimeUtils;
import io.swagger.annotations.Api;

@Api(tags = { "AlertDefinitions" }) // For Swagger
@Optio3RestEndpoint(name = "AlertDefinitions") // For Optio3 Shell
@Path("/v1/alert-definitions")
public class AlertDefinitions
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("filter")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<AlertDefinitionRecord> getFiltered(AlertDefinitionFilterRequest filters)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<AlertDefinitionRecord> helper = sessionHolder.createHelper(AlertDefinitionRecord.class);

            return AlertDefinitionRecord.filter(helper, filters);
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<AlertDefinition> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<AlertDefinitionRecord> helper = sessionHolder.createHelper(AlertDefinitionRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, AlertDefinitionRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AlertDefinition create(AlertDefinition model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<AlertDefinitionRecord> helper = sessionHolder.createHelper(AlertDefinitionRecord.class);

            AlertDefinitionRecord rec_alertDefinition = AlertDefinitionRecord.newInstance(model.purpose);

            // Always create inactive.
            model.active = false;

            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, model, rec_alertDefinition);

            helper.persist(rec_alertDefinition);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_alertDefinition);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public AlertDefinition get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, AlertDefinitionRecord.class, id);
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    AlertDefinition model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            AlertDefinitionRecord rec_alertDefinition = validation.sessionHolder.getEntity(AlertDefinitionRecord.class, id);

            if (validation.canProceed())
            {
                ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec_alertDefinition);
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
            RecordHelper<AlertDefinitionRecord> helper = validation.sessionHolder.createHelper(AlertDefinitionRecord.class);

            AlertDefinitionRecord rec_alertDefinition = helper.getOrNull(id);
            if (rec_alertDefinition != null)
            {
                if (validation.canProceed())
                {
                    rec_alertDefinition.remove(validation, helper);
                }
            }

            return validation.getResults();
        }
    }

    @POST
    @Path("parse-import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AlertDefinition parseImport(RawImport rawImport)
    {
        return rawImport.validate(AlertDefinition.class);
    }

    @POST
    @Path("item/{id}/log/filter")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LogRange> filterLog(@PathParam("id") String id,
                                    LogEntryFilterRequest filters) throws
                                                                   Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            AlertDefinitionRecord rec = sessionHolder.getEntity(AlertDefinitionRecord.class, id);

            try (var logHandler = AlertDefinitionRecord.allocateLogHandler(sessionHolder, rec))
            {
                return logHandler.filter(filters);
            }
        }
    }

    @GET
    @Path("item/{id}/log")
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
            AlertDefinitionRecord rec = sessionHolder.getEntity(AlertDefinitionRecord.class, id);

            List<LogLine> lines = Lists.newArrayList();

            try (var logHandler = AlertDefinitionRecord.allocateLogHandler(sessionHolder, rec))
            {
                logHandler.extract(fromOffset, toOffset, limit, (item, offset) ->
                {
                    LogLine newLine = new LogLine();
                    newLine.lineNumber = offset;
                    newLine.timestamp  = item.timestamp;
                    newLine.fd         = item.fd;
                    newLine.line       = item.line;
                    lines.add(newLine);
                });
            }

            return lines;
        }
    }

    @DELETE
    @Path("item/{id}/log")
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    @Produces(MediaType.APPLICATION_JSON)
    public int deleteLog(@PathParam("id") String id,
                         @QueryParam("olderThanXMinutes") Integer olderThanXMinutes) throws
                                                                                     Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<AlertDefinitionRecord> lock = sessionHolder.getEntityWithLock(AlertDefinitionRecord.class, id, 30, TimeUnit.SECONDS);

            int deleteCount;

            try (var logHandler = AlertDefinitionRecord.allocateLogHandler(lock))
            {
                ZonedDateTime olderThan = olderThanXMinutes != null ? TimeUtils.now()
                                                                               .minus(olderThanXMinutes, ChronoUnit.MINUTES) : null;

                deleteCount = logHandler.delete(olderThan);
            }

            sessionHolder.commit();

            return deleteCount;
        }
    }
}
