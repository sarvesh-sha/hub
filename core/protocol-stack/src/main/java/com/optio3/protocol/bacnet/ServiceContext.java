/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet;

import java.util.List;

import com.optio3.logging.RedirectingLogger;
import com.optio3.protocol.bacnet.model.enums.ConfirmedServiceChoice;
import com.optio3.protocol.bacnet.model.linklayer.BaseVirtualLinkLayer;
import com.optio3.protocol.bacnet.model.pdu.NetworkPDU;
import com.optio3.protocol.bacnet.model.pdu.application.AbortPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ApplicationPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ComplexAckPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ConfirmedRequestPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ErrorPDU;
import com.optio3.protocol.bacnet.model.pdu.application.RejectPDU;
import com.optio3.protocol.bacnet.model.pdu.application.SegmentAckPDU;
import com.optio3.protocol.bacnet.model.pdu.network.NetworkMessagePDU;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.bacnet.model.pdu.request.UnconfirmedServiceRequest;
import com.optio3.protocol.model.transport.TransportAddress;

public class ServiceContext extends RedirectingLogger
{
    public final BACnetManager    owner;
    public final TransportAddress source;
    public final int              packetLength;
    public       NetworkPDU       npdu;
    public       TransportAddress originatingAddress;

    ServiceContext(BACnetManager owner,
                   TransportAddress source,
                   int packetLength)
    {
        super(owner);

        this.owner        = owner;
        this.source       = source;
        this.packetLength = packetLength;
    }

    public TransportAddress getEffectiveAddress()
    {
        return originatingAddress != null ? originatingAddress : source;
    }

    //--//

    public void processNetworkRequest(NetworkPDU npdu)
    {
        this.npdu = npdu;

        if (npdu.message_type != null)
        {
            NetworkMessagePDU pdu = npdu.decodeNetworkMessageHeader();
            pdu.dispatch(this);
        }
        else
        {
            try (ApplicationPDU pdu = npdu.decodeApplicationHeader())
            {
                pdu.dispatch(this);
            }
        }
    }

    public void processNetworkMessageRequest(NetworkMessagePDU req)
    {
        owner.processRequest(req, this);
    }

    public void processLinkLayerRequest(BaseVirtualLinkLayer req)
    {
        owner.processRequest(req, this);
    }

    //--//

    public void processRequestChunk(ConfirmedRequestPDU pdu)
    {
        debug("processRequestChunk: %s", pdu.serviceChoice);

        List<IApplicationPduListener> listeners = owner.locateListeners(source, npdu.getSourceAddress(), pdu.invokeId.unbox());
        for (IApplicationPduListener listener : listeners)
        {
            listener.processRequestChunk(this, pdu);
        }
    }

    public void processResponseChunk(ComplexAckPDU pdu)
    {
        debug("processResponseChunk: %s", pdu.serviceChoice);

        List<IApplicationPduListener> listeners = owner.locateListeners(source, npdu.getSourceAddress(), pdu.invokeId.unbox());
        for (IApplicationPduListener listener : listeners)
        {
            listener.processResponseChunk(this, pdu);
        }
    }

    public void processRequest(UnconfirmedServiceRequest req)
    {
        owner.processRequest(req, this);
    }

    public void processRequest(ConfirmedServiceRequest req,
                               byte invokeId)
    {
        debug("processRequest: %s => 0x%2x", req.getClass(), invokeId & 0xFF);
    }

    //--//

    public void processResponse(ConfirmedServiceResponse<?> res,
                                byte invokeId,
                                ConfirmedServiceChoice serviceChoice)
    {
        debug("processResponse: 0x%02x => %s %s", invokeId & 0xFF, serviceChoice, res != null ? res.getClass() : "<none>");

        List<IApplicationPduListener> listeners = owner.locateListeners(source, npdu.getSourceAddress(), invokeId);
        for (IApplicationPduListener listener : listeners)
        {
            listener.processResponse(this, res);
        }
    }

    public void processSegmentAck(SegmentAckPDU pdu)
    {
        debug("processSegmentAck: %d %s %s - %d %d", pdu.invokeId.unboxUnsigned(), pdu.server, pdu.negativeAck, pdu.sequenceNumber.unboxUnsigned(), pdu.proposedWindowSize.unboxUnsigned());

        List<IApplicationPduListener> listeners = owner.locateListeners(source, npdu.getSourceAddress(), pdu.invokeId.unbox());
        for (IApplicationPduListener listener : listeners)
        {
            listener.processSegmentAck(this, pdu);
        }
    }

    public void processReject(RejectPDU pdu)
    {
        debug("processReject: %d => %s", pdu.invokeId.unboxUnsigned(), pdu.rejectReason);

        List<IApplicationPduListener> listeners = owner.locateListeners(source, npdu.getSourceAddress(), pdu.invokeId.unbox());
        for (IApplicationPduListener listener : listeners)
        {
            listener.processReject(this, pdu);
        }
    }

    public void processAbort(AbortPDU pdu)
    {
        debug("processAbort: %d => %s", pdu.invokeId.unboxUnsigned(), pdu.abortReason);

        List<IApplicationPduListener> listeners = owner.locateListeners(source, npdu.getSourceAddress(), pdu.invokeId.unbox());
        for (IApplicationPduListener listener : listeners)
        {
            listener.processAbort(this, pdu);
        }
    }

    public void processError(ErrorPDU pdu)
    {
        debug("processError: %d => %s", pdu.invokeId.unboxUnsigned(), pdu.errorChoice);

        List<IApplicationPduListener> listeners = owner.locateListeners(source, npdu.getSourceAddress(), pdu.invokeId.unbox());
        for (IApplicationPduListener listener : listeners)
        {
            listener.processError(this, pdu);
        }
    }
}
