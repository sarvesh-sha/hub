/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import java.util.concurrent.TimeUnit;

import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.interop.mediaaccess.SerialAccess;
import com.optio3.logging.Severity;
import com.optio3.protocol.common.ServiceWorkerWithWatchdog;
import com.optio3.protocol.model.ipn.objects.bluesky.BaseBlueSkyObjectModel;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.BufferUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public final class WorkerForIpn extends ServiceWorkerWithWatchdog implements IpnWorker
{
    private enum DecoderState
    {
        Idle,
        Success,
        Failure,
    }

    private class DecoderImpl extends FrameDecoder
    {
        private DecoderState   m_currentState = DecoderState.Idle;
        private MonotonousTime m_deadlineForFailure;

        private Class<? extends BaseBlueSkyObjectModel> m_lastMessageClass;
        private int                                     m_stuckControllerCounters;
        private int                                     m_stuckControllerReportDelay;
        private MonotonousTime                          m_stuckControllerReport;
        private boolean                                 m_stuckControllerReported;

        @Override
        protected void notifyBadChecksum(byte[] buffer,
                                         int length)
        {
            if (IpnManager.LoggerInstance.isEnabled(Severity.Debug))
            {
                IpnManager.LoggerInstance.debug("Frame with bad checksum:");

                BufferUtils.convertToHex(buffer, 0, length, 32, true, IpnManager.LoggerInstance::debug);
            }
        }

        @Override
        protected void notifyBadMessage(MessageCode code,
                                        int destinationAddress,
                                        int sourceAddress,
                                        BaseBlueSkyObjectModel val)
        {
            if (IpnManager.LoggerInstance.isEnabled(Severity.Debug))
            {
                IpnManager.LoggerInstance.debug("Frame with bad message: %s", ObjectMappers.prettyPrintAsJson(val));
            }
        }

        @Override
        protected void notifyGoodMessage(MessageCode code,
                                         int destinationAddress,
                                         int sourceAddress,
                                         BaseBlueSkyObjectModel val)
        {
            transitionToSuccess();

            if (val != null)
            {
                m_manager.recordValue(val);

                Class<? extends BaseBlueSkyObjectModel> clz = val.getClass();
                if (clz != m_lastMessageClass)
                {
                    if (m_stuckControllerReported)
                    {
                        IpnManager.LoggerInstance.warn("BlueSky Controller back to normal, after %d duplicate messages!!!", m_stuckControllerCounters);

                        m_stuckControllerReported = false;
                    }

                    m_lastMessageClass = clz;
                    m_stuckControllerCounters = 0;
                    m_stuckControllerReport = null;
                    m_stuckControllerReportDelay = 30;
                }
                else
                {
                    m_stuckControllerCounters++;

                    if (m_stuckControllerReport == null)
                    {
                        m_stuckControllerReport = TimeUtils.computeTimeoutExpiration(m_stuckControllerReportDelay, TimeUnit.SECONDS);
                        m_stuckControllerReportDelay = Math.min(m_stuckControllerReportDelay * 2, 8 * 3600);
                    }
                    else if (TimeUtils.isTimeoutExpired(m_stuckControllerReport))
                    {
                        IpnManager.LoggerInstance.warn("BlueSky Controller is stuck (%d duplicate messages)!!!", m_stuckControllerCounters);
                        m_stuckControllerReport = null;
                        m_stuckControllerReported = true;
                    }
                }
            }
        }

        private void transitionToSuccess()
        {
            m_deadlineForFailure = null;

            switch (m_currentState)
            {
                case Failure:
                    IpnManager.LoggerInstance.info("IPN data flow resumed!");
                    // Fallthrough

                case Idle:
                    m_currentState = DecoderState.Success;
                    break;
            }
        }

        private void transitionToFailure()
        {
            switch (m_currentState)
            {
                case Idle:
                case Success:
                    if (m_deadlineForFailure == null)
                    {
                        m_deadlineForFailure = TimeUtils.computeTimeoutExpiration(15, TimeUnit.SECONDS);
                    }

                    if (TimeUtils.isTimeoutExpired(m_deadlineForFailure))
                    {
                        IpnManager.LoggerInstance.error("IPN not receiving any data...");

                        m_currentState = DecoderState.Failure;
                    }
                    break;
            }
        }
    }

    //--//

    private final IpnManager   m_manager;
    private final String       m_ipnPort;
    private final int          m_ipnBaudrate;
    private final boolean      m_ipnInvert;
    private final DecoderImpl  m_decoder = new DecoderImpl();
    private       SerialAccess m_transport;

    public WorkerForIpn(IpnManager manager,
                        String ipnPort,
                        int ipnBaudrate,
                        boolean ipnInvert)
    {
        super(IpnManager.LoggerInstance, "IPN", 30, 2000, 30);

        m_manager = manager;
        m_ipnPort = ipnPort;
        m_ipnBaudrate = ipnBaudrate;
        m_ipnInvert = ipnInvert;
    }

    @Override
    public void startWorker()
    {
        start();
    }

    @Override
    public void stopWorker()
    {
        stop();
    }

    @Override
    public <T> T accessSubManager(Class<T> clz)
    {
        return null;
    }

    //--//

    @Override
    protected void shutdown()
    {
        closeTransport();
    }

    @Override
    protected void fireWatchdog()
    {
        m_decoder.transitionToFailure();

        closeTransport();
    }

    @Override
    protected void worker()
    {
        byte[] input = new byte[512];

        while (canContinue())
        {
            SerialAccess sa = m_transport;
            if (sa == null)
            {
                try
                {
                    FirmwareHelper f = FirmwareHelper.get();
                    f.selectPort(m_ipnPort, FirmwareHelper.PortFlavor.RS485, m_ipnInvert, false);

                    if (!f.mightBePresent(m_ipnPort, m_ipnInvert))
                    {
                        throw Exceptions.newRuntimeException("Device '%s' not found", m_ipnPort);
                    }

                    int ipnBaudrate = m_ipnBaudrate;
                    if (ipnBaudrate == 0)
                    {
                        ipnBaudrate = 33400;
                    }

                    // This is weird. If we open the serial port once and change the speed, it doesn't stick. We have to close it and reopen it...
                    m_transport = SerialAccess.openMultipleTimes(4, f.mapPort(m_ipnPort), ipnBaudrate, 8, 'N', 1);

                    m_manager.updateDiscoveryLatency(4, TimeUnit.SECONDS);

                    m_manager.notifyTransport(m_ipnPort, true, false);
                    reportErrorResolution("Reconnected to IPN!");
                    resetWatchdog();
                }
                catch (Throwable t)
                {
                    m_decoder.transitionToFailure();
                    reportFailure("Failed to start IPN", t);

                    workerSleep(10000);
                }
            }
            else
            {
                try
                {
                    int len = sa.read(input, 1000);
                    if (len <= 0)
                    {
                        m_decoder.reset();
                        m_decoder.transitionToFailure();

                        if (len < 0)
                        {
                            closeTransport();
                            workerSleep(500);
                        }
                    }
                    else
                    {
                        if (IpnManager.LoggerInstance.isEnabled(Severity.DebugVerbose))
                        {
                            BufferUtils.convertToHex(input, 0, len, 32, true, IpnManager.LoggerInstance::debugVerbose);
                        }

                        boolean progress = false;

                        for (int i = 0; i < len; i++)
                        {
                            progress |= m_decoder.push(input[i]);
                        }

                        if (progress)
                        {
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

                    m_decoder.transitionToFailure();
                    reportDebug("Received error: %s", t);

                    closeTransport();

                    m_decoder.reset();

                    workerSleep(10000);
                }
            }
        }
    }

    private synchronized void closeTransport()
    {
        if (m_transport != null)
        {
            try
            {
                m_transport.close();
            }
            catch (Throwable t)
            {
                // Ignore failures.
            }

            m_transport = null;
        }

        m_manager.notifyTransport(m_ipnPort, false, true);
    }
}