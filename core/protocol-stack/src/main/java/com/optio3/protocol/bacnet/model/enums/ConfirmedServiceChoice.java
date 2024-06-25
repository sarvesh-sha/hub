/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.enums;

import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.AcknowledgeAlarm;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.AddListElement;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.AtomicReadFile;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.AtomicWriteFile;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ConfirmedCOVNotification;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ConfirmedCOVNotificationMultiple;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ConfirmedEventNotification;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ConfirmedPrivateTransfer;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ConfirmedTextMessage;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.CreateObject;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.DeleteObject;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.DeviceCommunicationControl;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.GetAlarmSummary;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.GetEnrollmentSummary;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.GetEventInformation;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ReadProperty;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ReadPropertyMultiple;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ReadRange;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ReinitializeDevice;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.RemoveListElement;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.SubscribeCOV;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.SubscribeCOVProperty;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.SubscribeCOVPropertyMultiple;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.VtClose;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.VtData;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.VtOpen;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.WriteProperty;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.WritePropertyMultiple;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;

public enum ConfirmedServiceChoice
{
    // @formatter:off
 // Alarm and Event Services
    acknowledge_alarm                  (0 , AcknowledgeAlarm                .class, null                              , null),
    confirmed_cov_notification         (1 , ConfirmedCOVNotification        .class, null                              , null),
    confirmed_cov_notification_multiple(31, ConfirmedCOVNotificationMultiple.class, null                              , null),
    confirmed_event_notification       (2 , ConfirmedEventNotification      .class, null                              , null),
    get_alarm_summary                  (3 , GetAlarmSummary                 .class, GetAlarmSummary.Ack         .class, null),
    get_enrollment_summary             (4 , GetEnrollmentSummary            .class, GetEnrollmentSummary.Ack    .class, null),
    get_event_information              (29, GetEventInformation             .class, GetEventInformation.Ack     .class, null),
    life_safety_operation              (27, null                                  , null                              , null), // TODO BUGBUG: not implemented
//  life_safety_operation              (27, LifeSafetyOperation             .class, null                              , null),
    subscribe_cov                      (5 , SubscribeCOV                    .class, null                              , null),
    subscribe_cov_property             (28, SubscribeCOVProperty            .class, null                              , null),
    subscribe_cov_property_multiple    (30, SubscribeCOVPropertyMultiple    .class, null                              , null),
    
// File Access Services
    atomic_read_file                   (6 , AtomicReadFile                  .class, AtomicReadFile.Ack          .class, null),
    atomic_write_file                  (7 , AtomicWriteFile                 .class, AtomicWriteFile.Ack         .class, null),
    
// Object Access Services
    add_list_element                   (8 , AddListElement                  .class, null                              , null),
    remove_list_element                (9 , RemoveListElement               .class, null                              , null),
    create_object                      (10, CreateObject                    .class, CreateObject.Ack            .class, null),
    delete_object                      (11, DeleteObject                    .class, null                              , null),
    read_property                      (12, ReadProperty                    .class, ReadProperty.Ack            .class, null),
    read_property_multiple             (14, ReadPropertyMultiple            .class, ReadPropertyMultiple.Ack    .class, null),
    read_range                         (26, ReadRange                       .class, ReadRange.Ack               .class, null),
    write_property                     (15, WriteProperty                   .class, WriteProperty.Ack           .class, null),
    write_property_multiple            (16, WritePropertyMultiple           .class, null                              , null),
    
// Remote Device Management Services
    device_communication_control       (17, DeviceCommunicationControl      .class, null                              , null),
    confirmed_private_transfer         (18, ConfirmedPrivateTransfer        .class, ConfirmedPrivateTransfer.Ack.class, null),
    confirmed_text_message             (19, ConfirmedTextMessage            .class, null                              , null),
    reinitialize_device                (20, ReinitializeDevice              .class, null                              , null),
    
// Virtual Terminal Services
    vt_open                            (21, VtOpen                          .class, VtOpen.Ack                  .class, null),
    vt_close                           (22, VtClose                         .class, null                              , null),
    vt_data                            (23, VtData                          .class, VtData.Ack                  .class, null);
    // @formatter:on

    private final byte                                         m_encoding;
    private final Class<? extends ConfirmedServiceRequest>     m_request;
    private final Class<? extends ConfirmedServiceResponse<?>> m_response;
    private final Class<? extends ConfirmedServiceResponse<?>> m_error;

    ConfirmedServiceChoice(int encoding,
                           Class<? extends ConfirmedServiceRequest> request,
                           Class<? extends ConfirmedServiceResponse<?>> response,
                           Class<? extends ConfirmedServiceResponse<?>> error)
    {
        m_encoding = (byte) encoding;
        m_request  = request;
        m_response = response;
        m_error    = error;
    }

    @HandlerForDecoding
    public static ConfirmedServiceChoice parse(byte value)
    {
        for (ConfirmedServiceChoice t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }

    public static ConfirmedServiceChoice lookupRequest(Class<? extends ConfirmedServiceRequest> clz)
    {
        for (ConfirmedServiceChoice t : values())
        {
            if (t.m_request == clz)
            {
                return t;
            }
        }

        return null;
    }

    public static ConfirmedServiceChoice lookupResponse(Class<? extends ConfirmedServiceResponse<?>> clz)
    {
        for (ConfirmedServiceChoice t : values())
        {
            if (t.m_response == clz)
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

    public Class<? extends ConfirmedServiceRequest> request()
    {
        return m_request;
    }

    public Class<? extends ConfirmedServiceResponse<?>> response()
    {
        return m_response;
    }

    public Class<? extends ConfirmedServiceResponse<?>> error()
    {
        return m_error;
    }
}
