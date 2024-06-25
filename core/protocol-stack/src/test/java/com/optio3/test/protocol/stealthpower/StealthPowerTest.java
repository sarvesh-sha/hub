/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.stealthpower;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import com.optio3.concurrency.Executors;
import com.optio3.protocol.model.ipn.objects.stealthpower.BaseStealthPowerModel;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_FDNY;
import com.optio3.protocol.stealthpower.StealthPowerManager;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.test.common.Optio3Test;
import com.optio3.util.Resources;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

public class StealthPowerTest extends Optio3Test
{
    @Ignore("Manually enable to test, since it requires access to Stealth Power MCU")
    @Test
    public void stealthPowerTest() throws
                                   IOException
    {
//        StealthPowerManager.LoggerInstance.enable(Severity.Debug);
//        StealthPowerManager.LoggerInstance.enable(Severity.DebugVerbose);
        StealthPowerManager mgr = new StealthPowerManager("/optio3-dev/optio3_RS232")
        {
            boolean alreadyOffered = false;

            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
            }

            @Override
            protected byte[] detectedBootloader(byte bootloadVersion,
                                                byte hardwareVersion,
                                                byte hardwareRevision)
            {
                try
                {
                    System.out.printf("detectedBootloader: %02x %d %d\n", bootloadVersion, hardwareVersion, hardwareRevision);
                    if (!alreadyOffered)
                    {
                        alreadyOffered = true;

                        try (InputStream stream = Resources.openResourceAsStream(StealthPowerTest.class, "StealthPower/MCU8b_Optio_FDNY_Test_Feb2020_V200.X.production.bl2"))
                        {
                            return IOUtils.toByteArray(stream);
                        }
                    }
                }
                catch (IOException e)
                {
                    // Ignore failures..
                }

                return null;
            }

            @Override
            protected void reportDownloadResult(int statusCode)
            {
                System.out.printf("reportDownloadResult: %d\n", statusCode);
            }

            @Override
            protected void receivedMessage(BaseStealthPowerModel obj)
            {
                System.out.printf("receivedMessage: %s\n", ObjectMappers.prettyPrintAsJson(obj));

                StealthPower_FDNY obj_fdny = Reflection.as(obj, StealthPower_FDNY.class);
                if (obj_fdny != null)
                {
                    System.out.printf("activity: %d\n", obj_fdny.activity_timer);

                    if (obj_fdny.activity_timer < 40)
                    {
                        System.out.println("sendVehicleMovingNotification");
                        sendVehicleMovingNotification();
                    }

                    if (!StringUtils.equals(obj_fdny.firmware_version, "2.0.0"))
                    {
                        System.out.println("sendResetRequest");
                        sendResetRequest();
                    }
                }
            }
        };

        mgr.start();

        Executors.safeSleep(200000);
    }

    @Test
    public void stealthPowerMTA() throws
                                  IOException
    {
//        StealthPowerManager.LoggerInstance.enable(Severity.DebugVerbose);
        class StealthPowerManagerTest extends StealthPowerManager
        {
            public StealthPowerManagerTest()
            {
                super(null);
            }

            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
            }

            @Override
            protected byte[] detectedBootloader(byte bootloadVersion,
                                                byte hardwareVersion,
                                                byte hardwareRevision)
            {
                System.out.printf("detectedBootloader: %d %d\n", hardwareVersion, hardwareRevision);
                return null;
            }

            @Override
            protected void reportDownloadResult(int statusCode)
            {
                System.out.printf("reportDownloadResult: %d\n", statusCode);
            }

            @Override
            protected void receivedMessage(BaseStealthPowerModel obj)
            {
                System.out.printf("receivedMessage: %s\n", ObjectMappers.prettyPrintAsJson(obj));
            }

            void injectLine(String line)
            {
                decode((byte) 0xAB);

                for (byte c : line.getBytes())
                {
                    decode(c);
                }
                decode((byte) '\n');
                decode((byte) 0);
            }
        }

        StealthPowerManagerTest mgr = new StealthPowerManagerTest();

        BufferedReader reader = Resources.openResourceAsBufferedReader(StealthPowerTest.class, "StealthPower/MTA.txt");
        String         line;

        while ((line = reader.readLine()) != null)
        {
            System.out.printf("INPUT: %s\n", line);
            mgr.injectLine(line);
        }
    }
}
