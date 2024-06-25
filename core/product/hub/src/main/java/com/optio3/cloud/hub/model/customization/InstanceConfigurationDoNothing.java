/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowRecord;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;

@JsonTypeName("InstanceConfigurationDoNothing")
public class InstanceConfigurationDoNothing extends InstanceConfiguration
{
    @Override
    public void start()
    {
        // Nothing to do.
    }

    @Override
    public void stop()
    {
        // Nothing to do.
    }

    @Override
    public CompletableFuture<Void> preprocessResults(List<GatewayDiscoveryEntity> entities)
    {
        // Nothing to do.
        return wrapAsync(null);
    }

    @Override
    public boolean hasRoamingAssets()
    {
        return false;
    }

    @Override
    public boolean shouldAutoConfig()
    {
        return false;
    }

    @Override
    public boolean shouldReportWhenUnreachable(DeviceRecord rec,
                                               ZonedDateTime unresponsiveSince)
    {
        return false;
    }

    @Override
    public boolean fixupAutoConfig(ProtocolConfigForIpn cfg)
    {
        return false;
    }

    @Override
    public boolean prepareSamplingConfiguration(SessionHolder sessionHolder,
                                                DeviceRecord rec_device,
                                                DeviceElementRecord rec_obj,
                                                boolean checkNonZeroValue,
                                                List<DeviceElementSampling> config)
    {
        return false;
    }

    @Override
    public NormalizationRules updateNormalizationRules(SessionHolder sessionHolder,
                                                       NormalizationRules rules)
    {
        // Nothing to do.
        return null;
    }

    @Override
    public void reclassify()
    {
        // Nothing to do.
    }

    @Override
    public BackgroundActivityRecord scheduleClassification(SessionHolder sessionHolder,
                                                           NetworkAssetRecord rec_network) throws
                                                                                           Exception
    {
        // Nothing to do.
        return null;
    }

    @Override
    public void executeClassification(SessionProvider sessionProvider,
                                      RecordLocator<NetworkAssetRecord> loc_network)
    {
        // Nothing to do.
    }

    @Override
    public void handleWorkflowCreated(SessionHolder sessionHolder,
                                      WorkflowRecord rec,
                                      UserRecord rec_user)
    {
        // Nothing to do.
    }

    @Override
    public void handleWorkflowUpdated(SessionHolder sessionHolder,
                                      WorkflowRecord rec,
                                      UserRecord rec_user)
    {
        // Nothing to do.
    }

    @Override
    protected boolean shouldNotifyNewGateway(String instanceId)
    {
        return false;
    }

    @Override
    protected NetworkAssetRecord createInstanceId(SessionHolder sessionHolder,
                                                  String instanceId,
                                                  GatewayAssetRecord rec_gateway) throws
                                                                                  Exception
    {
        return createInstanceIdImpl(sessionHolder, instanceId, rec_gateway);
    }

    @Override
    protected void afterNetworkCreation(SessionHolder sessionHolder,
                                        GatewayAssetRecord rec_gateway,
                                        NetworkAssetRecord rec_network)
    {
        // Nothing to do.
    }
}
