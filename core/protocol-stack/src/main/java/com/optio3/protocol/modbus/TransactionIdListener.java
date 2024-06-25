/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus;

public class TransactionIdListener implements AutoCloseable
{
    private final ModbusManager           m_manager;
    private final Integer                 m_deviceIdentifier;
    private final int                     m_transactionId;
    private final IApplicationPduListener m_listener;

    TransactionIdListener(ModbusManager manager,
                          Integer deviceIdentifier,
                          int transactionId,
                          IApplicationPduListener listener)
    {
        m_manager = manager;
        m_deviceIdentifier = deviceIdentifier;
        m_transactionId = transactionId;
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

    public Integer getDeviceIdentifier()
    {
        return m_deviceIdentifier;
    }

    public int getTransactionId()
    {
        return m_transactionId;
    }
}
