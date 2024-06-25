/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.objects;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.protocol.model.bacnet.BACnetDate;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.BACnetTime;
import com.optio3.protocol.model.bacnet.constructed.BACnetAddressBinding;
import com.optio3.protocol.model.bacnet.constructed.BACnetCOVMultipleSubscription;
import com.optio3.protocol.model.bacnet.constructed.BACnetCOVSubscription;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetVTSession;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetRecipient;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimeStamp;
import com.optio3.protocol.model.bacnet.enums.BACnetBackupState;
import com.optio3.protocol.model.bacnet.enums.BACnetDeviceStatus;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetNotifyType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.BACnetRestartReason;
import com.optio3.protocol.model.bacnet.enums.BACnetSegmentation;
import com.optio3.protocol.model.bacnet.enums.BACnetVTClass;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetEventTransitionBits;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetObjectTypesSupported;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetServicesSupported;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:device")
public final class device extends BACnetObjectModel
{
    // @formatter:off
    @JsonIgnore // Avoid serializing the identity, it would be a duplicate in most cases.
    public BACnetObjectIdentifier              object_identifier;

    @JsonIgnore // Avoid serializing the type, we already know it.
    public BACnetObjectTypeOrUnknown           object_type;

    ///////////////////
    //
    // Required fields:
    //
    public long                                apdu_timeout;
    public String                              application_software_version;
    public long                                database_revision;
    public List<BACnetAddressBinding>          device_address_binding;
    public String                              firmware_revision;
    public long                                max_apdu_length_accepted;
    public String                              model_name;
    public long                                number_of_apdu_retries;
    public BACnetObjectIdentifier[]            object_list;
    public String                              object_name;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public BACnetObjectTypesSupported          protocol_object_types_supported;
    public long                                protocol_revision;
    public BACnetServicesSupported             protocol_services_supported;
    public long                                protocol_version;
    public BACnetSegmentation                  segmentation_supported;
    public BACnetDeviceStatus                  system_status;
    public long                                vendor_identifier;
    public String                              vendor_name;

    ///////////////////
    //
    // Optional fields:
    //
    public BACnetEventTransitionBits           acked_transitions;
    public List<BACnetCOVMultipleSubscription> active_cov_multiple_subscriptions;
    public List<BACnetCOVSubscription>         active_cov_subscriptions;
    public List<BACnetVTSession>               active_vt_sessions;
    public boolean                             align_intervals;
    public long                                apdu_segment_timeout;
    public BACnetBackupState                   backup_and_restore_state;
    public long                                backup_failure_timeout;
    public long                                backup_preparation_time;
    public BACnetObjectIdentifier[]            configuration_files;
    public boolean                             daylight_savings_status;
    public String                              deployed_profile_location;
    public String                              description;
    public boolean                             event_detection_enable;
    public BACnetEventTransitionBits           event_enable;
    public String[]                            event_message_texts;
    public String[]                            event_message_texts_config;
    public BACnetEventState                    event_state;
    public BACnetTimeStamp[]                   event_time_stamps;
    public long                                interval_offset;
    public BACnetRestartReason                 last_restart_reason;
    public BACnetTimeStamp                     last_restore_time;
    public BACnetDate                          local_date;
    public BACnetTime                          local_time;
    public String                              location;
    public long                                max_info_frames;
    public long                                max_master;
    public long                                max_segments_accepted;
    public long                                notification_class;
    public BACnetNotifyType                    notify_type;
    public String                              profile_location;
    public String                              profile_name;
    public BACnetReliability                   reliability;
    public boolean                             reliability_evaluation_inhibit;
    public List<BACnetRecipient>               restart_notification_recipients;
    public long                                restore_completion_time;
    public long                                restore_preparation_time;
    public String                              serial_number;
    public BACnetStatusFlags                   status_flags;
    public BACnetObjectIdentifier[]            structured_object_list;
    public BACnetNameValue[]                   tags;
    public BACnetTimeStamp                     time_of_device_restart;
    public long                                time_synchronization_interval;
    public List<BACnetRecipient>               time_synchronization_recipients;
    public int                                 utc_offset;
    public List<BACnetRecipient>               utc_time_synchronization_recipients;
    public List<BACnetVTClass>                 vt_classes_supported;
    // @formatter:on

    public device()
    {
        super(BACnetObjectType.device);
        active_cov_multiple_subscriptions = Lists.newArrayList();
        active_cov_subscriptions = Lists.newArrayList();
        active_vt_sessions = Lists.newArrayList();
        device_address_binding = Lists.newArrayList();
        event_message_texts = new String[3];
        event_message_texts_config = new String[3];
        event_time_stamps = new BACnetTimeStamp[3];
        event_time_stamps[0] = new BACnetTimeStamp();
        event_time_stamps[1] = new BACnetTimeStamp();
        event_time_stamps[2] = new BACnetTimeStamp();
        restart_notification_recipients = Lists.newArrayList();
        time_synchronization_recipients = Lists.newArrayList();
        utc_time_synchronization_recipients = Lists.newArrayList();
        vt_classes_supported = Lists.newArrayList();
    }
}
