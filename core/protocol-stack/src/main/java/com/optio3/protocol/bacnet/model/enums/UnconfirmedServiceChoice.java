/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.enums;

import com.optio3.protocol.bacnet.model.pdu.request.UnconfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.IAm;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.IHave;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.TimeSynchronization;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.UnconfirmedCOVNotification;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.UnconfirmedCOVNotificationMultiple;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.UnconfirmedEventNotification;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.UnconfirmedPrivateTransfer;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.UnconfirmedTextMessage;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.UtcTimeSynchronization;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.WhoHas;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.WhoIs;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.WriteGroup;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;

public enum UnconfirmedServiceChoice
{
    // @formatter:off
    i_am                                 (0 , IAm                               .class),
    i_have                               (1 , IHave                             .class),
    unconfirmed_cov_notification         (2 , UnconfirmedCOVNotification        .class),
    unconfirmed_event_notification       (3 , UnconfirmedEventNotification      .class),
    unconfirmed_private_transfer         (4 , UnconfirmedPrivateTransfer        .class),
    unconfirmed_text_message             (5 , UnconfirmedTextMessage            .class),
    time_synchronization                 (6 , TimeSynchronization               .class),
    who_has                              (7 , WhoHas                            .class),
    who_is                               (8 , WhoIs                             .class),
    utc_time_synchronization             (9 , UtcTimeSynchronization            .class),
    write_group                          (10, WriteGroup                        .class),
    unconfirmed_cov_notification_multiple(11, UnconfirmedCOVNotificationMultiple.class);
    // @formatter:on

    private final byte                                       m_encoding;
    private final Class<? extends UnconfirmedServiceRequest> m_request;

    UnconfirmedServiceChoice(int encoding,
                             Class<? extends UnconfirmedServiceRequest> request)
    {
        m_encoding = (byte) encoding;
        m_request  = request;
    }

    @HandlerForDecoding
    public static UnconfirmedServiceChoice parse(byte value)
    {
        for (UnconfirmedServiceChoice t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }

    public static UnconfirmedServiceChoice lookup(Class<? extends UnconfirmedServiceRequest> clz)
    {
        for (UnconfirmedServiceChoice t : values())
        {
            if (t.m_request == clz)
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

    public Class<? extends UnconfirmedServiceRequest> request()
    {
        return m_request;
    }
}
