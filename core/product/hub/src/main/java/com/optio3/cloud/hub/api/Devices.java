/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.DeviceHealthAggregate;
import com.optio3.cloud.hub.model.DeviceHealthSummary;
import com.optio3.cloud.hub.model.alert.AlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertType;
import com.optio3.cloud.hub.model.asset.DevicesReportProgress;
import com.optio3.cloud.hub.orchestration.tasks.TaskForDevicesReport;
import com.optio3.cloud.hub.orchestration.tasks.TaskForNetworkRefresh;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.BoxingUtils;
import io.swagger.annotations.Api;

@Api(tags = { "Devices" }) // For Swagger
@Optio3RestEndpoint(name = "Devices") // For Optio3 Shell
@Path("/v1/devices")
public class Devices
{
    @Inject
    private HubApplication m_app;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("health/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeviceHealthSummary getDeviceHealth(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeviceRecord        rec = sessionHolder.getEntity(DeviceRecord.class, id);
            DeviceHealthSummary res = new DeviceHealthSummary();

            EnumMap<AlertType, DeviceHealthAggregate> countsByType = Maps.newEnumMap(AlertType.class);

            for (AlertRecord rec_alert : rec.getAlerts())
            {
                AlertType     type     = rec_alert.getType();
                AlertSeverity severity = rec_alert.getSeverity();

                DeviceHealthAggregate agg = countsByType.get(type);
                if (agg == null)
                {
                    agg      = new DeviceHealthAggregate();
                    agg.type = type;
                    countsByType.put(type, agg);
                }

                agg.count++;

                if (severity.isMoreSevere(agg.maxSeverity))
                {
                    agg.maxSeverity = severity;
                }

                if (severity.isMoreSevere(res.overallStatus))
                {
                    res.overallStatus = severity;
                }
            }

            res.countsByType.addAll(countsByType.values());

            res.countsByType.sort((a, b) ->
                                  {
                                      // Sort by count, decreasing.
                                      return b.count - a.count;
                                  });

            return res;
        }
    }

    @POST
    @Path("rediscovery/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public TypedRecordIdentity<BackgroundActivityRecord> runRediscovery(@PathParam("id") String deviceId,
                                                                        @QueryParam("forceListObjects") Optional<Boolean> forceListObjects,
                                                                        @QueryParam("forceReadObjects") Optional<Boolean> forceReadObjects) throws
                                                                                                                                            Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, (sessionHolder) ->
        {
            DeviceRecord rec_device = sessionHolder.getEntity(DeviceRecord.class, deviceId);

            NetworkAssetRecord rec_network = rec_device.getParentAsset(NetworkAssetRecord.class);
            if (rec_network == null)
            {
                return null;
            }

            GatewayAssetRecord rec_gateway = rec_network.getBoundGateway();
            if (rec_gateway == null)
            {
                return null;
            }

            TaskForNetworkRefresh.Settings settings = new TaskForNetworkRefresh.Settings();
            settings.forceListObjects = forceListObjects.orElse(false);
            settings.forceReadObjects = forceReadObjects.orElse(false);

            return TaskForNetworkRefresh.scheduleTask(sessionHolder, settings, rec_gateway, (t) ->
            {
                t.targetNetworks = Lists.newArrayList(sessionHolder.createLocator(rec_network));
                t.targetDevices  = Lists.newArrayList(sessionHolder.createLocator(rec_device));
            });
        });

        return RecordIdentity.newTypedInstance(loc_task);
    }

    @POST
    @Path("summary/report")
    @Produces(MediaType.TEXT_PLAIN)
    public String startSummaryReport() throws
                                       Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, TaskForDevicesReport::scheduleTask);

        return loc_task.getIdRaw();
    }

    @GET
    @Path("summary/report/check/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DevicesReportProgress checkSummaryReport(@PathParam("id") String id,
                                                    @QueryParam("detailed") Boolean detailed)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.getProgress(helper, id, BoxingUtils.get(detailed), TaskForDevicesReport.class);
        }
    }

    @GET
    @Path("summary/report/excel/{id}/{fileName}")
    @Produces("application/octet-stream")
    @Optio3RequestLogLevel(Severity.Debug)
    public InputStream streamSummaryReport(@PathParam("id") String id,
                                           @PathParam("fileName") String fileName) throws
                                                                                   Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.streamContents(helper, id, TaskForDevicesReport.class);
        }
    }
}
