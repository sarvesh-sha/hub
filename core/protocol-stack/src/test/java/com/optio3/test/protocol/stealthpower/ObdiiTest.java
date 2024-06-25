/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.stealthpower;

import java.util.concurrent.TimeUnit;

import com.optio3.concurrency.Executors;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.obdii.iso15765.CalculatedEngineLoad;
import com.optio3.protocol.model.obdii.iso15765.DtcCheck;
import com.optio3.protocol.model.obdii.iso15765.DtcStatus;
import com.optio3.protocol.model.obdii.iso15765.EngineCoolantTemperature;
import com.optio3.protocol.model.obdii.iso15765.EngineRPM;
import com.optio3.protocol.model.obdii.iso15765.FuelPressure;
import com.optio3.protocol.model.obdii.iso15765.FuelRailGaugePressure;
import com.optio3.protocol.model.obdii.iso15765.FuelRailPressure;
import com.optio3.protocol.model.obdii.iso15765.FuelSystemStatus;
import com.optio3.protocol.model.obdii.iso15765.IntakeAirTemperature;
import com.optio3.protocol.model.obdii.iso15765.IntakeManifoldAbsolutePressure;
import com.optio3.protocol.model.obdii.iso15765.MassAirFlowRate;
import com.optio3.protocol.model.obdii.iso15765.RunTimeSinceEngineStart;
import com.optio3.protocol.model.obdii.iso15765.SupportedPIDs;
import com.optio3.protocol.model.obdii.iso15765.ThrottlePosition;
import com.optio3.protocol.model.obdii.iso15765.TimingAdvance;
import com.optio3.protocol.model.obdii.iso15765.VIN;
import com.optio3.protocol.model.obdii.iso15765.VehicleSpeed;
import com.optio3.protocol.obdii.ObdiiManager;
import com.optio3.serialization.ObjectMappers;
import com.optio3.test.common.Optio3Test;
import org.junit.Ignore;
import org.junit.Test;

public class ObdiiTest extends Optio3Test
{
    @Ignore("Manually enable to test, since it requires access to OBD-II adapter")
    @Test
    public void testElm327()
    {
        ObdiiManager.LoggerInstance.enable(Severity.Debug);
        ObdiiManager.LoggerInstance.enable(Severity.DebugVerbose);

//        ObdiiManager mgr = new ObdiiManager("/dev/cu.SLAB_USBtoUART", 38400)
        ObdiiManager mgr = new ObdiiManager("/dev/cu.usbserial-00003214", 115200)
        {
            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
            }
        };

        mgr.start();

        Executors.safeSleep(1000);

        {
            SupportedPIDs res = mgr.requestSinglePdu(SupportedPIDs.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("SupportedPIDs => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            DtcCheck res = mgr.requestSinglePdu(DtcCheck.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("DtcCheck => %s", ObjectMappers.prettyPrintAsJson(res));

            if (res != null && res.dct_count != 0)
            {
                DtcStatus res2 = mgr.requestSinglePdu(DtcStatus.class, 1, TimeUnit.SECONDS);
                ObdiiManager.LoggerInstance.info("DtcStatus => %s", ObjectMappers.prettyPrintAsJson(res2));
            }
        }

        {
            FuelSystemStatus res = mgr.requestSinglePdu(FuelSystemStatus.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("FuelSystemStatus => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            CalculatedEngineLoad res = mgr.requestSinglePdu(CalculatedEngineLoad.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("CalculatedEngineLoad => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            EngineCoolantTemperature res = mgr.requestSinglePdu(EngineCoolantTemperature.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("EngineCoolantTemperature => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            FuelPressure res = mgr.requestSinglePdu(FuelPressure.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("FuelPressure => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            IntakeManifoldAbsolutePressure res = mgr.requestSinglePdu(IntakeManifoldAbsolutePressure.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("IntakeManifoldAbsolutePressure => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            EngineRPM res = mgr.requestSinglePdu(EngineRPM.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("EngineRPM => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            VehicleSpeed res = mgr.requestSinglePdu(VehicleSpeed.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("VehicleSpeed => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            TimingAdvance res = mgr.requestSinglePdu(TimingAdvance.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("TimingAdvance => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            IntakeAirTemperature res = mgr.requestSinglePdu(IntakeAirTemperature.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("IntakeAirTemperature => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            MassAirFlowRate res = mgr.requestSinglePdu(MassAirFlowRate.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("MassAirFlowRate => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            ThrottlePosition res = mgr.requestSinglePdu(ThrottlePosition.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("ThrottlePosition => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            RunTimeSinceEngineStart res = mgr.requestSinglePdu(RunTimeSinceEngineStart.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("RunTimeSinceEngineStart => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            FuelRailPressure res = mgr.requestSinglePdu(FuelRailPressure.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("FuelRailPressure => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            FuelRailGaugePressure res = mgr.requestSinglePdu(FuelRailGaugePressure.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("FuelRailGaugePressure => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            VIN res = mgr.requestSinglePdu(VIN.class, 1, TimeUnit.SECONDS);
            ObdiiManager.LoggerInstance.info("VIN => %s", ObjectMappers.prettyPrintAsJson(res));
        }
    }
}
