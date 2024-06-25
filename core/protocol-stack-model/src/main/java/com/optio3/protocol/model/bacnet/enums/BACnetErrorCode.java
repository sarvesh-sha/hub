/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetErrorCode implements TypedBitSet.ValueGetter
{
    // @formatter:off
    abort_apdu_too_long                    (123), 
    abort_application_exceeded_reply_time  (124), 
    abort_buffer_overflow                  (51), 
    abort_insufficient_security            (135), 
    abort_invalid_apdu_in_this_state       (52), 
    abort_other                            (56), 
    abort_out_of_resources                 (125), 
    abort_preempted_by_higher_priority_task(53), 
    abort_proprietary                      (55), 
    abort_security_error                   (136), 
    abort_segmentation_not_supported       (54), 
    abort_tsm_timeout                      (126), 
    abort_window_size_out_of_range         (127), 
    access_denied                          (85), 
    addressing_error                       (115), 
    bad_destination_address                (86), 
    bad_destination_device_id              (87), 
    bad_signature                          (88), 
    bad_source_address                     (89), 
    bad_timestamp                          (90), 
    busy                                   (82), 
    cannot_use_key                         (91), 
    cannot_verify_message_id               (92), 
    character_set_not_supported            (41), 
    communication_disabled                 (83), 
    configuration_in_progress              (2), 
    correct_key_revision                   (93), 
    cov_subscription_failed                (43), 
    datatype_not_supported                 (47), 
    delete_fdt_entry_failed                (120), 
    destination_device_id_required         (94), 
    device_busy                            (3), 
    distribute_broadcast_failed            (121), 
    duplicate_entry                        (137), 
    duplicate_message                      (95), 
    duplicate_name                         (48), 
    duplicate_object_id                    (49), 
    dynamic_creation_not_supported         (4), 
    encryption_not_configured              (96), 
    encryption_required                    (97), 
    file_access_denied                     (5), 
    file_full                              (128), 
    inconsistent_configuration             (129), 
    inconsistent_object_type               (130), 
    inconsistent_parameters                (7), 
    inconsistent_selection_criterion       (8), 
    incorrect_key                          (98), 
    internal_error                         (131), 
    invalid_array_index                    (42), 
    invalid_configuration_data             (46), 
    invalid_data_type                      (9),

    invalid_event_state                    (73), 
    invalid_file_access_method             (10), 
    invalid_file_start_position            (11), 
    invalid_key_data                       (99), 
    invalid_parameter_data_type            (13), 
    invalid_tag                            (57), 
    invalid_timestamp                      (14), 
    invalid_value_in_this_state            (138), 
    key_update_in_progress                 (100), 
    list_element_not_found                 (81), 
    log_buffer_full                        (75), 
    logged_value_purged                    (76), 
    malformed_message                      (101), 
    message_too_long                       (113), 
    missing_required_parameter             (16), 
    network_down                           (58), 
    no_alarm_configured                    (74), 
    no_objects_of_specified_type           (17), 
    no_property_specified                  (77), 
    no_space_for_object                    (18), 
    no_space_to_add_list_element           (19), 
    no_space_to_write_property             (20), 
    no_vt_sessions_available               (21), 
    not_configured                         (132), 
    not_configured_for_triggered_logging   (78), 
    not_cov_property                       (44), 
    not_key_server                         (102), 
    not_router_to_dnet                     (110), 
    object_deletion_not_permitted          (23), 
    object_identifier_already_exists       (24), 
    operational_problem                    (25), 
    optional_functionality_not_supported   (45), 
    other                                  (0), 
    out_of_memory                          (133), 
    parameter_out_of_range                 (80), 
    password_failure                       (26), 
    property_is_not_a_list                 (22), 
    property_is_not_an_array               (50), 
    read_access_denied                     (27), 
    read_bdt_failed                        (117), 
    read_fdt_failed                        (119), 
    register_foreign_device_failed         (118), 
    reject_buffer_overflow                 (59), 
    reject_inconsistent_parameters         (60), 
    reject_invalid_parameter_data_type     (61), 
    reject_invalid_tag                     (62), 
    reject_missing_required_parameter      (63), 
    reject_other                           (69), 
    reject_parameter_out_of_range          (64), 
    reject_proprietary                     (68), 
    reject_too_many_arguments              (65), 
    reject_undefined_enumeration           (66), 
    reject_unrecognized_service            (67), 
    router_busy                            (111), 
    security_error                         (114), 
    security_not_configured                (103), 
    service_request_denied                 (29), 
    source_security_required               (104), 
    success                                (84), 
    timeout                                (30), 
    too_many_keys                          (105), 
    unknown_authentication_type            (106), 
    unknown_device                         (70), 
    unknown_file_size                      (122), 
    unknown_key                            (107), 
    unknown_key_revision                   (108), 
    unknown_network_message                (112), 
    unknown_object                         (31), 
    unknown_property                       (32), 
    unknown_route                          (71), 
    unknown_source_message                 (109), 
    unknown_subscription                   (79), 
    unknown_vt_class                       (34), 
    unknown_vt_session                     (35), 
    unsupported_object_type                (36), 
    value_not_initialized                  (72), 
    value_out_of_range                     (37), 
    value_too_long                         (134), 
    vt_session_already_closed              (38), 
    vt_session_termination_failure         (39), 
    write_access_denied                    (40), 
    write_bdt_failed                       (116);
    // @formatter:on

    private final byte m_encoding;

    BACnetErrorCode(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetErrorCode parse(byte value)
    {
        for (BACnetErrorCode t : values())
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
