/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetRelationship implements TypedBitSet.ValueGetter
{
    // @formatter:off
    unknown            (0),
    defaultRelationship(1),
    
    // Note that all of the following are in even/odd pairs indicating forward and reverse relationships.
    // Proprietary extensions shall also follow the same even/odd pairing so that consumers can determine, for
    // any given relationship, what the opposite relationship is.
    contains           (2),
    contained_by       (3),
    uses               (4),
    used_by            (5),
    commands           (6),
    commanded_by       (7),
    adjusts            (8),
    adjusted_by        (9),
    ingress            (10),
    egress             (11),
    supplies_air       (12),
    receives_air       (13),
    supplies_hot_air   (14),
    receives_hot_air   (15),
    supplies_cool_air  (16),
    receives_cool_air  (17),
    supplies_power     (18),
    receives_power     (19),
    supplies_gas       (20),
    receives_gas       (21),
    supplies_water     (22),
    receives_water     (23),
    supplies_hot_water (24),
    receives_hot_water (25),
    supplies_cool_water(26),
    receives_cool_water(27),
    supplies_steam     (28),
    receives_steam     (29);
    // @formatter:on

    private final byte m_encoding;

    BACnetRelationship(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetRelationship parse(byte value)
    {
        for (BACnetRelationship t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }

    @HandlerForEncoding
    public byte encoding()
    {
        return m_encoding;
    }

    @Override
    public int getEncodingValue()
    {
        return m_encoding;
    }
}
