/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.bergstrom;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.optio3.interop.mediaaccess.CanAccess;
import com.optio3.logging.Severity;
import com.optio3.protocol.can.CanManager;
import com.optio3.protocol.model.can.CanObjectModel;
import com.optio3.protocol.model.ipn.objects.nitephoenix.BaseBatteryNitePhoenixModel;
import com.optio3.protocol.model.ipn.objects.nitephoenix.NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs;
import com.optio3.protocol.model.ipn.objects.nitephoenix.NitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters;
import com.optio3.protocol.model.ipn.objects.nitephoenix.NitePhoenix_BatteryMonitoringSystem_StartingBatteryParameters;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.test.common.AutoRetryOnFailure;
import com.optio3.test.common.Optio3Test;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BergstromCurrentDecodingSamplesTest extends Optio3Test
{
    private boolean m_verbose;

    @Before
    public void setVerboseLogging()
    {
        if (failedOnFirstRun())
        {
            m_verbose = true;

            InputBuffer.LoggerInstance.enablePerThread(Severity.Debug);
            OutputBuffer.LoggerInstance.enablePerThread(Severity.Debug);
        }
        else
        {
            m_verbose = false;
        }
    }

    @After
    public void resetVerboseLogging()
    {
        InputBuffer.LoggerInstance.inheritPerThread(Severity.Debug);
        OutputBuffer.LoggerInstance.inheritPerThread(Severity.Debug);
    }

    //--//

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void testMasterValues() throws
                                   Exception
    {
        class State
        {
            boolean gotNitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs;
            boolean gotNitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters;
            boolean gotNitePhoenix_BatteryMonitoringSystem_StartingBatteryParameters;

            boolean gotUnit1;
            boolean gotUnit2;
            boolean gotUnit3;

            public void checkUnit(BaseBatteryNitePhoenixModel obj)
            {
                switch (obj.unitId)
                {
                    case 1:
                        gotUnit1 = true;
                        break;
                    case 2:
                        gotUnit2 = true;
                        break;
                    case 3:
                        gotUnit3 = true;
                        break;
                }
            }
        }

        var state = new State();

        try (CanManager mgr = new CanManager(null, 0, false, false)
        {
            @Override
            protected void notifyGoodMessage(CanObjectModel val)
            {
                String json = ObjectMappers.prettyPrintAsJson(val);

                LoggerInstance.info("Got: %s = %s", val.extractId(), json);

                NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs val1 = Reflection.as(val, NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs.class);
                if (val1 != null && val1.unitId > 0)
                {
                    state.gotNitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs = true;

                    state.checkUnit(val1);
                }

                NitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters val2 = Reflection.as(val, NitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters.class);
                if (val2 != null && val2.unitId > 0)
                {
                    state.gotNitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters = true;

                    state.checkUnit(val2);
                }

                NitePhoenix_BatteryMonitoringSystem_StartingBatteryParameters val3 = Reflection.as(val, NitePhoenix_BatteryMonitoringSystem_StartingBatteryParameters.class);
                if (val3 != null && val3.unitId > 0)
                {
                    state.gotNitePhoenix_BatteryMonitoringSystem_StartingBatteryParameters = true;

                    state.checkUnit(val3);
                }
            }

            @Override
            protected void notifyUnknownMessage(CanAccess.BaseFrame frame)
            {
                LoggerInstance.info("Got unknown CAN message: %x", frame.encodeId());
            }

            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {

            }
        })
        {
            for (CanAccess.can_frame frame : parseFile("bergstrom/current.txt"))
            {
                mgr.injectFrame(frame);
            }
        }

        assertTrue(state.gotNitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs);
        assertTrue(state.gotNitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters);
        assertTrue(state.gotNitePhoenix_BatteryMonitoringSystem_StartingBatteryParameters);
        assertTrue(state.gotUnit1);
        assertTrue(state.gotUnit2);
        assertTrue(state.gotUnit3);
    }

    //--//

    private List<CanAccess.can_frame> parseFile(String resource) throws
                                                                 IOException
    {
        Pattern                   lineDecoder = Pattern.compile(" *can[0-9] +([0-9A-F]+) +\\[([0-9])] +([0-9A-F ]+).*");
        List<CanAccess.can_frame> frames      = Lists.newArrayList();

        for (String line : loadResourceAsLines(resource, false))
        {
            Matcher matcher = lineDecoder.matcher(line);
            if (matcher.matches())
            {
                String id   = matcher.group(1);
                String len  = matcher.group(2);
                String data = matcher.group(3);

                int    parsedId      = Integer.parseInt(id, 16);
                int    parsedLen     = Integer.parseInt(len, 16);
                byte[] parsedPayload = decodePayload(data, parsedLen);

                frames.add(CanAccess.buildRawFrame(parsedId, id.length() == 8, parsedLen, parsedPayload));
            }
        }

        return frames;
    }

    private static byte[] decodePayload(String payload,
                                        int len)
    {
        String[] parts = StringUtils.split(payload, ' ');

        byte[] res = new byte[len];
        for (int i = 0; i < len; i++)
        {
            res[i] = (byte) Integer.parseInt(parts[i], 16);
        }

        return res;
    }
}
