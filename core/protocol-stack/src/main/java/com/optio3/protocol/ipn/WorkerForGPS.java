/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.interop.mediaaccess.SerialAccess;
import com.optio3.protocol.common.ServiceWorkerWithWatchdog;
import com.optio3.protocol.gps.NmeaDecoder;
import com.optio3.protocol.model.ipn.objects.IpnLocation;
import com.optio3.protocol.model.ipn.objects.IpnSatellite;
import com.optio3.protocol.model.ipn.objects.IpnSatelliteFix;

public final class WorkerForGPS extends ServiceWorkerWithWatchdog implements IpnWorker
{
    private final IpnManager   m_manager;
    private final String       m_gpsPort;
    private       SerialAccess m_transport;

    private final NmeaDecoder m_decoder = new NmeaDecoder()
    {
        private final IpnLocation m_state = new IpnLocation();

        @Override
        protected void reportFix(int fixMode,
                                 Set<Integer> fixSet,
                                 double pdop,
                                 double hdop,
                                 double vdop)
        {
            IpnSatelliteFix fix = new IpnSatelliteFix();
            switch (fixMode)
            {
                case 2:
                    fix.fixMode = IpnSatelliteFix.FixMode.TwoD;
                    break;

                case 3:
                    fix.fixMode = IpnSatelliteFix.FixMode.ThreeD;
                    break;

                default:
                    fix.fixMode = IpnSatelliteFix.FixMode.Unavailable;
                    break;
            }

            fix.fixSet = fixSet;
            fix.pdop   = pdop;
            fix.hdop   = hdop;
            fix.vdop   = vdop;

            LoggerInstance.debug("Fix %s: %s : pdop=%f hdop=%f vdop=%f", fix.fixMode, fixSet, pdop, hdop, vdop);

            m_manager.recordValue(fix);
        }

        @Override
        protected void reportSatellite(int totalInView,
                                       int prn,
                                       double elevation,
                                       double azimuth,
                                       double snr)
        {
            LoggerInstance.debug("Satellite %d: Elevation=%f Azimuth=%f SNR=%f (Total=%d)", prn, elevation, azimuth, snr, totalInView);

            IpnSatellite state = new IpnSatellite();
            state.satelliteId = prn;
            state.elevation   = elevation;
            state.azimuth     = azimuth;
            state.snr         = snr;
            state.totalInView = totalInView;

            m_manager.recordValue(state);
        }

        @Override
        protected boolean reportPosition(double latitude,
                                         double longitude,
                                         double altitude,
                                         double heightOfGeoid,
                                         double horizontalDilution,
                                         int numSatellites)
        {
            LoggerInstance.debug("Position: Lat=%f Lon=%f Alt=%f%n", latitude, longitude, altitude);

            if (!Double.isNaN(latitude) && !Double.isNaN(longitude))
            {
                m_state.latitude  = latitude;
                m_state.longitude = longitude;
                m_state.altitude  = (int) altitude;

                m_manager.recordValue(m_state);
            }

            return true;
        }

        @Override
        protected boolean reportSpeed(double trueTrack,
                                      double magneticTrack,
                                      double groundSpeedKnots,
                                      double groundSpeedKmh)
        {
            LoggerInstance.debug("Speed: %f Km/h%n", groundSpeedKmh);

            if (!Double.isNaN(groundSpeedKmh))
            {
                m_state.speed = (int) groundSpeedKmh;

                m_manager.recordValue(m_state);
            }

            return true;
        }
    };

    public WorkerForGPS(IpnManager manager,
                        String gpsPort)
    {
        super(IpnManager.LoggerInstance, "GPS", 60, 2000, 30);

        m_manager = manager;
        m_gpsPort = gpsPort;
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
        closeTransport();
    }

    @Override
    protected void worker()
    {
        final StringBuilder sb    = new StringBuilder();
        final byte[]        input = new byte[512];

        while (canContinue())
        {
            SerialAccess transport = m_transport;
            if (transport == null)
            {
                try
                {
                    FirmwareHelper f = FirmwareHelper.get();

                    if (!f.mightBePresent(m_gpsPort, false))
                    {
                        reportError("Failed to start GPS, device '%s' not found", m_gpsPort);

                        workerSleep(5000);
                        continue;
                    }

                    m_transport = new SerialAccess(f.mapPort(m_gpsPort), false);

                    m_manager.updateDiscoveryLatency(10, TimeUnit.SECONDS);

                    m_manager.notifyTransport(m_gpsPort, true, false);
                    reportErrorResolution("Reconnected to GPS!");
                    resetWatchdog();
                }
                catch (Throwable t)
                {
                    reportFailure("Failed to start GPS", t);

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
                        sb.setLength(0);

                        m_decoder.transitionToFailure();

                        if (m_decoder.shouldCloseTransport() || len < 0)
                        {
                            closeTransport();
                            workerSleep(500);
                        }
                    }
                    else
                    {
                        resetWatchdog();

                        for (int i = 0; i < len; i++)
                        {
                            char c = (char) input[i];

                            switch (c)
                            {
                                case '\n':
                                case '\r':
                                    if (sb.length() > 0)
                                    {
                                        m_decoder.process(sb.toString());
                                        sb.setLength(0);
                                    }
                                    break;

                                default:
                                    sb.append(c);
                                    break;
                            }
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

                    closeTransport();

                    reportDebug("Received error: %s", t);

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

        m_manager.notifyTransport(m_gpsPort, false, true);
    }
}