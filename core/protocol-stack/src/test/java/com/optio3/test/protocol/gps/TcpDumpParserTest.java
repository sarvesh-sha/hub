/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.gps;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.networking.TcpDumpParser;
import com.optio3.util.Resources;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class TcpDumpParserTest extends Optio3Test
{
    @Test
    public void parseTcpdump() throws
                               Exception
    {
        // for i in netdump_*; do tcpdump -n -S -r $i | sed "s/^/${i} # /";done >dump.txt

        TcpDumpParser.RegexTokenizer                     tokenizer         = new TcpDumpParser.RegexTokenizer();
        TcpDumpParser                                    parser            = new TcpDumpParser();
        Map<InetSocketAddress, TcpDumpParser.Connection> activeConnections = Maps.newHashMap();
        ZonedDateTime                                    firstTimestamp    = null;
        ZonedDateTime                                    lastTimestamp     = null;

        for (String line : Resources.loadResourceAsLines(TcpDumpParserTest.class, "TcpDump/tcpdump.txt", false))
        {
            String[] parts = StringUtils.splitByWholeSeparator(line, " # ");

            tokenizer.setInput(parts[0]);
            ZonedDateTime fileTimestamp = tokenizer.match("^netdump_(\\d+)-(\\d+)-(\\d+)_(\\d+)_(\\d+)__(\\d+)$", (groups) ->
            {
                int[] nums = TcpDumpParser.toInt(groups);

                return ZonedDateTime.of(nums[0], nums[1], nums[2], nums[3], nums[4], 0, 0, ZoneOffset.UTC);
            });
            if (fileTimestamp == null)
            {
                continue;
            }

            TcpDumpParser.Packet packet = parser.parse(fileTimestamp, parts[1]);
            if (packet != null)
            {
                if (firstTimestamp == null)
                {
                    firstTimestamp = packet.timestamp;
                }

                lastTimestamp = packet.timestamp;

                TcpDumpParser.Connection.update(activeConnections, packet);
            }
        }

        Multimap<InetSocketAddress, TcpDumpParser.Connection> reverseConnections = HashMultimap.create();
        for (InetSocketAddress src : activeConnections.keySet())
        {
            TcpDumpParser.Connection connection = activeConnections.get(src);

            reverseConnections.put(connection.destination, connection);
        }

        ZonedDateTime start = firstTimestamp.truncatedTo(ChronoUnit.DAYS);
        while (start.isBefore(lastTimestamp))
        {
            ZonedDateTime end = start.plus(1, ChronoUnit.HOURS);

            for (InetSocketAddress dst : TcpDumpParser.extractAddresses(reverseConnections.keySet()))
            {
                Collection<TcpDumpParser.Connection> connections = reverseConnections.get(dst);

                AtomicLong rxBytes        = new AtomicLong();
                AtomicLong txBytes        = new AtomicLong();
                AtomicLong rxPackets      = new AtomicLong();
                AtomicLong txPackets      = new AtomicLong();
                boolean    found          = false;
                int        numConnections = 0;
                for (TcpDumpParser.Connection connection : connections)
                {
                    boolean got = false;

                    got |= TcpDumpParser.Connection.addLength(connection.packetsRx, start, end, rxBytes, rxPackets);
                    got |= TcpDumpParser.Connection.addLength(connection.packetsTx, start, end, txBytes, txPackets);

                    if (got)
                    {
                        numConnections++;
                        found = true;
                    }
                }

                if (found)
                {
                    System.out.printf("%s : %s %d Rx: %,d (%d)  Tx: %,d (%d)\n", start, toString(dst, true), numConnections, rxBytes.get(), rxPackets.get(), txBytes.get(), txPackets.get());
                }
            }

            start = end;
        }
    }

    private static String toString(InetSocketAddress socket,
                                   boolean resolve)
    {
        InetAddress addr = socket.getAddress();

        return resolve ? String.format("%s:%d (%s)", addr.getHostAddress(), socket.getPort(), addr.getCanonicalHostName()) : String.format("%s:%d", addr.getHostAddress(), socket.getPort());
    }
}
