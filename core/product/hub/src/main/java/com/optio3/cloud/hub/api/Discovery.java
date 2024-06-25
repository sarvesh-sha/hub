/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
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
import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.model.asset.AzureDigitalTwinSyncProgress;
import com.optio3.cloud.hub.model.asset.DevicesSamplingTemplate;
import com.optio3.cloud.hub.model.asset.DevicesTemplate;
import com.optio3.cloud.hub.model.asset.DiscoveryReportProgress;
import com.optio3.cloud.hub.model.asset.DiscoveryReportRun;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.model.customization.InstanceConfigurationForCRE;
import com.optio3.cloud.hub.orchestration.tasks.TaskForDiscoveryReport;
import com.optio3.cloud.hub.orchestration.tasks.TaskForGatewayFlush;
import com.optio3.cloud.hub.orchestration.tasks.TaskForNetworkRefresh;
import com.optio3.cloud.hub.orchestration.tasks.TaskForPublishingToAzureDigitalTwin;
import com.optio3.cloud.hub.orchestration.tasks.TaskForSamplingSettings;
import com.optio3.cloud.hub.orchestration.tasks.TaskForTransportationAutoConfiguration;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceTypedValue;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.infra.integrations.azuredigitaltwins.AzureDigitalTwinsHelper;
import com.optio3.infra.integrations.azureiothub.AzureIotHubHelper;
import com.optio3.logging.Severity;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;

@Api(tags = { "Discovery" }) // For Swagger
@Optio3RestEndpoint(name = "Discovery") // For Optio3 Shell
@Path("/v1/discovery")
public class Discovery
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    //--//

    @GET
    @Path("bindings/{gatewayId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<RecordIdentity> getBindings(@PathParam("gatewayId") String gatewayId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            GatewayAssetRecord   recGateway = sessionHolder.getEntity(GatewayAssetRecord.class, gatewayId);
            List<RecordIdentity> res        = Lists.newArrayList();

            for (NetworkAssetRecord recNetwork : recGateway.getBoundNetworks())
            {
                res.add(RecordIdentity.newTypedInstance(sessionHolder.createHelper(NetworkAssetRecord.class), recNetwork));
            }

            return res;
        }
    }

    @GET
    @Path("reverse-bindings/{networkId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public RecordIdentity getReverseBindings(@PathParam("networkId") String networkId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            NetworkAssetRecord rec_network = sessionHolder.getEntity(NetworkAssetRecord.class, networkId);

            GatewayAssetRecord rec_gateway = rec_network.getBoundGateway();

            return RecordIdentity.newTypedInstance(sessionHolder.createHelper(GatewayAssetRecord.class), rec_gateway);
        }
    }

    @POST
    @Path("bindings/{gatewayId}/add/{networkId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public boolean bind(@PathParam("gatewayId") String gatewayId,
                        @PathParam("networkId") String networkId,
                        @FormParam("forceDiscovery") Optional<Boolean> forceDiscovery,
                        @FormParam("forceListObjects") Optional<Boolean> forceListObjects,
                        @FormParam("forceReadObjects") Optional<Boolean> forceReadObjects) throws
                                                                                           Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<GatewayAssetRecord> helper_gateway = sessionHolder.createHelper(GatewayAssetRecord.class);
            RecordHelper<NetworkAssetRecord> helper_network = sessionHolder.createHelper(NetworkAssetRecord.class);
            GatewayAssetRecord               rec_gateway    = helper_gateway.get(gatewayId);
            NetworkAssetRecord               rec_network    = helper_network.get(networkId);
            boolean                          added          = false;

            GatewayAssetRecord rec_gatewayOld = rec_network.getBoundGateway();
            if (rec_gatewayOld != null && rec_gatewayOld != rec_gateway)
            {
                boolean removed = rec_gatewayOld.getBoundNetworks()
                                                .remove(rec_network);

                if (removed)
                {
                    TaskForNetworkRefresh.Settings settings = new TaskForNetworkRefresh.Settings();
                    settings.forceSamplingConfiguration = true;
                    settings.dontQueueIfAlreadyActive   = true;
                    settings.sleepOnStart               = 4; // Delay a bit, we might have a batch of changes.

                    TaskForNetworkRefresh.scheduleTask(sessionHolder, settings, rec_gatewayOld, null);
                }
            }

            List<NetworkAssetRecord> boundNetworks = rec_gateway.getBoundNetworks();
            added |= SessionHolder.addIfMissingAndNotNull(boundNetworks, rec_network);

            TaskForNetworkRefresh.Settings settings = new TaskForNetworkRefresh.Settings();
            settings.forceDiscovery             = forceDiscovery.orElse(false);
            settings.forceListObjects           = forceListObjects.orElse(false);
            settings.forceReadObjects           = forceReadObjects.orElse(false);
            settings.forceSamplingConfiguration = true;

            TaskForNetworkRefresh.scheduleTask(sessionHolder, settings, rec_gateway, (t) ->
            {
                t.targetNetworks = Lists.newArrayList(helper_network.asLocator(rec_network));
            });

            sessionHolder.commit();

            return added;
        }
    }

    @POST
    @Path("bindings/{gatewayId}/remove/{networkId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public boolean unbind(@PathParam("gatewayId") String gatewayId,
                          @PathParam("networkId") String networkId) throws
                                                                    Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<GatewayAssetRecord> helper_gateway = sessionHolder.createHelper(GatewayAssetRecord.class);
            RecordHelper<NetworkAssetRecord> helper_network = sessionHolder.createHelper(NetworkAssetRecord.class);
            GatewayAssetRecord               rec_gateway    = helper_gateway.get(gatewayId);
            NetworkAssetRecord               rec_network    = helper_network.get(networkId);

            boolean removed = rec_gateway.getBoundNetworks()
                                         .remove(rec_network);

            if (removed)
            {
                TaskForNetworkRefresh.Settings settings = new TaskForNetworkRefresh.Settings();
                settings.forceSamplingConfiguration = true;

                TaskForNetworkRefresh.scheduleTask(sessionHolder, settings, rec_gateway, null);
            }

            sessionHolder.commit();

            return removed;
        }
    }

    @POST
    @Path("bindings/{gatewayId}/flush")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public TypedRecordIdentity<BackgroundActivityRecord> flushEntities(@PathParam("gatewayId") String gatewayId) throws
                                                                                                                 Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, (sessionHolder) ->
        {
            RecordHelper<GatewayAssetRecord> helper_gateway = sessionHolder.createHelper(GatewayAssetRecord.class);
            GatewayAssetRecord               recGateway     = helper_gateway.get(gatewayId);

            return TaskForGatewayFlush.scheduleTask(sessionHolder, recGateway, true, false);
        });

        return RecordIdentity.newTypedInstance(loc_task);
    }

    @POST
    @Path("bindings/{gatewayId}/flush-hb")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public TypedRecordIdentity<BackgroundActivityRecord> flushHeartbeat(@PathParam("gatewayId") String gatewayId) throws
                                                                                                                  Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, (sessionHolder) ->
        {
            RecordHelper<GatewayAssetRecord> helper_gateway = sessionHolder.createHelper(GatewayAssetRecord.class);
            GatewayAssetRecord               recGateway     = helper_gateway.get(gatewayId);

            return TaskForGatewayFlush.scheduleTask(sessionHolder, recGateway, false, true);
        });

        return RecordIdentity.newTypedInstance(loc_task);
    }

    //--//

    @POST
    @Path("report/start")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String startReport(DiscoveryReportRun run) throws
                                                      Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, (sessionHolder) ->
        {
            RecordHelper<NetworkAssetRecord>        helper   = sessionHolder.createHelper(NetworkAssetRecord.class);
            List<RecordLocator<NetworkAssetRecord>> locators = RecordLocator.createList(helper, run.networks);

            return TaskForDiscoveryReport.scheduleTask(sessionHolder, locators, run.filter);
        });

        return loc_task.getIdRaw();
    }

    @GET
    @Path("report/check/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DiscoveryReportProgress checkReport(@PathParam("id") String id,
                                               @QueryParam("detailed") Boolean detailed)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.getProgress(helper, id, BoxingUtils.get(detailed), TaskForDiscoveryReport.class);
        }
    }

    @GET
    @Path("report/excel/{id}/{fileName}")
    @Produces("application/octet-stream")
    @Optio3RequestLogLevel(Severity.Debug)
    public InputStream streamReport(@PathParam("id") String id,
                                    @PathParam("fileName") String fileName) throws
                                                                            Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.streamContents(helper, id, TaskForDiscoveryReport.class);
        }
    }

    //--//

    @GET
    @Path("azure-digital-twin/cred")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    @Optio3RequestLogLevel(Severity.Debug)
    public AzureDigitalTwinsHelper.Credentials getAzureDigitalTwinCredentials() throws
                                                                                Exception
    {
        InstanceConfiguration       cfg    = m_sessionProvider.getServiceNonNull(InstanceConfiguration.class);
        InstanceConfigurationForCRE cfgCRE = Reflection.as(cfg, InstanceConfigurationForCRE.class);
        if (cfgCRE != null)
        {
            AzureDigitalTwinsHelper.Credentials cred = cfgCRE.getAzureDigitalTwinCredentials();
            if (cred != null)
            {
                cred.obfuscate();
                return cred;
            }
        }

        return null;
    }

    @POST
    @Path("azure-digital-twin/cred")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    @Optio3RequestLogLevel(Severity.Debug)
    public AzureDigitalTwinsHelper.Credentials setAzureDigitalTwinCredentials(AzureDigitalTwinsHelper.Credentials cred) throws
                                                                                                                        Exception
    {
        InstanceConfiguration       cfg    = m_sessionProvider.getServiceNonNull(InstanceConfiguration.class);
        InstanceConfigurationForCRE cfgCRE = Reflection.as(cfg, InstanceConfigurationForCRE.class);
        if (cfgCRE != null)
        {
            cfgCRE.setAzureDigitalTwinCredentials(cred);
            if (cred != null)
            {
                cred.obfuscate();
                return cred;
            }
        }

        return null;
    }

    @GET
    @Path("azure-digital-twin/publish")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    @Optio3RequestLogLevel(Severity.Debug)
    public String pushToAzureDigitalTwin() throws
                                           Exception
    {
        InstanceConfiguration       cfg    = m_sessionProvider.getServiceNonNull(InstanceConfiguration.class);
        InstanceConfigurationForCRE cfgCRE = Reflection.as(cfg, InstanceConfigurationForCRE.class);
        if (cfgCRE != null)
        {
            AzureDigitalTwinsHelper.Credentials cred = cfgCRE.getAzureDigitalTwinCredentials();
            if (cred != null)
            {
                RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, (sessionHolder) ->
                {
                    RecordHelper<NetworkAssetRecord> helper = sessionHolder.createHelper(NetworkAssetRecord.class);

                    List<RecordLocator<NetworkAssetRecord>> locators = CollectionUtils.transformToListNoNulls(helper.listAll(), (rec_network) ->
                    {
                        return rec_network.getState() == AssetState.operational ? sessionHolder.createLocator(rec_network) : null;
                    });

                    return TaskForPublishingToAzureDigitalTwin.scheduleTask(sessionHolder, locators);
                });

                return loc_task.getIdRaw();
            }
        }

        return null;
    }

    @GET
    @Path("azure-digital-twin/publish-check/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public AzureDigitalTwinSyncProgress checkPushToAzureDigitalTwin(@PathParam("id") String id,
                                                                    @QueryParam("detailed") Boolean detailed)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.getProgress(helper, id, BoxingUtils.get(detailed), TaskForPublishingToAzureDigitalTwin.class);
        }
    }

    //--//

    @GET
    @Path("azure-iot-hub/cred")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    @Optio3RequestLogLevel(Severity.Debug)
    public AzureIotHubHelper.Credentials getAzureIoTHubCredentials() throws
                                                                     Exception
    {
        InstanceConfiguration       cfg    = m_sessionProvider.getServiceNonNull(InstanceConfiguration.class);
        InstanceConfigurationForCRE cfgCRE = Reflection.as(cfg, InstanceConfigurationForCRE.class);
        if (cfgCRE != null)
        {
            AzureIotHubHelper.Credentials cred = cfgCRE.getAzureIotHubCredentials();
            if (cred != null)
            {
                cred.obfuscate();
                return cred;
            }
        }

        return null;
    }

    @POST
    @Path("azure-iot-hub/cred")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    @Optio3RequestLogLevel(Severity.Debug)
    public AzureIotHubHelper.Credentials setAzureIoTHubCredentials(AzureIotHubHelper.Credentials cred) throws
                                                                                                       Exception
    {
        InstanceConfiguration       cfg    = m_sessionProvider.getServiceNonNull(InstanceConfiguration.class);
        InstanceConfigurationForCRE cfgCRE = Reflection.as(cfg, InstanceConfigurationForCRE.class);
        if (cfgCRE != null)
        {
            AzureIotHubHelper.Credentials credOld = cfgCRE.getAzureIotHubCredentials();
            if (cred == null || (credOld != null && !(StringUtils.equals(cred.hostName, credOld.hostName) && StringUtils.equals(cred.deviceId, credOld.deviceId))))
            {
                cfgCRE.setAzureIotHubConnectionHistory(null);
            }

            cfgCRE.setAzureIotHubCredentials(cred);
            if (cred != null)
            {
                cred.obfuscate();
                return cred;
            }
        }

        return null;
    }

    //--//

    @GET
    @Path("auto-config/{networkId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public boolean autoConfig(@PathParam("networkId") String networkId) throws
                                                                        Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<NetworkAssetRecord> helper_network = sessionHolder.createHelper(NetworkAssetRecord.class);
            NetworkAssetRecord               rec_network    = helper_network.get(networkId);
            boolean                          started        = false;

            GatewayAssetRecord rec_gateway = rec_network.getBoundGateway();
            if (rec_gateway != null)
            {
                InstanceConfiguration cfg = sessionHolder.getServiceNonNull(InstanceConfiguration.class);
                if (cfg.shouldAutoConfig())
                {
                    if (!TaskForTransportationAutoConfiguration.alreadyRunning(sessionHolder, rec_gateway))
                    {
                        TaskForTransportationAutoConfiguration.scheduleTask(sessionHolder, rec_gateway, rec_network);
                    }

                    started = true;
                }
            }

            sessionHolder.commit();

            return started;
        }
    }

    @POST
    @Path("sampling/{networkId}/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public TypedRecordIdentity<BackgroundActivityRecord> updateSampling(@PathParam("networkId") String networkId,
                                                                        @QueryParam("dryRun") Boolean dryRun,
                                                                        @QueryParam("startWithClassId") Boolean startWithClassId,
                                                                        @QueryParam("stopWithoutClassId") Boolean stopWithoutClassId,
                                                                        @QueryParam("triggerConfiguration") Boolean triggerConfiguration) throws
                                                                                                                                          Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, (sessionHolder) ->
        {
            NetworkAssetRecord rec_network = sessionHolder.getEntity(NetworkAssetRecord.class, networkId);

            return TaskForSamplingSettings.scheduleTask(sessionHolder,
                                                        rec_network,
                                                        BoxingUtils.get(dryRun),
                                                        BoxingUtils.get(startWithClassId),
                                                        BoxingUtils.get(stopWithoutClassId),
                                                        BoxingUtils.get(triggerConfiguration));
        });

        return RecordIdentity.newTypedInstance(loc_task);
    }

    //--//

    @GET
    @Path("device-templates/describe")
    @Produces(MediaType.APPLICATION_JSON)
    public DevicesTemplate describeDeviceTemplates()
    {
        DevicesTemplate res = new DevicesTemplate();
        res.collect();

        return res;
    }

    @GET
    @Path("device-templates/config")
    @Produces(MediaType.APPLICATION_JSON)
    public DevicesSamplingTemplate getDeviceSamplingTemplate() throws
                                                               IOException
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            return SystemPreferenceRecord.getTypedValue(sessionHolder, SystemPreferenceTypedValue.SamplingTemplate, DevicesSamplingTemplate.class);
        }
    }

    @POST
    @Path("device-templates/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public DevicesSamplingTemplate setDeviceSamplingTemplate(DevicesSamplingTemplate val) throws
                                                                                          Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DevicesSamplingTemplate valRoundTrip = SystemPreferenceRecord.setTypedValue(sessionHolder, SystemPreferenceTypedValue.SamplingTemplate, val);

            sessionHolder.commit();

            return valRoundTrip;
        }
    }

    @PUT
    @Path("device-templates/reclassify")
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public void reclassify()
    {
        final InstanceConfiguration cfg = m_sessionProvider.getServiceNonNull(InstanceConfiguration.class);
        cfg.reclassify();
    }
}
