/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet;

import java.util.Objects;

import com.optio3.protocol.model.bacnet.BACnetAddress;
import com.optio3.protocol.model.transport.TransportAddress;

public class InvokeIdListener implements AutoCloseable
{
    private final BACnetManager           m_manager;
    private final DeviceIdentity          m_target;
    private final byte                    m_invokeId;
    private final IApplicationPduListener m_listener;

    InvokeIdListener(BACnetManager manager,
                     DeviceIdentity target,
                     byte invokeId,
                     IApplicationPduListener listener)
    {
        m_manager  = manager;
        m_target   = target;
        m_invokeId = invokeId;
        m_listener = listener;
    }

    @Override
    public void close()
    {
        m_manager.removeListener(this);
    }

    IApplicationPduListener getListener()
    {
        return m_listener;
    }

    public DeviceIdentity getTarget()
    {
        return m_target;
    }

    public byte getInvokeId()
    {
        return m_invokeId;
    }

    public boolean isMatch(TransportAddress transportAddress,
                           BACnetAddress bacnetAddress)
    {
        if (bacnetAddress != null)
        {
            if (Objects.equals(bacnetAddress, m_target.getBACnetAddress()))
            {
                return true;
            }
        }

        return Objects.equals(transportAddress, m_target.getTransportAddress());
    }
}
