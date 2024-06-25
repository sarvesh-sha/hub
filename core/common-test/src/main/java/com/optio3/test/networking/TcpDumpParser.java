/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.networking;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.util.function.FunctionWithException;
import org.apache.commons.lang3.StringUtils;

public class TcpDumpParser
{
    public static class RegexTokenizer
    {
        private final Map<String, Pattern> m_patterns = Maps.newHashMap();
        private       String               m_input;

        public void setInput(String input)
        {
            m_input = input;
        }

        public boolean skip(String prefix)
        {
            String input = m_input.trim();
            if (!input.startsWith(prefix))
            {
                return false;
            }

            m_input = input.substring(prefix.length())
                           .trim();
            return true;
        }

        public <T> T match(String regEx,
                           FunctionWithException<String[], T> callback) throws
                                                                        Exception
        {
            Pattern pattern = m_patterns.get(regEx);
            if (pattern == null)
            {
                pattern = Pattern.compile(regEx);
                m_patterns.put(regEx, pattern);
            }

            Matcher matcher = pattern.matcher(m_input);
            if (!matcher.find())
            {
                return null;
            }

            int      groups      = matcher.groupCount();
            String[] groupValues = new String[groups];
            for (int i = 0; i < groups; i++)
            {
                groupValues[i] = matcher.group(i + 1);
            }

            T res = callback.apply(groupValues);
            if (res != null)
            {
                m_input = m_input.substring(matcher.end());
            }

            return res;
        }
    }

    public static class Packet
    {
        public ZonedDateTime     timestamp;
        public InetSocketAddress source;
        public InetSocketAddress destination;
        public Long              seq;
        public Long              seqEnd;
        public Long              ack;
        public Long              win;
        public Long              length;
        public String            flags;
        public String            options;
    }

    public static class Connection
    {
        public final InetSocketAddress source;
        public final InetSocketAddress destination;
        public final List<Packet>      packetsRx = Lists.newArrayList();
        public final List<Packet>      packetsTx = Lists.newArrayList();

        public Connection(InetSocketAddress source,
                          InetSocketAddress destination)
        {
            this.source      = source;
            this.destination = destination;
        }

        public static void update(Map<InetSocketAddress, Connection> activeConnections,
                                  Packet packet)
        {
            if (StringUtils.equals(packet.flags, "S"))
            {
                activeConnections.put(packet.source, new Connection(packet.source, packet.destination));
            }

            Connection tx = activeConnections.get(packet.source);
            if (tx != null)
            {
                tx.packetsTx.add(packet);
            }

            Connection rx = activeConnections.get(packet.destination);
            if (rx != null)
            {
                rx.packetsRx.add(packet);
            }
        }

        public static boolean addLength(List<Packet> lst,
                                        ZonedDateTime start,
                                        ZonedDateTime end,
                                        AtomicLong bytes,
                                        AtomicLong numPackets)
        {
            boolean found = false;

            for (Packet packet : lst)
            {
                if (packet.length != null)
                {
                    if (start != null && start.isAfter(packet.timestamp))
                    {
                        continue;
                    }

                    if (end != null && !end.isAfter(packet.timestamp))
                    {
                        continue;
                    }

                    bytes.addAndGet(packet.length);
                    numPackets.incrementAndGet();
                    found = true;
                }
            }

            return found;
        }
    }

    //--//

    private final RegexTokenizer m_tokenizer = new RegexTokenizer();

    public Packet parse(ZonedDateTime baselineTimestamp,
                        String line) throws
                                     Exception
    {
        //    02:43:34.511678 IP 25.167.92.99.40344 > 34.222.248.224.443: Flags [S], seq 2059901652, win 29200, options [mss 1460,sackOK,TS val 1123774621 ecr 0,nop,wscale 7], length 0
        //    02:43:35.515244 IP 25.167.92.99.40344 > 34.222.248.224.443: Flags [S], seq 2059901652, win 29200, options [mss 1460,sackOK,TS val 1123775625 ecr 0,nop,wscale 7], length 0
        //    02:43:36.028528 IP 34.222.248.224.443 > 25.167.92.99.40344: Flags [S.], seq 2390594448, ack 2059901653, win 28960, options [mss 1400,sackOK,TS val 133143883 ecr 1123774621,nop,wscale 7], length 0
        //    02:43:36.028652 IP 25.167.92.99.40344 > 34.222.248.224.443: Flags [.], ack 1, win 229, options [nop,nop,TS val 1123776138 ecr 133143883], length 0
        //    02:43:36.052922 IP 25.167.92.99.40344 > 34.222.248.224.443: Flags [P.], seq 1:152, ack 1, win 229, options [nop,nop,TS val 1123776162 ecr 133143883], length 151
        //    02:43:36.070017 IP 34.222.248.224.443 > 25.167.92.99.40344: Flags [S.], seq 2390594448, ack 2059901653, win 28960, options [mss 1400,sackOK,TS val 133143922 ecr 1123774621,nop,wscale 7], length 0
        //    02:43:36.070109 IP 25.167.92.99.40344 > 34.222.248.224.443: Flags [.], ack 1, win 229, options [nop,nop,TS val 1123776180 ecr 133143883], length 0
        //    02:43:36.430153 IP 34.222.248.224.443 > 25.167.92.99.40344: Flags [.], ack 152, win 235, options [nop,nop,TS val 133144298 ecr 1123776162], length 0
        //    02:43:36.794775 IP 34.222.248.224.443 > 25.167.92.99.40344: Flags [.], seq 1:1389, ack 152, win 235, options [nop,nop,TS val 133144303 ecr 1123776180], length 1388
        //    02:43:36.794868 IP 25.167.92.99.40344 > 34.222.248.224.443: Flags [.], ack 1389, win 251, options [nop,nop,TS val 1123776904 ecr 133144303], length 0
        //    02:43:36.873159 IP 34.222.248.224.443 > 25.167.92.99.40344: Flags [.], seq 1389:2777, ack 152, win 235, options [nop,nop,TS val 133144303 ecr 1123776180], length 1388
        //    02:43:36.873272 IP 25.167.92.99.40344 > 34.222.248.224.443: Flags [.], ack 2777, win 274, options [nop,nop,TS val 1123776983 ecr 133144303], length 0

        Packet packet = new Packet();

        m_tokenizer.setInput(line);
        packet.timestamp = m_tokenizer.match("^(\\d+):(\\d+):(\\d+)\\.(\\d+)", (groups) -> // 34.212.1.173.443
        {
            int[] nums = toInt(groups);

            return baselineTimestamp.plus(nums[0], ChronoUnit.HOURS)
                                    .plus(nums[1], ChronoUnit.MINUTES)
                                    .plus(nums[2], ChronoUnit.SECONDS)
                                    .plus(nums[3], ChronoUnit.MICROS)
                                    .minus(8, ChronoUnit.HOURS);
        });
        if (packet.timestamp == null || !m_tokenizer.skip("IP"))
        {
            return null;
        }

        packet.source = m_tokenizer.match("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)", (groups) ->
        {
            int[] nums = toInt(groups);

            InetAddress addr = InetAddress.getByAddress(new byte[] { (byte) nums[0], (byte) nums[1], (byte) nums[2], (byte) nums[3] });
            return new InetSocketAddress(addr, nums[4]);
        });
        if (packet.source == null || !m_tokenizer.skip(">"))
        {
            return null;
        }

        packet.destination = m_tokenizer.match("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)", (groups) ->
        {
            int[] nums = toInt(groups);

            InetAddress addr = InetAddress.getByAddress(new byte[] { (byte) nums[0], (byte) nums[1], (byte) nums[2], (byte) nums[3] });
            return new InetSocketAddress(addr, nums[4]);
        });
        if (packet.destination == null || !m_tokenizer.skip(": Flags"))
        {
            return null;
        }

        packet.flags = m_tokenizer.match("^\\[([A-Z\\.]*)\\]", (groups) -> groups[0]);
        if (packet.flags == null)
        {
            return null;
        }

        if (m_tokenizer.skip(", seq"))
        {
            long[] seqRange = m_tokenizer.match("^(\\d+):(\\d+)", (groups) -> new long[] { Long.parseLong(groups[0]), Long.parseLong(groups[1]) });
            if (seqRange != null)
            {
                packet.seq    = seqRange[0];
                packet.seqEnd = seqRange[1];
            }
            else
            {
                packet.seq = m_tokenizer.match("^(\\d+)", (groups) -> Long.parseLong(groups[0]));
                if (packet.seq == null)
                {
                    return null;
                }
            }
        }

        if (m_tokenizer.skip(", ack"))
        {
            packet.ack = m_tokenizer.match("^(\\d+)", (groups) -> Long.parseLong(groups[0]));
            if (packet.ack == null)
            {
                return null;
            }
        }

        if (m_tokenizer.skip(", win"))
        {
            packet.win = m_tokenizer.match("^(\\d+)", (groups) -> Long.parseLong(groups[0]));
            if (packet.win == null)
            {
                return null;
            }
        }

        if (m_tokenizer.skip(", options"))
        {
            packet.options = m_tokenizer.match("^\\[([0-9a-zA-Z, :{}]*)\\]", (groups) -> groups[0]);
            if (packet.options == null)
            {
                return null;
            }
        }

        if (m_tokenizer.skip(", length"))
        {
            packet.length = m_tokenizer.match("^(\\d+)", (groups) -> Long.parseLong(groups[0]));
            if (packet.length == null)
            {
                return null;
            }
        }

        return packet;
    }

    public static List<InetSocketAddress> extractAddresses(Collection<InetSocketAddress> coll)
    {
        List<InetSocketAddress> sorted = Lists.newArrayList(coll);

        sorted.sort((a, b) ->
                    {
                        int diff = StringUtils.compare(a.getAddress()
                                                        .getHostAddress(),
                                                       b.getAddress()
                                                        .getHostAddress());
                        if (diff == 0)
                        {
                            diff = Integer.compare(a.getPort(), b.getPort());
                        }
                        return diff;
                    });

        return sorted;
    }

    public static int[] toInt(String[] val)
    {
        int[] res = new int[val.length];
        for (int i = 0; i < res.length; i++)
        {
            res[i] = Integer.parseInt(val[i]);
        }
        return res;
    }
}
