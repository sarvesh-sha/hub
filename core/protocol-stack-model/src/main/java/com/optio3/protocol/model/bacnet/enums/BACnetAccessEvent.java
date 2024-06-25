/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetAccessEvent implements TypedBitSet.ValueGetter
{
    // @formatter:off
    none                                  (0),
    granted                               (1),
    muster                                (2),
    passback_detected                     (3),
    duress                                (4),
    trace                                 (5),
    lockout_max_attempts                  (6),
    lockout_other                         (7),
    lockout_relinquished                  (8),
    locked_by_higher_priority             (9),
    out_of_service                        (10),
    out_of_service_relinquished           (11),
    accompaniment_by                      (12),
    authentication_factor_read            (13),
    authorization_delayed                 (14),
    verification_required                 (15),
    no_entry_after_granted                (16),
    denied_deny_all                       (128),
    denied_unknown_credential             (129),
    denied_authentication_unavailable     (130),
    denied_authentication_factor_timeout  (131),
    denied_incorrect_authentication_factor(132),
    denied_zone_no_access_rights          (133),
    denied_point_no_access_rights         (134),
    denied_no_access_rights               (135),
    denied_out_of_time_range              (136),
    denied_threat_level                   (137),
    denied_passback                       (138),
    denied_unexpected_location_usage      (139),
    denied_max_attempts                   (140),
    denied_lower_occupancy_limit          (141),
    denied_upper_occupancy_limit          (142),
    denied_authentication_factor_lost     (143),
    denied_authentication_factor_stolen   (144),
    denied_authentication_factor_damaged  (145),
    denied_authentication_factor_destroyed(146),
    denied_authentication_factor_disabled (147),
    denied_authentication_factor_error    (148),
    denied_credential_unassigned          (149),
    denied_credential_not_provisioned     (150),
    denied_credential_not_yet_active      (151),
    denied_credential_expired             (152),
    denied_credential_manual_disable      (153),
    denied_credential_lockout             (154),
    denied_credential_max_days            (155),
    denied_credential_max_uses            (156),
    denied_credential_inactivity          (157),
    denied_credential_disabled            (158),
    denied_no_accompaniment               (159),
    denied_incorrect_accompaniment        (160),
    denied_lockout                        (161),
    denied_verification_failed            (162),
    denied_verification_timeout           (163),
    denied_other                          (164);
    // @formatter:on

    private final byte m_encoding;

    BACnetAccessEvent(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetAccessEvent parse(byte value)
    {
        for (BACnetAccessEvent t : values())
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
