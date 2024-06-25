/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.List;

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
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.model.alert.Alert;
import com.optio3.cloud.hub.model.alert.AlertEventLevel;
import com.optio3.cloud.hub.model.alert.AlertEventType;
import com.optio3.cloud.hub.model.alert.AlertFilterRequest;
import com.optio3.cloud.hub.model.audit.AuditFilterRequest;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.model.event.Event;
import com.optio3.cloud.hub.model.event.EventFilterRequest;
import com.optio3.cloud.hub.model.workflow.Workflow;
import com.optio3.cloud.hub.model.workflow.WorkflowFilterRequest;
import com.optio3.cloud.hub.persistence.alert.AlertHistoryRecord;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.alert.AuditRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.event.EventRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowHistoryRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowRecord;
import com.optio3.cloud.model.PaginatedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.serialization.Reflection;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;

@Api(tags = { "Events" }) // For Swagger
@Optio3RestEndpoint(name = "Events") // For Optio3 Shell
@Path("/v1/events")
public class Events
{
    @Inject
    private HubConfiguration m_cfg;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public PaginatedRecordIdentityList getFiltered(EventFilterRequest filters)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            AlertFilterRequest alertFilters = Reflection.as(filters, AlertFilterRequest.class);
            if (alertFilters != null)
            {
                return AlertRecord.filter(sessionHolder.createHelper(AlertRecord.class), alertFilters);
            }

            AuditFilterRequest auditFilters = Reflection.as(filters, AuditFilterRequest.class);
            if (auditFilters != null)
            {
                return AuditRecord.filter(sessionHolder.createHelper(AuditRecord.class), auditFilters);
            }

            WorkflowFilterRequest workflowFilters = Reflection.as(filters, WorkflowFilterRequest.class);
            if (workflowFilters != null)
            {
                return WorkflowRecord.filter(sessionHolder.createHelper(WorkflowRecord.class), workflowFilters);
            }

            return EventRecord.filter(sessionHolder.createHelper(EventRecord.class), filters);
        }
    }

    @POST
    @Path("count")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public long getFilteredCount(EventFilterRequest filters)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            AlertFilterRequest alterFilters = Reflection.as(filters, AlertFilterRequest.class);
            if (alterFilters != null)
            {
                return AlertRecord.count(sessionHolder.createHelper(AlertRecord.class), alterFilters);
            }

            AuditFilterRequest auditFilters = Reflection.as(filters, AuditFilterRequest.class);
            if (auditFilters != null)
            {
                return AuditRecord.count(sessionHolder.createHelper(AuditRecord.class), auditFilters);
            }

            WorkflowFilterRequest workflowFilters = Reflection.as(filters, WorkflowFilterRequest.class);
            if (workflowFilters != null)
            {
                return WorkflowRecord.count(sessionHolder.createHelper(WorkflowRecord.class), workflowFilters);
            }

            return EventRecord.count(sessionHolder.createHelper(EventRecord.class), filters);
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<Event> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<EventRecord> helper = sessionHolder.createHelper(EventRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, EventRecord.getBatch(helper, ids));
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public Event get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, EventRecord.class, id);
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    Event model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            EventRecord rec      = validation.sessionHolder.getEntity(EventRecord.class, id);
            UserRecord  rec_user = m_cfg.getUserFromAccessor(validation.sessionHolder, m_principalAccessor);

            if (validation.canProceed())
            {
                String oldExtendedDescription = rec.getExtendedDescription();

                ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec);

                AlertRecord rec_alert = Reflection.as(rec, AlertRecord.class);
                if (rec_alert != null)
                {
                    Alert model2 = (Alert) model;

                    RecordHelper<AlertHistoryRecord> helper = validation.sessionHolder.createHelper(AlertHistoryRecord.class);

                    rec_alert.updateStatus(helper, null, model2.status, null);

                    if (!StringUtils.equals(oldExtendedDescription, model.extendedDescription))
                    {
                        rec_alert.addHistoryEntry(helper, null, AlertEventLevel.info, AlertEventType.updatedWithNotes, "%s", model.extendedDescription);
                    }
                }

                WorkflowRecord rec_workflow = Reflection.as(rec, WorkflowRecord.class);
                if (rec_workflow != null)
                {
                    Workflow                            model2                = (Workflow) model;
                    RecordHelper<WorkflowHistoryRecord> workflowHistoryHelper = validation.sessionHolder.createHelper(WorkflowHistoryRecord.class);

                    rec_workflow.updateExtendedDescription(workflowHistoryHelper, rec_user, oldExtendedDescription, model2.extendedDescription);

                    if (model2.status != rec_workflow.getStatus())
                    {
                        rec_workflow.updateStatus(workflowHistoryHelper, rec_user, model2.status, null);
                        InstanceConfiguration cfg = validation.sessionHolder.getServiceNonNull(InstanceConfiguration.class);
                        cfg.handleWorkflowUpdated(validation.sessionHolder, rec_workflow, rec_user);
                    }
                }
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
            validation.checkAnyRoles(m_principalAccessor, WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator);

            RecordHelper<EventRecord> helper = validation.sessionHolder.createHelper(EventRecord.class);
            EventRecord               rec    = helper.getOrNull(id);
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
