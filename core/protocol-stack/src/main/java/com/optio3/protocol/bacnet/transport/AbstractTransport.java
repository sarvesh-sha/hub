/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.transport;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.logging.RedirectingLogger;
import com.optio3.logging.Severity;
import com.optio3.protocol.bacnet.BACnetManager;
import com.optio3.protocol.bacnet.model.pdu.NetworkPDU;
import com.optio3.protocol.model.transport.TransportAddress;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.BufferUtils;

public abstract class AbstractTransport extends RedirectingLogger
{
    // Just a marker for the logger.
    public static class Decoding
    {
    }

    public static final Logger LoggerInstance            = new Logger(AbstractTransport.class);
    public static final Logger LoggerInstanceForDecoding = LoggerInstance.createSubLogger(Decoding.class);

    public AbstractTransport()
    {
        super(LoggerInstance);
    }

    //--//

    @Override
    public abstract boolean equals(Object obj);

    public abstract void start(BACnetManager manager);

    public abstract void close();

    public abstract void setSourceAddress(NetworkPDU npdu,
                                          Integer networkNumber);

    public abstract boolean canSend(TransportAddress destination);

    public abstract int sendDirect(OutputBuffer ob,
                                   TransportAddress destination);

    public abstract void sendBroadcast(OutputBuffer ob);

    public abstract boolean canReachAddress(InetAddress address);

    //--//

    public abstract void registerBBMD(TransportAddress ta);

    public abstract void enableBBMDs();

    public abstract CompletableFuture<Void> disableBBMDs() throws
                                                           Exception;

    //--//

    protected void reportDecodeError(byte[] buf,
                                     int length,
                                     Exception e)
    {
        if (LoggerInstanceForDecoding.isEnabled(Severity.DebugVerbose))
        {
            LoggerInstanceForDecoding.debugVerbose("Encountered an exception while processing incoming packet of length %d: %s", length, e);

            dumpBuffer(LoggerInstanceForDecoding, Severity.DebugVerbose, buf, length);
        }
        else if (LoggerInstanceForDecoding.isEnabled(Severity.Debug))
        {
            LoggerInstanceForDecoding.debug("Encountered an exception while processing incoming packet of length %d: %s", length, e.getMessage());

            dumpBuffer(LoggerInstanceForDecoding, Severity.Debug, buf, length);
        }
    }

    protected void dumpBuffer(ILogger logger,
                              Severity level,
                              byte[] buf,
                              int length)
    {
        if (logger.isEnabled(level))
        {
            BufferUtils.convertToHex(buf, 0, length, 32, true, (line) -> logger.log(null, level, null, line));
        }
    }
}
