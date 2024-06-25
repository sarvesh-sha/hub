/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

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
import com.optio3.cloud.builder.model.common.LogLine;
import com.optio3.cloud.builder.model.worker.Host;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
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

@Api(tags = { "Hosts" }) // For Swagger
@Optio3RestEndpoint(name = "Hosts") // For Optio3 Shell
@Path("/v1/hosts")
@Optio3RequestLogLevel(Severity.Debug)
public class Hosts
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    public TypedRecordIdentityList<HostRecord> getAll()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<HostRecord> helper = sessionHolder.createHelper(HostRecord.class);

            return HostRecord.list(helper);
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<Host> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<HostRecord> helper = sessionHolder.createHelper(HostRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, HostRecord.getBatch(helper, ids));
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Host get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, HostRecord.class, id);
    }

    //--//

    @POST
    @Path("item/{hostId}/log/filter")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LogRange> filterLog(@PathParam("hostId") String hostId,
                                    LogEntryFilterRequest filters) throws
                                                                   Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            HostRecord rec = sessionHolder.getEntity(HostRecord.class, hostId);

            var logHandler = HostRecord.allocateLogHandler(sessionHolder, rec);
            {
                return logHandler.filter(filters);
            }
        }
    }

    @GET
    @Path("item/{hostId}/log")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LogLine> getLog(@PathParam("hostId") String hostId,
                                @QueryParam("fromOffset") Integer fromOffset,
                                @QueryParam("toOffset") Integer toOffset,
                                @QueryParam("limit") Integer limit) throws
                                                                    Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            HostRecord rec = sessionHolder.getEntity(HostRecord.class, hostId);

            List<LogLine> lines = Lists.newArrayList();

            var logHandler = HostRecord.allocateLogHandler(sessionHolder, rec);
            logHandler.extract(fromOffset, toOffset, limit, (item, offset) ->
            {
                LogLine newLine = new LogLine();
                newLine.lineNumber = offset;
                newLine.copyFrom(item);
                lines.add(newLine);
            });

            return lines;
        }
    }

    @DELETE
    @Path("item/{hostId}/log")
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    @Produces(MediaType.APPLICATION_JSON)
    public int deleteLog(@PathParam("hostId") String hostId,
                         @QueryParam("olderThanXMinutes") Integer olderThanXMinutes) throws
                                                                                     Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<HostRecord> lock = sessionHolder.getEntityWithLock(HostRecord.class, hostId, 30, TimeUnit.SECONDS);

            int deleteCount;

            try (var logHandler = HostRecord.allocateLogHandler(lock))
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
