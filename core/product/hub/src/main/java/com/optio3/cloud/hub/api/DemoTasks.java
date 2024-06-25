/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
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
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.logic.simulator.SimulatedGateway;
import com.optio3.cloud.hub.logic.simulator.generators.AHUGroup;
import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.orchestration.tasks.TaskForDeviceElementNormalization;
import com.optio3.cloud.hub.orchestration.tasks.TaskForNetworkRefresh;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.hub.persistence.normalization.NormalizationRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.config.ProtocolConfigForBACnet;
import com.optio3.util.IdGenerator;
import io.swagger.annotations.Api;

@Api(tags = { "DemoTasks" }) // For Swagger
@Optio3RestEndpoint(name = "DemoTasks") // For Optio3 Shell
@Path("/v1/demo-tasks")
@Optio3RequestLogLevel(Severity.Debug)
public class DemoTasks
{
    @Inject
    private HubConfiguration m_cfg;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    //--//

    @GET
    @Path("demodata")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean hasDemoData()
    {
        return m_cfg.developerSettings.includeDemoData;
    }

    @GET
    @Path("trigger-demo-messages")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean triggerDemoMessages()
    {
        if (m_cfg.developerSettings.includeDemoData)
        {
            // TODO: trigger demo messages.

            return true;
        }

        return false;
    }

    //--//

    @POST
    @Path("simulated-gateway/create")
    @Consumes(MediaType.APPLICATION_JSON)
    public String createGateway(@QueryParam("name") String name,
                                @QueryParam("numDevice") Optional<Integer> numDevices,
                                @QueryParam("numHistoricalDays") Optional<Integer> numHistoricalDays,
                                @QueryParam("samplingPeriod") Optional<Integer> samplingPeriod) throws
                                                                                                Exception
    {
        if (!m_cfg.developerSettings.includeDemoData)
        {
            throw new NotAuthorizedException(null);
        }

        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<AssetRecord>  helper_asset  = sessionHolder.createHelper(AssetRecord.class);
            RecordHelper<DeviceRecord> helper_device = sessionHolder.createHelper(DeviceRecord.class);

            String instanceId = IdGenerator.newGuid();

            LocationRecord rec_location = new LocationRecord();
            rec_location.setPhysicalName("1411 4th Avenue");
            rec_location.setType(LocationType.BUILDING);
            rec_location.setAddress("1411 4th Avenue, Seattle, WA 98104, USA");
            sessionHolder.persistEntity(rec_location);

            GatewayAssetRecord rec_gateway = new GatewayAssetRecord();
            rec_gateway.setInstanceId(instanceId);
            rec_gateway.setPhysicalName(name);
            rec_gateway.setState(AssetState.operational);
            rec_gateway.setLocation(rec_location);
            sessionHolder.persistEntity(rec_gateway);

            NetworkAssetRecord rec_network = new NetworkAssetRecord();
            rec_network.setPhysicalName("Simulated Gateway Network " + instanceId);
            rec_network.setCidr("127.0.0.1");
            rec_network.setSamplingPeriod(samplingPeriod.orElse(300));
            rec_network.setLocation(rec_location);

            ProtocolConfigForBACnet protocolCfg = new ProtocolConfigForBACnet();
            rec_network.setProtocolsConfiguration(Lists.newArrayList(protocolCfg));
            sessionHolder.persistEntity(rec_network);

            rec_gateway.getBoundNetworks()
                       .add(rec_network);

            // Initialize Simulator
            SimulatedGateway gateway = SimulatedGateway.create(sessionHolder.getService(HubApplication.class), instanceId, numHistoricalDays.orElse(30));

            BackgroundActivityRecord taskNormalization = null;

            NormalizationRecord rec_normalization = NormalizationRecord.findActive(sessionHolder.createHelper(NormalizationRecord.class));
            if (rec_normalization != null)
            {
                NormalizationRules rules = rec_normalization.getRules();
                // Persist Air Handler
                for (int i = 1; i <= numDevices.orElse(1); i++)
                {
                    AHUGroup ahGroup = new AHUGroup(i);
                    ahGroup.persist(helper_asset, rec_network, rules.pointOverrides);

                    ahGroup.register(gateway);
                }

                rec_normalization.setRules(rules);

                // Kick off normalization, if any.
                List<RecordLocator<DeviceRecord>> locators = Lists.newArrayList();

                rec_network.enumerateChildren(helper_device, false, -1, null, (rec_device) ->
                {
                    locators.add(helper_device.asLocator(rec_device));
                    return StreamHelperNextAction.Continue;
                });

                taskNormalization = TaskForDeviceElementNormalization.scheduleTask(sessionHolder, locators, rules, null, false);
            }

            // Kick off refresh
            TaskForNetworkRefresh.Settings settings = new TaskForNetworkRefresh.Settings();
            settings.forceDiscovery   = true;
            settings.forceListObjects = true;
            settings.forceReadObjects = true;
            settings.sleepOnStart     = 4; // Delay a bit to let the Simulated Gateway complete startup.

            BackgroundActivityRecord taskRefresh = TaskForNetworkRefresh.scheduleTask(sessionHolder, settings, rec_gateway, (t) ->
            {
                t.targetNetworks = Lists.newArrayList(sessionHolder.createLocator(rec_network));
            });

            if (taskNormalization != null)
            {
                taskRefresh.transitionToWaiting(taskNormalization, null);
            }

            sessionHolder.commit();

            return instanceId;
        }
    }

    @GET
    @Path("simulated-gateway/check/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public boolean checkGatewayProgress(@PathParam("id") String instanceId)
    {
        SimulatedGateway gateway = SimulatedGateway.get(instanceId);
        if (gateway == null)
        {
            return false;
        }

        return gateway.getHistoricalDataStartDate() == null;
    }
}
