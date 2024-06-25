/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.StreamHelperNextAction;

/**
 * This helper class is used when configuring the gateway.
 *
 * It deals with enumerating networks and devices, allowing the creation of small-sized configuration batches.
 *
 * This is necessary because when we have hundreds of thousands of configuration pieces, that can't happen in one shot.
 */
public final class GatewayConfigStateMachine
{
    public enum NextAction
    {
        Continue,
        Exit,
    }

    public interface Provider
    {
        boolean shouldEnumerateNetwork(RecordHelper<AssetRecord> helper_asset,
                                       NetworkAssetRecord rec_network) throws
                                                                       Exception;

        NextAction beginNetworkEnumeration(RecordHelper<AssetRecord> helper_asset,
                                           NetworkAssetRecord rec_network);

        NextAction handleDevice(RecordHelper<AssetRecord> helper_asset,
                                NetworkAssetRecord rec_network,
                                DeviceRecord rec_device) throws
                                                         Exception;

        NextAction endNetworkEnumeration(RecordHelper<AssetRecord> helper_asset,
                                         NetworkAssetRecord rec) throws
                                                                 Exception;
    }

    //--//

    public List<RecordLocator<NetworkAssetRecord>> loc_networks = Lists.newArrayList();
    public List<RecordLocator<DeviceRecord>>       loc_devices;

    public RecordLocator<NetworkAssetRecord> loc_currentNetwork;
    public RecordLocator<DeviceRecord>       loc_currentDevice;

    //--//

    public void init(List<RecordLocator<NetworkAssetRecord>> loc_networks)
    {
        this.loc_networks.addAll(loc_networks);
        loc_devices = null;
        loc_currentNetwork = null;
        loc_currentDevice = null;
    }

    @JsonIgnore
    public boolean isDone()
    {
        return loc_currentNetwork == null && loc_networks.isEmpty();
    }

    public void advance(SessionProvider sessionProvider,
                        boolean readOnly,
                        Provider provider) throws
                                           Exception
    {
        try (SessionHolder sessionHolder = readOnly ? sessionProvider.newReadOnlySession() : sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<AssetRecord>  helper_asset  = sessionHolder.createHelper(AssetRecord.class);
            RecordHelper<DeviceRecord> helper_device = sessionHolder.createHelper(DeviceRecord.class);

            while (true)
            {
                if (loc_currentNetwork == null)
                {
                    if (loc_networks.isEmpty())
                    {
                        break;
                    }

                    loc_currentNetwork = loc_networks.remove(0);
                    loc_devices = null;
                }

                NetworkAssetRecord rec_network = sessionHolder.fromLocator(loc_currentNetwork);

                if (loc_devices == null)
                {
                    loc_devices = Lists.newArrayList();
                    loc_currentDevice = null;

                    if (!provider.shouldEnumerateNetwork(helper_asset, rec_network))
                    {
                        loc_currentNetwork = null;
                        continue;
                    }

                    rec_network.enumerateChildrenNoNesting(helper_device, -1, (filters) -> filters.addState(AssetState.operational), (rec_device) ->
                    {
                        loc_devices.add(helper_device.asLocator(rec_device));

                        return StreamHelperNextAction.Continue_Evict;
                    });

                    if (provider.beginNetworkEnumeration(helper_asset, rec_network) == NextAction.Exit)
                    {
                        break;
                    }
                }

                if (loc_currentDevice == null)
                {
                    if (loc_devices.isEmpty())
                    {
                        loc_currentNetwork = null;

                        if (provider.endNetworkEnumeration(helper_asset, rec_network) == NextAction.Exit)
                        {
                            break;
                        }

                        continue;
                    }

                    loc_currentDevice = loc_devices.remove(0);
                }

                DeviceRecord rec_device = helper_device.fromLocator(loc_currentDevice);

                if (provider.handleDevice(helper_asset, rec_network, rec_device) == NextAction.Exit)
                {
                    break;
                }

                helper_device.evict(rec_device);
            }

            if (!readOnly)
            {
                sessionHolder.commit();
            }
        }
    }

    public void doneWithDevice()
    {
        loc_currentDevice = null;
    }
}
