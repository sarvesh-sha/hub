/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

package com.optio3.infra;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.interop.mediaaccess.EthernetAccess;
import com.optio3.logging.Logger;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.Exceptions;
import com.optio3.util.ProcessUtils;
import com.optio3.util.function.ConsumerWithException;
import com.sun.jna.Platform;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public final class NetworkHelper
{
    public static final Logger LoggerInstance = new Logger(NetworkHelper.class);

    private final static NonRoutableRange[] s_nonRoutableRanges;

    static
    {
        //
        // The Internet Engineering Task Force (IETF) has directed the Internet Assigned Numbers Authority (IANA) to reserve the following IPv4 address ranges for private networks, as published in RFC 1918:[1]
        //
        // RFC1918 name  IP address range               number of addresses largest CIDR block (subnet mask) host id size    mask bits   classful description[Note 1]
        // 24-bit block  10.0.0.0    - 10.255.255.255            16,777,216 10.0.0.0/8         (255.0.0.0)   24 bits         8 bits      single class A network
        // 20-bit block  172.16.0.0  - 172.31.255.255             1,048,576 172.16.0.0/12      (255.240.0.0) 20 bits         12 bits     16 contiguous class B networks
        // 16-bit block  192.168.0.0 - 192.168.255.255               65,536 192.168.0.0/16     (255.255.0.0) 16 bits         16 bits     256 contiguous class C networks
        //
        s_nonRoutableRanges = new NonRoutableRange[] { new NonRoutableRange(0xFF_00_0000, 0x0A_00_0000),
                                                       new NonRoutableRange(0xFFF0_0000, 0xAC_10_0000),
                                                       new NonRoutableRange(0xFFFF_0000, 0xC0_A8_0000) };
    }

    //--//

    public static final class IpHeader
    {
        @SerializationTag(number = 0, bitOffset = 4, width = 4, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int version;

        @SerializationTag(number = 0, bitOffset = 0, width = 4, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int internetHeaderLength;

        @SerializationTag(number = 1, bitOffset = 2, width = 6, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int differentiatedServicesCodePoint;

        @SerializationTag(number = 1, bitOffset = 0, width = 2, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int explicitCongestionNotification;

        @SerializationTag(number = 2, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int totalLength;

        @SerializationTag(number = 4, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int identification;

        @SerializationTag(number = 6, bitOffset = 8 + 5, width = 3, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int flags;

        @SerializationTag(number = 6, bitOffset = 0, width = 8 + 5, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int fragmentOffset;

        @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int timeToLive;

        @SerializationTag(number = 9, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int protocol;

        @SerializationTag(number = 10, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int headerChecksum;

        @SerializationTag(number = 12, width = 32, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int sourceIPAddress;

        @SerializationTag(number = 16, width = 32, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int destinationIPAddress;

        //--//

        public InetAddress getSourceAddress()
        {
            return ipv4ToAddress(sourceIPAddress);
        }

        public InetAddress getDestinationAddress()
        {
            return ipv4ToAddress(destinationIPAddress);
        }
    }

    public static final class PerformanceCounters
    {
        public long rxBytes;
        public long rxPackets;
        public long rxErrors;
        public long rxDrops;
        public long rxFifo;
        public long rxFrame;
        public long rxCompressed;
        public long rxMulticast;

        public long txBytes;
        public long txPackets;
        public long txErrors;
        public long txDrops;
        public long txFifo;
        public long txCollisions;
        public long txCarrier;
        public long txCompressed;
    }

    public static class InterfaceAddressDetails
    {
        public final NetworkInterface      networkInterface;
        public final InetAddress           localAddress;
        public final InetAddress           maskAddress;
        public final InetAddress           broadcastAddress;
        public final InetAddressWithPrefix cidr;

        public InterfaceAddressDetails(NetworkInterface itf,
                                       InterfaceAddress ia)
        {
            networkInterface = itf;

            localAddress = ia.getAddress();
            LoggerInstance.debug("InterfaceAddressDetails: localAddress: %s", localAddress);

            // Get address as 32-bit integer.
            int ipv4 = addressToIpv4(localAddress);

            // Create broadcast mask
            int prefix = ia.getNetworkPrefixLength();
            int mask   = generateMask(prefix);

            broadcastAddress = ipv4ToAddress(ipv4 | mask);
            maskAddress      = ipv4ToAddress(~mask);

            LoggerInstance.debug("InterfaceAddressDetails: broadcastAddress: %s", broadcastAddress);
            LoggerInstance.debug("InterfaceAddressDetails: maskAddress: %s", maskAddress);

            cidr = new InetAddressWithPrefix(localAddress, prefix);
        }

        public boolean sameInterface(String itf)
        {
            return networkInterface.getName()
                                   .equals(itf);
        }
    }

    public static class InetAddressWithPrefix
    {
        public final InetAddress address;
        public final int         prefixLength;

        private final int m_ipv4Prefix;
        private final int m_ipv4Mask;

        public InetAddressWithPrefix(InetAddress address,
                                     int prefixLength)
        {
            this.address      = address;
            this.prefixLength = prefixLength;

            m_ipv4Mask   = generateMask(prefixLength);
            m_ipv4Prefix = addressToIpv4(address) & ~m_ipv4Mask;
        }

        //--//

        public static InetAddressWithPrefix parse(String input) throws
                                                                UnknownHostException
        {
            InetAddress address;
            int         prefixLength;

            int pos = input.indexOf('/');
            if (pos >= 0)
            {
                address      = InetAddress.getByName(input.substring(0, pos));
                prefixLength = Integer.parseInt(input.substring(pos + 1));
            }
            else
            {
                address      = InetAddress.getByName(input);
                prefixLength = 24;
            }

            return new InetAddressWithPrefix(address, prefixLength);
        }

        @Override
        public String toString()
        {
            return address.getHostAddress() + "/" + prefixLength;
        }

        //--//

        public int getIpv4Prefix()
        {
            return m_ipv4Prefix;
        }

        public int getIpv4Mask()
        {
            return m_ipv4Mask;
        }

        public int getSize()
        {
            return 1 << (32 - prefixLength);
        }

        public InetAddress generateAddress(int offset)
        {
            return ipv4ToAddress(m_ipv4Prefix | (offset & m_ipv4Mask));
        }

        public boolean isInSubnet(InetAddress target)
        {
            int targetIpv4   = addressToIpv4(target);
            int targetSubnet = targetIpv4 & (~m_ipv4Mask);

            return m_ipv4Prefix == targetSubnet;
        }
    }

    //--//

    public static class NonRoutableRange
    {
        private final int m_mask;
        private final int m_range;

        public NonRoutableRange(int mask,
                                int range)
        {
            m_mask  = mask;
            m_range = range;
        }

        public boolean isInRange(int addressIPv4)
        {
            return (addressIPv4 & m_mask) == m_range;
        }
    }

    public static NonRoutableRange isNonRoutableRange(InetAddress address)
    {
        return isNonRoutableRange(addressToIpv4(address));
    }

    public static NonRoutableRange isNonRoutableRange(int addressIPv4)
    {
        for (NonRoutableRange range : s_nonRoutableRanges)
        {
            if (range.isInRange(addressIPv4))
            {
                return range;
            }
        }

        return null;
    }

    //--//

    public static InetSocketAddress parseInetAddressWithPort(String input,
                                                             int defaultPort)
    {
        String[] parts = input.split(":");
        if (parts.length == 2)
        {
            defaultPort = Integer.parseInt(parts[1]);
        }

        return new InetSocketAddress(parts[0], defaultPort);
    }

    public static InetAddress ipv4ToAddress(int ipv4)
    {
        try
        {
            try (OutputBuffer ob = new OutputBuffer())
            {
                ob.emit4Bytes(ipv4);
                return InetAddress.getByAddress(ob.toByteArray());
            }
        }
        catch (UnknownHostException e)
        {
            // This should never happen...
            return null;
        }
    }

    public static int addressToIpv4(InetAddress addr)
    {
        try (InputBuffer ib = InputBuffer.createFrom(addr.getAddress()))
        {
            return ib.read4BytesSigned();
        }
    }

    //--//

    public static void decodeRawPackets(EthernetAccess pcap,
                                        int maxPackets,
                                        ConsumerWithException<IpHeader> callback)
    {
        pcap.loop(maxPackets, (payload) ->
        {
            try
            {
                try (InputBuffer ib = InputBuffer.createFrom(payload))
                {
                    if (pcap.isEthernetHeader())
                    {
                        ib.setPosition(14);

                        IpHeader header = new IpHeader();
                        SerializationHelper.read(ib, header);
                        callback.accept(header);
                    }
                    else if (pcap.isLinuxHeader())
                    {
                        ib.setPosition(16);

                        IpHeader header = new IpHeader();
                        SerializationHelper.read(ib, header);
                        callback.accept(header);
                    }
                    else if (pcap.isLoopback())
                    {
                        ib.setPosition(4);

                        IpHeader header = new IpHeader();
                        SerializationHelper.read(ib, header);
                        callback.accept(header);
                    }
                }
            }
            catch (Exception e)
            {
                // Ignore failures
            }
        });
    }

    public static Map<String, PerformanceCounters> collectPerformanceCounters() throws
                                                                                IOException
    {
        Map<String, PerformanceCounters> res = Maps.newHashMap();

        if (Platform.isLinux())
        {
            boolean skipHeader = true;

            for (String line : FileUtils.readLines(new File("/proc/net/dev"), Charset.defaultCharset()))
            {
                if (skipHeader)
                {
                    if (!line.contains(":"))
                    {
                        continue;
                    }

                    skipHeader = false;
                }

                String[] parts = StringUtils.split(line, ':');
                if (parts.length == 2)
                {
                    String name   = parts[0].trim();
                    String values = parts[1];

                    parts = StringUtils.split(values, ' ');

                    PerformanceCounters pc = new PerformanceCounters();
                    res.put(name, pc);

                    //
                    // Inter-|Receive                                                   |Transmit
                    // face  |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
                    //
                    int offset = 0;

                    pc.rxBytes      = parseLong(parts, offset++);
                    pc.rxPackets    = parseLong(parts, offset++);
                    pc.rxErrors     = parseLong(parts, offset++);
                    pc.rxDrops      = parseLong(parts, offset++);
                    pc.rxFifo       = parseLong(parts, offset++);
                    pc.rxFrame      = parseLong(parts, offset++);
                    pc.rxCompressed = parseLong(parts, offset++);
                    pc.rxMulticast  = parseLong(parts, offset++);

                    pc.txBytes      = parseLong(parts, offset++);
                    pc.txPackets    = parseLong(parts, offset++);
                    pc.txErrors     = parseLong(parts, offset++);
                    pc.txDrops      = parseLong(parts, offset++);
                    pc.txFifo       = parseLong(parts, offset++);
                    pc.txCollisions = parseLong(parts, offset++);
                    pc.txCarrier    = parseLong(parts, offset++);
                    pc.txCompressed = parseLong(parts, offset++);
                }
            }
        }
        else if (Platform.isMac())
        {
            for (String line : ProcessUtils.execAndCaptureOutputAsLines(null, 10, TimeUnit.SECONDS, "netstat", "-b", "-i"))
            {
                if (line.contains("<Link#"))
                {
                    String[] parts = StringUtils.split(line, ' ');
                    String   name  = parts[0].trim();

                    PerformanceCounters pc = new PerformanceCounters();
                    res.put(name, pc);

                    int offset = 4;

                    pc.rxPackets = parseLong(parts, offset++);
                    pc.rxErrors  = parseLong(parts, offset++);
                    pc.rxBytes   = parseLong(parts, offset++);

                    pc.txPackets    = parseLong(parts, offset++);
                    pc.txErrors     = parseLong(parts, offset++);
                    pc.txBytes      = parseLong(parts, offset++);
                    pc.txCollisions = parseLong(parts, offset++);
                }
            }
        }

        return res;
    }

    private static long parseLong(String[] parts,
                                  int offset)
    {
        return offset < parts.length ? Long.parseLong(parts[offset]) : 0;
    }

    //--//

    public static List<NetworkInterface> listNetworkInterfaces(boolean includeDownInterfaces,
                                                               boolean includeVirtual,
                                                               boolean includeLoopback) throws
                                                                                        IOException

    {
        List<NetworkInterface> list = Lists.newArrayList();

        for (Enumeration<NetworkInterface> itfs = NetworkInterface.getNetworkInterfaces(); itfs.hasMoreElements(); )
        {
            NetworkInterface itf = itfs.nextElement();
            LoggerInstance.debug("listNetworkInterfaces: Interface %s - %s", itf.getName(), itf.getDisplayName());
            if (itf.isLoopback() && !includeLoopback)
            {
                LoggerInstance.debug("listNetworkInterfaces: Loopback, skipping...");
                continue;
            }

            if (!itf.isUp() && !includeDownInterfaces)
            {
                LoggerInstance.debug("listNetworkInterfaces: Down, skipping...");
                continue;
            }

            if (itf.isVirtual() && !includeVirtual)
            {
                LoggerInstance.debug("listNetworkInterfaces: Virtual, skipping...");
                continue;
            }

            list.add(itf);
        }

        return list;
    }

    public static List<InterfaceAddressDetails> listNetworkAddresses(boolean includeDownInterfaces,
                                                                     boolean includeVirtual,
                                                                     boolean includeLoopback,
                                                                     boolean includeWithoutBroadcast,
                                                                     BiFunction<NetworkInterface, InterfaceAddress, Boolean> filter) throws
                                                                                                                                     IOException
    {
        List<InterfaceAddressDetails> list = Lists.newArrayList();

        for (NetworkInterface itf : listNetworkInterfaces(includeDownInterfaces, includeVirtual, includeLoopback))
        {
            for (InterfaceAddress ia : itf.getInterfaceAddresses())
            {
                if (ia.getBroadcast() == null && !includeWithoutBroadcast)
                {
                    continue;
                }

                if (filter == null || filter.apply(itf, ia))
                {
                    list.add(new InterfaceAddressDetails(itf, ia));
                }
            }
        }

        return list;
    }

    public static void setNetworks(String itf,
                                   InetAddressWithPrefix... addresses) throws
                                                                       IOException
    {
        //
        // Make sure we don't try to reprogram the interface with connection to the Internet.
        //
        Map<String, InetAddress> itfToGateway = extractGateways();
        if (itfToGateway.containsKey(itf))
        {
            throw Exceptions.newIllegalArgumentException("Can't add network through interface '%s' because it's the interface with the default Gateway", itf);
        }

        List<InterfaceAddressDetails> existingAddresses = listNetworkAddresses(true, false, false, false, null);

        //
        // Are any of the new addresses conflicting with those on different interfaces?
        //
        for (InetAddressWithPrefix address : addresses)
        {
            int ipv4 = address.getIpv4Prefix();
            int mask = address.getIpv4Mask();

            for (InterfaceAddressDetails other : existingAddresses)
            {
                int ipv4Broadcast = addressToIpv4(other.broadcastAddress);
                if (ipv4Broadcast == (ipv4 | mask))
                {
                    if (!other.sameInterface(itf))
                    {
                        throw Exceptions.newIllegalArgumentException("There's already a network with the same broadcast: '%s' on '%s'", other.broadcastAddress, other.networkInterface.getName());
                    }
                }
            }
        }

        //
        // Pre-check passed.
        //
        // First, remove all old addresses.
        //
        for (InterfaceAddressDetails other : existingAddresses)
        {
            if (other.sameInterface(itf))
            {
                exec("/sbin/ifconfig", itf, "del", other.localAddress);
            }
        }

        //
        // Zero out the address on the interface.
        //
        exec("/sbin/ifconfig", itf, "0.0.0.0");

        //
        // Add all the interfaces.
        //
        boolean first = true;

        for (InetAddressWithPrefix address : addresses)
        {
            int ipv4 = addressToIpv4(address.address);
            int mask = generateMask(address.prefixLength);

            InetAddress broadcastAddress = ipv4ToAddress(ipv4 | mask);
            InetAddress maskAddress      = ipv4ToAddress(~mask);

            if (first)
            {
                first = false;

                exec("/sbin/ifconfig", itf, address.address, "netmask", maskAddress, "broadcast", broadcastAddress);
            }
            else
            {
                exec("/sbin/ifconfig", itf, "add", address.address, "netmask", maskAddress, "broadcast", broadcastAddress);
            }
        }

        exec("/sbin/ifconfig", itf, "up");
    }

    public static Map<String, InetAddress> extractGateways() throws
                                                             IOException
    {
        Map<String, InetAddress> lookup = Maps.newHashMap();

        if (Platform.isLinux())
        {
            // Iface   Destination Gateway     Flags   RefCnt  Use Metric  Mask        MTU Window  IRTT
            final int indexItf         = 0;
            final int indexDestination = 1;
            final int indexGateway     = 2;
            final int indexFlags       = 3;
            final int indexRefCnt      = 4;
            final int indexUse         = 5;
            final int indexMetric      = 6;
            final int indexMask        = 7;
            final int indexMTU         = 8;
            final int indexwindow      = 9;
            final int indexIRTT        = 10;

            final int flagUp      = 0x1;
            final int flagGateway = 0x2;

            boolean first = true;

            for (String line : FileUtils.readLines(new File("/proc/net/route"), Charset.defaultCharset()))
            {
                if (first)
                {
                    first = false;
                    continue;
                }

                String[] parts = line.split("\t");
                if (parts.length > indexMask)
                {
                    String itf   = parts[indexItf];
                    int    flags = parseHex(parts[indexFlags]);
                    if ((flags & flagUp) == 0)
                    {
                        continue;
                    }

                    if ((flags & flagGateway) != 0)
                    {
                        int mask = parseHexBigEndian(parts[indexMask]);
                        if (mask != 0)
                        {
                            continue;
                        }

                        int gateway = parseHexBigEndian(parts[indexGateway]);
                        if (gateway == 0)
                        {
                            continue;
                        }

                        lookup.put(itf, ipv4ToAddress(gateway));
                    }
                    else
                    {
                        int destination = parseHexBigEndian(parts[indexDestination]);
                        if (destination != 0)
                        {
                            continue;
                        }

                        lookup.put(itf, ipv4ToAddress(0));
                    }
                }
            }
        }
        else if (Platform.isMac())
        {
            String gateway = null;
            String itf     = null;

            for (String line : ProcessUtils.execAndCaptureOutputAsLines(null, 10, TimeUnit.SECONDS, "route", "-n", "get", "repo.dev.optio3.io"))
            {
                String[] parts = line.split(":");
                if (parts.length == 2)
                {
                    String key = parts[0].trim();
                    String val = parts[1].trim();

                    switch (key)
                    {
                        case "gateway":
                            gateway = val;
                            break;

                        case "interface":
                            itf = val;
                            break;
                    }
                }

                if (gateway != null && itf != null)
                {
                    lookup.put(itf, InetAddress.getByName(gateway));

                    gateway = null;
                    itf     = null;
                }
            }
        }

        return lookup;
    }

    public static InetAddress findGatewayByInterfaces(String... itfs)
    {
        try
        {
            Map<String, InetAddress> itfToGateway = extractGateways();

            for (String itf : itfs)
            {
                InetAddress addr = itfToGateway.get(itf);
                if (addr != null)
                {
                    return addr;
                }
            }
        }
        catch (Throwable t)
        {
            // Ignore failures.
        }

        return null;
    }

    public static boolean isCellularConnection()
    {
        if (Platform.isARM())
        {
            return findGatewayByInterfaces("wwan0", "ppp0") != null;
        }

        // No other platform uses a cellular connection.
        return false;
    }

    //--//

    private static int generateMask(int prefix)
    {
        return (int) ((1L << (32 - prefix)) - 1);
    }

    private static int parseHex(String text)
    {
        return Integer.parseUnsignedInt(text, 16);
    }

    private static int parseHexBigEndian(String text)
    {
        int val = parseHex(text);

        ByteBuffer bbuf = ByteBuffer.allocate(4);
        bbuf.order(ByteOrder.BIG_ENDIAN);
        bbuf.putInt(val);
        bbuf.order(ByteOrder.LITTLE_ENDIAN);
        return bbuf.getInt(0);
    }

    private static void exec(Object... args) throws
                                             IOException
    {
        String[] textArgs = new String[args.length];

        for (int i = 0; i < args.length; i++)
        {
            Object val = args[i];

            if (val instanceof InetAddress)
            {
                InetAddress ia = (InetAddress) val;
                textArgs[i] = ia.getHostAddress();
            }
            else
            {
                textArgs[i] = val.toString();
            }
        }

        LoggerInstance.info("Executing: %s", String.join(" ", textArgs));

        if (Platform.isLinux())
        {
            ProcessUtils.exec(null, 10, TimeUnit.SECONDS, textArgs);
        }
    }
}
