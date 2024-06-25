/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import static com.optio3.util.Exceptions.getAndUnwrapException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.persistence.PessimisticLockException;
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
import com.google.common.collect.Maps;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.builder.model.common.LogLine;
import com.optio3.cloud.builder.model.deployment.DeploymentTask;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.client.deployer.model.LogEntry;
import com.optio3.cloud.client.deployer.proxy.DeployerDockerApi;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogEntryFilterRequest;
import com.optio3.cloud.persistence.LogHandler;
import com.optio3.cloud.persistence.LogHolder;
import com.optio3.cloud.persistence.LogRange;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "DeploymentTasks" }) // For Swagger
@Optio3RestEndpoint(name = "DeploymentTasks") // For Optio3 Shell
@Path("/v1/deployment-tasks")
public class DeploymentTasks
{
    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @GET
    @Path("all/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<DeploymentTaskRecord> getAll(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostRecord rec = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            return TypedRecordIdentityList.toList(rec.getTasks());
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<DeploymentTask> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<DeploymentTaskRecord> helper = sessionHolder.createHelper(DeploymentTaskRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, QueryHelperWithCommonFields.getBatch(helper, ids));
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentTask get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, DeploymentTaskRecord.class, id);
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
            RecordHelper<DeploymentTaskRecord> helper = validation.sessionHolder.createHelper(DeploymentTaskRecord.class);

            DeploymentTaskRecord rec = helper.getOrNull(id);
            if (rec != null)
            {
                rec.remove(validation, helper);
            }

            return validation.getResults();
        }
    }

    //--//

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
            DeploymentTaskRecord rec = sessionHolder.getEntity(DeploymentTaskRecord.class, id);

            try (var logHandler = DeploymentTaskRecord.allocateLogHandler(sessionHolder, rec))
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
        Executors.getDefaultLongRunningThreadPool()
                 .queue(() -> fetchOutputInBackground(m_sessionProvider, id));

        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentTaskRecord rec = sessionHolder.getEntity(DeploymentTaskRecord.class, id);

            List<LogLine> lines = Lists.newArrayList();

            try (var logHandler = DeploymentTaskRecord.allocateLogHandler(sessionHolder, rec))
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

    @GET
    @Path("item/{id}/restart")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults restart(@PathParam("id") String id,
                                     @QueryParam("dryRun") Boolean dryRun) throws
                                                                           Exception
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            RecordHelper<DeploymentTaskRecord> helper = validation.sessionHolder.createHelper(DeploymentTaskRecord.class);
            DeploymentTaskRecord               rec    = helper.getOrNull(id);
            if (rec != null)
            {
                rec.scheduleRestart(validation);
            }

            return validation.getResults();
        }
    }

    @GET
    @Path("item/{id}/terminate")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults terminate(@PathParam("id") String id,
                                       @QueryParam("dryRun") Boolean dryRun) throws
                                                                             Exception
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            RecordHelper<DeploymentTaskRecord> helper = validation.sessionHolder.createHelper(DeploymentTaskRecord.class);
            DeploymentTaskRecord               rec    = helper.getOrNull(id);
            if (rec != null)
            {
                rec.scheduleTerminate(validation);
            }

            return validation.getResults();
        }
    }

    //--//

    private static final ConcurrentMap<String, Boolean> s_logGating = Maps.newConcurrentMap();

    private static void fetchOutputInBackground(SessionProvider sessionProvider,
                                                String id)
    {
        if (s_logGating.put(id, true) == Boolean.TRUE)
        {
            // Another thread is fetching the log, avoid parallel requests.
            return;
        }

        try
        {
            DeployerDockerApi proxy;
            String            dockerId;
            ZonedDateTime     lastOutput;

            try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
            {
                DeploymentTaskRecord rec_task = sessionHolder.getEntityOrNull(DeploymentTaskRecord.class, id);
                if (rec_task == null)
                {
                    return;
                }

                dockerId = rec_task.getDockerId();
                if (dockerId == null)
                {
                    // The container has not started or was terminated, nothing to do.
                    return;
                }

                try
                {
                    lastOutput = rec_task.getLastOutput();

                    proxy = getAndUnwrapException(rec_task.getDockerProxy(sessionProvider));
                    if (proxy == null)
                    {
                        return;
                    }
                }
                catch (Throwable t)
                {
                    LogHandler.LoggerInstance.error("Failed to fetch log from task '%s': %s", dockerId, t);
                    return;
                }
            }

            List<LogEntry> entries = Lists.newArrayList();

            try
            {
                getAndUnwrapException(proxy.fetchOutput(dockerId, 1000, 64 * 1024, false, lastOutput, (lst) ->
                {
                    entries.addAll(lst);

                    return AsyncRuntime.NullResult;
                }));
            }
            catch (Throwable t)
            {
                LogHandler.LoggerInstance.error("Failed to fetch log from task '%s': %s", dockerId, t);
                return;
            }

            if (!entries.isEmpty())
            {
                try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
                {
                    RecordLocked<DeploymentTaskRecord> lock_task = sessionHolder.getEntityWithLockOrNull(DeploymentTaskRecord.class, id, 10, TimeUnit.SECONDS);
                    if (lock_task == null)
                    {
                        return;
                    }

                    try (var logHandler = DeploymentTaskRecord.allocateLogHandler(lock_task))
                    {
                        try (LogHolder log = logHandler.newLogHolder())
                        {
                            for (LogEntry en : entries)
                            {
                                log.addLineSync(en.fd, en.timestamp, null, null, null, null, en.line);
                            }
                        }
                    }

                    sessionHolder.commit();
                }
            }
        }
        catch (PessimisticLockException e)
        {
            LogHandler.LoggerInstance.debug("Can't fetch log because Task record is locked by another thread");
        }
        finally
        {
            s_logGating.put(id, false);
        }
    }
}
