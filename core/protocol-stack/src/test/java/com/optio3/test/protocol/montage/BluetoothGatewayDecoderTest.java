/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.montage;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.optio3.concurrency.Executors;
import com.optio3.protocol.ipn.IpnManager;
import com.optio3.protocol.ipn.WorkerForMontageBluetoothGateway;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.protocol.model.ipn.objects.IpnLocation;
import com.optio3.protocol.model.ipn.objects.montage.BaseBluetoothGatewayObjectModel;
import com.optio3.protocol.model.ipn.objects.montage.BluetoothGateway_PixelTagRaw;
import com.optio3.protocol.model.ipn.objects.montage.BluetoothGateway_SmartLock;
import com.optio3.protocol.model.ipn.objects.montage.BluetoothGateway_TemperatureHumiditySensor;
import com.optio3.protocol.montage.BluetoothGatewayDecoder;
import com.optio3.serialization.ObjectMappers;
import com.optio3.test.common.Optio3Test;
import com.optio3.util.Resources;
import org.junit.Ignore;
import org.junit.Test;

public class BluetoothGatewayDecoderTest extends Optio3Test
{
    Set<String> pixelTags                  = Sets.newHashSet();
    Set<String> smartLocks                 = Sets.newHashSet();
    Set<String> temperatureHumiditySensors = Sets.newHashSet();

    @Test
    public void basicTests() throws
                             IOException
    {
        var decoder = new BluetoothGatewayDecoder()
        {
            @Override
            protected void receivedHeartbeat(Map<String, String> fields)
            {
                System.out.printf("Heartbeat %s\n", fields);
            }

            @Override
            protected void receivedMessage(BaseBluetoothGatewayObjectModel obj)
            {
                if (obj instanceof BluetoothGateway_PixelTagRaw)
                {
                    BluetoothGatewayDecoderTest.this.pixelTags.add(obj.unitId);
                }

                if (obj instanceof BluetoothGateway_SmartLock)
                {
                    BluetoothGatewayDecoderTest.this.smartLocks.add(obj.unitId);
                }

                if (obj instanceof BluetoothGateway_TemperatureHumiditySensor)
                {
                    BluetoothGatewayDecoderTest.this.temperatureHumiditySensors.add(obj.unitId);
                }

                try
                {
                    System.out.printf("Message %s\n", ObjectMappers.SkipNulls.writeValueAsString(obj));
                }
                catch (JsonProcessingException ignored)
                {
                    ;
                }
            }

            @Override
            protected IpnLocation getLastLocation()
            {
                return null;
            }
        };

        BufferedReader reader = Resources.openResourceAsBufferedReader(BluetoothGatewayDecoderTest.class, "montage/Log_v008_20231004.01.txt");
        String         line;

        while ((line = reader.readLine()) != null)
        {
            System.out.printf("Line %s\n", line);
            decoder.process(line);
        }

        assertEquals(1, pixelTags.size());
        assertEquals(1, smartLocks.size());
        assertEquals(1, temperatureHumiditySensors.size());
        System.out.printf("pixelTags: %s\n", pixelTags);
        System.out.printf("smartLocks: %s\n", smartLocks);
        System.out.printf("temperatureHumiditySensors: %s\n", temperatureHumiditySensors);
    }

    @Ignore("Manually enable to test, since it requires access to hardware")
    @Test
    public void liveTests()
    {
        var cfg = new ProtocolConfigForIpn();
        var manager = new IpnManager(cfg)
        {
            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
            }

            @Override
            protected void streamSamples(IpnObjectModel obj) throws
                                                             Exception
            {
                System.out.printf("streamSamples %s %s\n", obj.extractId(), ObjectMappers.SkipNulls.writeValueAsString(obj));
            }

            @Override
            protected void notifySamples(IpnObjectModel obj,
                                         String field)
            {
            }

            @Override
            protected byte[] detectedStealthPowerBootloader(byte bootloadVersion,
                                                            byte hardwareVersion,
                                                            byte hardwareRevision)
            {
                return null;
            }

            @Override
            protected void completedStealthPowerBootloader(int statusCode)
            {
            }
        };

        var worker = new WorkerForMontageBluetoothGateway(manager, "/optio3-dev/montage_bluetooth");
        worker.start();

        Executors.safeSleep(100000);
    }
}
