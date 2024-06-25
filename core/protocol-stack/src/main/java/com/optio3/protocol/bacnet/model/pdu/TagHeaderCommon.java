/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu;

import com.optio3.logging.Logger;
import com.optio3.protocol.bacnet.model.enums.ApplicationTag;

// @formatter:off
// |  7  |  6  |  5  |  4  |  3  |  2  |  1  |  0  |
// |-----|-----|-----|-----|-----|-----|-----|-----|
// | Tag Number            |Class| Len/Value/Type  |
// |-----|-----|-----|-----|-----|-----|-----|-----|
// @formatter:on

public abstract class TagHeaderCommon
{
    public static final Logger LoggerInstance = new Logger(TagHeaderCommon.class);

    //--//

    protected static final int c_markerOpeningTag   = 0b110;
    protected static final int c_markerClosingTag   = 0b111;
    protected static final int c_markerTagExtension = 0xF;

    protected static final int c_markerLength1Byte  = 5;
    protected static final int c_markerLength2Bytes = 254;
    protected static final int c_markerLength4Bytes = 255;

    protected static final int c_ValueOffset = 0;
    protected static final int c_ValueSize   = 3;

    protected static final int c_ClassOffset = 3;

    protected static final int c_TagOffset = 4;
    protected static final int c_TagSize   = 4;

    //--//

    public ApplicationTag applicationTag;
    public int            contextTag;
    public int            length;

    public boolean isOpeningTag;
    public boolean isClosingTag;

    protected boolean m_boolValue;

    public boolean isContextSpecific()
    {
        return applicationTag == null;
    }

    //--//

    protected static int extract(int val,
                                 int offset,
                                 int size)
    {
        return (val >> offset) & ((1 << size) - 1);
    }

    protected static boolean extract(int val,
                                     int offset)
    {
        return extract(val, offset, 1) != 0;
    }

    protected static int insert(int offset,
                                int size,
                                int value)
    {
        value &= ((1 << size) - 1);
        return value << offset;
    }

    protected static int insert(int offset,
                                boolean value)
    {
        return (value ? 1 : 0) << offset;
    }

    //--//

    @Override
    public String toString()
    {
        if (isOpeningTag)
        {
            return String.format("OpeningTag: %d", contextTag);
        }

        if (isClosingTag)
        {
            return String.format("OpeningTag: %d", contextTag);
        }

        if (applicationTag != null)
        {
            return String.format("ApplicationTag: %s length=%d", applicationTag, length);
        }

        return String.format("ContextTag: %d", contextTag);
    }
}
