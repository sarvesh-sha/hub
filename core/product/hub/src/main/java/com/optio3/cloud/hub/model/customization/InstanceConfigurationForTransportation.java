/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.model.asset.DeviceTemplate;
import com.optio3.cloud.hub.model.asset.DevicesSamplingTemplate;
import com.optio3.cloud.hub.model.customization.epower.InstanceConfigurationForEPower;
import com.optio3.cloud.hub.model.customization.merlinsolar.InstanceConfigurationForMerlinSolar;
import com.optio3.cloud.hub.model.customization.montage.InstanceConfigurationForMontageWalmart;
import com.optio3.cloud.hub.model.customization.palfinger.InstanceConfigurationForPalfinger;
import com.optio3.cloud.hub.model.customization.stealthpower.InstanceConfigurationForStealthPower;
import com.optio3.cloud.hub.orchestration.tasks.TaskForTransportationAutoConfiguration;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.IpnDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceTypedValue;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.protocol.model.ipn.objects.IpnLocation;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@JsonSubTypes({ @JsonSubTypes.Type(value = InstanceConfigurationForEPower.class),
                @JsonSubTypes.Type(value = InstanceConfigurationForPalfinger.class),
                @JsonSubTypes.Type(value = InstanceConfigurationForMerlinSolar.class),
                @JsonSubTypes.Type(value = InstanceConfigurationForMontageWalmart.class),
                @JsonSubTypes.Type(value = InstanceConfigurationForStealthPower.class) })
public abstract class InstanceConfigurationForTransportation extends InstanceConfigurationWithWellKnownClasses
{
    private static final IpnLocation s_location = new IpnLocation();

    public boolean disableAutoConfig;

    @Override
    public void start()
    {
    }

    //--//

    @Override
    public void stop()
    {
        // Nothing to do.
    }

    @Override
    public CompletableFuture<Void> preprocessResults(List<GatewayDiscoveryEntity> entities) throws
                                                                                            Exception
    {
        // Nothing to do.
        return wrapAsync(null);
    }

    @Override
    public boolean shouldAutoConfig()
    {
        return !disableAutoConfig;
    }

    protected boolean shouldReportWhenUnreachableImpl(DeviceRecord rec,
                                                      ZonedDateTime unresponsiveSince)
    {
        if (isLocationRecord(rec))
        {
            // Don't notify if it's a problem that lasted less than a day.
            return !TimeUtils.wasUpdatedRecently(unresponsiveSince, 1, TimeUnit.DAYS);
        }

        return true;
    }

    protected boolean isLocationRecord(DeviceRecord rec)
    {
        BaseAssetDescriptor desc = rec.getIdentityDescriptor();
        if (desc != null)
        {
            String deviceId = desc.toString();

            if (StringUtils.equals(deviceId, s_location.extractId()))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean prepareSamplingConfiguration(SessionHolder sessionHolder,
                                                DeviceRecord rec_device,
                                                DeviceElementRecord rec_obj,
                                                boolean checkNonZeroValue,
                                                List<DeviceElementSampling> config) throws
                                                                                    IOException
    {
        if (SessionHolder.isEntityOfClass(rec_device, IpnDeviceRecord.class))
        {
            DevicesSamplingTemplate settings = SystemPreferenceRecord.getTypedValue(sessionHolder, SystemPreferenceTypedValue.SamplingTemplate, DevicesSamplingTemplate.class);
            if (settings != null)
            {
                IpnObjectModel obj = rec_obj.getTypedContents(IpnObjectModel.getObjectMapper(), IpnObjectModel.class);

                String deviceId  = DeviceTemplate.extractPath(obj);
                String elementId = rec_obj.getIdentifier();

                Integer period = settings.lookup(deviceId, elementId);
                if (period != null)
                {
                    DeviceElementSampling.add(config, DeviceElementRecord.DEFAULT_PROP_NAME, period);
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public void reclassify()
    {
        //
        // Rerun classification, in case point classes have changed.
        //
        reclassifyAllNetworks();
    }

    @Override
    protected boolean shouldNotifyNewGateway(String instanceId)
    {
        return true;
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
                                        NetworkAssetRecord rec_network) throws
                                                                        Exception
    {
        TaskForTransportationAutoConfiguration.scheduleTask(sessionHolder, rec_gateway, rec_network);
    }
}
