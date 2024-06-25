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
import javax.ws.rs.PUT;
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
import com.optio3.cloud.hub.model.common.LogLine;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.persistence.LogEntryFilterRequest;
import com.optio3.cloud.persistence.LogRange;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.TimeUtils;
import io.swagger.annotations.Api;

@Api(tags = { "Networks" }) // For Swagger
@Optio3RestEndpoint(name = "Networks") // For Optio3 Shell
@Path("/v1/networks")
@Optio3RequestLogLevel(Severity.Debug)
public class Networks
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("item/{networkId}/log/filter")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LogRange> filterLog(@PathParam("networkId") String networkId,
                                    LogEntryFilterRequest filters) throws
                                                                   Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            NetworkAssetRecord rec = sessionHolder.getEntity(NetworkAssetRecord.class, networkId);

            try (var logHandler = NetworkAssetRecord.allocateLogHandler(sessionHolder, rec))
            {
                return logHandler.filter(filters);
            }
        }
    }

    @GET
    @Path("item/{networkId}/log")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LogLine> getLog(@PathParam("networkId") String networkId,
                                @QueryParam("fromOffset") Integer fromOffset,
                                @QueryParam("toOffset") Integer toOffset,
                                @QueryParam("limit") Integer limit) throws
                                                                    Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            NetworkAssetRecord rec = sessionHolder.getEntity(NetworkAssetRecord.class, networkId);

            List<LogLine> lines = Lists.newArrayList();

            try (var logHandler = NetworkAssetRecord.allocateLogHandler(sessionHolder, rec))
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

    @DELETE
    @Path("item/{networkId}/log")
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    @Produces(MediaType.APPLICATION_JSON)
    public int deleteLog(@PathParam("networkId") String networkId,
                         @QueryParam("olderThanXMinutes") Integer olderThanXMinutes) throws
                                                                                     Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<NetworkAssetRecord> lock = sessionHolder.getEntityWithLock(NetworkAssetRecord.class, networkId, 30, TimeUnit.SECONDS);

            int deleteCount;

            try (var logHandler = NetworkAssetRecord.allocateLogHandler(lock))
            {
                ZonedDateTime olderThan = olderThanXMinutes != null ? TimeUtils.now()
                                                                               .minus(olderThanXMinutes, ChronoUnit.MINUTES) : null;

                deleteCount = logHandler.delete(olderThan);
            }

            sessionHolder.commit();

            return deleteCount;
        }
    }

    @PUT
    @Path("item/{networkId}/reclassify")
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    @Produces(MediaType.APPLICATION_JSON)
    public boolean reclassify(@PathParam("networkId") String networkId) throws
                                                                        Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            NetworkAssetRecord rec = sessionHolder.getEntity(NetworkAssetRecord.class, networkId);

            InstanceConfiguration cfg = sessionHolder.getServiceNonNull(InstanceConfiguration.class);
            cfg.executeClassification(m_sessionProvider, sessionHolder.createLocator(rec));

            sessionHolder.commit();

            return true;
        }
    }
}
