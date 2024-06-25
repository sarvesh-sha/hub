/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

package com.optio3.interop.mediaaccess;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.concurrency.Executors;
import com.optio3.util.ResourceCleaner;
import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

public final class EthernetAccess
{
    static class ResourceState extends ResourceCleaner
    {
        private Pointer m_handle;

        public ResourceState(Object holder)
        {
            super(holder);
        }

        public synchronized int open(String device,
                                     int timeoutMillisec)
        {
            Errbuf  errbuf = new Errbuf();
            Pointer handle = pcap_create(device, errbuf);
            EthernetAccess.checkError(handle == null, errbuf);

            m_handle = handle;

            pcap_set_timeout(handle, timeoutMillisec);
            pcap_set_immediate_mode(handle, 1);

            checkError(pcap_activate(handle));

            return pcap_datalink(handle);
        }

        @Override
        protected synchronized void closeUnderCleaner()
        {
            if (m_handle != null)
            {
                pcap_close(m_handle);
                m_handle = null;
            }
        }

        public boolean isClosed()
        {
            return m_handle == null;
        }

        //--//

        public synchronized void setFilter(String filter)
        {
            bpf_program fp = new bpf_program();
            try
            {
                checkError(pcap_compile(m_handle, fp, filter, 0, Integer.MIN_VALUE));

                checkError(pcap_setfilter(m_handle, fp));
            }
            finally
            {
                pcap_freecode(fp);
            }
        }

        public synchronized void sendRaw(byte[] packet)
        {
            if (m_handle != null)
            {
                if (packet.length < 60)
                {
                    packet = Arrays.copyOf(packet, 60);
                }

                pcap_inject(m_handle, packet, packet.length);
            }
        }

        public synchronized void loop(int count,
                                      PacketListener callback)
        {
            if (m_handle != null)
            {
                if (Platform.isMac())
                {
                    // BUGBUG: Temporarily disable. JNA generates incorrect code on Apple Silicon.
                    Executors.safeSleep(1000);
                    return;
                }

                pcap_dispatch(m_handle, count, (args, header, packet) ->
                {
                    pcap_pkthdr hdr = new pcap_pkthdr(header);

                    byte[] payload = packet.getByteArray(0, hdr.caplen);

                    Executors.getDefaultThreadPool()
                             .execute(() -> callback.invoke(payload));
                }, null);
            }
        }

        //--//

        private void checkError(int error)
        {
            if (error != 0)
            {
                throw new RuntimeException(pcap_geterr(m_handle));
            }
        }
    }

    public static class Errbuf extends Structure
    {
        public static final int PCAP_ERRBUF_SIZE = 256;

        public byte[] buf = new byte[PCAP_ERRBUF_SIZE];

        public Errbuf()
        {
        }

        public int length()
        {
            return toString().length();
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("buf");
        }

        @Override
        public String toString()
        {
            return Native.toString(buf);
        }
    }

    public static class pcap_if extends Structure
    {
        public pcap_if.ByReference   next;
        public String                name;
        public String                description;
        public pcap_addr.ByReference addresses;
        public int                   flags;

        public pcap_if()
        {
        }

        public pcap_if(Pointer p)
        {
            super(p);

            read();
        }

        public static class ByReference extends pcap_if implements Structure.ByReference
        {
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("next", "name", "description", "addresses", "flags");
        }
    }

    public static class pcap_addr extends Structure
    {
        public pcap_addr.ByReference next;
        public sockaddr.ByReference  addr;
        public sockaddr.ByReference  netmask;
        public sockaddr.ByReference  broadaddr;
        public sockaddr.ByReference  dstaddr;

        public pcap_addr()
        {
        }

        public pcap_addr(Pointer p)
        {
            super(p);

            read();
        }

        public static class ByReference extends pcap_addr implements Structure.ByReference
        {
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("next", "addr", "netmask", "broadaddr", "dstaddr");
        }
    }

    //--//

    public static class sockaddr extends Structure
    {
        public short  sa_family;
        public byte[] sa_data = new byte[14];

        public sockaddr()
        {
        }

        public sockaddr(Pointer p)
        {
            super(p);

            read();
        }

        public static class ByReference extends sockaddr implements Structure.ByReference
        {
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("sa_family", "sa_data");
        }

        //--//

        public short getSaFamily()
        {
            return getSaFamily(sa_family);
        }

        public static short getSaFamily(short family)
        {
            if (Platform.isWindows())
            {
                return family;
            }

            if (Platform.isLinux() && Platform.isARM())
            {
                return (short) (family & 0xFF);
            }

            if (ByteOrder.nativeOrder()
                         .equals(ByteOrder.BIG_ENDIAN))
            {
                return (short) (family & 0xFF);
            }
            else
            {
                return (short) ((family >> 8) & 0xFF);
            }
        }
    }

    public static class sockaddr_in extends Structure
    {
        public short  sin_family;
        public short  sin_port;
        public byte[] sin_addr = new byte[4];
        public byte[] sin_zero = new byte[8];

        public sockaddr_in()
        {
        }

        public sockaddr_in(Pointer p)
        {
            super(p);

            read();
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("sin_family", "sin_port", "sin_addr", "sin_zero");
        }

        public short getSaFamily()
        {
            return sockaddr.getSaFamily(sin_family);
        }
    }

    public static class sockaddr_in6 extends Structure
    {
        public short  sin6_family;
        public short  sin6_port;
        public int    sin6_flowinfo;
        public byte[] sin6_addr = new byte[16];
        public int    sin6_scope_id;

        public sockaddr_in6()
        {
        }

        public sockaddr_in6(Pointer p)
        {
            super(p);

            read();
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("sin6_family", "sin6_port", "sin6_flowinfo", "sin6_addr", "sin6_scope_id");
        }

        public short getSaFamily()
        {
            return sockaddr.getSaFamily(sin6_family);
        }
    }

    // Linux specific
    public static class sockaddr_ll extends Structure
    {
        public short  sll_family;
        public short  sll_protocol;
        public int    sll_ifindex;
        public short  sll_hatype;
        public byte   sll_pkttype;
        public byte   sll_halen;
        public byte[] sll_addr = new byte[8];

        public sockaddr_ll()
        {
        }

        public sockaddr_ll(Pointer p)
        {
            super(p);

            read();
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("sll_family", "sll_protocol", "sll_ifindex", "sll_hatype", "sll_pkttype", "sll_halen", "sll_addr");
        }

        public short getSaFamily()
        {
            return sockaddr.getSaFamily(sll_family);
        }

        public byte[] getAddress()
        {
            int addrLength = sll_halen & 0xFF;
            return Arrays.copyOf(sll_addr, addrLength);
        }
    }

    // Mac OS X and BSD specific
    public static class sockaddr_dl extends Structure
    {
        public byte   sdl_len;
        public byte   sdl_family;
        public short  sdl_index;
        public byte   sdl_type;
        public byte   sdl_nlen;
        public byte   sdl_alen;
        public byte   sdl_slen;
        public byte[] sdl_data = new byte[46]; // minimum work area, can be larger; contains both if name and ll address

        public sockaddr_dl()
        {
        }

        public sockaddr_dl(Pointer p)
        {
            super(p);

            read();
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("sdl_len", "sdl_family", "sdl_index", "sdl_type", "sdl_nlen", "sdl_alen", "sdl_slen", "sdl_data");
        }

        public byte[] getAddress()
        {
            return getPointer().getByteArray(8 + (sdl_nlen & 0xFF), sdl_alen & 0xFF);
        }
    }

    //--//

    public static class pcap_pkthdr extends Structure
    {
        public static final int TS_OFFSET;
        public static final int CAPLEN_OFFSET;
        public static final int LEN_OFFSET;

        static
        {
            pcap_pkthdr ph = new pcap_pkthdr();
            TS_OFFSET     = ph.fieldOffset("ts");
            CAPLEN_OFFSET = ph.fieldOffset("caplen");
            LEN_OFFSET    = ph.fieldOffset("len");
        }

        public timeval ts;// struct timeval
        public int     caplen; // bpf_u_int32
        public int     len;// bpf_u_int32

        public pcap_pkthdr()
        {
        }

        public pcap_pkthdr(Pointer p)
        {
            super(p);
            read();
        }

        public static class ByReference extends pcap_pkthdr implements Structure.ByReference
        {
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("ts", "caplen", "len");
        }

        static NativeLong getTvSec(Pointer p)
        {
            return p.getNativeLong(TS_OFFSET + timeval.TV_SEC_OFFSET);
        }

        static NativeLong getTvUsec(Pointer p)
        {
            return p.getNativeLong(TS_OFFSET + timeval.TV_USEC_OFFSET);
        }

        static int getCaplen(Pointer p)
        {
            return p.getInt(CAPLEN_OFFSET);
        }

        static int getLen(Pointer p)
        {
            return p.getInt(LEN_OFFSET);
        }
    }

    public static class timeval extends Structure
    {
        public static final int TV_SEC_OFFSET;
        public static final int TV_USEC_OFFSET;

        static
        {
            timeval tv = new timeval();
            TV_SEC_OFFSET  = tv.fieldOffset("tv_sec");
            TV_USEC_OFFSET = tv.fieldOffset("tv_usec");
        }

        public NativeLong tv_sec; // long
        public NativeLong tv_usec; // long

        public timeval()
        {
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("tv_sec", "tv_usec");
        }
    }

    public static class bpf_program extends Structure
    {
        public int                  bf_len; // u_int
        public bpf_insn.ByReference bf_insns; // struct bpf_insn *

        public bpf_program()
        {
            setAutoSynch(false);
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("bf_len", "bf_insns");
        }
    }

    public static class bpf_insn extends Structure
    {
        public short code; // u_short
        public byte  jt; // u_char
        public byte  jf; // u_char
        public int   k; // bpf_u_int32

        public bpf_insn()
        {
            setAutoSynch(false);
        }

        public static class ByReference extends bpf_insn implements Structure.ByReference
        {
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("code", "jt", "jf", "k");
        }
    }

    @FunctionalInterface
    static interface pcap_handler extends Callback
    {
        // void got_packet(u_char *args, const struct pcap_pkthdr *header, const u_char *packet);
        public void invoke(Pointer args,
                           Pointer header,
                           Pointer packet);
    }

    //--//

    private static final int AF_INET_DEFAULT   = 2;
    private static final int AF_PACKET_DEFAULT = 17;
    private static final int AF_LINK_DEFAULT   = 18;
    private static final int DLT_RAW_DEFAULT   = 12;
    private static final int DLT_RAW_OPENBSD   = 14;
    private static final int AF_INET6_DEFAULT  = 23;
    private static final int AF_INET6_LINUX    = 10;
    private static final int AF_INET6_FREEBSD  = 28;
    private static final int AF_INET6_MAC      = 30;

    public static int getAfInet()
    {
        return AF_INET_DEFAULT;
    }

    public static int getAfInet6()
    {
        switch (Platform.getOSType())
        {
            case Platform.MAC:
                return AF_INET6_MAC;

            case Platform.FREEBSD:
            case Platform.KFREEBSD:
                return AF_INET6_FREEBSD;

            case Platform.LINUX:
            case Platform.ANDROID:
                return AF_INET6_LINUX;

            default:
                return AF_INET6_DEFAULT;
        }
    }

    // For Linux
    public static int getAfPacket()
    {
        if (Platform.isLinux())
        {
            return AF_PACKET_DEFAULT;
        }

        return -1;
    }

    // For BSD and Mac OS X
    public static int getAfLink()
    {
        if (Platform.isMac() || Platform.isFreeBSD() || Platform.isOpenBSD() || Platform.iskFreeBSD())
        {
            return AF_LINK_DEFAULT;
        }

        return -1;
    }

    static
    {
        Native.register(EthernetAccess.class, NativeLibrary.getInstance(Platform.isWindows() ? "wpcap" : "pcap"));
    }

    // char *pcap_lookupdev(char *errbuf)
    private static native String pcap_lookupdev(Errbuf errbuf);

    // int pcap_findalldevs(pcap_if_t **alldevsp, char *errbuf)
    static native int pcap_findalldevs(PointerByReference alldevsp,
                                       Errbuf errbuf);

    // void  pcap_freealldevs (pcap_if_t *alldevsp)
    static native void pcap_freealldevs(Pointer alldevsp);

    // char *pcap_geterr(pcap_t *p)
    private static native String pcap_geterr(Pointer p);

    // pcap_t *pcap_create (const char *device, char *ebuf)
    private static native Pointer pcap_create(String device,
                                              Errbuf ebuf);

    // int pcap_datalink(pcap_t *p)
    private static native int pcap_datalink(Pointer p);

    // int pcap_set_immediate_mode(pcap_t *p, int immediate)
    private static native int pcap_set_immediate_mode(Pointer p,
                                                      int immediate);

    // int pcap_set_timeout(pcap_t *p, int timeout_ms)
    private static native int pcap_set_timeout(Pointer p,
                                               int timeout_ms);

    // int pcap_activate(pcap_t *p)
    private static native int pcap_activate(Pointer p);

    // int pcap_compile(pcap_t *p, struct bpf_program *fp, char *str, int optimize, bpf_u_int32 netmask)
    private static native int pcap_compile(Pointer p,
                                           bpf_program fp,
                                           String str,
                                           int optimize,
                                           int netmask);

    // int pcap_setfilter(pcap_t *p, struct bpf_program *fp)
    private static native int pcap_setfilter(Pointer p,
                                             bpf_program fp);

    // void pcap_freecode(struct bpf_program *fp)
    private static native void pcap_freecode(bpf_program fp);

    // int pcap_dispatch(pcap_t *p, int cnt, pcap_handler callback, u_char *user)
    private static native int pcap_dispatch(Pointer p,
                                            int cnt,
                                            pcap_handler callback,
                                            Pointer user);

    // void pcap_breakloop(pcap_t *p)
    private static native int pcap_breakloop(Pointer p);

    // int pcap_inject(pcap_t *p, const void *buf, size_t size)
    private static native int pcap_inject(Pointer p,
                                          byte[] buf,
                                          int size);

    // void pcap_close(pcap_t *p)
    private static native void pcap_close(Pointer p);

    //--//

    public static class InterfaceDescriptor
    {
        public String                 name;
        public String                 description;
        public List<InterfaceAddress> addresses = Lists.newArrayList();
        public int                    flags;
    }

    public static class InterfaceAddress
    {
        public Address addr;
        public Address netmask;
        public Address broadaddr;
        public Address dstaddr;

        public static Address convert(sockaddr.ByReference src)
        {
            if (src != null)
            {
                int family = src.getSaFamily();

                if (family == getAfInet())
                {
                    sockaddr_in addr = new sockaddr_in(src.getPointer());

                    Address res = new Address();
                    res.family = AddressFamily.IpV4;
                    res.data   = addr.sin_addr;
                    return res;
                }

                if (family == getAfInet6())
                {
                    sockaddr_in6 addr = new sockaddr_in6(src.getPointer());

                    Address res = new Address();
                    res.family = AddressFamily.IpV6;
                    res.data   = addr.sin6_addr;
                    return res;
                }

                if (family == getAfPacket())
                {
                    sockaddr_ll sll = new sockaddr_ll(src.getPointer());

                    Address res = new Address();
                    res.family = AddressFamily.Packet;
                    res.data   = sll.getAddress();
                    return res;
                }

                if (family == getAfLink())
                {
                    sockaddr_dl sdl = new sockaddr_dl(src.getPointer());

                    Address res = new Address();
                    res.family = AddressFamily.Link;
                    res.data   = sdl.getAddress();
                    return res;
                }
            }

            return null;
        }
    }

    public enum AddressFamily
    {
        IpV4,
        IpV6,
        Packet,
        Link,
    }

    public static class Address
    {
        public AddressFamily family;
        public byte[]        data;

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("%s: ", family));

            for (int i = 0; i < data.length; i++)
            {
                if (i != 0)
                {
                    sb.append(":");
                }

                sb.append(String.format("%02x", data[i]));
            }

            return sb.toString();
        }
    }

    @FunctionalInterface
    public interface PacketListener extends Callback
    {
        void invoke(byte[] packet);
    }

    //--//

    private final ResourceState m_state = new ResourceState(this);
    private final int           m_dataLink;

    public EthernetAccess(String device,
                          int timeoutMillisec)
    {
        m_dataLink = m_state.open(device, timeoutMillisec);
    }

    public void close()
    {
        m_state.clean();
    }

    public boolean isClosed()
    {
        return m_state.isClosed();
    }

    public boolean isEthernetHeader()
    {
        return m_dataLink == 1; // LINKTYPE_ETHERNET
    }

    public boolean isLinuxHeader()
    {
        // Linux "cooked" capture encapsulation.
        return m_dataLink == 113; // LINKTYPE_LINUX_SLL
    }

    public boolean isLoopback()
    {
        return m_dataLink == 0; // LINKTYPE_NULL
    }

    public void setFilter(String filter)
    {
        m_state.setFilter(filter);
    }

    public void loop(int count,
                     PacketListener callback)
    {
        m_state.loop(count, callback);
    }

    public void sendRaw(byte[] packet)
    {
        m_state.sendRaw(packet);
    }

    //--//

    public static List<InterfaceDescriptor> findAllDevices()
    {
        Errbuf             errbuf    = new Errbuf();
        PointerByReference alldevsPP = new PointerByReference();
        int                rc        = pcap_findalldevs(alldevsPP, errbuf);
        checkError(rc != 0, errbuf);

        List<InterfaceDescriptor> results = Lists.newArrayList();

        Pointer alldevsp = alldevsPP.getValue();
        if (alldevsp != null)
        {
            try
            {
                pcap_if pcapIf = new pcap_if(alldevsp);

                for (pcap_if pif = pcapIf; pif != null; pif = pif.next)
                {
                    InterfaceDescriptor id = new InterfaceDescriptor();
                    id.name        = pif.name;
                    id.description = pif.description;
                    id.flags       = pif.flags;

                    for (pcap_addr pcapAddr = pif.addresses; pcapAddr != null; pcapAddr = pcapAddr.next)
                    {
                        InterfaceAddress ia = new InterfaceAddress();

                        ia.addr      = InterfaceAddress.convert(pcapAddr.addr);
                        ia.netmask   = InterfaceAddress.convert(pcapAddr.netmask);
                        ia.broadaddr = InterfaceAddress.convert(pcapAddr.broadaddr);
                        ia.dstaddr   = InterfaceAddress.convert(pcapAddr.dstaddr);

                        id.addresses.add(ia);
                    }
                    results.add(id);
                }
            }
            finally
            {
                pcap_freealldevs(alldevsp);
            }
        }

        return results;
    }

    public static String lookupDev()
    {
        Errbuf errbuf = new Errbuf();
        String result = pcap_lookupdev(errbuf);
        checkError(result == null, errbuf);

        return result;
    }

    //--//

    private static void checkError(boolean failed,
                                   Errbuf errbuf)
    {
        if (failed || errbuf.length() != 0)
        {
            throw new RuntimeException(errbuf.toString());
        }
    }
}
