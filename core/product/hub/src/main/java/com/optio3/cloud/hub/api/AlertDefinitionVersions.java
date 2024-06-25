/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
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
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.engine.alerts.AlertDefinitionDetails;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionSteps;
import com.optio3.cloud.hub.model.alert.AlertDefinitionVersion;
import com.optio3.cloud.hub.model.alert.AlertTestProgress;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphResponse;
import com.optio3.cloud.hub.model.common.LogLine;
import com.optio3.cloud.hub.orchestration.tasks.TaskForAlertTest;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionRecord;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionVersionRecord;
import com.optio3.cloud.model.RawImport;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.ModelSanitizerContext;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;
import io.dropwizard.jersey.jsr310.ZonedDateTimeParam;
import io.swagger.annotations.Api;

@Api(tags = { "AlertDefinitionVersions" }) // For Swagger
@Optio3RestEndpoint(name = "AlertDefinitionVersions") // For Optio3 Shell
@Path("/v1/alert-definition-versions")
public class AlertDefinitionVersions
{
    @Inject
    private HubConfiguration m_cfg;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<AlertDefinitionVersion> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<AlertDefinitionVersionRecord> helper = sessionHolder.createHelper(AlertDefinitionVersionRecord.class);

            return CollectionUtils.transformToList(AlertDefinitionVersionRecord.getBatch(helper, ids), (rec) ->
            {
                AlertEngineExecutionContext.validate(sessionHolder, rec);

                return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
            });
        }
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AlertDefinitionVersion create(AlertDefinitionVersion model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<AlertDefinitionRecord>        helper_def = sessionHolder.createHelper(AlertDefinitionRecord.class);
            RecordHelper<AlertDefinitionVersionRecord> helper_ver = sessionHolder.createHelper(AlertDefinitionVersionRecord.class);

            RecordLocked<AlertDefinitionRecord> lock_alertDefinition = TypedRecordIdentity.getWithLockOrNull(helper_def, model.definition, 5, TimeUnit.SECONDS);
            AlertDefinitionRecord               rec_alertDefinition  = lock_alertDefinition.get();

            AlertDefinitionVersionRecord rec_predecessor = TypedRecordIdentity.getOrNull(helper_ver, model.predecessor);
            if (rec_predecessor == null)
            {
                rec_predecessor = rec_alertDefinition.getHeadVersion();
            }

            AlertDefinitionVersionRecord rec_alertDefinitionVersion = AlertDefinitionVersionRecord.newInstance(helper_ver, model, rec_alertDefinition, rec_predecessor, null);

            AlertEngineExecutionContext.validate(sessionHolder, rec_alertDefinitionVersion);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_alertDefinitionVersion);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public AlertDefinitionVersion get(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            AlertDefinitionVersionRecord rec_reportDefinition = sessionHolder.getEntity(AlertDefinitionVersionRecord.class, id);

            AlertEngineExecutionContext.validate(sessionHolder, rec_reportDefinition);

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_reportDefinition);
        }
    }

    @GET
    @Path("item/{id}/make-head")
    @Produces(MediaType.APPLICATION_JSON)
    public AlertDefinitionVersion makeHead(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            AlertDefinitionVersionRecord rec = sessionHolder.getEntity(AlertDefinitionVersionRecord.class, id);

            rec.makeHead();

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{id}/make-release")
    @Produces(MediaType.APPLICATION_JSON)
    public AlertDefinitionVersion makeRelease(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            AlertDefinitionVersionRecord rec = sessionHolder.getEntity(AlertDefinitionVersionRecord.class, id);

            rec.makeRelease();

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{predId}/link/{succId}")
    @Produces(MediaType.APPLICATION_JSON)
    public AlertDefinitionVersion link(@PathParam("predId") String predId,
                                       @PathParam("succId") String succId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            AlertDefinitionVersionRecord rec_pred = sessionHolder.getEntity(AlertDefinitionVersionRecord.class, predId);
            AlertDefinitionVersionRecord rec_succ = sessionHolder.getEntity(AlertDefinitionVersionRecord.class, succId);

            rec_succ.setPredecessor(rec_pred);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_pred);
        }
    }

    @GET
    @Path("item/{baseId}/squash/{finalId}")
    @Produces(MediaType.APPLICATION_JSON)
    public AlertDefinitionVersion squash(@PathParam("baseId") String baseId,
                                         @PathParam("finalId") String finalId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<AlertDefinitionVersionRecord> helper = sessionHolder.createHelper(AlertDefinitionVersionRecord.class);

            AlertDefinitionVersionRecord rec = AlertDefinitionVersionRecord.squash(helper, baseId, finalId);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @POST
    @Path("parse-import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AlertDefinitionDetails parseImport(RawImport rawImport)
    {
        AlertDefinitionDetails details = rawImport.validate(AlertDefinitionDetails.class);
        if (details != null)
        {
            try (var ctx = new ModelSanitizerContext.SimpleLazy(m_sessionProvider::newReadOnlySession))
            {
                details = ctx.processTyped(details);
            }
        }

        return details;
    }

    @GET
    @Path("eval/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public AlertEngineExecutionSteps evaluate(@PathParam("id") String id,
                                              @QueryParam("maxSteps") Integer maxSteps,
                                              @QueryParam("when") ZonedDateTimeParam when,
                                              @QueryParam("trace") Boolean trace)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            AlertDefinitionVersionRecord rec = sessionHolder.getEntityOrNull(AlertDefinitionVersionRecord.class, id);
            if (rec == null)
            {
                return null;
            }

            AlertEngineExecutionSteps snapshot = new AlertEngineExecutionSteps();

            try (AlertEngineExecutionContext ctx = new AlertEngineExecutionContext(null, sessionHolder, rec, null))
            {
                ZonedDateTime threshold = when != null ? when.get() : null;

                for (AssetGraphResponse.Resolved graphTuple : ctx.forEachGraphTuple())
                {
                    ctx.alertHolder.ignoreRealAlerts();
                    ctx.traceExecution = BoxingUtils.get(trace);
                    ctx.reset(threshold);
                    ctx.setGraphTuple(graphTuple);
                    ctx.evaluate(BoxingUtils.get(maxSteps, 1000), (stack, line) ->
                    {
                        LogLine ll = new LogLine();
                        ll.timestamp = threshold != null ? threshold : TimeUtils.now();
                        ll.line      = line;
                        snapshot.logEntries.add(ll);
                    });
                }

                snapshot.steps.addAll(ctx.steps);
            }

            return snapshot;
        }
    }

    @GET
    @Path("eval-range/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String evaluateOverRange(@PathParam("id") String id,
                                    @QueryParam("maxSteps") Integer maxSteps,
                                    @QueryParam("interval") Integer interval,
                                    @QueryParam("rangeStart") ZonedDateTimeParam rangeStart,
                                    @QueryParam("rangeEnd") ZonedDateTimeParam rangeEnd) throws
                                                                                         Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, (sessionHolder) ->
        {
            AlertDefinitionVersionRecord rec = sessionHolder.getEntityOrNull(AlertDefinitionVersionRecord.class, id);
            if (rec == null || rangeStart == null || rangeEnd == null)
            {
                return null;
            }

            ZonedDateTime start        = rangeStart.get();
            ZonedDateTime end          = rangeEnd.get();
            int           stepInterval = Math.max(10, BoxingUtils.get(interval, 900));
            int           limitSteps   = BoxingUtils.get(maxSteps, 1000);

            return TaskForAlertTest.scheduleTask(sessionHolder, rec, limitSteps, stepInterval, start, end);
        });

        return loc_task.getIdRaw();
    }

    @GET
    @Path("eval-range/{id}/check")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public AlertTestProgress checkEvaluation(@PathParam("id") String id,
                                             @QueryParam("detailed") Boolean detailed)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.getProgress(helper, id, BoxingUtils.get(detailed), TaskForAlertTest.class);
        }
    }
}
