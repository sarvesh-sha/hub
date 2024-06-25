/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.common;

public class CRC
{
    private final int[] m_table;

    public CRC(int polynomial)
    {
        m_table = new int[256];

        for (int i = 0; i < 256; i++)
        {
            int n = i;

            for (int j = 8; j > 0; j--)
            {
                if ((n & 1) == 1)
                {
                    n = (n >>> 1) ^ polynomial;
                }
                else
                {
                    n = n >>> 1;
                }
            }
            m_table[i] = n;
        }
    }

    public int computeLittleEndian(int initial,
                                   byte[] buf,
                                   int offset,
                                   int count)
    {
        int crc = initial;

        for (int i = 0; i < count; i++)
        {
            crc = (crc >>> 8) ^ m_table[(buf[i + offset] & 0xFF) ^ (crc & 0x000000FF)];
        }

        return crc;
    }
}
