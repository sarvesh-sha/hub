/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.util.Exceptions;

//
//    Intel Hex Data Layout
//    Each line in an Intel Hex file has the same basic layout, like this:
//
//    :BBAAAATT[DDDDDDDD]CC
//
//        where : is start of line marker
//    BB is number of data bytes on line
//    AAAA is address in bytes
//    TT is type discussed below but 00 means data
//    DD is data bytes, number depends on BB value
//    CC is checksum (2s-complement of number of bytes+address+data)
//
public class IntelHexParser
{
    private Map<Integer, byte[]>  m_map             = Maps.newHashMap();
    private Integer               m_activeAddress   = null;
    private Integer               m_activeddressEnd = null;
    private ByteArrayOutputStream m_activeStream    = null;

    public void parse(List<String> lines)
    {
        for (String line : lines)
        {
            if (!line.startsWith(":"))
            {
                throw Exceptions.newRuntimeException("Unexpected line format: %s", line);
            }

            int lineLen = line.length();
            if (lineLen % 2 != 1 && lineLen < (5 * 2 + 1))
            {
                throw Exceptions.newRuntimeException("Invalid line format: %s", line);
            }

            byte[] rawData = new byte[lineLen / 2];

            int checksum = 0;

            for (int i = 1; i < lineLen; i += 2)
            {
                byte val = (byte) Integer.parseUnsignedInt(line.substring(i, i + 2), 16);
                rawData[i / 2] = val;
                checksum += val;
            }

            if ((byte) checksum != 0)
            {
                throw Exceptions.newRuntimeException("Checksum failure (sum=%d) in line: %s", checksum, line);
            }

            int len     = extract(rawData, 0);
            int address = (extract(rawData, 1) << 8) | extract(rawData, 2);
            int type    = extract(rawData, 3);

            if (type == 0)
            {
                if (m_activeStream == null || m_activeddressEnd != address)
                {
                    flush();

                    m_activeStream = new ByteArrayOutputStream();
                    m_activeAddress = address;
                    m_activeddressEnd = address;
                }

                m_activeStream.write(rawData, 4, len);
                m_activeddressEnd += len;
            }
        }
    }

    public Map<Integer, byte[]> toMap()
    {
        flush();

        return m_map;
    }

    private static int extract(byte[] rawData,
                               int offset)
    {
        return rawData[offset] & 0xFF;
    }

    private void flush()
    {
        if (m_activeStream != null)
        {
            m_map.put(m_activeAddress, m_activeStream.toByteArray());

            m_activeStream = null;
            m_activeAddress = null;
        }
    }
}
