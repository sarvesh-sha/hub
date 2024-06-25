/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetAuthenticationFactorType implements TypedBitSet.ValueGetter
{
    // @formatter:off
    undefined           (0),
    error               (1),
    custom              (2),
    simple_number16     (3),
    simple_number32     (4),
    simple_number56     (5),
    simple_alpha_numeric(6),
    aba_track2          (7),
    wiegand26           (8),
    wiegand37           (9),
    wiegand37_facility  (10),
    facility16_card32   (11),
    facility32_card32   (12),
    fasc_n              (13),
    fasc_n_bcd          (14),
    fasc_n_large        (15),
    fasc_n_large_bcd    (16),
    gsa75               (17),
    chuid               (18),
    chuid_full          (19),
    guid                (20),
    cbeff_a             (21),
    cbeff_b             (22),
    cbeff_c             (23),
    user_password       (24);
    // @formatter:on

    private final byte m_encoding;

    BACnetAuthenticationFactorType(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetAuthenticationFactorType parse(byte value)
    {
        for (BACnetAuthenticationFactorType t : values())
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
