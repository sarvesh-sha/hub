/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
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
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.model.SummaryFlavor;
import com.optio3.cloud.hub.model.SummaryResult;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.model.workflow.Workflow;
import com.optio3.cloud.hub.model.workflow.WorkflowEventType;
import com.optio3.cloud.hub.model.workflow.WorkflowFilterRequest;
import com.optio3.cloud.hub.model.workflow.WorkflowHistory;
import com.optio3.cloud.hub.model.workflow.WorkflowPriority;
import com.optio3.cloud.hub.model.workflow.WorkflowType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowHistoryRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.BoxingUtils;
import io.dropwizard.jersey.jsr310.ZonedDateTimeParam;
import io.swagger.annotations.Api;

@Api(tags = { "Workflows" }) // For Swagger
@Optio3RestEndpoint(name = "Workflows") // For Optio3 Shell
@Path("/v1/workflows")
public class Workflows
{
    @Inject
    private HubConfiguration m_cfg;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Workflow create(Workflow model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<WorkflowRecord> helper             = sessionHolder.createHelper(WorkflowRecord.class);
            UserRecord                   rec_user           = m_cfg.getUserFromAccessor(sessionHolder, m_principalAccessor);
            UserRecord                   rec_userAssignedTo = sessionHolder.fromIdentityOrNull(model.assignedTo);
            AssetRecord                  rec_asset          = sessionHolder.fromIdentityOrNull(model.asset);

            model.details.onCreate(sessionHolder);

            WorkflowRecord rec = WorkflowRecord.newInstance(helper, rec_user, rec_asset, model.details);

            ModelMapperPolicy policy = m_cfg.getPolicyWithOverrideForMaintenanceUser(sessionHolder, m_principalAccessor);

            ModelMapper.fromModel(sessionHolder, policy, model, rec);

            helper.persist(rec);

            rec.addHistoryEntry(sessionHolder, rec_user, WorkflowEventType.created, "Workflow created");

            if (rec_userAssignedTo != null)
            {
                rec.assignToUser(sessionHolder, rec_userAssignedTo);
            }

            sessionHolder.commitAndBeginNewTransaction();

            InstanceConfiguration cfg = sessionHolder.getServiceNonNull(InstanceConfiguration.class);
            cfg.handleWorkflowCreated(sessionHolder, rec, rec_user);

            sessionHolder.commit();

            return (Workflow) ModelMapper.toModel(sessionHolder, policy, rec);
        }
    }

    @GET
    @Path("assign/{id}/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Workflow assign(@PathParam("id") String id,
                           @PathParam("userId") String userId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            WorkflowRecord rec_workflow = sessionHolder.getEntity(WorkflowRecord.class, id);
            UserRecord     rec_user     = sessionHolder.getEntity(UserRecord.class, userId);

            rec_workflow.assignToUser(sessionHolder, rec_user);

            sessionHolder.commit();

            return (Workflow) ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_workflow);
        }
    }

    @POST
    @Path("summary")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<SummaryResult> getSummary(@QueryParam("groupBy") SummaryFlavor groupBy,
                                          WorkflowFilterRequest filters)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<WorkflowRecord> helper = sessionHolder.createHelper(WorkflowRecord.class);
            List<SummaryResult>          res    = Lists.newArrayList();

            switch (BoxingUtils.get(groupBy, SummaryFlavor.location))
            {
                case location:
                {
                    Map<String, Number> counts = WorkflowRecord.countWorkflowsByLocation(helper, filters);

                    LocationsEngine          locationsEngine   = sessionHolder.getServiceNonNull(LocationsEngine.class);
                    LocationsEngine.Snapshot locationsSnapshot = locationsEngine.acquireSnapshot(false);
                    locationsSnapshot.accumulateByTopLevelLocations(res, counts);
                    break;
                }

                case type:
                {
                    Map<WorkflowType, Number> counts = WorkflowRecord.countWorkflowsByType(helper, filters);

                    for (WorkflowType type : counts.keySet())
                    {
                        SummaryResult obj = new SummaryResult();
                        obj.id    = type.name();
                        obj.type  = SummaryFlavor.type;
                        obj.label = type.name();
                        obj.count = counts.get(type)
                                          .intValue();
                        res.add(obj);
                    }
                    break;
                }

                case priority:
                {
                    Map<WorkflowPriority, Number> counts = WorkflowRecord.countWorkflowsByPriority(helper, filters);

                    for (WorkflowPriority priority : counts.keySet())
                    {
                        SummaryResult obj = new SummaryResult();
                        obj.id    = priority.name();
                        obj.type  = SummaryFlavor.priority;
                        obj.label = priority.name();
                        obj.count = counts.get(priority)
                                          .intValue();
                        res.add(obj);
                    }
                    break;
                }
            }

            return res;
        }
    }

    @GET
    @Path("history/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<WorkflowHistoryRecord> listWorkflowHistoryByID(@PathParam("id") String id,
                                                                                  @QueryParam("rangeStart") ZonedDateTimeParam rangeStartParam,
                                                                                  @QueryParam("rangeEnd") ZonedDateTimeParam rangeEndParam)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            WorkflowRecord rec        = sessionHolder.getEntity(WorkflowRecord.class, id);
            ZonedDateTime  rangeStart = rangeStartParam != null ? rangeStartParam.get() : null;
            ZonedDateTime  rangeEnd   = rangeEndParam != null ? rangeEndParam.get() : null;

            return WorkflowHistoryRecord.listSorted(sessionHolder.createHelper(WorkflowHistoryRecord.class), rec, rangeStart, rangeEnd);
        }
    }

    //--//

    @GET
    @Path("feed")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<WorkflowHistoryRecord> getWorkflowFeed(@QueryParam("rangeStart") ZonedDateTimeParam rangeStartParam,
                                                                          @QueryParam("rangeEnd") ZonedDateTimeParam rangeEndParam)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            ZonedDateTime rangeStart = rangeStartParam != null ? rangeStartParam.get() : null;
            ZonedDateTime rangeEnd   = rangeEndParam != null ? rangeEndParam.get() : null;

            return WorkflowHistoryRecord.listSorted(sessionHolder.createHelper(WorkflowHistoryRecord.class), (WorkflowRecord) null, rangeStart, rangeEnd);
        }
    }

    @GET
    @Path("fetch-history/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public WorkflowHistory getWorkflowHistoryByID(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, WorkflowHistoryRecord.class, id);
    }

    @POST
    @Path("batch-history")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<WorkflowHistory> getWorkflowHistoryBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<WorkflowHistoryRecord> helper = sessionHolder.createHelper(WorkflowHistoryRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, WorkflowHistoryRecord.getBatch(helper, ids));
        }
    }
}