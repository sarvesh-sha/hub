/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.gps;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Set;

import com.google.common.collect.Sets;
import com.optio3.protocol.gps.NmeaDecoder;
import com.optio3.protocol.model.ipn.objects.IpnSatellite;
import com.optio3.test.common.Optio3Test;
import com.optio3.util.Resources;
import org.junit.Test;

public class NmeaDecoderTest extends Optio3Test
{
    Set<Integer> satellitesTracked = Sets.newHashSet();
    int          fixMode;
    Set<Integer> fixSet;

    @Test
    public void basicTests() throws
                             IOException
    {
        NmeaDecoder decoder = new NmeaDecoder()
        {
            @Override
            protected void reportFix(int fixMode,
                                     Set<Integer> fixSet,
                                     double pdop,
                                     double hdop,
                                     double vdop)
            {
                NmeaDecoderTest.this.fixMode = fixMode;
                NmeaDecoderTest.this.fixSet  = fixSet;
            }

            @Override
            protected void reportSatellite(int totalInView,
                                           int prn,
                                           double elevation,
                                           double azimuth,
                                           double snr)
            {
                System.out.printf("Sat %d: Elevation=%f Azimuth=%f SNR=%f%n", prn, elevation, azimuth, snr);

                IpnSatellite state = new IpnSatellite();
                state.satelliteId = prn;
                state.elevation   = elevation;
                state.azimuth     = azimuth;
                state.snr         = snr;

                if (state.isTracked())
                {
                    satellitesTracked.add(prn);
                }
            }

            @Override
            protected boolean reportPosition(double latitude,
                                             double longitude,
                                             double altitude,
                                             double heightOfGeoid,
                                             double horizontalDilution,
                                             int numSatellites)
            {
                if (Double.isNaN(latitude))
                {
                    return false;
                }

                System.out.printf("Pos: Lat=%f Lon=%f Alt=%f%n", latitude, longitude, altitude);
                assertEquals(47.795387, latitude, 0.0005);
                assertEquals(-122.228773, longitude, 0.0005);
                return true;
            }

            @Override
            protected boolean reportSpeed(double trueTrack,
                                          double magneticTrack,
                                          double groundSpeedKnots,
                                          double groundSpeedKmh)
            {
                if (Double.isNaN(groundSpeedKmh))
                {
                    return false;
                }

                System.out.printf("Speed: %f Km/h%n", groundSpeedKmh);
                assertEquals(0.0, groundSpeedKmh, 0.3);
                return true;
            }
        };

        BufferedReader reader = Resources.openResourceAsBufferedReader(NmeaDecoderTest.class, "GPS/gps_data_from_U-Blox.txt");
        String         line;

        while ((line = reader.readLine()) != null)
        {
            decoder.process(line);
        }

        assertEquals(3, fixMode);
        assertEquals(11, satellitesTracked.size());
        assertEquals(8, fixSet.size());
        System.out.printf("Tracked satellites: %s\n", satellitesTracked);
        System.out.printf("Fix satellites: %s\n", fixSet);
    }
}
