/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus.transport;

import java.util.concurrent.CompletableFuture;

import com.optio3.logging.Logger;
import com.optio3.logging.RedirectingLogger;
import com.optio3.logging.Severity;
import com.optio3.protocol.modbus.ModbusManager;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.BufferUtils;
import com.optio3.util.function.BiConsumerWithException;

public abstract class AbstractTransport extends RedirectingLogger
{
    public static final Logger LoggerInstance = new Logger(AbstractTransport.class);

    protected AbstractTransport()
    {
        super(LoggerInstance);
    }

    public abstract void start(ModbusManager manager,
                               BiConsumerWithException<Boolean, Boolean> notifyTransportState);

    public abstract void close() throws
                                 Exception;

    public abstract CompletableFuture<Void> send(Integer destination,
                                                 int transactionId,
                                                 OutputBuffer ob);

    //--//

    protected void dumpBuffer(Severity level,
                              byte[] buf,
                              int length)
    {
        if (LoggerInstance.isEnabled(level))
        {
            BufferUtils.convertToHex(buf, 0, length, 32, true, this::warn);
        }
    }
}
