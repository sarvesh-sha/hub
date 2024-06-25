/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.gps;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import com.optio3.logging.Logger;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Decoder for GPS adapters using NMEA format, see http://www.gpsinformation.org/dale/nmea.htm
 */
public abstract class NmeaDecoder
{
    public static final Logger LoggerInstance = new Logger(NmeaDecoder.class);

    private enum DecoderState
    {
        Idle,
        Success,
        Failure,
    }

    private DecoderState   m_currentState = DecoderState.Idle;
    private MonotonousTime m_deadlineForTransportReset;
    private MonotonousTime m_deadlineForTransportReport;
    private boolean        m_transportIssueReported;

    public boolean process(String line) throws
                                        IOException
    {
        if (line.startsWith("$GP"))
        {
            transitionToSuccess();

            String[] parts = StringUtils.splitPreserveAllTokens(line, ",");
            switch (parts[0])
            {
                case "$GPGSV":
                    if (parts.length >= 20)
                    {
                        //    GPS Satellites in view
                        //
                        //    eg. $GPGSV,3,1,11,03,03,111,00,04,15,270,00,06,01,010,00,13,06,292,00*74
                        //    $GPGSV,3,2,11,14,25,170,00,16,57,208,39,18,67,296,40,19,40,246,00*74
                        //    $GPGSV,3,3,11,22,42,067,42,24,14,311,43,27,05,244,00,,,,*4D
                        //
                        //
                        //    $GPGSV,1,1,13,02,02,213,,03,-3,000,,11,00,121,,14,13,172,05*67
                        //
                        //
                        //    1    = Total number of messages of this type in this cycle
                        //    2    = Message number
                        //    3    = Total number of SVs in view
                        //    4    = SV PRN number
                        //    5    = Elevation in degrees, 90 maximum
                        //    6    = Azimuth, degrees from true north, 000 to 359
                        //    7    = SNR, 00-99 dB (null when not tracking)
                        //    8-11 = Information about second SV, same as field 4-7
                        //    12-15= Information about third SV, same as field 4-7
                        //    16-19= Information about fourth SV, same as field 4-7
                        int numOfSatellites = parseInt(parts, 3, 0);

                        for (int i = 0; i < 4; i++)
                        {
                            int prn = parseInt(parts, 4 + i * 4, -1);
                            if (prn >= 0)
                            {
                                double elevation = parseDouble(parts, 5 + i * 4, Double.NaN);
                                double azimuth   = parseDouble(parts, 6 + i * 4, Double.NaN);
                                double snr       = parseDouble(parts, 7 + i * 4, Double.NaN);

                                reportSatellite(numOfSatellites, prn, elevation, azimuth, snr);
                            }
                        }
                    }
                    break;

                case "$GPGSA":
                {
                    if (parts.length >= 18)
                    {
                        //    $GPGSA
                        //
                        //    GPS DOP and active satellites
                        //
                        //    eg1. $GPGSA,A,3,,,,,,16,18,,22,24,,,3.6,2.1,2.2*3C
                        //    eg2. $GPGSA,A,3,19,28,14,18,27,22,31,39,,,,,1.7,1.0,1.3*35
                        //
                        //
                        //    1    = Mode:
                        //    M=Manual, forced to operate in 2D or 3D
                        //    A=Automatic, 3D/2D
                        //    2    = Mode:
                        //    1=Fix not available
                        //    2=2D
                        //    3=3D
                        //    3-14 = IDs of SVs used in position fix (null for unused fields)
                        //    15   = PDOP
                        //    16   = HDOP
                        //    17   = VDOP

                        int          fixMode = parseInt(parts, 2, 1);
                        double       pdop    = parseDouble(parts, 15, Double.NaN);
                        double       hdop    = parseDouble(parts, 16, Double.NaN);
                        double       vdop    = parseDouble(parts, 17, Double.NaN);
                        Set<Integer> fixSet  = Sets.newHashSet();
                        for (int i = 3; i <= 14; i++)
                        {
                            int sv = parseInt(parts, i, -1);
                            if (sv >= 0)
                            {
                                fixSet.add(sv);
                            }
                        }

                        reportFix(fixMode, fixSet, pdop, hdop, vdop);
                    }
                }
                break;

                case "$GPGGA":
                    if (parts.length >= 13)
                    {
                        // GGA - essential fix data which provide 3D location and accuracy data.
                        //
                        //  $GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
                        //
                        // Where:
                        //      GGA          Global Positioning System Fix Data
                        //      123519       Fix taken at 12:35:19 UTC
                        //      4807.038,N   Latitude 48 deg 07.038' N
                        //      01131.000,E  Longitude 11 deg 31.000' E
                        //      1            Fix quality: 0 = invalid
                        //                                1 = GPS fix (SPS)
                        //                                2 = DGPS fix
                        //                                3 = PPS fix
                        // 			       4 = Real Time Kinematic
                        // 			       5 = Float RTK
                        //                                6 = estimated (dead reckoning) (2.3 feature)
                        // 			       7 = Manual input mode
                        // 			       8 = Simulation mode
                        //      08           Number of satellites being tracked
                        //      0.9          Horizontal dilution of position
                        //      545.4,M      Altitude, Meters, above mean sea level
                        //      46.9,M       Height of geoid (mean sea level) above WGS84
                        //                       ellipsoid
                        //      (empty field) time in seconds since last DGPS update
                        //      (empty field) DGPS station ID number
                        //      *47          the checksum data, always begins with *

                        int    fixTaken           = parseInt(parts, 1, -1);
                        double latitude           = parseLatLon(parts, 2);
                        double longitude          = parseLatLon(parts, 4);
                        int    fixQuality         = parseInt(parts, 6, -1);
                        int    numSatellites      = parseInt(parts, 7, -1);
                        double horizontalDilution = parseDouble(parts, 8, Double.NaN);
                        double altitude           = parseAltitude(parts, 9);
                        double heightOfGeoid      = parseAltitude(parts, 11);

                        if (fixQuality > 0 && fixQuality < 6)
                        {
                            if (reportPosition(latitude, longitude, altitude, heightOfGeoid, horizontalDilution, numSatellites))
                            {
                                return true;
                            }
                        }
                    }
                    break;

                case "$GPVTG":
                    if (parts.length >= 9)
                    {
                        // VTG - Velocity made good. The gps receiver may use the LC prefix instead of GP if it is emulating Loran output.
                        //   $GPVTG,054.7,T,034.4,M,005.5,N,010.2,K*48
                        // where:
                        //         VTG          Track made good and ground speed
                        //         054.7,T      True track made good (degrees)
                        //         034.4,M      Magnetic track made good
                        //         005.5,N      Ground speed, knots
                        //         010.2,K      Ground speed, Kilometers per hour
                        //         *48          Checksum

                        double trueTrack        = parseDouble(parts, 1, Double.NaN);
                        double magneticTrack    = parseDouble(parts, 3, Double.NaN);
                        double groundSpeedKnots = parseDouble(parts, 5, Double.NaN);
                        double groundSpeedKmh   = parseDouble(parts, 7, Double.NaN);

                        if (reportSpeed(trueTrack, magneticTrack, groundSpeedKnots, groundSpeedKmh))
                        {
                            return true;
                        }
                    }
                    break;
            }
        }

        return false;
    }

    protected abstract void reportSatellite(int totalInView,
                                            int prn,
                                            double elevation,
                                            double azimuth,
                                            double snr);

    protected abstract void reportFix(int fixMode,
                                      Set<Integer> fixSet,
                                      double pdop,
                                      double hdop,
                                      double vdop);

    protected abstract boolean reportPosition(double latitude,
                                              double longitude,
                                              double altitude,
                                              double heightOfGeoid,
                                              double horizontalDilution,
                                              int numSatellites);

    protected abstract boolean reportSpeed(double trueTrack,
                                           double magneticTrack,
                                           double groundSpeedKnots,
                                           double groundSpeedKmh);

    //--//

    private int parseInt(String[] parts,
                         int index,
                         int defaultValue)
    {
        String val = parts[index];
        if (StringUtils.isEmpty(val))
        {
            return defaultValue;
        }

        try
        {
            return Integer.valueOf(val);
        }
        catch (NumberFormatException t)
        {
            return defaultValue;
        }
    }

    private double parseDouble(String[] parts,
                               int index,
                               double defaultValue)
    {
        String val = parts[index];
        if (StringUtils.isEmpty(val))
        {
            return defaultValue;
        }

        try
        {
            return Double.valueOf(val);
        }
        catch (NumberFormatException t)
        {
            return defaultValue;
        }
    }

    private double parseLatLon(String[] parts,
                               int index)
    {
        double num      = parseDouble(parts, index, Double.NaN);
        double whole    = Math.floor(num);
        double fraction = num - whole;

        // The number is in degrees and minutes, like DDDmm. So we have to convert to degrees.
        double degree  = Math.floor(whole / 100.0);
        double minutes = whole - degree * 100.0 + fraction;

        degree += minutes / 60.0;

        switch (parts[index + 1])
        {
            case "S":
            case "s":
            case "W":
            case "w":
                return -degree;

            default:
                return degree;
        }
    }

    private double parseAltitude(String[] parts,
                                 int index)
    {
        double num = parseDouble(parts, index, Double.NaN);

        switch (parts[index + 1])
        {
            case "M":
            case "m":
                return num;

            case "F":
            case "f":
                return num / 3.2808;

            default:
                return num;
        }
    }

    //--//

    public void transitionToSuccess()
    {
        m_deadlineForTransportReset  = null;
        m_deadlineForTransportReport = null;

        switch (m_currentState)
        {
            case Failure:
                if (m_transportIssueReported)
                {
                    LoggerInstance.info("GPS data flow resumed!");

                    m_transportIssueReported = false;
                }
                // Fallthrough

            case Idle:
                m_currentState = DecoderState.Success;
                break;
        }
    }

    public void transitionToFailure()
    {
        switch (m_currentState)
        {
            case Idle:
            case Success:
                if (m_deadlineForTransportReset == null)
                {
                    setDeadlineForTransportReset();
                }

                if (TimeUtils.isTimeoutExpired(m_deadlineForTransportReset))
                {
                    m_currentState = DecoderState.Failure;
                }
                break;

            case Failure:
                if (m_deadlineForTransportReport == null)
                {
                    m_deadlineForTransportReport = TimeUtils.computeTimeoutExpiration(20, TimeUnit.MINUTES);
                }

                if (!m_transportIssueReported && TimeUtils.isTimeoutExpired(m_deadlineForTransportReport))
                {
                    LoggerInstance.error("GPS not receiving any data...");

                    m_transportIssueReported = true;
                }
                break;
        }
    }

    public boolean shouldCloseTransport()
    {
        if (m_deadlineForTransportReset != null && TimeUtils.isTimeoutExpired(m_deadlineForTransportReset))
        {
            setDeadlineForTransportReset();

            return true;
        }

        return false;
    }

    private void setDeadlineForTransportReset()
    {
        m_deadlineForTransportReset = TimeUtils.computeTimeoutExpiration(10, TimeUnit.SECONDS);
    }
}