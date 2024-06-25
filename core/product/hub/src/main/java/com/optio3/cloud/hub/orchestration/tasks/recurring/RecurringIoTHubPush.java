/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks.recurring;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeReason;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.hub.logic.samples.SamplesCache;
import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.model.customization.InstanceConfigurationForCRE;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesExtract;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.infra.integrations.azureiothub.AzureIotHubHelper;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.serialization.Reflection;
import com.optio3.util.IdGenerator;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

@Optio3RecurringProcessor
public class RecurringIoTHubPush extends RecurringActivityHandler
{
    public static class ForProgress
    {
    }

    public static final Logger LoggerInstance            = BackgroundActivityScheduler.LoggerInstance.createSubLogger(RecurringIoTHubPush.class);
    public static final Logger LoggerInstanceForProgress = BackgroundActivityScheduler.LoggerInstance.createSubLogger(ForProgress.class);

    public static class Telemetry
    {
        public String externalId;

        public String externalNetworkId; // The identity of the network.
        public String controllerTransport; // The network address to reach the controller.
        public String controllerId; // The identity of the device: network/instance number.
        public String controllerSubAddress; // Sometimes it holds the information for the MS/TP trunk.
        public String objectId;

        public ZonedDateTime timestamp;
        public Object        value;
    }

    public static class TelemetryBatch
    {
        public String deviceId;
        public String messageId;

        public List<Telemetry> samples;
    }

    public static class History
    {
        public final Map<String, ZonedDateTime> lastSample = Maps.newHashMap();
    }

    static class DeviceDetails
    {
        String                     networkSysId;
        String                     deviceSysId;
        BACnetDeviceDescriptor     deviceDescriptor;
        DeviceElementFilterRequest filter;
    }

    //--/

    @Override
    public Duration startupDelay()
    {
        // Delay first report by a few minutes after startup.
        return Duration.of(3, ChronoUnit.MINUTES);
    }

    @Override
    public CompletableFuture<ZonedDateTime> process(SessionProvider sessionProvider) throws
                                                                                     Exception
    {
        InstanceConfiguration cfg = sessionProvider.getServiceNonNull(InstanceConfiguration.class);

        var cfgForCRE = Reflection.as(cfg, InstanceConfigurationForCRE.class);
        if (cfgForCRE != null)
        {
            AzureIotHubHelper.Credentials cred = cfgForCRE.getAzureIotHubCredentials();
            if (cred != null)
            {
                process(sessionProvider, cfgForCRE, cred);
            }

            return wrapAsync(TimeUtils.future(5, TimeUnit.MINUTES));
        }

        return AsyncRuntime.asNull();
    }

    @Override
    public void shutdown()
    {
        // Nothing to do.
    }

    private void process(SessionProvider sessionProvider,
                         InstanceConfigurationForCRE cfg,
                         AzureIotHubHelper.Credentials cred) throws
                                                             Exception
    {
        History history = cfg.getAzureIotHubConnectionHistory();
        if (history == null)
        {
            history = new History();
        }

        History finalHistory = history;

        final AtomicInteger numDevices    = new AtomicInteger();
        final AtomicInteger numObjects    = new AtomicInteger();
        final AtomicInteger numTimestamps = new AtomicInteger();
        final AtomicInteger numBatches    = new AtomicInteger();

        try (var helper = new AzureIotHubHelper(cred)
        {
            private List<Telemetry> m_pending = Lists.newArrayList();

            private MonotonousTime m_flush;

            @Override
            public void close() throws
                                Exception
            {
                super.close();

                cfg.setAzureIotHubConnectionHistory(finalHistory);
            }

            @Override
            protected void reportStatusChange(IotHubConnectionStatus status,
                                              IotHubConnectionStatusChangeReason changeReason,
                                              Throwable t)
            {
                if (t == null)
                {
                    LoggerInstance.debug("IoT Hub status chage: %s, %s", status, changeReason);
                }
                else
                {
                    LoggerInstance.debug("IoT Hub status chage: %s, %s, due to %s", status, changeReason, t);
                }
            }

            @Override
            protected void reportMessageSent(String messageId,
                                             Object payload,
                                             IotHubStatusCode responseStatus)
            {
                LoggerInstance.debugVerbose("IoT Hub responded to message %s with status %s", messageId, responseStatus.name());

                switch (responseStatus)
                {
                    case OK:
                    case OK_EMPTY:
                        recordSuccess(payload);
                        break;

                    default:
                        LoggerInstance.debug("Failed to send message %s: %s", messageId, responseStatus.name());
                        break;
                }
            }

            private void recordSuccess(Object payload)
            {
                Telemetry telemetry = Reflection.as(payload, Telemetry.class);
                if (telemetry != null)
                {
                    finalHistory.lastSample.put(telemetry.externalId, telemetry.timestamp);
                    return;
                }

                TelemetryBatch telemetryBatch = Reflection.as(payload, TelemetryBatch.class);
                if (telemetryBatch != null)
                {
                    for (Telemetry t : telemetryBatch.samples)
                    {
                        recordSuccess(t);
                    }
                }
            }

            void pushSample(Telemetry payload) throws
                                               Exception
            {
                m_pending.add(payload);
                flushPending(100);

                if (TimeUtils.isTimeoutExpired(m_flush))
                {
                    cfg.setAzureIotHubConnectionHistory(finalHistory);

                    m_flush = TimeUtils.computeTimeoutExpiration(10, TimeUnit.MINUTES);
                }
            }

            void flushPending(int max)
            {
                if (m_pending.size() >= max)
                {
                    try
                    {
                        var telemetryBatch = new TelemetryBatch();
                        telemetryBatch.deviceId  = cred.deviceId;
                        telemetryBatch.messageId = IdGenerator.newGuid();
                        telemetryBatch.samples   = m_pending;

                        sendMessage(telemetryBatch.messageId, telemetryBatch, 200);
                        numBatches.incrementAndGet();

                        m_pending = Lists.newArrayList();
                    }
                    catch (Exception e)
                    {
                        LoggerInstance.debug("Failed telemetry: %s", e);
                    }
                }
            }
        })
        {
            SamplesCache samplesCache = sessionProvider.getServiceNonNull(SamplesCache.class);

            List<DeviceDetails> devices = collectDevices(sessionProvider);

            for (DeviceDetails deviceDetails : devices)
            {
                numDevices.incrementAndGet();

                LoggerInstanceForProgress.debug("Device: %s # devices=%,d objects=%,d samples=%,d batches=%,d",
                                                deviceDetails.deviceSysId,
                                                numDevices.get(),
                                                numObjects.get(),
                                                numTimestamps.get(),
                                                numBatches.get());

                try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
                {
                    DeviceElementRecord.enumerateNoNesting(sessionHolder.createHelper(DeviceElementRecord.class), deviceDetails.filter, (rec_object) ->
                    {
                        if (rec_object.getAzureDigitalTwinModel() != null)
                        {
                            numObjects.incrementAndGet();

                            BACnetDeviceDescriptor desc                 = deviceDetails.deviceDescriptor;
                            String                 controllerTransport  = desc.transport.toString();
                            String                 controllerId         = desc.address.toString();
                            String                 controllerSubAddress = desc.bacnetAddress != null ? desc.bacnetAddress.toString() : null;

                            String sysId = rec_object.getSysId();
                            String name  = rec_object.getName();

                            ZonedDateTime lastSample = finalHistory.lastSample.get(sysId);
                            if (lastSample != null)
                            {
                                // Move to the next time slot.
                                lastSample = lastSample.plusSeconds(1);
                            }
                            else
                            {
                                // TODO: make it configurable.
                                lastSample = TimeUtils.past(1, TimeUnit.DAYS);
                            }

                            try (TimeSeriesExtract<?> extractSrc = samplesCache.extractSamples(sysId, DeviceElementRecord.DEFAULT_PROP_NAME, null, false, lastSample, null, Object.class, null))
                            {
                                int numSamples = extractSrc.size();

                                numTimestamps.addAndGet(numSamples);

                                LoggerInstanceForProgress.debugVerbose("Object: %s # %s # %,d", sysId, name, numSamples);

                                for (int pos = 0; pos < numSamples; pos++)
                                {
                                    Object        val  = extractSrc.getValue(pos);
                                    ZonedDateTime time = TimeUtils.fromTimestampToUtcTime(extractSrc.getNthTimestamp(pos));

                                    LoggerInstanceForProgress.debugObnoxious("Object: %s # %s # %s %s", sysId, name, time, val);

                                    var telemetry = new Telemetry();
                                    telemetry.externalId = sysId;

                                    telemetry.externalNetworkId    = deviceDetails.networkSysId;
                                    telemetry.controllerTransport  = controllerTransport;
                                    telemetry.controllerId         = controllerId;
                                    telemetry.controllerSubAddress = controllerSubAddress;

                                    telemetry.objectId  = rec_object.getIdentifier();
                                    telemetry.timestamp = time;
                                    telemetry.value     = val;

                                    helper.pushSample(telemetry);
                                }
                            }

                            //return StreamHelperNextAction.Stop_Evict;
                        }

                        return StreamHelperNextAction.Continue_Evict;
                    });
                }
            }

            helper.flushPending(-1);

            LoggerInstanceForProgress.debug("Completed push: devices=%,d objects=%,d samples=%,d batches=%,d", numDevices.get(), numObjects.get(), numTimestamps.get(), numBatches.get());
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to push to IoT Hub, due to %s", t);
        }
    }

    private List<DeviceDetails> collectDevices(SessionProvider sessionProvider) throws
                                                                                Exception
    {
        List<DeviceDetails> res = Lists.newArrayList();

        try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
        {
            RecordHelper<NetworkAssetRecord> helper_network = sessionHolder.createHelper(NetworkAssetRecord.class);
            RecordHelper<DeviceRecord>       helper_device  = sessionHolder.createHelper(DeviceRecord.class);

            for (NetworkAssetRecord rec_network : helper_network.listAll())
            {
                rec_network.enumerateChildren(helper_device, true, -1, (filters) -> filters.addState(AssetState.operational), (rec_device) ->
                {
                    if (SessionHolder.isEntityOfClass(rec_device, BACnetDeviceRecord.class))
                    {
                        var details = new DeviceDetails();
                        details.networkSysId     = rec_network.getSysId();
                        details.deviceSysId      = rec_device.getSysId();
                        details.deviceDescriptor = rec_device.getIdentityDescriptor(BACnetDeviceDescriptor.class);

                        DeviceElementFilterRequest filter = DeviceElementFilterRequest.createFilterForParent(rec_device);
                        filter.hasAnySampling = true;
                        filter.isNotHidden    = true;
                        details.filter = filter;

                        res.add(details);
                    }

                    return StreamHelperNextAction.Continue_Evict;
                });
            }
        }

        return res;
    }
}
