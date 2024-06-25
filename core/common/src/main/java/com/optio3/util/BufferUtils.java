/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.util.function.Consumer;

import com.optio3.stream.OutputBuffer;

public class BufferUtils
{
    private final static char[] s_hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    //--//

    public static void convertToHex(byte[] buf,
                                    int offset,
                                    int length,
                                    int chunks,
                                    boolean showAscii,
                                    Consumer<String> callback)
    {
        StringBuilder sb = new StringBuilder();

        int cursor = 0;

        while (cursor < length)
        {
            sb.setLength(0);

            sb.append(String.format("0x%04x: ", cursor));

            convertToHex(sb, buf, offset + cursor, Math.min(chunks, length - cursor), chunks, true, showAscii);

            callback.accept(sb.toString());

            cursor += chunks;
        }
    }

    public static String convertToHex(byte[] buf,
                                      int offset,
                                      int length,
                                      int padToLength,
                                      boolean addSpaceBetweenDigits,
                                      boolean showAscii)
    {
        StringBuilder sb = new StringBuilder();

        convertToHex(sb, buf, offset, length, padToLength, addSpaceBetweenDigits, showAscii);

        return sb.toString();
    }

    public static void convertToHex(StringBuilder sb,
                                    byte[] buf,
                                    int offset,
                                    int length,
                                    int padToLength,
                                    boolean addSpaceBetweenDigits,
                                    boolean showAscii)
    {
        int numDigits = Math.max(length, padToLength);

        for (int i = 0; i < numDigits; i++)
        {
            if (addSpaceBetweenDigits && i > 0)
            {
                sb.append(' ');
            }

            if (i < length)
            {
                byte b = buf[offset + i];
                sb.append(toHex(b >> 4));
                sb.append(toHex(b));
            }
            else
            {
                sb.append(' ');
                sb.append(' ');
            }
        }

        if (showAscii)
        {
            sb.append(" - ");

            for (int i = 0; i < length; i++)
            {
                char c = (char) buf[offset + i];

                sb.append((c < 32 || c > 127) ? '.' : c);
            }
        }
    }

    public static byte[] convertFromHex(String text)
    {
        try (OutputBuffer ob = new OutputBuffer())
        {
            for (int i = 0; i < text.length(); i += 2)
            {
                int partHigh = fromHex(text.charAt(i));
                int partLow  = fromHex(text.charAt(i + 1));

                if (partHigh < 0 || partLow < 0)
                {
                    break;
                }

                ob.emit1Byte((partHigh << 4) | partLow);
            }

            return ob.toByteArray();
        }
    }

    public static byte convertFromHex(String text,
                                      int offset)
    {
        int partHigh = fromHex(text.charAt(offset));
        int partLow  = fromHex(text.charAt(offset + 1));

        if (partHigh < 0 || partLow < 0)
        {
            throw new RuntimeException("Invalid HEX text: " + text.substring(offset));
        }

        return (byte) ((partHigh << 4) | partLow);
    }

    public static short convertFromHex16(String text,
                                         int offset)
    {
        int partHigh = 0xFF & convertFromHex(text, offset);
        int partLow  = 0xFF & convertFromHex(text, offset + 2);

        return (short) ((partHigh << 8) | partLow);
    }

    public static int convertFromHex32(String text,
                                       int offset)
    {
        int partHigh = 0xFFFF & convertFromHex16(text, offset);
        int partLow  = 0xFFFF & convertFromHex16(text, offset + 4);

        return (partHigh << 16) | partLow;
    }

    public static long convertFromHex64(String text,
                                        int offset)
    {
        long partHigh = 0xFFFF_FFFFL & convertFromHex32(text, offset);
        long partLow  = 0xFFFF_FFFFL & convertFromHex32(text, offset + 8);

        return (partHigh << 32) | partLow;
    }

    public static char toHex(int b)
    {
        return s_hexDigits[b & 0xF];
    }

    public static int fromHex(char c)
    {
        if (c >= '0' && c <= '9')
        {
            return c - '0';
        }

        if (c >= 'a' && c <= 'f')
        {
            return c - 'a' + 10;
        }

        if (c >= 'A' && c <= 'F')
        {
            return c - 'A' + 10;
        }

        return -1;
    }
}
