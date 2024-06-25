/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet;

import com.optio3.protocol.bacnet.model.pdu.application.AbortPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ComplexAckPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ConfirmedRequestPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ErrorPDU;
import com.optio3.protocol.bacnet.model.pdu.application.RejectPDU;
import com.optio3.protocol.bacnet.model.pdu.application.SegmentAckPDU;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;

public interface IApplicationPduListener
{
    public void processRequestChunk(ServiceContext serviceContext,
                                    ConfirmedRequestPDU pdu);

    public void processResponseChunk(ServiceContext sc,
                                     ComplexAckPDU pdu);

    public void processSegmentAck(ServiceContext sc,
                                  SegmentAckPDU pdu);

    public void processResponse(ServiceContext sc,
                                ConfirmedServiceResponse<?> res);

    public void processReject(ServiceContext sc,
                              RejectPDU pdu);

    public void processAbort(ServiceContext sc,
                             AbortPDU pdu);

    public void processError(ServiceContext sc,
                             ErrorPDU pdu);
}
