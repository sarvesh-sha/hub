/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.logic;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.client.gateway.model.prober.ProberBBMD;
import com.optio3.cloud.client.gateway.model.prober.ProberBroadcastDistributionTableEntry;
import com.optio3.cloud.client.gateway.model.prober.ProberForeignDeviceTableEntry;
import com.optio3.cloud.client.gateway.model.prober.ProberObject;
import com.optio3.cloud.client.gateway.model.prober.ProberObjectBACnet;
import com.optio3.cloud.client.gateway.model.prober.ProberOperation;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForBACnet;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForBACnetToAutoDiscovery;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForBACnetToDiscoverBBMDs;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForBACnetToDiscoverDevices;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForBACnetToDiscoverRouters;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForBACnetToReadBBMDs;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForBACnetToReadDevices;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForBACnetToReadObjectNames;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForBACnetToReadObjects;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForBACnetToScanMstpTrunkForDevices;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForBACnetToScanSubnetForDevices;
import com.optio3.cloud.gateway.GatewayApplication;
import com.optio3.concurrency.AsyncWaitMultiple;
import com.optio3.infra.NetworkHelper;
import com.optio3.interop.util.ClassBasedAsyncDispatcherWithContext;
import com.optio3.lang.Unsigned16;
import com.optio3.logging.Logger;
import com.optio3.protocol.bacnet.BACnetManager;
import com.optio3.protocol.bacnet.DeviceIdentity;
import com.optio3.protocol.bacnet.TimeoutSpec;
import com.optio3.protocol.bacnet.model.linklayer.BroadcastDistributionTableEntry;
import com.optio3.protocol.bacnet.model.linklayer.ForeignDeviceTableEntry;
import com.optio3.protocol.bacnet.transport.EthernetTransport;
import com.optio3.protocol.bacnet.transport.EthernetTransportBuilder;
import com.optio3.protocol.bacnet.transport.UdpTransport;
import com.optio3.protocol.bacnet.transport.UdpTransportBuilder;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.config.BACnetBBMD;
import com.optio3.protocol.model.transport.TransportAddress;
import com.optio3.protocol.model.transport.UdpTransportAddress;
import com.optio3.serialization.ObjectMappers;
import org.apache.commons.lang3.StringUtils;

public class ProberForBACnet
{
    public static final Logger LoggerInstance = new Logger(ProberForBACnet.class);

//    static
//    {
//        LoggerInstance.enable(Severity.Debug);
//        LoggerInstance.enable(Severity.DebugVerbose);
//    }

    private final GatewayApplication                   m_app;
    private final ProberOperationForBACnet             m_input;
    private       ProberOperationForBACnet.BaseResults m_output;

    private final ClassBasedAsyncDispatcherWithContext<ProberForBACnet> m_dispatcher;

    private BACnetManager     m_mgr;
    private EthernetTransport m_transportEthernet;
    private UdpTransport      m_transportUdp;

    public ProberForBACnet(GatewayApplication app,
                           ProberOperationForBACnet input)
    {

        m_app   = app;
        m_input = input;

        m_dispatcher = new ClassBasedAsyncDispatcherWithContext<>();
        m_dispatcher.add(ProberOperationForBACnetToAutoDiscovery.class, true, ProberForBACnet::executeAutoDiscovery);
        m_dispatcher.add(ProberOperationForBACnetToDiscoverBBMDs.class, true, ProberForBACnet::executeDiscoverBBMDs);
        m_dispatcher.add(ProberOperationForBACnetToDiscoverDevices.class, true, ProberForBACnet::executeDiscoverDevices);
        m_dispatcher.add(ProberOperationForBACnetToDiscoverRouters.class, true, ProberForBACnet::executeDiscoverRouters);
        m_dispatcher.add(ProberOperationForBACnetToReadBBMDs.class, true, ProberForBACnet::executeReadBBMDs);
        m_dispatcher.add(ProberOperationForBACnetToReadDevices.class, true, ProberForBACnet::executeReadDevices);
        m_dispatcher.add(ProberOperationForBACnetToReadObjectNames.class, true, ProberForBACnet::executeReadObjectNames);
        m_dispatcher.add(ProberOperationForBACnetToReadObjects.class, true, ProberForBACnet::executeReadObjects);
        m_dispatcher.add(ProberOperationForBACnetToScanMstpTrunkForDevices.class, true, ProberForBACnet::executeScanMstpTrunks);
        m_dispatcher.add(ProberOperationForBACnetToScanSubnetForDevices.class, true, ProberForBACnet::executeScanSubnet);
    }

    public CompletableFuture<ProberOperation.BaseResults> execute() throws
                                                                    Exception
    {
        try
        {
            if (StringUtils.isNotEmpty(m_input.networkInterface))
            {
                if (StringUtils.isNotEmpty(m_input.staticAddress))
                {
                    LoggerInstance.info("Using static address: %s (netmask %s)", m_input.staticAddress, m_input.cidr);

                    InetAddress                         ourAddress  = InetAddress.getByName(m_input.staticAddress);
                    NetworkHelper.InetAddressWithPrefix networkInfo = NetworkHelper.InetAddressWithPrefix.parse(m_input.cidr);

                    networkInfo = new NetworkHelper.InetAddressWithPrefix(ourAddress, networkInfo.prefixLength);

                    NetworkHelper.setNetworks(m_input.networkInterface, networkInfo);
                }
            }

            m_mgr = new BACnetManager();
            m_mgr.setDefaultTimeout(Duration.of(Math.max(250, m_input.defaultTimeout), ChronoUnit.MILLIS));
            m_mgr.setDefaultRetries(3);

            if (m_input.useEthernet)
            {
                LoggerInstance.info("Using Ethernet transport");

                EthernetTransportBuilder transportBuilder = EthernetTransportBuilder.newBuilder();

                if (StringUtils.isNotEmpty(m_input.networkInterface))
                {
                    transportBuilder.setDevice(m_input.networkInterface);
                }

                m_transportEthernet = transportBuilder.build();
                m_mgr.addTransport(m_transportEthernet);
            }

            int udpPort = m_input.udpPort;
            if (udpPort == 0)
            {
                udpPort = BACnetManager.c_DefaultPort;
            }

            if (m_input.useUDP)
            {
                LoggerInstance.info("Using UDP transport on port %d", udpPort);

                UdpTransportBuilder transportBuilder = UdpTransportBuilder.newBuilder();

                if (StringUtils.isNotEmpty(m_input.networkInterface))
                {
                    transportBuilder.setDevice(m_input.networkInterface);
                }

                transportBuilder.setNetworkPort(udpPort);

                m_transportUdp = transportBuilder.build();
                m_mgr.addTransport(m_transportUdp);
            }

            if (m_input.defaultTimeout > 0)
            {
                m_mgr.setDefaultTimeout(Duration.ofSeconds(m_input.defaultTimeout));
            }

            try
            {
                if (m_input.bbmds != null)
                {
                    for (BACnetBBMD bbmd : m_input.bbmds)
                    {
                        int bbmdPort = bbmd.networkPort;
                        if (bbmdPort == 0)
                        {
                            bbmdPort = udpPort;
                        }

                        InetSocketAddress bbmdAddress = new InetSocketAddress(bbmd.networkAddress, bbmdPort);
                        this.m_mgr.addBBMD(new UdpTransportAddress(bbmdAddress));

                        LoggerInstance.info("Using BBMD at %s", bbmdAddress);
                    }
                }

                this.m_mgr.start();

                await(m_dispatcher.dispatch(this, m_input));
            }
            finally
            {
                this.m_mgr.close();
                this.m_mgr = null;
            }
        }
        catch (Throwable t)
        {
            Class<? extends ProberOperationForBACnet> clz = m_input.getClass();
            LoggerInstance.error("Execution of %s failed with %s", clz.getName(), t);

            throw t;
        }

        return wrapAsync(m_output);
    }

    //--//

    private CompletableFuture<Void> executeDiscoverRouters(ProberOperationForBACnetToDiscoverRouters input) throws
                                                                                                            Exception
    {
        final Map<TransportAddress, Set<Integer>> results = Maps.newHashMap();

        await(m_mgr.scanForRouters(-1, (msg, ta) ->
        {
            synchronized (results)
            {
                if (msg.networks != null)
                {
                    for (Unsigned16 netId : msg.networks)
                    {
                        int network = netId.unboxUnsigned();

                        Set<Integer> set = results.computeIfAbsent(ta, (k) -> Sets.newHashSet());
                        set.add(network);
                    }
                }
            }
        }, getDefaultTimeout(), TimeUnit.SECONDS));

        ProberOperationForBACnetToDiscoverRouters.Results output = new ProberOperationForBACnetToDiscoverRouters.Results();

        LoggerInstance.info("Discover Routers: found %d entries", results.size());

        for (TransportAddress ta : results.keySet())
        {
            Set<Integer> networks = results.get(ta);

            LoggerInstance.info("Router: %s -> %s", ta, networks);

            BACnetDeviceDescriptor res = new BACnetDeviceDescriptor();
            res.transport = ta;

            ProberOperationForBACnetToDiscoverRouters.RouterNetwork router = new ProberOperationForBACnetToDiscoverRouters.RouterNetwork();
            router.device = res;
            router.networks.addAll(networks);

            output.discoveredRouters.add(router);
        }

        m_output = output;
        return wrapAsync(null);
    }

    //--//

    private CompletableFuture<Void> executeDiscoverBBMDs(ProberOperationForBACnetToDiscoverBBMDs input) throws
                                                                                                        Exception
    {
        ProberOperationForBACnetToDiscoverBBMDs.Results output  = new ProberOperationForBACnetToDiscoverBBMDs.Results();
        final Set<InetSocketAddress>                    results = Sets.newHashSet();

        NetworkHelper.InetAddressWithPrefix subnet = extractSubnet(input.targetSubnet);
        await(m_mgr.scanForBBMDs(subnet, (addr, table) ->
        {
            LoggerInstance.debug("  >> Found BBMD at %s", addr);
            synchronized (results)
            {
                results.add(addr);
            }
        }, input.broadcastRetries, getDefaultTimeout(), TimeUnit.SECONDS));

        LoggerInstance.info("Discover BBMDs: found %d entries", results.size());

        for (InetSocketAddress result : results)
        {
            LoggerInstance.info("  %s", result);

            BACnetDeviceDescriptor res = new BACnetDeviceDescriptor();
            res.transport = new UdpTransportAddress(result);
            output.discoveredDevices.add(res);
        }

        m_output = output;
        return wrapAsync(null);
    }

    //--//

    private CompletableFuture<Void> executeScanMstpTrunks(ProberOperationForBACnetToScanMstpTrunkForDevices input) throws
                                                                                                                   Exception
    {
        ProberOperationForBACnetToScanMstpTrunkForDevices.Results output           = new ProberOperationForBACnetToScanMstpTrunkForDevices.Results();
        TimeoutSpec                                               timeSpec         = TimeoutSpec.create(input.maxRetries, Duration.of(Math.min(100, input.defaultTimeout), ChronoUnit.MILLIS));
        Set<DeviceIdentity>                                       deviceIdentities = Sets.newHashSet();

        {
            AsyncWaitMultiple waiter = new AsyncWaitMultiple();

            for (ProberOperationForBACnetToScanMstpTrunkForDevices.Network target : input.targets)
            {
                waiter.add(scanMstpTrunk(deviceIdentities, timeSpec, target));

                await(sleep(20, TimeUnit.MILLISECONDS));
            }

            await(waiter.drain());
        }

        LoggerInstance.info("Scan MS/TP Trunk: found %d entries", deviceIdentities.size());
        convertToResult(deviceIdentities, output.discoveredDevices);

        m_output = output;
        return wrapAsync(null);
    }

    //--//

    private CompletableFuture<Void> executeScanSubnet(ProberOperationForBACnetToScanSubnetForDevices input) throws
                                                                                                            Exception
    {
        ProberOperationForBACnetToScanSubnetForDevices.Results output           = new ProberOperationForBACnetToScanSubnetForDevices.Results();
        TimeoutSpec                                            timeSpec         = TimeoutSpec.create(input.maxRetries, Duration.of(Math.min(100, input.defaultTimeout), ChronoUnit.MILLIS));
        AsyncWaitMultiple                                      waiter           = new AsyncWaitMultiple();
        Set<DeviceIdentity>                                    deviceIdentities = Sets.newHashSet();

        //
        // Sweep each address.
        //
        NetworkHelper.InetAddressWithPrefix subnet = extractSubnet(input.targetSubnet);
        for (int offset = 1; offset < subnet.getSize(); offset++)
        {
            InetAddress address = subnet.generateAddress(offset);

            waiter.add(checkTarget(deviceIdentities, address, timeSpec));

            await(sleep(20, TimeUnit.MILLISECONDS));
        }

        await(waiter.drain());

        LoggerInstance.info("Subnet Scan: found %d entries", deviceIdentities.size());
        convertToResult(deviceIdentities, output.discoveredDevices);

        m_output = output;
        return wrapAsync(null);
    }

    //--//

    private CompletableFuture<Void> executeDiscoverDevices(ProberOperationForBACnetToDiscoverDevices input) throws
                                                                                                            Exception
    {
        ProberOperationForBACnetToDiscoverDevices.Results output = new ProberOperationForBACnetToDiscoverDevices.Results();

        Set<DeviceIdentity> deviceIdentities = await(m_mgr.scanForDevices(LoggerInstance, input.broadcastRetries, input.limitScan, input.sweepMSTP, input.includeNetworksFromRouters, null, (msg, di) ->
        {
            LoggerInstance.debug("  >> Got IAm from %s at %s", msg.i_am_device_identifier, di.getTransportAddress());
        }));

        LoggerInstance.info("Discovery Devices: found %d entries:", deviceIdentities.size());
        convertToResult(deviceIdentities, output.discoveredDevices);

        m_output = output;
        return wrapAsync(null);
    }

    //--//

    private CompletableFuture<Void> executeAutoDiscovery(ProberOperationForBACnetToAutoDiscovery input) throws
                                                                                                        Exception
    {
        ProberOperationForBACnetToAutoDiscovery.Results output   = new ProberOperationForBACnetToAutoDiscovery.Results();
        TimeoutSpec                                     timeSpec = TimeoutSpec.create(input.maxRetries, Duration.of(Math.min(100, input.defaultTimeout), ChronoUnit.MILLIS));

        final List<BACnetDeviceDescriptor> bbmds            = Lists.newArrayList();
        final Set<DeviceIdentity>          deviceIdentities = Sets.newHashSet();

        //
        // Step 1) Discover BBMDs
        //
        {
            final Set<InetSocketAddress> results = Sets.newHashSet();

            NetworkHelper.InetAddressWithPrefix subnet = extractSubnet(input.targetSubnet);
            await(m_mgr.scanForBBMDs(subnet, (addr, table) ->
            {
                LoggerInstance.debug("  >> Found BBMD at %s", addr);
                synchronized (results)
                {
                    results.add(addr);
                }
            }, input.maxRetries, getDefaultTimeout(), TimeUnit.SECONDS));

            LoggerInstance.info("Discover BBMDs: found %d entries", results.size());

            for (InetSocketAddress result : results)
            {
                LoggerInstance.info("  %s", result);

                BACnetDeviceDescriptor res = new BACnetDeviceDescriptor();
                res.transport = new UdpTransportAddress(result);
                bbmds.add(res);
            }
        }

        //
        // Step 2) Register BBMDs
        //
        {
            for (BACnetDeviceDescriptor bbmd : bbmds)
            {
                m_mgr.addBBMD(bbmd.transport);
            }
        }

        //
        // Step 3) Discover Devices with broadcast.
        //
        {
            Set<DeviceIdentity> discoveredDevices = await(m_mgr.scanForDevices(LoggerInstance, input.maxRetries, input.limitScan, true, true, null, (msg, di) ->
            {
                LoggerInstance.debug("  >> Got IAm from %s at %s", msg.i_am_device_identifier, di.getTransportAddress());
            }));

            LoggerInstance.info("Discovery Devices with broadcast: found %d entries:", discoveredDevices.size());

            deviceIdentities.addAll(discoveredDevices);
        }

        //
        // Step 5) Discover Devices through subnet scan
        //
        {
            AsyncWaitMultiple waiter = new AsyncWaitMultiple();

            //
            // Sweep each address.
            //
            NetworkHelper.InetAddressWithPrefix subnet = extractSubnet(input.targetSubnet);
            for (int offset = 1; offset < subnet.getSize(); offset++)
            {
                InetAddress address = subnet.generateAddress(offset);

                waiter.add(checkTarget(deviceIdentities, address, timeSpec));

                await(sleep(20, TimeUnit.MILLISECONDS));
            }

            await(waiter.drain());
        }

        LoggerInstance.info("Auto Scan: found %d entries", deviceIdentities.size());
        convertToResult(deviceIdentities, output.discoveredDevices);

        m_output = output;
        return wrapAsync(null);
    }

    //--//

    private CompletableFuture<Void> executeReadBBMDs(ProberOperationForBACnetToReadBBMDs input) throws
                                                                                                Exception
    {
        ProberOperationForBACnetToReadBBMDs.Results output = new ProberOperationForBACnetToReadBBMDs.Results();
        AsyncWaitMultiple                           waiter = new AsyncWaitMultiple();

        for (BACnetBBMD bbmd : input.bbmds)
        {
            ProberBBMD bbmdResponse = new ProberBBMD();
            bbmdResponse.descriptor = bbmd;
            output.bbmds.add(bbmdResponse);
            waiter.add(readBBMD(bbmdResponse, bbmd, input.maxRetries));
        }

        await(waiter.drain());

        for (ProberBBMD bbmd : output.bbmds)
        {
            LoggerInstance.info("Read BBMD %s:%s - found %d foreign devices and %d broadcast distributions.",
                                bbmd.descriptor.networkAddress,
                                bbmd.descriptor.networkPort,
                                bbmd.fdt.size(),
                                bbmd.bdt.size());
        }

        m_output = output;
        return wrapAsync(null);
    }

    private CompletableFuture<Void> readBBMD(ProberBBMD output,
                                             BACnetBBMD bbmd,
                                             int retries) throws
                                                          Exception
    {
        InetSocketAddress bbmdAddress = new InetSocketAddress(bbmd.networkAddress, bbmd.networkPort);

        List<ForeignDeviceTableEntry> fdt = await(m_transportUdp.readForeignDeviceTable(bbmdAddress, retries, getDefaultTimeout(), TimeUnit.SECONDS));
        if (fdt != null)
        {
            for (ForeignDeviceTableEntry entry : fdt)
            {
                ProberForeignDeviceTableEntry outputEntry = new ProberForeignDeviceTableEntry();
                InetSocketAddress             address     = UdpTransportAddress.getSocketAddress(entry.device_address, entry.port);
                outputEntry.address             = address.toString()
                                                         .substring(1);
                outputEntry.timeToLive          = entry.time_to_live;
                outputEntry.remainingTimeToLive = entry.remaining_time_to_live;
                output.fdt.add(outputEntry);
            }
        }

        List<BroadcastDistributionTableEntry> bdt = await(m_transportUdp.readBroadcastDistributionTable(bbmdAddress, retries, getDefaultTimeout(), TimeUnit.SECONDS));
        if (bdt != null)
        {
            for (BroadcastDistributionTableEntry entry : bdt)
            {
                ProberBroadcastDistributionTableEntry outputEntry = new ProberBroadcastDistributionTableEntry();
                InetSocketAddress                     address     = UdpTransportAddress.getSocketAddress(entry.device_address, entry.port);
                InetSocketAddress                     mask        = UdpTransportAddress.getSocketAddress(entry.mask, Unsigned16.box(0));
                outputEntry.address = address.toString()
                                             .substring(1);
                outputEntry.mask    = mask.getHostString();
                output.bdt.add(outputEntry);
            }
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> executeReadDevices(ProberOperationForBACnetToReadDevices input) throws
                                                                                                    Exception
    {
        ProberOperationForBACnetToReadDevices.Results output = new ProberOperationForBACnetToReadDevices.Results();
        AsyncWaitMultiple                             waiter = new AsyncWaitMultiple();

        for (BACnetDeviceDescriptor device : input.targetDevices)
        {
            waiter.add(readDevice(output.objects, device));
        }

        await(waiter.drain());

        LoggerInstance.info("Read Devices: found %d objects:", output.objects.size());

        output.objects.sort(ProberObjectBACnet::compareTo);

        m_output = output;
        return wrapAsync(null);
    }

    private CompletableFuture<Void> readDevice(List<ProberObjectBACnet> objects,
                                               BACnetDeviceDescriptor device) throws
                                                                              Exception
    {
        DeviceIdentity di = ensureDeviceIdentity(device);
        if (di == null)
        {
            return wrapAsync(null);
        }

        return readObject(objects, di, di.getDeviceId());
    }

    //--//

    private CompletableFuture<Void> executeReadObjectNames(ProberOperationForBACnetToReadObjectNames input) throws
                                                                                                            Exception
    {
        ProberOperationForBACnetToReadObjectNames.Results output = new ProberOperationForBACnetToReadObjectNames.Results();
        AsyncWaitMultiple                                 waiter = new AsyncWaitMultiple();

        for (BACnetDeviceDescriptor device : input.targetDevices)
        {
            waiter.add(readObjectName(output.objects, device));
        }

        await(waiter.drain());

        LoggerInstance.info("Read Devices: found %d objects:", output.objects.size());

        output.objects.sort(ProberObjectBACnet::compareTo);

        m_output = output;
        return wrapAsync(null);
    }

    private CompletableFuture<Void> readObjectName(List<ProberObjectBACnet> objects,
                                                   BACnetDeviceDescriptor device) throws
                                                                                  Exception
    {
        DeviceIdentity di = ensureDeviceIdentity(device);
        if (di != null)
        {
            TreeMap<BACnetObjectIdentifier, DeviceIdentity.ObjectDescriptor> map = await(di.getObjects());

            DeviceIdentity.BatchReader reader = di.createBatchReader();

            map.forEach((objId, objDesc) ->
                        {
                            reader.add(objDesc, BACnetPropertyIdentifier.object_name.forRequest());
                        });

            DeviceIdentity.BatchReaderResult output = await(reader.execute(m_mgr.getDefaultTimeout()));

            map.forEach((objId, objDesc) ->
                        {
                            ProberObjectBACnet obj = new ProberObjectBACnet();
                            obj.device   = di.extractIdentifier();
                            obj.objectId = objId.toJsonValue();
                            DeviceIdentity.ObjectReadResult res = output.values.get(objDesc);
                            if (res != null)
                            {
                                obj.properties = ObjectMappers.SkipNulls.valueToTree(res.state);
                            }

                            LoggerInstance.info("  >> Read Object Name %s on %s", objId, di);

                            synchronized (objects)
                            {
                                objects.add(obj);
                            }
                        });
        }

        return wrapAsync(null);
    }

    //--//

    private CompletableFuture<Void> executeReadObjects(ProberOperationForBACnetToReadObjects input) throws
                                                                                                    Exception
    {
        ProberOperationForBACnetToReadObjects.Results output = new ProberOperationForBACnetToReadObjects.Results();
        AsyncWaitMultiple                             waiter = new AsyncWaitMultiple();

        for (ProberObject object : input.targetObjects)
        {
            DeviceIdentity di = ensureDeviceIdentity((BACnetDeviceDescriptor) object.device);
            if (di != null)
            {
                BACnetObjectIdentifier objectId = new BACnetObjectIdentifier(object.objectId);

                waiter.add(readObject(output.objects, di, objectId));
            }
        }

        await(waiter.drain());

        LoggerInstance.info("Read Objects: processed %d objects:", output.objects.size());

        output.objects.sort(ProberObjectBACnet::compareTo);

        m_output = output;
        return wrapAsync(null);
    }

    private CompletableFuture<Void> readObject(List<ProberObjectBACnet> objects,
                                               DeviceIdentity di,
                                               BACnetObjectIdentifier objId) throws
                                                                             Exception
    {
        DeviceIdentity.ObjectDescriptor objDesc = await(di.getObjectDescriptor(objId));

        Set<BACnetPropertyIdentifierOrUnknown> set = await(objDesc.getProperties());

        DeviceIdentity.BatchReader reader = di.createBatchReader();

        for (BACnetPropertyIdentifierOrUnknown prop : set)
        {
            reader.add(objDesc, prop);
        }

        DeviceIdentity.BatchReaderResult output = await(reader.execute(m_mgr.getDefaultTimeout()));

        ProberObjectBACnet obj = new ProberObjectBACnet();
        obj.device   = di.extractIdentifier();
        obj.objectId = objId.toJsonValue();
        DeviceIdentity.ObjectReadResult res = output.values.get(objDesc);
        if (res != null)
        {
            obj.properties = ObjectMappers.SkipNulls.valueToTree(res.state);
        }

        LoggerInstance.info("  >> Read Object %s on %s", objId, di);

        synchronized (objects)
        {
            objects.add(obj);
        }

        return wrapAsync(null);
    }

    //--//

    private int getDefaultTimeout()
    {
        return m_input.defaultTimeout > 0 ? m_input.defaultTimeout : 5;
    }

    private NetworkHelper.InetAddressWithPrefix extractSubnet(String targetSubnet) throws
                                                                                   UnknownHostException
    {
        NetworkHelper.InetAddressWithPrefix subnet;
        if (targetSubnet != null)
        {
            subnet = NetworkHelper.InetAddressWithPrefix.parse(targetSubnet);
        }
        else
        {
            int mask = NetworkHelper.addressToIpv4(m_transportUdp.getMaskAddress());
            mask = ~mask + 1;
            int prefix = 32;
            while (mask != 0)
            {
                prefix--;
                mask >>>= 1;
            }

            subnet = new NetworkHelper.InetAddressWithPrefix(m_transportUdp.getLocalAddressWithPort()
                                                                           .getAddress(), prefix);
        }
        return subnet;
    }

    private DeviceIdentity ensureDeviceIdentity(BACnetDeviceDescriptor device)
    {
        return m_mgr.registerDevice(device.transport, device.bacnetAddress, device.address, device.segmentation, device.maxAdpu);
    }

    private static void convertToResult(Set<DeviceIdentity> inputs,
                                        List<BACnetDeviceDescriptor> outputs)
    {
        List<DeviceIdentity> sortedInputs = Lists.newArrayList(inputs);
        sortedInputs.sort(DeviceIdentity::compareTo);

        for (DeviceIdentity di : sortedInputs)
        {
            LoggerInstance.info("   %s", di);

            outputs.add(di.extractIdentifier());
        }
    }

    private CompletableFuture<Void> checkTarget(Set<DeviceIdentity> deviceIdentities,
                                                InetAddress address,
                                                TimeoutSpec timeSpec) throws
                                                                      Exception
    {
        UdpTransportAddress ta = new UdpTransportAddress(address.getHostAddress(),
                                                         m_transportUdp.getLocalAddressWithPort()
                                                                       .getPort());

        DeviceIdentity di = await(m_mgr.probeTarget(LoggerInstance, ta, null, 0, timeSpec));
        if (di != null)
        {
            LoggerInstance.info("Subnet scan found %s", di);

            synchronized (deviceIdentities)
            {
                deviceIdentities.add(di);
            }
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> scanMstpTrunk(Set<DeviceIdentity> deviceIdentities,
                                                  TimeoutSpec timeSpec,
                                                  ProberOperationForBACnetToScanMstpTrunkForDevices.Network target) throws
                                                                                                                    Exception
    {
        Set<DeviceIdentity> set = await(m_mgr.scanForDevicesInMstpTruck(LoggerInstance, target.transport, target.networkNumber, timeSpec));

        synchronized (deviceIdentities)
        {
            deviceIdentities.addAll(set);
        }

        return wrapAsync(null);
    }
}
