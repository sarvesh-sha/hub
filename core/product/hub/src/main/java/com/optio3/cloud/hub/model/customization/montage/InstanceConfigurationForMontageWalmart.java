/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.montage;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Path;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.hub.model.customization.InstanceConfigurationForTransportation;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.ResultStagingRecord;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.concurrency.AsyncMutex;
import com.optio3.infra.integrations.WiliotHelper;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.WellKnownPointClassOrCustom;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.IpnDeviceDescriptor;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.protocol.model.ipn.objects.IpnLocation;
import com.optio3.protocol.model.ipn.objects.montage.BluetoothGateway_PixelTag;
import com.optio3.protocol.model.ipn.objects.montage.BluetoothGateway_PixelTagRaw;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("InstanceConfigurationForMontageWalmart")
public class InstanceConfigurationForMontageWalmart extends InstanceConfigurationForTransportation
{
    class PendingSample
    {
        MonotonousTime expiration;
        String         networkId;
        double         realTimestamp;
        long           fakeTimestamp;
        int            rssi;
        String         bridge;
        String         payload;
        IpnLocation    location;

        public void queueSample(DecodedSample input) throws
                                                     Exception
        {
            if (StringUtils.equals(input.eventName, "temperature"))
            {
                GatewayDiscoveryEntity en_network  = newDiscoveryEntry(null, GatewayDiscoveryEntitySelector.Network, networkId);
                GatewayDiscoveryEntity en_protocol = newDiscoveryEntry(en_network, GatewayDiscoveryEntitySelector.Protocol, GatewayDiscoveryEntity.Protocol_Ipn);

                var obj = new BluetoothGateway_PixelTag();
                obj.unitId      = input.assetId;
                obj.temperature = Float.parseFloat(input.value);
                obj.rssi        = rssi;

                IpnDeviceDescriptor desc = new IpnDeviceDescriptor();
                desc.name = obj.extractId();
                GatewayDiscoveryEntity en_device = newDiscoveryEntry(en_protocol, GatewayDiscoveryEntitySelector.Ipn_Device, desc);

                boolean queueObject = m_seenAssets.add(obj.unitId);
                for (var fieldDesc : obj.getDescriptors())
                {
                    GatewayDiscoveryEntity en_object = newDiscoveryEntry(en_device, GatewayDiscoveryEntitySelector.Ipn_Object, fieldDesc.name);
                    if (queueObject)
                    {
                        en_object.setContentsAsObject(IpnObjectModel.getObjectMapper(), obj);
                    }

                    newDiscoverySample(en_object, realTimestamp, obj);
                }

                var sessionProvider = new SessionProvider(m_app, null, Optio3DbRateLimiter.Normal);

                try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
                {
                    ResultStagingRecord.queue(sessionHolder.createHelper(ResultStagingRecord.class), Lists.newArrayList(en_network));

                    sessionHolder.commit();
                }
            }
        }
    }

    static class DecodedSample
    {
        public String assetId;
        public String categoryId;
        public String eventName; // "temperature",
        public long   start;
        public long   end;
        public String ownerId;
        public double confidence;
        public String value;
        public long   createdOn;
    }

    public static final String endpoint__WILIOT = "wiliot";

    private static final String c_credentialContextForWiliot             = "Wiliot";
    private static final String c_credentialKeyForOwnerId                = "ownerId";
    private static final String c_credentialKeyForAccessKeyForAsset      = "accessKeyForAsset";
    private static final String c_credentialKeyForAccessKeyForEdge       = "accessKeyForEdge";
    private static final String c_credentialKeyForAccessKeyForBaseUrl    = "baseUrl";
    private static final String c_credentialKeyForAccessKeyForMqttBroker = "mqttBroker";

    public static final Logger LoggerInstance = new Logger(InstanceConfigurationForMontageWalmart.class);

    private final AsyncMutex                m_mutex          = new AsyncMutex();
    private final LinkedList<PendingSample> m_pendingSamples = new LinkedList<>();
    private final Set<String>               m_seenAssets     = Sets.newHashSet();
    private final String                    m_deviceIdRaw;
    private       WiliotHelper              m_wiliotHelper;

    //--//

    public InstanceConfigurationForMontageWalmart()
    {
        String deviceIdRaw = "<not init>";

        try
        {
            IpnDeviceDescriptor desc = new IpnDeviceDescriptor();
            desc.name   = new BluetoothGateway_PixelTagRaw().extractId();
            deviceIdRaw = ObjectMappers.SkipNulls.writeValueAsString(desc);
        }
        catch (JsonProcessingException ignored)
        {
        }

        m_deviceIdRaw = deviceIdRaw;
    }

    //--//

    @Override
    public void start()
    {
        super.start();

        try
        {
            String ownerId           = m_app.computeWithPrivateValue(c_credentialContextForWiliot, c_credentialKeyForOwnerId, (v) -> v);
            String accessKeyForAsset = m_app.computeWithPrivateValue(c_credentialContextForWiliot, c_credentialKeyForAccessKeyForAsset, (v) -> v);
            String accessKeyForEdge  = m_app.computeWithPrivateValue(c_credentialContextForWiliot, c_credentialKeyForAccessKeyForEdge, (v) -> v);
            String baseUrl           = m_app.computeWithPrivateValue(c_credentialContextForWiliot, c_credentialKeyForAccessKeyForBaseUrl, (v) -> v);
            String mqttBroker        = m_app.computeWithPrivateValue(c_credentialContextForWiliot, c_credentialKeyForAccessKeyForMqttBroker, (v) -> v);

            if (ownerId == null)
            {
                LoggerInstance.warn("No Wiliot ownerId secret, cannot start MQTT broker");
                return;
            }

            if (accessKeyForAsset == null)
            {
                LoggerInstance.warn("No Wiliot accessKeyForAsset secret, cannot start MQTT broker");
                return;
            }

            if (accessKeyForEdge == null)
            {
                LoggerInstance.warn("No Wiliot accessKeyForEdge secret, cannot start MQTT broker");
                return;
            }

            m_wiliotHelper = new WiliotHelper(baseUrl, mqttBroker, ownerId, accessKeyForAsset, accessKeyForEdge, "Optio3_connector");
        }
        catch (Exception e)
        {
            LoggerInstance.error("Failed to create Wiliot helper: %s", e);
        }
    }

    @Override
    public CompletableFuture<Void> preprocessResults(List<GatewayDiscoveryEntity> entities) throws
                                                                                            Exception
    {
        List<PendingSample> samples = Lists.newArrayList();

        for (GatewayDiscoveryEntity en_network : GatewayDiscoveryEntity.filter(entities, GatewayDiscoveryEntitySelector.Network))
        {
            for (GatewayDiscoveryEntity en_protocol : en_network.filter(GatewayDiscoveryEntitySelector.Protocol, GatewayDiscoveryEntity.Protocol_Ipn))
            {
                for (GatewayDiscoveryEntity en_device : en_protocol.filter(GatewayDiscoveryEntitySelector.Ipn_Device, m_deviceIdRaw))
                {
                    for (GatewayDiscoveryEntity en_object : en_device.filter(GatewayDiscoveryEntitySelector.Ipn_Object))
                    {
                        for (GatewayDiscoveryEntity en_sample : en_object.filter(GatewayDiscoveryEntitySelector.Ipn_ObjectSample))
                        {
                            BluetoothGateway_PixelTagRaw obj = en_sample.getContentsAsObject(IpnObjectModel.getObjectMapper(), new TypeReference<>()
                            {
                            });

                            prepareSample(samples, en_network.selectorValue, obj, en_sample.getTimestampEpoch());
                        }

                        en_object.subEntities = null; // Reset to avoid generating samples.
                    }
                }
            }
        }

        if (!samples.isEmpty())
        {
            // Don't wait, let run in the background.
            queueSampleDecoding(samples);
        }

        return wrapAsync(null);
    }

    @Override
    public boolean fixupAutoConfig(ProtocolConfigForIpn cfg)
    {
        return false;
    }

    @Override
    protected boolean shouldIncludeObjectInClassification(IpnObjectModel contents)
    {
        return true;
    }

    @Override
    protected boolean shouldBeSingletonInClassification(WellKnownPointClassOrCustom pointClass,
                                                        Set<String> pointTags)
    {
        return false;
    }

    @Override
    public boolean hasRoamingAssets()
    {
        return true;
    }

    @Override
    public boolean shouldReportWhenUnreachable(DeviceRecord rec,
                                               ZonedDateTime unresponsiveSince)
    {
        return true;
    }

    @Override
    protected boolean shouldNotifyNewGateway(String instanceId)
    {
        return false;
    }

    //--//

    @Path(endpoint__WILIOT)
    public boolean handlePoints(SessionProvider sessionProvider,
                                JsonNode json) throws
                                               Exception
    {
        try
        {
            if (json == null)
            {
                return false;
            }

            synchronized (this)
            {
                LoggerInstance.debug(ObjectMappers.prettyPrintAsJson(json));
            }

            var decodedSample = ObjectMappers.SkipNulls.treeToValue(json, DecodedSample.class);
            synchronized (m_pendingSamples)
            {
                for (var it = m_pendingSamples.iterator(); it.hasNext(); )
                {
                    var pendingSample = it.next();
                    if (pendingSample.fakeTimestamp == decodedSample.start)
                    {
                        it.remove();

                        if (StringUtils.equals(decodedSample.eventName, "temperature"))
                        {
                            LoggerInstance.debug("Decoded temperature for PixelTag: %s", decodedSample.assetId);
                            pendingSample.queueSample(decodedSample);
                        }
                        break;
                    }
                }

                flushStaleSamples();
            }
        }
        catch (Exception e)
        {
            LoggerInstance.error("Failed to decode Wiliot message: %s", e);
        }

        return true;
    }

    //--//

    private void prepareSample(List<PendingSample> samples,
                               String networkId,
                               BluetoothGateway_PixelTagRaw obj,
                               double timestamp)
    {
        if (m_wiliotHelper == null || obj == null)
        {
            return;
        }

        try
        {
            var raw = obj.getRaw();
            if (raw != null)
            {
                if (raw.tag == null)
                {
                    var sample = new PendingSample();
                    sample.networkId     = networkId;
                    sample.rssi          = raw.rssi;
                    sample.realTimestamp = timestamp;
                    sample.expiration    = TimeUtils.computeTimeoutExpiration(15, TimeUnit.MINUTES);
                    sample.bridge        = raw.lastBridge;
                    sample.payload       = StringUtils.EMPTY;
                    sample.location      = raw.location;
                    samples.add(sample);
                }
                else
                {
                    var sample = new PendingSample();
                    sample.networkId     = networkId;
                    sample.rssi          = raw.rssi;
                    sample.realTimestamp = timestamp;
                    sample.expiration    = TimeUtils.computeTimeoutExpiration(15, TimeUnit.MINUTES);
                    sample.bridge        = raw.lastBridge;
                    sample.payload       = raw.tag;
                    sample.location      = raw.location;
                    samples.add(sample);
                }
            }
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to pre-process result, due to %s", t);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private CompletableFuture<Void> queueSampleDecoding(List<PendingSample> samples) throws
                                                                                     Exception
    {
        for (var sample : samples)
        {
            try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
            {
                if (sample.location != null)
                {
                    sample.fakeTimestamp = m_wiliotHelper.sendPixelTag(sample.bridge, sample.payload, sample.location.latitude, sample.location.longitude);
                }
                else
                {
                    sample.fakeTimestamp = m_wiliotHelper.sendPixelTag(sample.bridge, sample.payload, Double.NaN, Double.NaN);
                }

                LoggerInstance.debug("Scheduled decoding for PixelTag: network:(%s) timestamp:(%s) queuetime:(%s) bridge:(%s) payload:(%s)",
                                     sample.networkId,
                                     (long) (sample.realTimestamp * 1000),
                                     sample.fakeTimestamp,
                                     sample.bridge,
                                     sample.payload);

                synchronized (m_pendingSamples)
                {
                    m_pendingSamples.add(sample);
                }

                // Yield processor, to throttle Wiliot MQTT and get unique timestamps.
                await(sleep(100, TimeUnit.MILLISECONDS));
            }

            // Dead time requires by Wiliot, otherwise it will drop samples.
            await(sleep(6, TimeUnit.SECONDS));
        }

        synchronized (m_pendingSamples)
        {
            flushStaleSamples();
        }

        return wrapAsync(null);
    }

    private void flushStaleSamples()
    {
        for (var it = m_pendingSamples.iterator(); it.hasNext(); )
        {
            var pendingSample = it.next();
            if (TimeUtils.isTimeoutExpired(pendingSample.expiration))
            {
                it.remove();
                LoggerInstance.debugVerbose("Dropped stale PixelTag: network:(%s) timestamp:(%s) queuetime:(%s)", pendingSample.networkId, pendingSample.realTimestamp, pendingSample.fakeTimestamp);
            }
        }
    }
}
