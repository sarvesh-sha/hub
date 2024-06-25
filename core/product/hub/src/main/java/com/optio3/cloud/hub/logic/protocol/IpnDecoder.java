/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.protocol;

import java.io.IOException;
import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.spooler.ResultStagingSpooler;
import com.optio3.cloud.hub.logic.spooler.StagedResultsSummary;
import com.optio3.cloud.hub.model.location.LongitudeLatitude;
import com.optio3.cloud.hub.persistence.asset.IpnDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.WellKnownPointClassOrCustom;
import com.optio3.protocol.model.ipn.IpnDeviceDescriptor;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.protocol.model.ipn.objects.IpnLocation;

public class IpnDecoder implements IProtocolDecoder
{
    private static class LocationState
    {
        IpnLocation obj;
        double      latitudeTime;
        double      longitudeTime;
    }

    private final Map<String, LocationState> m_locationCache = Maps.newHashMap();

    @Override
    public GatewayDiscoveryEntitySelector getRootSelector()
    {
        return GatewayDiscoveryEntitySelector.Network;
    }

    @Override
    public String getProtocolValue()
    {
        return GatewayDiscoveryEntity.Protocol_Ipn;
    }

    @Override
    public GatewayDiscoveryEntitySelector getDeviceSelector()
    {
        return GatewayDiscoveryEntitySelector.Ipn_Device;
    }

    @Override
    public GatewayDiscoveryEntitySelector getDeviceReachabilitySelector()
    {
        return GatewayDiscoveryEntitySelector.Ipn_Reachability;
    }

    @Override
    public GatewayDiscoveryEntitySelector getObjectSelector()
    {
        return GatewayDiscoveryEntitySelector.Ipn_Object;
    }

    @Override
    public GatewayDiscoveryEntitySelector getSampleSelector()
    {
        return GatewayDiscoveryEntitySelector.Ipn_ObjectSample;
    }

    @Override
    public IpnDeviceDescriptor decodeDeviceContext(GatewayDiscoveryEntity en_root,
                                                   GatewayDiscoveryEntity en_device)
    {
        return en_device.getSelectorValueAsObject(IpnDeviceDescriptor.class);
    }

    @Override
    public void mergeDetails(BaseAssetDescriptor target,
                             BaseAssetDescriptor source)
    {
        // Nothing to do.
    }

    //--//

    @Override
    public void trackSampleIfNeeded(StagedResultsSummary.ForRoot root,
                                    BaseAssetDescriptor device,
                                    String objectIdentifier,
                                    double timestampEpochSeconds,
                                    String contents)
    {
        IpnObjectModel obj = IpnObjectModel.allocateFromDescriptor((IpnDeviceDescriptor) device);
        if (obj instanceof IpnLocation && contents != null)
        {
            FieldModel fieldModel = obj.getDescriptor(objectIdentifier, false);
            if (fieldModel != null)
            {
                WellKnownPointClassOrCustom pointClass = fieldModel.getPointClass(obj);
                if (pointClass != null && pointClass.known != null)
                {
                    switch (pointClass.known)
                    {
                        case LocationLatitude:
                        case LocationLongitude:
                            try
                            {
                                IpnLocation obj2 = IpnObjectModel.deserializeFromJson(IpnLocation.class, contents);

                                LocationState state = m_locationCache.get(root.sysId);
                                if (state == null)
                                {
                                    state     = new LocationState();
                                    state.obj = obj2;
                                    m_locationCache.put(root.sysId, state);
                                }

                                if (pointClass.known == WellKnownPointClass.LocationLatitude)
                                {
                                    // Only look at the newest sample.
                                    if (timestampEpochSeconds > state.latitudeTime && obj2.latitude != 0.0)
                                    {
                                        state.obj.latitude = obj2.latitude;
                                        state.latitudeTime = timestampEpochSeconds;
                                    }
                                }
                                else
                                {
                                    // Only look at the newest sample.
                                    if (timestampEpochSeconds > state.longitudeTime && obj2.longitude != 0.0)
                                    {
                                        state.obj.longitude = obj2.longitude;
                                        state.longitudeTime = timestampEpochSeconds;
                                    }
                                }
                            }
                            catch (IOException e)
                            {
                                // Ignore failures.
                            }
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void importDone(SessionHolder holder)
    {
        m_locationCache.entrySet()
                       .removeIf((entry) -> processLocation(holder, entry.getKey(), entry.getValue()));
    }

    private boolean processLocation(SessionHolder holder,
                                    String sysId,
                                    LocationState state)
    {
        // The lat/long samples cannot be temporally too afar.
        if (state.latitudeTime > 0 && state.longitudeTime > 0 && Math.abs(state.latitudeTime - state.longitudeTime) < 900)
        {
            try (SessionHolder subHolder = holder.spawnNewSessionWithoutTransaction())
            {
                NetworkAssetRecord rec_network = subHolder.getEntityOrNull(NetworkAssetRecord.class, sysId);
                if (rec_network != null)
                {
                    IpnDeviceRecord rec_device = IpnDeviceRecord.findByIdentifier(subHolder.createHelper(IpnDeviceRecord.class), rec_network, state.obj.extractId());
                    if (rec_device != null)
                    {
                        LocationRecord rec_location = rec_device.getLocation();
                        if (rec_location != null)
                        {
                            subHolder.beginTransaction();

                            LongitudeLatitude geo = new LongitudeLatitude();
                            geo.longitude = state.obj.longitude;
                            geo.latitude  = state.obj.latitude;
                            rec_location.setGeo(geo);

                            subHolder.commit();

                            return true;
                        }
                    }
                }
            }
            catch (Throwable t)
            {
                ResultStagingSpooler.LoggerInstance.error("Failed to flush location for '%s': %s", sysId, t);
            }
        }

        return false;
    }

    //--//

    @Override
    public CommonProtocolHandlerForIngestion<?, ?> prepareIngestor(HubConfiguration cfg,
                                                                   Logger logger,
                                                                   StagedResultsSummary.ForAsset forAsset)
    {
        return new IpnHandlerForBatchIngestion(cfg, logger, forAsset);
    }
}
