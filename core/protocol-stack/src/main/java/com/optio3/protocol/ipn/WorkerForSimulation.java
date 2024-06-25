/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.common.ServiceWorker;
import com.optio3.protocol.model.ipn.enums.IpnChargeState;
import com.optio3.protocol.model.ipn.enums.Ipn_PalFinger_DisplayCode;
import com.optio3.protocol.model.ipn.objects.sensors.IpnAccelerometer;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_MasterValues;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_ProRemoteValues;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_UnitValues;
import com.optio3.protocol.model.ipn.objects.epsolar.EpSolar_RealTimeData;
import com.optio3.protocol.model.ipn.objects.hendrickson.Hendrickson_Watchman;
import com.optio3.protocol.model.ipn.objects.hendrickson.enums.WheelConnectionStatus;
import com.optio3.protocol.model.ipn.objects.hendrickson.enums.WheelElectricalStatus;
import com.optio3.protocol.model.ipn.objects.hendrickson.enums.WheelLeakStatus;
import com.optio3.protocol.model.ipn.objects.hendrickson.enums.WheelPressureStatus;
import com.optio3.protocol.model.ipn.objects.hendrickson.enums.WheelTemperatureStatus;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStarAlarm;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStarFault;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStar_Status;
import com.optio3.protocol.model.ipn.objects.nitephoenix.NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs;
import com.optio3.protocol.model.ipn.objects.nitephoenix.NitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters;
import com.optio3.protocol.model.ipn.objects.nitephoenix.NitePhoenix_ControlPanel;
import com.optio3.protocol.model.ipn.objects.palfinger.Palfinger_Counters;
import com.optio3.protocol.model.ipn.objects.palfinger.Palfinger_DigitalStatusInputs;
import com.optio3.protocol.model.ipn.objects.palfinger.Palfinger_DigitalStatusOutputs;
import com.optio3.protocol.model.ipn.objects.palfinger.Palfinger_SupplyVoltage;
import com.optio3.protocol.model.obdii.VehicleIdentification;
import com.optio3.protocol.model.obdii.iso15765.DtcStatus;
import com.optio3.protocol.model.obdii.pgn.CabMessage1;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public final class WorkerForSimulation extends ServiceWorker implements IpnWorker
{
    private final IpnManager m_manager;
    private final float      m_accelerometerFrequency;
    private final float      m_accelerometerRange;
    private final float      m_accelerometerThreshold;

    public WorkerForSimulation(IpnManager manager,
                               float accelerometerFrequency,
                               float accelerometerRange,
                               float accelerometerThreshold)
    {
        super(IpnManager.LoggerInstance, "Simulation", 0, 2000);

        m_manager                = manager;
        m_accelerometerFrequency = accelerometerFrequency;
        m_accelerometerRange     = accelerometerRange;
        m_accelerometerThreshold = accelerometerThreshold;
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
    }

    @Override
    protected void worker()
    {
        var rnd = new Random();

        var    accel     = new IpnAccelerometer();
        double accelTime = TimeUtils.nowEpochSeconds();

        var val_BlueSkyMasterValues    = new BlueSky_MasterValues();
        var val_BlueSkyProRemoteValues = new BlueSky_ProRemoteValues();
        var val_BlueSkyUnitValues      = new BlueSky_UnitValues();
        val_BlueSkyUnitValues.unitId = 0;

        //--//

        var val_Watchman_wheel11 = new Hendrickson_Watchman();
        val_Watchman_wheel11.wheelRow                 = 1;
        val_Watchman_wheel11.wheelPosition            = 1;
        val_Watchman_wheel11.wheelTemperatureStatus   = WheelTemperatureStatus.Ok;
        val_Watchman_wheel11.wheelPressureStatusPortA = WheelPressureStatus.Ok;
        val_Watchman_wheel11.wheelPressureStatusPortB = WheelPressureStatus.Ok;
        val_Watchman_wheel11.wheelConnectionStatus    = WheelConnectionStatus.Ok;
        val_Watchman_wheel11.wheelLeakStatusPortA     = WheelLeakStatus.Ok;
        val_Watchman_wheel11.wheelLeakStatusPortB     = WheelLeakStatus.Ok;
        val_Watchman_wheel11.wheelElectricalStatus    = WheelElectricalStatus.Fault;

        var val_Watchman_wheel23 = new Hendrickson_Watchman();
        val_Watchman_wheel23.wheelRow                 = 2;
        val_Watchman_wheel23.wheelPosition            = 3;
        val_Watchman_wheel23.wheelTemperatureStatus   = WheelTemperatureStatus.Ok;
        val_Watchman_wheel23.wheelPressureStatusPortA = WheelPressureStatus.Ok;
        val_Watchman_wheel23.wheelPressureStatusPortB = WheelPressureStatus.Ok;
        val_Watchman_wheel23.wheelConnectionStatus    = WheelConnectionStatus.Ok;
        val_Watchman_wheel23.wheelLeakStatusPortA     = WheelLeakStatus.Ok;
        val_Watchman_wheel23.wheelLeakStatusPortB     = WheelLeakStatus.Ok;
        val_Watchman_wheel23.wheelElectricalStatus    = WheelElectricalStatus.Fault;

        //--//

        var val_NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs = new NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs();
        var val_NitePhoenix_ControlPanel                             = new NitePhoenix_ControlPanel();

        var val_NitePhoenix_AuxiliaryBatteryParameters1 = new NitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters();
        val_NitePhoenix_AuxiliaryBatteryParameters1.unitId = 1;

        var val_NitePhoenix_AuxiliaryBatteryParameters2 = new NitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters();
        val_NitePhoenix_AuxiliaryBatteryParameters2.unitId = 2;

        var val_NitePhoenix_AuxiliaryBatteryParameters3 = new NitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters();
        val_NitePhoenix_AuxiliaryBatteryParameters3.unitId = 3;

        //--//

        var val_Palfinger_Counters             = new Palfinger_Counters();
        var val_Palfinger_SupplyVoltage        = new Palfinger_SupplyVoltage();
        var val_Palfinger_DigitalStatusInputs  = new Palfinger_DigitalStatusInputs();
        var val_Palfinger_DigitalStatusOutputs = new Palfinger_DigitalStatusOutputs();

        val_Palfinger_SupplyVoltage.supplyVoltage_V27 = 12000;
        val_Palfinger_SupplyVoltage.plcTemperature    = 20;
        var segmentDisplay = Ipn_PalFinger_DisplayCode.None;

        //--//

        var val_TriStar_Status = new TriStar_Status();
        val_TriStar_Status.fault = new TriStarFault();
        val_TriStar_Status.alarm = new TriStarAlarm();

        TriStarFault.Values[] valuesFault = TriStarFault.Values.values();
        TriStarAlarm.Values[] valuesAlarm = TriStarAlarm.Values.values();

        //--//

        var val_epSolar1_status = new EpSolar_RealTimeData();
        var val_epSolar2_status = new EpSolar_RealTimeData();
        val_epSolar1_status.unitId = 1;
        val_epSolar2_status.unitId = 2;

        //--//

        var val_CabMessage1 = new CabMessage1();
        val_CabMessage1.sourceAddress                    = 23;
        val_CabMessage1.Cab_Interior_Temperature_Command = 20f;

        var val_VehicleIdentification = new VehicleIdentification();
        val_VehicleIdentification.VIN = "ABCtest";

        var val_DtcStatus = new DtcStatus();
        val_DtcStatus.fault_codes = new String[] { "ABCtest" };

        MonotonousTime delayForCabMessage  = TimeUtils.computeTimeoutExpiration(30, TimeUnit.SECONDS);
        MonotonousTime delayForUnreachable = TimeUtils.computeTimeoutExpiration(60, TimeUnit.SECONDS);

        //--//

        while (canContinue())
        {
            if (m_accelerometerFrequency > 0)
            {
                double step = 1.0 / m_accelerometerFrequency;
                double now  = TimeUtils.nowEpochSeconds();

                while (accelTime < now)
                {
                    accel.x += -5 + (10 * rnd.nextFloat());
                    accel.y += -5 + (10 * rnd.nextFloat());
                    accel.z += -5 + (10 * rnd.nextFloat());

                    accel.x = Math.max(-2000, Math.min(2000, accel.x));
                    accel.y = Math.max(-2000, Math.min(2000, accel.y));
                    accel.z = Math.max(-2000, Math.min(2000, accel.z));

                    m_manager.recordValue(accelTime, accel);

                    accelTime += step;
                }
            }

            val_BlueSkyMasterValues.batteryVoltage        = 13 + 3 * rnd.nextFloat();
            val_BlueSkyMasterValues.daysSinceLastEqualize = Unsigned8.box(255);
            val_BlueSkyMasterValues.chargeState           = IpnChargeState.values()[rnd.nextInt(IpnChargeState.values().length)];
            m_manager.recordValue(val_BlueSkyMasterValues);

            //--//

            val_BlueSkyProRemoteValues.maxBatteryVolt = 14 + rnd.nextFloat();
            val_BlueSkyProRemoteValues.minBatteryVolt = 12 + rnd.nextFloat();
            m_manager.recordValue(val_BlueSkyProRemoteValues);

            //--//

            val_BlueSkyUnitValues.heatSinkTemperature = 17 + 3 * rnd.nextFloat();
            val_BlueSkyUnitValues.inputVoltage        = 12 + 3 * rnd.nextFloat();
            m_manager.recordValue(val_BlueSkyUnitValues);

            //--//

            val_Watchman_wheel11.wheelTemperature       = 20 + 3 * rnd.nextFloat();
            val_Watchman_wheel11.wheelPressurePortA     = 20 + rnd.nextInt(10);
            val_Watchman_wheel11.wheelPressurePortB     = 20 + rnd.nextInt(10);
            val_Watchman_wheel11.wheelElectricalStatus  = rnd.nextBoolean() ? WheelElectricalStatus.Ok : WheelElectricalStatus.Error;
            val_Watchman_wheel11.wheelTemperatureStatus = rnd.nextBoolean() ? WheelTemperatureStatus.Ok : WheelTemperatureStatus.Error;
            m_manager.recordValue(val_Watchman_wheel11);

            val_Watchman_wheel23.wheelTemperature         = 20 + 3 * rnd.nextFloat();
            val_Watchman_wheel23.wheelPressurePortA       = 20 + rnd.nextInt(10);
            val_Watchman_wheel23.wheelPressurePortB       = 20 + rnd.nextInt(10);
            val_Watchman_wheel23.wheelPressureStatusPortA = rnd.nextBoolean() ? WheelPressureStatus.Ok : WheelPressureStatus.Error;
            val_Watchman_wheel23.wheelPressureStatusPortB = rnd.nextBoolean() ? WheelPressureStatus.Ok : WheelPressureStatus.Error;
            m_manager.recordValue(val_Watchman_wheel23);

            //--//

            val_NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs.timeRemaining = 10 + 3 * rnd.nextFloat();
            val_NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs.stateOfCharge = 10 + rnd.nextInt(3);
            m_manager.recordValue(val_NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs);

            val_NitePhoenix_ControlPanel.requestedFanSpeed     = 5 + rnd.nextInt(3);
            val_NitePhoenix_ControlPanel.temperatureSettingRaw = rnd.nextInt(20);
            m_manager.recordValue(val_NitePhoenix_ControlPanel);

            val_NitePhoenix_AuxiliaryBatteryParameters1.current = 10 + 3 * rnd.nextFloat();
            m_manager.recordValue(val_NitePhoenix_AuxiliaryBatteryParameters1);
            val_NitePhoenix_AuxiliaryBatteryParameters2.current = 10 + 3 * rnd.nextFloat();
            m_manager.recordValue(val_NitePhoenix_AuxiliaryBatteryParameters2);
            val_NitePhoenix_AuxiliaryBatteryParameters3.current = 10 + 3 * rnd.nextFloat();
            m_manager.recordValue(val_NitePhoenix_AuxiliaryBatteryParameters3);

            //--//

            if (rnd.nextFloat() > 0.8)
            {
                val_Palfinger_Counters.counterNonResettable++;
            }

            if (rnd.nextFloat() > 0.8)
            {
                val_Palfinger_Counters.counterService++;
            }
            m_manager.recordValue(val_Palfinger_Counters);

            //--//

            val_Palfinger_SupplyVoltage.supplyVoltage_V27 += 50 * (rnd.nextFloat() - 0.5);
            if (val_Palfinger_SupplyVoltage.supplyVoltage_V27 < 11000)
            {
                val_Palfinger_SupplyVoltage.supplyVoltage_V27 = 11000;
            }
            if (val_Palfinger_SupplyVoltage.supplyVoltage_V27 > 15000)
            {
                val_Palfinger_SupplyVoltage.supplyVoltage_V27 = 15000;
            }

            val_Palfinger_SupplyVoltage.plcTemperature += (rnd.nextFloat() - 0.5);
            if (val_Palfinger_SupplyVoltage.plcTemperature < 0)
            {
                val_Palfinger_SupplyVoltage.plcTemperature = 0;
            }
            if (val_Palfinger_SupplyVoltage.plcTemperature > 40)
            {
                val_Palfinger_SupplyVoltage.plcTemperature = 40;
            }

            m_manager.recordValue(val_Palfinger_SupplyVoltage);

            //--//

            val_Palfinger_DigitalStatusInputs.commandOpen  = rnd.nextFloat() > 0.9;
            val_Palfinger_DigitalStatusInputs.commandClose = rnd.nextFloat() > 0.9;

            val_Palfinger_DigitalStatusInputs.sensorB16WarnLightsOn       = rnd.nextFloat() > 0.5;
            val_Palfinger_DigitalStatusInputs.sensorB16PositionHorizontal = rnd.nextFloat() > 0.5;
            val_Palfinger_DigitalStatusInputs.J11_1_CabinControl_On       = rnd.nextFloat() > 0.5;

            m_manager.recordValue(val_Palfinger_DigitalStatusInputs);

            //--//

            val_Palfinger_DigitalStatusOutputs.light         = rnd.nextFloat() > 0.5;
            val_Palfinger_DigitalStatusOutputs.warningLights = rnd.nextFloat() > 0.5;

            if (val_Palfinger_DigitalStatusOutputs.segmentDisplay == 0)
            {
                val_Palfinger_DigitalStatusOutputs.segmentDisplay = segmentDisplay.encoding();
            }
            else
            {
                val_Palfinger_DigitalStatusOutputs.segmentDisplay = 0;
            }

            if (rnd.nextFloat() > 0.9)
            {
                Ipn_PalFinger_DisplayCode[] values = Ipn_PalFinger_DisplayCode.values();
                segmentDisplay = values[rnd.nextInt(values.length)];
            }

            m_manager.recordValue(val_Palfinger_DigitalStatusOutputs);

            //--//

            if (rnd.nextFloat() > 0.8)
            {
                if (rnd.nextFloat() > 0.5)
                {
                    val_TriStar_Status.fault.set(valuesFault[rnd.nextInt(valuesFault.length)]);
                }
                else
                {

                    val_TriStar_Status.fault.clear(valuesFault[rnd.nextInt(valuesFault.length)]);
                }
            }

            if (rnd.nextFloat() > 0.8)
            {
                if (rnd.nextFloat() > 0.5)
                {
                    val_TriStar_Status.alarm.set(valuesAlarm[rnd.nextInt(valuesAlarm.length)]);
                }
                else
                {

                    val_TriStar_Status.alarm.clear(valuesAlarm[rnd.nextInt(valuesAlarm.length)]);
                }
            }

            m_manager.recordValue(val_TriStar_Status);

            //--//

            val_epSolar1_status.battery_voltage = 13 + 3 * rnd.nextFloat();
            m_manager.recordValue(val_epSolar1_status);

            if (!TimeUtils.isTimeoutExpired(delayForUnreachable))
            {
                val_epSolar2_status.battery_voltage = 13 + 3 * rnd.nextFloat();
                m_manager.recordValue(val_epSolar2_status);
            }

            //--//

            if (TimeUtils.isTimeoutExpired(delayForCabMessage))
            {
                val_CabMessage1.Cab_Interior_Temperature_Command = 20 + 3 * rnd.nextFloat();
                m_manager.recordValue(val_CabMessage1);
            }

            m_manager.recordValue(val_VehicleIdentification);

            if (rnd.nextFloat() > 0.8)
            {
                val_DtcStatus.fault_codes = new String[] { "ABCtest", "Second Fault" };
            }
            else
            {
                val_DtcStatus.fault_codes = new String[] { "ABCtest" };
            }

            m_manager.recordValue(val_DtcStatus);

            //--//

            workerSleep(100);
        }
    }
}
