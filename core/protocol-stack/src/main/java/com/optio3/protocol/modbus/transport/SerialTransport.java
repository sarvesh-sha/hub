/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus.transport;

import java.nio.file.ClosedFileSystemException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.interop.mediaaccess.SerialAccess;
import com.optio3.logging.Severity;
import com.optio3.protocol.common.CRC;
import com.optio3.protocol.common.ServiceWorker;
import com.optio3.protocol.common.ServiceWorkerWithWatchdog;
import com.optio3.protocol.modbus.FunctionType;
import com.optio3.protocol.modbus.ModbusManager;
import com.optio3.protocol.modbus.pdu.ApplicationPDU;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.BufferUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.BiConsumerWithException;

public final class SerialTransport extends AbstractTransport
{
    private class FrameDecoder
    {
        private final CRC            m_crcEngine;
        private final byte[]         m_frame    = new byte[512];
        private       int            m_framePos = 0;
        private       MonotonousTime m_timeout;
        private       byte           m_stationAddress;
        private       FunctionType   m_functionCode;

        FrameDecoder(CRC crcEngine)
        {
            m_crcEngine = crcEngine;
        }

        void reset()
        {
            m_framePos = 0;
            m_timeout  = null;
        }

        boolean push(byte c)
        {
            if (TimeUtils.isTimeoutExpired(m_timeout) || m_framePos >= m_frame.length)
            {
                m_framePos = 0;
                m_timeout  = TimeUtils.computeTimeoutExpiration(100, TimeUnit.MILLISECONDS);
            }

            m_frame[m_framePos] = c;

            switch (m_framePos++)
            {
                case 0:
                    m_stationAddress = c;
                    break;

                case 1:
                    m_functionCode = FunctionType.parse((byte) (c & 0x7F));
                    if (m_functionCode == null)
                    {
                        reset();
                        return false;
                    }
                    break;

                default:
                    if (m_framePos > 4) // Minimum message is 5 bytes.
                    {
                        //
                        // Unfortunately, requests and replies are ambiguous.
                        // We need to find the length, because the CRC follows the data.
                        // So we have to resort to trying and decoding one byte at a time.
                        //
                        if (isChecksumValid())
                        {
                            try (InputBuffer ib = InputBuffer.createFrom(m_frame, 1, m_framePos - 3))
                            {
                                ApplicationPDU.Response apdu = ApplicationPDU.decodeResponse(ib); // If this doesn't throw, it's a good message response.

                                notifyGoodMessage(m_stationAddress, apdu);
                                reset();
                                return true;
                            }
                            catch (Throwable t)
                            {
                                // Partial buffer...
                            }

                            try (InputBuffer ib = InputBuffer.createFrom(m_frame, 1, m_framePos - 3))
                            {
                                ApplicationPDU apdu = ApplicationPDU.decodeRequest(ib); // If this doesn't throw, it's a good message response.
                                notifyGoodMessage(m_stationAddress, apdu);
                                reset();
                                return true;
                            }
                            catch (Throwable t)
                            {
                                // Partial buffer...
                            }
                        }
                        else
                        {
                            notifyBadChecksum(m_frame, m_framePos);
                        }
                    }
                    break;
            }

            return false;
        }

        private boolean isChecksumValid()
        {
            int crcLo    = m_frame[m_framePos - 2] & 0xFF;
            int crcHi    = m_frame[m_framePos - 1] & 0xFF;
            int checksum = crcLo | (crcHi << 8);

            int crc = m_crcEngine.computeLittleEndian(0xFFFF, m_frame, 0, m_framePos - 2);
            return checksum == crc;
        }

        private void notifyBadChecksum(byte[] buffer,
                                       int length)
        {
            if (LoggerInstance.isEnabled(Severity.DebugObnoxious))
            {
                LoggerInstance.debugObnoxious("Frame with bad checksum:");

                BufferUtils.convertToHex(buffer, 0, length, 32, true, LoggerInstance::debugObnoxious);
            }
        }

        private void notifyGoodMessage(int stationAddress,
                                       ApplicationPDU apdu)
        {
            try
            {
                m_manager.processResponse(m_currentTransaction, stationAddress, apdu);
            }
            catch (Exception e)
            {
                warn("Encountered an exception while processing packet of type %s: %s", apdu.getClass(), e);
            }
        }
    }

    static final class Config
    {
        String port;
        int    baudRate = 9600;
    }

    //--//

    private final CRC    m_crcEngine = new CRC(0xA001);
    private final Config m_config;

    private ModbusManager                             m_manager;
    private BiConsumerWithException<Boolean, Boolean> m_notifyTransportState;

    private final FrameDecoder  m_decoder = new FrameDecoder(m_crcEngine);
    private final ServiceWorker m_serialWorker;
    private       SerialAccess  m_serialTransport;
    private       boolean       m_lastRxFailed;
    private       boolean       m_lastTxFailed;
    private       int           m_currentTransaction;

    SerialTransport(Config config)
    {
        m_config = config;

        m_serialWorker = new ServiceWorkerWithWatchdog(LoggerInstance, "Modbus Serial", 60, 2000, 30)
        {
            @Override
            protected void shutdown()
            {
                closeTransport();
            }

            @Override
            protected void fireWatchdog()
            {
                reportError("Modbus data flow stopped!");

                closeTransport();
            }

            @Override
            protected void worker()
            {
                byte[] input = new byte[512];

                while (canContinue())
                {
                    SerialAccess transport = m_serialTransport;
                    if (transport == null)
                    {
                        try
                        {
                            FirmwareHelper f = FirmwareHelper.get();

                            // This is weird. If we open the serial port once and change the speed, it doesn't stick. We have to close it and reopen it...
                            m_serialTransport = SerialAccess.openMultipleTimes(4, f.mapPort(m_config.port), m_config.baudRate, 8, 'N', 1);

                            if (m_notifyTransportState != null)
                            {
                                m_notifyTransportState.accept(true, false);
                            }

                            resetWatchdog();
                        }
                        catch (Throwable t)
                        {
                            reportFailure("Failed to start Modbus serial", t);

                            workerSleep(10000);
                        }
                    }
                    else
                    {
                        try
                        {
                            int len = transport.read(input, 1000);
                            if (len <= 0)
                            {
                                m_decoder.reset();

                                if (len < 0)
                                {
                                    closeTransport();
                                    workerSleep(500);
                                }
                            }
                            else
                            {
                                resetWatchdog();

                                m_lastRxFailed = false;

                                if (LoggerInstance.isEnabled(Severity.DebugVerbose))
                                {
                                    BufferUtils.convertToHex(input, 0, len, 32, true, (line) -> LoggerInstance.debugVerbose("RX: %s", line));
                                }

                                boolean progress = false;

                                for (int i = 0; i < len; i++)
                                {
                                    progress |= m_decoder.push(input[i]);
                                }

                                if (progress)
                                {
                                    reportErrorResolution("Modbus data flow resumed!");

                                    resetWatchdog();
                                }
                            }
                        }
                        catch (Throwable t)
                        {
                            if (!canContinue())
                            {
                                // The manager has been stopped, exit.
                                return;
                            }

                            Severity level;

                            if (t instanceof ClosedFileSystemException)
                            {
                                // Expected, due to watchdog.
                                level = Severity.Debug;
                            }
                            else if (!m_lastRxFailed)
                            {
                                m_lastRxFailed = true;
                                level          = Severity.Info;
                            }
                            else
                            {
                                level = Severity.Debug;
                            }

                            log(null, level, null, null, "Got an exception trying to receive message: %s", t);

                            closeTransport();

                            workerSleep(10000);
                        }
                    }
                }
            }
        };
    }

    private synchronized void closeTransport()
    {
        if (m_serialTransport != null)
        {
            try
            {
                m_serialTransport.close();

                if (m_notifyTransportState != null)
                {
                    m_notifyTransportState.accept(false, true);
                }
            }
            catch (Throwable t)
            {
                // Ignore failures.
            }

            m_serialTransport = null;
        }
    }

    //--//

    @Override
    public void start(ModbusManager manager,
                      BiConsumerWithException<Boolean, Boolean> notifyTransportState)
    {
        if (m_manager == null)
        {
            m_manager              = manager;
            m_notifyTransportState = notifyTransportState;

            m_serialWorker.start();
        }
    }

    @Override
    public void close() throws
                        Exception
    {
        m_serialWorker.close();

        m_manager              = null;
        m_notifyTransportState = null;
    }

    @Override
    public CompletableFuture<Void> send(Integer destination,
                                        int transactionId,
                                        OutputBuffer ob)
    {
        m_currentTransaction = transactionId;

        CompletableFuture<Void> res = new CompletableFuture<>();

        try (OutputBuffer ndpu = new OutputBuffer())
        {
            if (destination != null)
            {
                ndpu.emit1Byte((int) destination);
            }
            else
            {
                ndpu.emit1Byte(0xFF);
            }

            ndpu.emitNestedBlock(ob);

            byte[] partialBuf = ndpu.toByteArray();
            int    crc        = m_crcEngine.computeLittleEndian(0xFFFF, partialBuf, 0, partialBuf.length);
            ndpu.littleEndian = true;
            ndpu.emit2Bytes(crc);

            try
            {
                byte[] fullBuf = ndpu.toByteArray();

                if (LoggerInstance.isEnabled(Severity.DebugVerbose))
                {
                    BufferUtils.convertToHex(fullBuf, 0, fullBuf.length, 32, true, (line) -> LoggerInstance.debugVerbose("TX: %s", line));
                }

                final SerialAccess serialTransport = m_serialTransport;
                if (serialTransport != null)
                {
                    serialTransport.write(fullBuf, fullBuf.length);
                    m_lastTxFailed = false;
                }

                res.complete(null);
            }
            catch (Exception e)
            {
                Severity level;

                if (!m_lastTxFailed)
                {
                    m_lastTxFailed = true;
                    level          = Severity.Info;
                }
                else
                {
                    level = Severity.Debug;
                }

                log(null, level, null, null, "Got an exception trying to send message to %s : %s", destination, e);

                res.completeExceptionally(e);
            }
        }

        return res;
    }
}
