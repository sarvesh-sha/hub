/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.application;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.bacnet.model.enums.ConfirmedServiceChoice;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.model.bacnet.enums.BACnetAbortReason;
import com.optio3.protocol.model.bacnet.enums.BACnetRejectReason;
import com.optio3.serialization.ConditionalFieldSelector;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;

//
// | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
// |---|---|---|---|---|---|---|---|
// | PDU Type      |SEG|MOR| SA| 0 |
// |---|---|---|---|---|---|---|---|
// | 0 | Max Segs  | Max Resp      |
// |---|---|---|---|---|---|---|---|
// | Invoke ID                     |
// |---|---|---|---|---|---|---|---|
// | Sequence Number               | Only present if SEG = 1
// |---|---|---|---|---|---|---|---|
// | Proposed Window Size          | Only present if SEG = 1
// |---|---|---|---|---|---|---|---|
// | Service Choice                |
// |---|---|---|---|---|---|---|---|
// | Service Request               |
// |---|---|---|---|---|---|---|---|
//
public final class ConfirmedRequestPDU extends ApplicationPDU implements ConditionalFieldSelector
{
    public static final int MaxSegment_none = 0;
    public static final int MaxSegment_2    = 2;
    public static final int MaxSegment_4    = 4;
    public static final int MaxSegment_8    = 8;
    public static final int MaxSegment_16   = 16;
    public static final int MaxSegment_32   = 32;
    public static final int MaxSegment_64   = 64;
    public static final int MaxSegment_Any  = 128;

    private static final int c_maxSegment_none_enc = 0b000;
    private static final int c_maxSegment_2_enc    = 0b001;
    private static final int c_maxSegment_4_enc    = 0b010;
    private static final int c_maxSegment_8_enc    = 0b011;
    private static final int c_maxSegment_16_enc   = 0b100;
    private static final int c_maxSegment_32_enc   = 0b101;
    private static final int c_maxSegment_64_enc   = 0b110;
    private static final int c_maxSegment_Any_enc  = 0b111;

    //--//

    public static final int MaxAPDU_50   = 50;
    public static final int MaxAPDU_128  = 128;
    public static final int MaxAPDU_206  = 206;
    public static final int MaxAPDU_480  = 480;
    public static final int MaxAPDU_1024 = 1024;
    public static final int MaxAPDU_1476 = 1476;

    private static final int c_maxAPDU_50_enc   = 0b0000;
    private static final int c_maxAPDU_128_enc  = 0b0001;
    private static final int c_maxAPDU_206_enc  = 0b0010;
    private static final int c_maxAPDU_480_enc  = 0b0011;
    private static final int c_maxAPDU_1024_enc = 0b0100;
    private static final int c_maxAPDU_1476_enc = 0b0101;

    //--//

    @SerializationTag(number = 0, bitOffset = 3, width = 1)
    public boolean segmentedMessage;

    @SerializationTag(number = 0, bitOffset = 2, width = 1)
    public boolean moreFollows;

    @SerializationTag(number = 0, bitOffset = 1, width = 1)
    public boolean segmentedResponseAccepted;

    @SerializationTag(number = 1, bitOffset = 4, width = 3)
    private int m_maxSegmentsAccepted;

    @SerializationTag(number = 1, bitOffset = 0, width = 3)
    private int m_maxApduLengthAccepted;

    @SerializationTag(number = 2)
    public Unsigned8 invokeId;

    @SerializationTag(number = 3)
    public Unsigned8 sequenceNumber; // Only present if segmentedMessage is true.

    @SerializationTag(number = 4)
    public Unsigned8 proposedWindowSize; // Only present if segmentedMessage is true.

    @SerializationTag(number = 5, width = 8)
    public ConfirmedServiceChoice serviceChoice;

    //--//

    public ConfirmedRequestPDU(InputBuffer buffer)
    {
        super(buffer);
    }

    //--//

    @Override
    public boolean shouldEncode(String fieldName)
    {
        switch (fieldName)
        {
            case "sequenceNumber":
            case "proposedWindowSize":
                return segmentedMessage;
        }

        return true;
    }

    @Override
    public boolean shouldDecode(String fieldName)
    {
        switch (fieldName)
        {
            case "sequenceNumber":
            case "proposedWindowSize":
                return segmentedMessage;
        }

        return true;
    }

    //--//

    public int getMaxSegmentsAccepted()
    {
        switch (m_maxSegmentsAccepted)
        {
            //@formatter:off
            case c_maxSegment_none_enc: return MaxSegment_none; // Unspecified number of segments accepted.
            case c_maxSegment_2_enc:    return MaxSegment_2;
            case c_maxSegment_4_enc:    return MaxSegment_4;
            case c_maxSegment_8_enc:    return MaxSegment_8;
            case c_maxSegment_16_enc:   return MaxSegment_16;
            case c_maxSegment_32_enc:   return MaxSegment_32;
            case c_maxSegment_64_enc:   return MaxSegment_64;
            case c_maxSegment_Any_enc:  return MaxSegment_Any; // Greater than 64 segments accepted.
            default:                   return MaxSegment_none; // Minimum common denominator...
            //@formatter:on
        }
    }

    public void setMaxSegmentsAccepted(int maxSegment)
    {
        //@formatter:off
        if      (maxSegment >= MaxSegment_Any) m_maxSegmentsAccepted = c_maxSegment_Any_enc;
        else if (maxSegment >= MaxSegment_64 ) m_maxSegmentsAccepted = c_maxSegment_64_enc;
        else if (maxSegment >= MaxSegment_32 ) m_maxSegmentsAccepted = c_maxSegment_32_enc;
        else if (maxSegment >= MaxSegment_16 ) m_maxSegmentsAccepted = c_maxSegment_16_enc;
        else if (maxSegment >= MaxSegment_8  ) m_maxSegmentsAccepted = c_maxSegment_8_enc;
        else if (maxSegment >= MaxSegment_4  ) m_maxSegmentsAccepted = c_maxSegment_4_enc;
        else if (maxSegment >= MaxSegment_2  ) m_maxSegmentsAccepted = c_maxSegment_2_enc;
        else                                   m_maxSegmentsAccepted = c_maxSegment_none_enc;
        //@formatter:on
    }

    public int getMaxApduLengthAccepted()
    {
        //@formatter:off
        switch (m_maxApduLengthAccepted)
        {
            case c_maxAPDU_50_enc:   return MaxAPDU_50; // Up to MinimumMessageSize (50 octets)
            case c_maxAPDU_128_enc:  return MaxAPDU_128;
            case c_maxAPDU_206_enc:  return MaxAPDU_206; // fits in a LonTalk frame
            case c_maxAPDU_480_enc:  return MaxAPDU_480; // fits in an ARCNET frame
            case c_maxAPDU_1024_enc: return MaxAPDU_1024;
            case c_maxAPDU_1476_enc: return MaxAPDU_1476; // fits in an Ethernet frame
            default:                 return MaxAPDU_50; // Minimum common denominator...
        }
        //@formatter:on
    }

    public void setMaxApduLengthAccepted(int maxApdu)
    {
        //@formatter:off
        if      (maxApdu >= MaxAPDU_1476) m_maxApduLengthAccepted = c_maxAPDU_1476_enc;
        else if (maxApdu >= MaxAPDU_1024) m_maxApduLengthAccepted = c_maxAPDU_1024_enc;
        else if (maxApdu >= MaxAPDU_480 ) m_maxApduLengthAccepted = c_maxAPDU_480_enc;
        else if (maxApdu >= MaxAPDU_206 ) m_maxApduLengthAccepted = c_maxAPDU_206_enc;
        else if (maxApdu >= MaxAPDU_128 ) m_maxApduLengthAccepted = c_maxAPDU_128_enc;
        else                              m_maxApduLengthAccepted = c_maxAPDU_50_enc;
        //@formatter:on
    }

    //--//

    public RejectPDU createReject(BACnetRejectReason reason)
    {
        RejectPDU pdu = new RejectPDU(null);
        pdu.invokeId     = invokeId;
        pdu.rejectReason = reason;
        return pdu;
    }

    public AbortPDU createAbort(BACnetAbortReason reason)
    {
        AbortPDU pdu = new AbortPDU(null);
        pdu.invokeId    = invokeId;
        pdu.server      = true;
        pdu.abortReason = reason;
        return pdu;
    }

    public ServiceCommon allocateError()
    {
        ErrorPDU pdu = new ErrorPDU(null);
        pdu.invokeId    = invokeId;
        pdu.errorChoice = serviceChoice;

        return Reflection.newInstance(serviceChoice.error());
    }

    public <T extends ConfirmedServiceRequest> ConfirmedServiceResponse<T> prepareResponse()
    {
        Class<? extends ConfirmedServiceResponse<?>> responseClz = serviceChoice.response();
        if (responseClz == null)
        {
            SimpleAckPDU pdu2 = new SimpleAckPDU(null);
            pdu2.invokeId      = invokeId;
            pdu2.serviceChoice = serviceChoice;

            responseClz = ConfirmedServiceResponse.NoDataReply.class;
        }
        else
        {
            ComplexAckPDU pdu2 = new ComplexAckPDU(null);
            pdu2.invokeId      = invokeId;
            pdu2.serviceChoice = serviceChoice;
        }

        @SuppressWarnings("unchecked") ConfirmedServiceResponse<T> res = (ConfirmedServiceResponse<T>) Reflection.newInstance(responseClz);

        return res;
    }

    //--//

    @Override
    protected ServiceCommon allocatePayload()
    {
        return Reflection.newInstance(serviceChoice.request());
    }

    @Override
    public void dispatch(ServiceContext sc)
    {
        if (segmentedMessage)
        {
            sc.processRequestChunk(this);
            return;
        }

        ConfirmedServiceRequest request = (ConfirmedServiceRequest) decodePayload();
        sc.processRequest(request, invokeId.unbox());
    }
}
