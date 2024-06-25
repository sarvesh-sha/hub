/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus.transport;

import java.io.InputStream;
import java.net.PortUnreachableException;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Severity;
import com.optio3.protocol.modbus.ModbusManager;
import com.optio3.protocol.modbus.pdu.ApplicationPDU;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.function.BiConsumerWithException;
import org.apache.commons.io.IOUtils;

public final class TcpTransport extends AbstractTransport
{
    static final class Config
    {
        String address;
        int    port = 502; // Default Modbus port.
    }

    //--//

    private final Object m_lock = new Object();
    private final Config m_config;

    private ModbusManager                             m_manager;
    private BiConsumerWithException<Boolean, Boolean> m_notifyTransportState;
    private boolean                                   m_shutdown;

    private Socket m_socket;

    private boolean m_lastSendFailed;

    private Thread m_receiveWorker;

    TcpTransport(Config config)
    {
        m_config = config;
    }

    //--//

    @Override
    public void start(ModbusManager manager,
                      BiConsumerWithException<Boolean, Boolean> notifyTransportState)
    {
        if (m_manager != null)
        {
            return;
        }

        m_manager              = manager;
        m_notifyTransportState = notifyTransportState;

        m_receiveWorker = new Thread(this::worker);
        m_receiveWorker.setName("Modbus worker");
        m_receiveWorker.start();
    }

    @Override
    public void close()
    {
        Thread receiveWorker;

        m_shutdown = true;

        closeSocket();

        synchronized (m_lock)
        {
            receiveWorker   = m_receiveWorker;
            m_receiveWorker = null;
        }

        if (receiveWorker != null)
        {
            try
            {
                receiveWorker.join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        m_manager              = null;
        m_notifyTransportState = null;
    }

    @Override
    public CompletableFuture<Void> send(Integer destination,
                                        int transactionId,
                                        OutputBuffer ob)
    {
        CompletableFuture<Void> res = new CompletableFuture<>();

        try (OutputBuffer ndpu = new OutputBuffer())
        {
            ndpu.emit2Bytes(transactionId);
            ndpu.emit2Bytes(0); // Protocol Identifier
            ndpu.emit2Bytes(ob.size() + 1); // Include Unit Identifier in the length;

            if (destination != null)
            {
                ndpu.emit1Byte((int) destination);
            }
            else
            {
                ndpu.emit1Byte(0xFF);
            }

            ndpu.emitNestedBlock(ob);

            try
            {
                Socket socket = ensureSocket();
                socket.getOutputStream()
                      .write(ndpu.toByteArray());

                res.complete(null);
            }
            catch (Exception e)
            {
                closeSocket();

                Severity level;
                String   fmt;

                if (!m_lastSendFailed)
                {
                    m_lastSendFailed = true;
                    level            = Severity.Info;
                }
                else
                {
                    level = Severity.Debug;
                }

                if (e instanceof PortUnreachableException)
                {
                    fmt = "Can't send message to %s, port unreachable : %s";
                }
                else
                {
                    fmt = "Received an exception trying to send message to %s : %s";
                }

                log(null, level, null, null, fmt, destination, e);

                res.completeExceptionally(e);
            }

            return res;
        }
    }

    //--//

    private Socket ensureSocket()
    {
        synchronized (m_lock)
        {
            if (m_shutdown)
            {
                throw new RuntimeException("Shutdown");
            }

            if (m_socket != null && m_socket.isClosed())
            {
                m_socket = null;
            }

            if (m_socket == null)
            {
                try
                {
                    m_socket = new Socket(m_config.address, m_config.port);

                    if (m_notifyTransportState != null)
                    {
                        m_notifyTransportState.accept(true, false);
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            return m_socket;
        }
    }

    private void closeSocket()
    {
        Socket socket;

        synchronized (m_lock)
        {
            socket = m_socket;

            m_socket = null;
        }

        if (socket != null)
        {
            try
            {
                socket.close();

                if (m_notifyTransportState != null)
                {
                    m_notifyTransportState.accept(false, true);
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private void worker()
    {
        final int c_MaxPacketSize = 64 * 1024;

        byte[] buf                  = new byte[c_MaxPacketSize];
        int    sleepBetweenFailures = 1;

        while (!m_shutdown)
        {
            InputBuffer ib;
            int         transactionId;
            int         protocolId;
            int         unitIdentifier;
            int         length;

            try
            {
                Socket      socket = ensureSocket();
                InputStream stream = socket.getInputStream();

                IOUtils.readFully(stream, buf, 0, 7);

                try (InputBuffer npdu = InputBuffer.createFrom(buf, 0, 7))
                {
                    transactionId  = npdu.read2BytesUnsigned();
                    protocolId     = npdu.read2BytesUnsigned();
                    length         = npdu.read2BytesUnsigned();
                    unitIdentifier = npdu.read1ByteUnsigned();
                }

                IOUtils.readFully(stream, buf, 0, length - 1);
                ib = InputBuffer.createFrom(buf, 0, length - 1);
            }
            catch (Exception e)
            {
                closeSocket();

                if (m_shutdown)
                {
                    // The manager has been stopped, exit.
                    return;
                }

                Executors.safeSleep(sleepBetweenFailures);

                // Exponential backoff if something goes wrong.
                sleepBetweenFailures = 2 * sleepBetweenFailures;
                continue;
            }

            sleepBetweenFailures = 1;
            debugVerbose("Received TCP packet: %d", ib.size());
            dumpBuffer(Severity.DebugVerbose, buf, ib.size());

            try
            {
                ApplicationPDU apdu = ApplicationPDU.decodeResponse(ib);

                m_manager.processResponse(transactionId, unitIdentifier, apdu);
            }
            catch (Exception e)
            {
                warn("Encountered an exception while processing packet of length %d: %s", ib.size(), e);

                dumpBuffer(Severity.Warn, buf, ib.size());
            }

            ib.close();
        }
    }
}
