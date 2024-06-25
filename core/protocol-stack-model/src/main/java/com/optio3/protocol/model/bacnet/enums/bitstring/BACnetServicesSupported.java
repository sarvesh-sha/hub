/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums.bitstring;

import java.util.BitSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.optio3.serialization.TypedBitSet;

public final class BACnetServicesSupported extends TypedBitSet<BACnetServicesSupported.Values>
{
    public enum Values implements TypedBitSet.ValueGetter
    {
        // @formatter:off
        // -- Alarm and Event Services
        acknowledge_alarm                    (0),
        confirmed_cov_notification           (1),
        confirmed_event_notification         (2),
        get_alarm_summary                    (3),
        get_enrollment_summary               (4),
        subscribe_cov                        (5),
        // -- File Access Services
        atomic_read_file                     (6),
        atomic_write_file                    (7),
        // -- Object Access Services
        add_list_element                     (8),
        remove_list_element                  (9),
        create_object                        (10),
        delete_object                        (11),
        read_property                        (12),
        read_property_multiple               (14),
        write_property                       (15),
        write_property_multiple              (16),
        // -- Remote Device Management Services
        device_communication_control         (17),
        confirmed_private_transfer           (18),
        confirmed_text_message               (19),
        reinitialize_device                  (20),
        // -- Virtual Terminal Services
        vt_open                              (21),
        vt_close                             (22),
        vt_data                              (23),
        // -- Unconfirmed Services
        i_am                                 (26),
        i_have                               (27),
        unconfirmed_cov_notification         (28),
        unconfirmed_event_notification       (29),
        unconfirmed_private_transfer         (30),
        unconfirmed_text_message             (31),
        time_synchronization                 (32),
        who_has                              (33),
        who_is                               (34),
        // -- Services added after 1995
        read_range                           (35), // -- Object Access Service
        utc_time_synchronization             (36), // -- Remote Device Management Service
        life_safety_operation                (37), // -- Alarm and Event Service
        subscribe_cov_property               (38), // -- Alarm and Event Service
        get_event_information                (39), // -- Alarm and Event Service
        write_group                          (40), // -- Object Access Services
        // -- Services added after 2012
        subscribe_cov_property_multiple      (41), // -- Alarm and Event Service
        confirmed_cov_notification_multiple  (42), // -- Alarm and Event Service
        unconfirmed_cov_notification_multiple(43); // -- Alarm and Event Service
        // @formatter:on

        private final int m_offset;

        Values(int offset)
        {
            this.m_offset = offset;
        }

        @Override
        public int getEncodingValue()
        {
            return m_offset;
        }
    }

    public BACnetServicesSupported()
    {
        super(Values.class, Values.values());
    }

    public BACnetServicesSupported(BitSet bs)
    {
        super(Values.class, Values.values(), bs);
    }

    @JsonCreator
    public BACnetServicesSupported(List<String> inputs)
    {
        super(Values.class, Values.values(), inputs);
    }
}
