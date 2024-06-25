/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.model.SummaryFlavor;
import com.optio3.cloud.hub.model.SummaryResult;
import com.optio3.cloud.hub.model.alert.AlertFilterRequest;
import com.optio3.cloud.hub.model.alert.AlertFilterResponse;
import com.optio3.cloud.hub.model.alert.AlertHistory;
import com.optio3.cloud.hub.model.alert.AlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertStatus;
import com.optio3.cloud.hub.model.alert.AlertType;
import com.optio3.cloud.hub.model.alert.AlertsReportProgress;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.orchestration.tasks.TaskForAlertsReport;
import com.optio3.cloud.hub.orchestration.tasks.TaskForEquipmentReport;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionRecord;
import com.optio3.cloud.hub.persistence.alert.AlertHistoryRecord;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.BoxingUtils;
import io.dropwizard.jersey.jsr310.ZonedDateTimeParam;
import io.swagger.annotations.Api;

@Api(tags = { "Alerts" }) // For Swagger
@Optio3RestEndpoint(name = "Alerts") // For Optio3 Shell
@Path("/v1/alerts")
public class Alerts
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("summary")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public AlertFilterResponse getSummary(@QueryParam("groupBy") SummaryFlavor groupBy,
                                          @QueryParam("rollupType") LocationType rollupType,
                                          AlertFilterRequest filters)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<AlertRecord> helper = sessionHolder.createHelper(AlertRecord.class);
            AlertFilterResponse       res    = new AlertFilterResponse();

            switch (BoxingUtils.get(groupBy, SummaryFlavor.location))
            {
                case location:
                {
                    Map<String, Number> counts = AlertRecord.countAlertsByLocation(helper, filters);

                    LocationsEngine          locationsEngine   = sessionHolder.getServiceNonNull(LocationsEngine.class);
                    LocationsEngine.Snapshot locationsSnapshot = locationsEngine.acquireSnapshot(false);
                    locationsSnapshot.accumulateByRollupType(res.results, counts, rollupType);
                    break;
                }

                case type:
                {
                    Map<AlertType, Number> counts = AlertRecord.countAlertsByType(helper, filters);

                    for (AlertType type : counts.keySet())
                    {
                        SummaryResult obj = new SummaryResult();
                        obj.id    = type.name();
                        obj.type  = SummaryFlavor.type;
                        obj.label = type.getDisplayName();
                        obj.count = counts.get(type)
                                          .intValue();
                        res.results.add(obj);
                    }
                    break;
                }

                case severity:
                {
                    Map<AlertSeverity, Number> counts = AlertRecord.countAlertsBySeverity(helper, filters);

                    for (AlertSeverity severity : counts.keySet())
                    {
                        SummaryResult obj = new SummaryResult();
                        obj.id    = severity.name();
                        obj.type  = SummaryFlavor.severity;
                        obj.label = severity.getDisplayName();
                        obj.count = counts.get(severity)
                                          .intValue();
                        res.results.add(obj);
                    }
                    break;
                }

                case status:
                {
                    Map<AlertStatus, Number> counts = AlertRecord.countAlertsByStatus(helper, filters);

                    for (AlertStatus status : counts.keySet())
                    {
                        SummaryResult obj = new SummaryResult();
                        obj.id    = status.name();
                        obj.type  = SummaryFlavor.status;
                        obj.label = status.getDisplayName();
                        obj.count = counts.get(status)
                                          .intValue();
                        res.results.add(obj);
                    }
                    break;
                }

                case rule:
                {
                    Map<AlertDefinitionRecord, Number> counts = AlertRecord.countAlertsByRule(helper, filters);

                    for (AlertDefinitionRecord rule : counts.keySet())
                    {
                        SummaryResult obj = new SummaryResult();
                        obj.id    = rule.getSysId();
                        obj.type  = SummaryFlavor.rule;
                        obj.label = rule.getTitle();
                        obj.count = counts.get(rule)
                                          .intValue();
                        res.results.add(obj);
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
    public TypedRecordIdentityList<AlertHistoryRecord> listAlertHistoryByID(@PathParam("id") String id,
                                                                            @QueryParam("rangeStart") ZonedDateTimeParam rangeStartParam,
                                                                            @QueryParam("rangeEnd") ZonedDateTimeParam rangeEndParam)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            AlertRecord   rec        = sessionHolder.getEntity(AlertRecord.class, id);
            ZonedDateTime rangeStart = rangeStartParam != null ? rangeStartParam.get() : null;
            ZonedDateTime rangeEnd   = rangeEndParam != null ? rangeEndParam.get() : null;

            return AlertHistoryRecord.listSorted(sessionHolder.createHelper(AlertHistoryRecord.class), rec, rangeStart, rangeEnd, 0);
        }
    }

    //--//

    @GET
    @Path("feed")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<AlertHistoryRecord> getAlertFeed(@QueryParam("rangeStart") ZonedDateTimeParam rangeStartParam,
                                                                    @QueryParam("rangeEnd") ZonedDateTimeParam rangeEndParam)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            ZonedDateTime rangeStart = rangeStartParam != null ? rangeStartParam.get() : null;
            ZonedDateTime rangeEnd   = rangeEndParam != null ? rangeEndParam.get() : null;

            return AlertHistoryRecord.listSorted(sessionHolder.createHelper(AlertHistoryRecord.class), (AlertRecord) null, rangeStart, rangeEnd, 0);
        }
    }

    @GET
    @Path("fetch-history/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public AlertHistory getAlertHistoryByID(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, AlertHistoryRecord.class, id);
    }

    @POST
    @Path("batch-history")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<AlertHistory> getAlertHistoryBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<AlertHistoryRecord> helper = sessionHolder.createHelper(AlertHistoryRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, AlertHistoryRecord.getBatch(helper, ids));
        }
    }

    //--//

    @POST
    @Path("report")
    @Produces(MediaType.TEXT_PLAIN)
    public String startAlertsReport() throws
                                      Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, TaskForAlertsReport::scheduleTask);

        return loc_task.getIdRaw();
    }

    @GET
    @Path("report/check/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public AlertsReportProgress checkAlertsReport(@PathParam("id") String id,
                                                  @QueryParam("detailed") Boolean detailed)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.getProgress(helper, id, BoxingUtils.get(detailed), TaskForAlertsReport.class);
        }
    }

    @GET
    @Path("report/excel/{id}/{fileName}")
    @Produces("application/octet-stream")
    @Optio3RequestLogLevel(Severity.Debug)
    public InputStream streamAlertsReport(@PathParam("id") String id,
                                          @PathParam("fileName") String fileName) throws
                                                                                  Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.streamContents(helper, id, TaskForAlertsReport.class);
        }
    }
}
