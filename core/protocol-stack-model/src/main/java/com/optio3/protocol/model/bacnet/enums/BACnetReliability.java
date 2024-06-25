/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetReliability implements TypedBitSet.ValueGetter
{
    // @formatter:off
    no_fault_detected               (0),
    no_sensor                       (1),
    over_range                      (2),
    under_range                     (3),
    open_loop                       (4),
    shorted_loop                    (5),
    no_output                       (6),
    unreliable_other                (7),
    process_error                   (8),
    multi_state_fault               (9),
    configuration_error             (10),
    communication_failure           (12),
    member_fault                    (13),
    monitored_object_fault          (14),
    tripped                         (15),
    lamp_failure                    (16),
    activation_failure              (17),
    renew_dhcp_failure              (18),
    renew_fd_registration_failure   (19),
    restart_auto_negotiation_failure(20),
    restart_failure                 (21),
    proprietary_command_failure     (22),
    faults_listed                   (23),
    referenced_object_fault         (24);
    // @formatter:on

    private final byte m_encoding;

    BACnetReliability(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetReliability parse(byte value)
    {
        for (BACnetReliability t : values())
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
