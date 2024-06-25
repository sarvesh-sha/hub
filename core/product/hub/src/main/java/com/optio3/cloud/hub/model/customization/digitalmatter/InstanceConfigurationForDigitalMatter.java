/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.client.builder.model.DeploymentInstance;
import com.optio3.cloud.client.builder.model.DeviceDetails;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.model.customization.InstanceConfigurationWithWellKnownClasses;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseAsyncModel;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseDataFieldModel;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseWireModel;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.Packet;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.async.ConnectToOemAsync;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.AnalogData16Field;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.AnalogDatum16Field;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.DebugEventField;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.DigitalDataField;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.GpsDataField;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.ProfilingCountersField;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.AsyncMessageRequestPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.AsyncMessageResponsePayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.AsyncSessionCompletePayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.CommitRequestPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.CommitResponsePayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.DataFieldPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.DisconnectSocketPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.HelloPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.HelloResponsePayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.RequestAsyncSessionStartPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.SendDataRecordPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.SendDataRecordsPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.UnknownDataPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.VersionDataPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.VersionDataResponsePayload;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.orchestration.tasks.TaskForAutoNetworkClassification;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.IpnDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.asset.ResultStagingRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.persistence.LogHolder;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.concurrency.Executors;
import com.optio3.lang.Unsigned32;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.common.ServiceWorker;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.WellKnownPointClassOrCustom;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.IpnDeviceDescriptor;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.protocol.model.ipn.objects.IpnLocation;
import com.optio3.protocol.model.ipn.objects.digitalmatter.DigitalMatter_AnalogState;
import com.optio3.protocol.model.ipn.objects.digitalmatter.DigitalMatter_Counters;
import com.optio3.protocol.model.ipn.objects.digitalmatter.DigitalMatter_DigitalState;
import com.optio3.protocol.model.ipn.objects.digitalmatter.DigitalMatter_Flag;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.BufferUtils;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.ConsumerWithException;

@JsonTypeName("InstanceConfigurationForDigitalMatter")
public class InstanceConfigurationForDigitalMatter extends InstanceConfigurationWithWellKnownClasses
{
    public static final Logger LoggerInstance = new Logger(InstanceConfigurationForDigitalMatter.class);
    public static final int    TCP_PORT       = 10000;

    private static final EngineeringUnitsFactors cm_per_second;

    static
    {
        EngineeringUnitsFactors cm      = EngineeringUnits.centimeters.getConversionFactors();
        EngineeringUnitsFactors seconds = EngineeringUnits.seconds.getConversionFactors();
        cm_per_second = cm.divideBy(seconds, true);
    }

    //--//

    class SocketSession
    {
        class PendingAsync
        {
            int                                                m_messageId;
            ConsumerWithException<AsyncMessageResponsePayload> m_callback;
        }

        class PendingRecord
        {
            final ZonedDateTime                                        timestamp;
            final Map<Class<? extends IpnObjectModel>, IpnObjectModel> values = Maps.newHashMap();
            final List<DebugEventField>                                spew   = Lists.newArrayList();

            PendingRecord(ZonedDateTime timestamp)
            {
                this.timestamp = timestamp;
            }

            boolean isEmpty()
            {
                return values.isEmpty() && spew.isEmpty();
            }

            <T extends IpnObjectModel> T ensureValue(Class<T> clz)
            {
                T val = clz.cast(values.get(clz));
                if (val == null)
                {
                    val = Reflection.newInstance(clz);
                    values.put(clz, val);
                }

                return val;
            }
        }

        private final Socket       m_socket;
        private final InputStream  m_inputStream;
        private final OutputStream m_outputStream;

        private       int                m_asyncIdNext = 1;
        private final List<PendingAsync> m_asyncQueue  = Lists.newArrayList();

        private       RecordLocator<GatewayAssetRecord> m_loc_gateway;
        private       RecordLocator<NetworkAssetRecord> m_loc_network;
        private       AssetState                        m_state_gateway;
        private final List<PendingRecord>               m_records = Lists.newArrayList();

        public SocketSession(Socket socket) throws
                                            IOException
        {
            m_socket       = socket;
            m_inputStream  = socket.getInputStream();
            m_outputStream = socket.getOutputStream();
        }

        public void process()
        {
            SocketAddress fromAddress = m_socket.getRemoteSocketAddress();
            LoggerInstance.debug("Connection from %s", fromAddress);

            try
            {
                while (!m_socket.isClosed())
                {
                    Packet packet = Packet.decode(LoggerInstance, m_inputStream);
                    if (packet == null)
                    {
                        break;
                    }

                    if (packet.payload instanceof UnknownDataPayload)
                    {
                        LoggerInstance.debug("Unknown packet from %s: %d", fromAddress, packet.messageType);
                        continue;
                    }

                    if (!dispatch(packet.payload))
                    {
                        break;
                    }
                }

                LoggerInstance.debug("Closing connection from %s", fromAddress);
            }
            catch (Throwable t)
            {
                LoggerInstance.debug("Failed to process data from %s: %s", fromAddress, t);
            }
        }

        private boolean dispatch(BaseWireModel payload)
        {
            if (LoggerInstance.isEnabled(Severity.DebugVerbose))
            {
                LoggerInstance.debugVerbose("New packet: %s", payload.getClass());
                LoggerInstance.debugVerbose(ObjectMappers.prettyPrintAsJson(payload));
            }

            try
            {
                if (payload instanceof DisconnectSocketPayload)
                {
                    return false;
                }

                if (payload instanceof HelloPayload)
                {
                    registerDevice((HelloPayload) payload);

                    HelloResponsePayload response = new HelloResponsePayload();
                    response.timestamp = TimeUtils.now();
                    send(response);
                    return true;
                }

                if (payload instanceof VersionDataPayload)
                {
                    VersionDataPayload payloadTyped = (VersionDataPayload) payload;

                    // TODO: Actually process request.

                    VersionDataResponsePayload response = new VersionDataResponsePayload();
                    response.deviceSerial = payloadTyped.deviceSerial;
                    response.canAddress   = payloadTyped.canAddress;
                    send(response);
                    return true;
                }

                if (payload instanceof AsyncMessageResponsePayload)
                {
                    AsyncMessageResponsePayload payloadTyped = (AsyncMessageResponsePayload) payload;

                    Iterator<PendingAsync> it = m_asyncQueue.iterator();
                    while (it.hasNext())
                    {
                        PendingAsync pendingAsync = it.next();
                        if (payloadTyped.messageId.equals(pendingAsync.m_messageId))
                        {
                            pendingAsync.m_callback.accept(payloadTyped);
                            it.remove();
                            break;
                        }
                    }

                    checkAsyncDone();
                }

                if (payload instanceof RequestAsyncSessionStartPayload)
                {
                    if (m_state_gateway == AssetState.maintenance)
                    {
                        ConnectToOemAsync async1 = new ConnectToOemAsync();
                        queueAsync(async1, (response) ->
                        {

                        });
                    }

                    checkAsyncDone();
                    return true;
                }

                if (payload instanceof SendDataRecordsPayload)
                {
                    SendDataRecordsPayload payloadTyped = (SendDataRecordsPayload) payload;
                    for (SendDataRecordPayload record : payloadTyped.records)
                    {
                        PendingRecord pendingRecord = new PendingRecord(record.timestamp);

                        for (DataFieldPayload value : record.payload)
                        {
                            BaseDataFieldModel fieldValue = value.field;

                            GpsDataField gps = Reflection.as(fieldValue, GpsDataField.class);
                            if (gps != null)
                            {
                                IpnLocation obj = pendingRecord.ensureValue(IpnLocation.class);
                                obj.latitude  = gps.latitude;
                                obj.longitude = gps.longitude;
                                obj.altitude  = gps.altitude;
                                obj.speed     = (int) EngineeringUnits.convert(gps.groundSpeed, cm_per_second, EngineeringUnits.kilometers_per_hour.getConversionFactors());
                                obj.heading   = gps.heading;
                            }

                            AnalogData16Field analog16 = Reflection.as(fieldValue, AnalogData16Field.class);
                            if (analog16 != null)
                            {
                                DigitalMatter_AnalogState obj = pendingRecord.ensureValue(DigitalMatter_AnalogState.class);
                                obj.batteryVoltage      = analog16.extractFloat(AnalogDatum16Field.Id.InternalBatteryVoltage, 1.0);
                                obj.batteryCapacity     = analog16.extractFloat(AnalogDatum16Field.Id.InternalBatteryCapacity, 0.01);
                                obj.internalTemperature = analog16.extractFloat(AnalogDatum16Field.Id.InternalTemperature, 0.01);
                                obj.signalStrength      = (int) analog16.extractFloat(AnalogDatum16Field.Id.GsmSignalStrength, 1.0);
                            }

                            DigitalDataField digital = Reflection.as(fieldValue, DigitalDataField.class);
                            if (digital != null)
                            {
                                DigitalMatter_DigitalState obj = pendingRecord.ensureValue(DigitalMatter_DigitalState.class);
                                obj.inTrip             = digital.inTrip ? DigitalMatter_Flag.Active : DigitalMatter_Flag.Inactive;
                                obj.tamperAlert        = digital.tamperAlert ? DigitalMatter_Flag.Active : DigitalMatter_Flag.Inactive;
                                obj.recoveryModeActive = digital.recoveryModeActive ? DigitalMatter_Flag.Active : DigitalMatter_Flag.Inactive;
                            }

                            ProfilingCountersField counters = Reflection.as(fieldValue, ProfilingCountersField.class);
                            if (counters != null)
                            {
                                DigitalMatter_Counters obj = pendingRecord.ensureValue(DigitalMatter_Counters.class);
                                obj.successfulUploads     = counters.extractInteger(ProfilingCountersField.Id.SuccessfulUploads, 1.0);
                                obj.successfulUploadsTime = counters.extractInteger(ProfilingCountersField.Id.SuccessfulUploadTime, 1.0);
                                obj.failedUploads         = counters.extractInteger(ProfilingCountersField.Id.FailedUploads, 1.0);
                                obj.failedUploadsTime     = counters.extractInteger(ProfilingCountersField.Id.FailedUploadTime, 1.0);
                                obj.successfulGpsFixes    = counters.extractInteger(ProfilingCountersField.Id.SuccessfulGpsFixes, 1.0);
                                obj.successfulGpsFixTime  = counters.extractInteger(ProfilingCountersField.Id.SuccessfulGpsFixTime, 1.0);
                                obj.failedGpsFixes        = counters.extractInteger(ProfilingCountersField.Id.FailedGpsFixes, 1.0);
                                obj.failedGpsFixTime      = counters.extractInteger(ProfilingCountersField.Id.FailedGpsFixTime, 1.0);
                                obj.gpsFreshenAttempts    = counters.extractInteger(ProfilingCountersField.Id.GpsFreshenAttempts, 1.0);
                                obj.gpsFreshenTime        = counters.extractInteger(ProfilingCountersField.Id.GpsFreshenTime, 1.0);
                                obj.accelerometerWakeups  = counters.extractInteger(ProfilingCountersField.Id.AccelerometerWakeups, 1.0);
                                obj.trips                 = counters.extractInteger(ProfilingCountersField.Id.Trips, 1.0);
                            }

                            DebugEventField debugEvent = Reflection.as(fieldValue, DebugEventField.class);
                            if (debugEvent != null)
                            {
                                pendingRecord.spew.add(debugEvent);
                            }
                        }

                        if (!pendingRecord.isEmpty())
                        {
                            m_records.add(pendingRecord);
                        }
                    }

                    return true;
                }

                if (payload instanceof CommitRequestPayload)
                {
                    CommitResponsePayload response = new CommitResponsePayload();
                    response.success = commitRecords();
                    send(response);
                    return true;
                }

                // Accept all messages.
                return true;
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to dispatch message '%s'', due to %s", payload.getClass(), t);

                return false;
            }
        }

        private void checkAsyncDone() throws
                                      IOException
        {
            if (m_asyncQueue.isEmpty())
            {
                AsyncSessionCompletePayload response = new AsyncSessionCompletePayload();
                send(response);
            }
        }

        private void queueAsync(BaseAsyncModel msg,
                                ConsumerWithException<AsyncMessageResponsePayload> callback) throws
                                                                                             Exception
        {
            PendingAsync pendingAsync = new PendingAsync();
            pendingAsync.m_messageId = m_asyncIdNext++;
            pendingAsync.m_callback  = callback;

            m_asyncQueue.add(pendingAsync);

            AsyncMessageRequestPayload request = new AsyncMessageRequestPayload();
            request.messageId = Unsigned32.box(pendingAsync.m_messageId);
            request.setPayload(msg);
            send(request);
        }

        private void send(BaseWireModel response) throws
                                                  IOException
        {
            if (LoggerInstance.isEnabled(Severity.DebugVerbose))
            {
                LoggerInstance.debugVerbose("New reply: %s", response.getClass());
                LoggerInstance.debugVerbose(ObjectMappers.prettyPrintAsJson(response));
            }

            Packet packet = new Packet();
            packet.payload = response;
            byte[] buf = packet.encode();
            if (LoggerInstance.isEnabled(Severity.DebugObnoxious))
            {
                BufferUtils.convertToHex(buf, 0, buf.length, 32, true, (line) -> LoggerInstance.debugObnoxious("Data Reply: %s", line));
            }

            m_outputStream.write(buf);
        }

        //--//

        private void registerDevice(HelloPayload payload) throws
                                                          Exception
        {
            try (SessionHolder sessionHolder = SessionHolder.createWithNewSessionWithTransaction(m_app, null, Optio3DbRateLimiter.Normal))
            {
                String        instanceId = String.format("%s__%d", payload.productId, payload.deviceSerial.unboxUnsigned());
                InstanceState state      = resolveInstanceId(sessionHolder, instanceId);
                if (state.rec_network == null)
                {
                    LoggerInstance.error("Failed to locate network for tracker '%s'!!", instanceId);
                    return;
                }

                sessionHolder.commit();

                if (state.createdGateway)
                {
                    DeviceDetails deviceDetails = new DeviceDetails();
                    deviceDetails.instanceType     = DeploymentInstance.DigitalMatter;
                    deviceDetails.hostId           = instanceId;
                    deviceDetails.imei             = payload.imei;
                    deviceDetails.iccid            = payload.iccid;
                    deviceDetails.productId        = payload.productId.toString();
                    deviceDetails.hardwareRevision = String.format("V%d", payload.hardwareRevisionNumber & 0xFF);
                    deviceDetails.firmwareVersion  = String.format("V%d.%d", payload.firmwareMajor & 0xFF, payload.firmwareMinor & 0xFF);
                    m_app.sendDeviceNotification(sessionHolder, deviceDetails);
                }

                m_loc_gateway = sessionHolder.createLocator(state.rec_gateway);
                m_loc_network = sessionHolder.createLocator(state.rec_network);

                m_state_gateway = state.rec_gateway.getState();
            }
        }

        private boolean commitRecords()
        {
            try (SessionHolder sessionHolder = SessionHolder.createWithNewSessionWithTransaction(m_app, null, Optio3DbRateLimiter.Normal))
            {
                NetworkAssetRecord                               rec_network  = sessionHolder.fromLocator(m_loc_network);
                RecordLocked<GatewayAssetRecord>                 lock_gateway = sessionHolder.fromLocatorWithLock(m_loc_gateway, 20, TimeUnit.SECONDS);
                GatewayDiscoveryEntity                           en_network   = newDiscoveryEntry(null, GatewayDiscoveryEntitySelector.Network, rec_network.getSysId());
                GatewayDiscoveryEntity                           en_protocol  = newDiscoveryEntry(en_network, GatewayDiscoveryEntitySelector.Protocol, GatewayDiscoveryEntity.Protocol_Ipn);
                Map<IpnDeviceDescriptor, GatewayDiscoveryEntity> lookupDevice = Maps.newHashMap();

                try (var logHandler = GatewayAssetRecord.allocateLogHandler(lock_gateway))
                {
                    try (LogHolder log = logHandler.newLogHolder())
                    {
                        for (PendingRecord record : m_records)
                        {
                            for (DebugEventField debugEvent : record.spew)
                            {
                                log.addLineSync(1,
                                                record.timestamp,
                                                null,
                                                null,
                                                null,
                                                null,
                                                String.format("%s [%s / %s] %s", debugEvent.severity, debugEvent.moduleId, debugEvent.eventCode, debugEvent.text));
                            }
                        }
                    }
                }

                for (PendingRecord record : m_records)
                {
                    for (IpnObjectModel obj : record.values.values())
                    {
                        commitRecord(sessionHolder, rec_network, lookupDevice, en_protocol, record.timestamp, obj);
                    }
                }

                ResultStagingRecord.queue(sessionHolder.createHelper(ResultStagingRecord.class), Lists.newArrayList(en_network));

                final ZonedDateTime now         = TimeUtils.now();
                GatewayAssetRecord  rec_gateway = lock_gateway.get();
                rec_gateway.setLastUpdatedDate(now);

                sessionHolder.commit();
                return true;
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to commit records: %s", t);
                return false;
            }
        }

        private void commitRecord(SessionHolder sessionHolder,
                                  NetworkAssetRecord rec_network,
                                  Map<IpnDeviceDescriptor, GatewayDiscoveryEntity> lookupDevice,
                                  GatewayDiscoveryEntity en_protocol,
                                  ZonedDateTime timestamp,
                                  IpnObjectModel obj) throws
                                                      Exception
        {
            String deviceId = obj.extractId();

            IpnDeviceDescriptor desc = new IpnDeviceDescriptor();
            desc.name = deviceId;

            boolean deviceExists;

            GatewayDiscoveryEntity en_device = lookupDevice.get(desc);
            if (en_device != null)
            {
                deviceExists = true;
            }
            else
            {
                deviceExists = IpnDeviceRecord.findByIdentifier(sessionHolder.createHelper(IpnDeviceRecord.class), rec_network, deviceId) != null;

                en_device = newDiscoveryEntry(en_protocol, GatewayDiscoveryEntitySelector.Ipn_Device, desc);
                lookupDevice.put(desc, en_device);
            }

            double timestampEpochSeconds = TimeUtils.fromUtcTimeToTimestamp(timestamp);

            if (!deviceExists)
            {
                en_device.setTimestampEpoch(timestampEpochSeconds);
            }

            for (FieldModel fieldModel : obj.getDescriptors())
            {
                String fieldName = fieldModel.name;

                if (obj.shouldIncludeProperty(fieldName))
                {
                    GatewayDiscoveryEntity en_object = newDiscoveryEntry(en_device, GatewayDiscoveryEntitySelector.Ipn_Object, fieldName);

                    if (!deviceExists)
                    {
                        en_object.setTimestampEpoch(timestampEpochSeconds);
                        en_object.setContentsAsObject(ObjectMappers.SkipNulls, obj);
                    }

                    GatewayDiscoveryEntity en_objectSample = newDiscoveryEntry(en_object, GatewayDiscoveryEntitySelector.Ipn_ObjectSample, null);
                    en_objectSample.setTimestampEpoch(timestampEpochSeconds);
                    en_objectSample.setContentsAsObject(ObjectMappers.SkipNulls, obj);
                }
            }
        }
    }

    //--//

    private ServiceWorker m_worker;

    //--//

    public InstanceConfigurationForDigitalMatter()
    {
        m_worker = new ServiceWorker(LoggerInstance, "DigitalMatter", 0, 2000)
        {
            private ServerSocket m_socket;
            private final LinkedList<Socket> m_connections = new LinkedList<>();

            @Override
            protected void worker()
            {
                ServerSocket socket;

                try
                {
                    socket = new ServerSocket(TCP_PORT);
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Failed to open socket for Cadent, due to %s", t);
                    return;
                }

                m_socket = socket;

                int sleepBetweenFailures = 1;

                while (!socket.isClosed())
                {
                    try
                    {
                        Socket clientSocket = socket.accept();
                        clientSocket.setSoTimeout(60 * 1000);

                        synchronized (m_connections)
                        {
                            m_connections.add(clientSocket);
                        }

                        Executors.getDefaultLongRunningThreadPool()
                                 .queue(() ->
                                        {
                                            try
                                            {
                                                SocketSession socketSession = new SocketSession(clientSocket);

                                                socketSession.process();
                                            }
                                            finally
                                            {
                                                clientSocket.close();

                                                synchronized (m_connections)
                                                {
                                                    m_connections.remove(clientSocket);
                                                }
                                            }
                                        });
                    }
                    catch (Exception e)
                    {
                        if (m_socket == null)
                        {
                            // The manager has been stopped, exit.
                            return;
                        }

                        Executors.safeSleep(sleepBetweenFailures);

                        // Exponential backoff if something goes wrong.
                        sleepBetweenFailures = 2 * sleepBetweenFailures;
                        continue;
                    }

                    sleepBetweenFailures = 1;
                }
            }

            @Override
            protected void shutdown()
            {
                ServerSocket socket = m_socket;
                if (socket != null)
                {
                    m_socket = null;

                    try
                    {
                        socket.close();
                    }
                    catch (IOException e)
                    {
                        // Ignore failures
                    }
                }

                LinkedList<Socket> connections;

                synchronized (m_connections)
                {
                    connections = new LinkedList<>(m_connections);
                    m_connections.clear();
                }

                for (Socket connection : connections)
                {
                    try
                    {
                        connection.close();
                    }
                    catch (IOException e)
                    {
                        // Ignore failures
                    }
                }
            }
        };
    }

    //--//

    @Override
    public void start()
    {
        stop();

        m_worker.start();
    }

    @Override
    public void stop()
    {
        m_worker.stop();
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
        return true;
    }

    @Override
    public boolean shouldAutoConfig()
    {
        return true;
    }

    @Override
    public boolean fixupAutoConfig(ProtocolConfigForIpn cfg)
    {
        // No networks to configure.
        return false;
    }

    @Override
    public boolean shouldReportWhenUnreachable(DeviceRecord rec,
                                               ZonedDateTime unresponsiveSince)
    {
        return true;
    }

    @Override
    public boolean prepareSamplingConfiguration(SessionHolder sessionHolder,
                                                DeviceRecord rec_device,
                                                DeviceElementRecord rec_obj,
                                                boolean checkNonZeroValue,
                                                List<DeviceElementSampling> config)
    {
        DeviceElementSampling.add(config, DeviceElementRecord.DEFAULT_PROP_NAME, 86400);
        return true;
    }

    @Override
    public BackgroundActivityRecord scheduleClassification(SessionHolder sessionHolder,
                                                           NetworkAssetRecord rec_network) throws
                                                                                           Exception
    {
        return TaskForAutoNetworkClassification.scheduleTaskIfNotRunning(sessionHolder, rec_network);
    }

    @Override
    public void reclassify()
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
        RecordHelper<GatewayAssetRecord> helper_gateway  = sessionHolder.createHelper(GatewayAssetRecord.class);
        RecordHelper<NetworkAssetRecord> helper_network  = sessionHolder.createHelper(NetworkAssetRecord.class);
        RecordHelper<LocationRecord>     helper_location = sessionHolder.createHelper(LocationRecord.class);

        LocationRecord rec_location = new LocationRecord();
        rec_location.setPhysicalName(String.format("Tracker for %s", instanceId));
        rec_location.setType(LocationType.TRUCK);
        helper_location.persist(rec_location);

        //--//

        NetworkAssetRecord rec_network = new NetworkAssetRecord();
        rec_network.setPhysicalName(String.format("Network for %s", instanceId));
        rec_network.setLocation(rec_location);
        rec_network.setSamplingPeriod(1800);
        helper_network.persist(rec_network);

        //--//

        rec_gateway.setPhysicalName(String.format("Gateway for %s", instanceId));
        rec_gateway.setLocation(rec_location);
        rec_gateway.setState(AssetState.passive);
        helper_gateway.persist(rec_gateway);

        rec_gateway.getBoundNetworks()
                   .add(rec_network);

        return rec_network;
    }

    @Override
    protected void afterNetworkCreation(SessionHolder sessionHolder,
                                        GatewayAssetRecord rec_gateway,
                                        NetworkAssetRecord rec_network) throws
                                                                        Exception
    {
        rec_gateway.setWarningThreshold(2 * 24 * 60);
        rec_gateway.setAlertThreshold(3 * 24 * 60);
    }

    @Override
    protected boolean shouldIncludeObjectInClassification(IpnObjectModel contents)
    {
        // Include everything
        return true;
    }

    @Override
    protected boolean shouldBeSingletonInClassification(WellKnownPointClassOrCustom pointClass,
                                                        Set<String> pointTags)
    {
        // Include everything
        return false;
    }
}
