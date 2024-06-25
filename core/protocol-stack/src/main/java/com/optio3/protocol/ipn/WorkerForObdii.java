/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.concurrency.Executors;
import com.optio3.protocol.model.obdii.ObdiiObjectModel;
import com.optio3.protocol.model.obdii.iso15765.CalculatedEngineLoad;
import com.optio3.protocol.model.obdii.iso15765.DistanceTraveledWithMalfunction;
import com.optio3.protocol.model.obdii.iso15765.DtcCheck;
import com.optio3.protocol.model.obdii.iso15765.DtcStatus;
import com.optio3.protocol.model.obdii.iso15765.EngineCoolantTemperature;
import com.optio3.protocol.model.obdii.iso15765.EngineHours;
import com.optio3.protocol.model.obdii.iso15765.EngineOilTemperature;
import com.optio3.protocol.model.obdii.iso15765.EngineRPM;
import com.optio3.protocol.model.obdii.iso15765.FuelPressure;
import com.optio3.protocol.model.obdii.iso15765.FuelSystemStatus;
import com.optio3.protocol.model.obdii.iso15765.IntakeAirTemperature;
import com.optio3.protocol.model.obdii.iso15765.IntakeManifoldAbsolutePressure;
import com.optio3.protocol.model.obdii.iso15765.MassAirFlowRate;
import com.optio3.protocol.model.obdii.iso15765.Odometer;
import com.optio3.protocol.model.obdii.iso15765.RunTimeSinceEngineStart;
import com.optio3.protocol.model.obdii.iso15765.SupportedPIDs;
import com.optio3.protocol.model.obdii.iso15765.TimeRunWithMalfunction;
import com.optio3.protocol.model.obdii.iso15765.TimingAdvance;
import com.optio3.protocol.model.obdii.iso15765.VIN;
import com.optio3.protocol.model.obdii.iso15765.VehicleSpeed;
import com.optio3.protocol.obdii.ObdiiManager;
import com.optio3.serialization.Reflection;

public final class WorkerForObdii implements IpnWorker
{
    private final IpnManager                  m_manager;
    private final String                      m_obdiiPort;
    private final int                         m_obdiiFrequency;
    private       ObdiiManager                m_obdiiManager;
    private       Map<Integer, SupportedPIDs> m_obdii_SupportedPIDs;
    private       Map<Integer, VIN>           m_obdii_VIN;

    public WorkerForObdii(IpnManager manager,
                          String obdiiPort,
                          int obdiiFrequency)
    {
        m_manager = manager;
        m_obdiiPort = obdiiPort;
        m_obdiiFrequency = obdiiFrequency;
    }

    @Override
    public void startWorker()
    {
        m_obdiiManager = new ObdiiManager(m_obdiiPort, m_obdiiFrequency)
        {
            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
                m_manager.notifyTransport(port, opened, closed);
            }
        };

        m_obdiiManager.start();

        queueObdiiPoll(0);

        m_manager.updateDiscoveryLatency(10, TimeUnit.SECONDS);
    }

    @Override
    public void stopWorker()
    {
        if (m_obdiiManager != null)
        {
            m_obdiiManager.close();
            m_obdiiManager = null;
        }
    }

    @Override
    public <T> T accessSubManager(Class<T> clz)
    {
        T mgr = Reflection.as(m_obdiiManager, clz);
        if (mgr != null)
        {
            return mgr;
        }

        return null;
    }

    //--//

    private void queueObdiiPoll(int delay)
    {
        Executors.scheduleOnDefaultPool(this::pollObdii, delay, TimeUnit.SECONDS);
    }

    private void pollObdii()
    {
        ObdiiManager mgr = m_obdiiManager;
        if (mgr != null)
        {
            try
            {
                if (m_obdii_SupportedPIDs == null)
                {
                    Map<Integer, SupportedPIDs> supportedPIDs = Maps.newHashMap();

                    Map<Integer, SupportedPIDs.Req00> map00 = mgr.requestPdu(SupportedPIDs.Req00.class, 1, TimeUnit.SECONDS);
                    for (SupportedPIDs.Req00 value : map00.values())
                    {
                        collectPids(supportedPIDs, value.sourceAddress, value.pids);
                    }

                    Map<Integer, SupportedPIDs.Req20> map20 = mgr.requestPdu(SupportedPIDs.Req20.class, 1, TimeUnit.SECONDS);
                    for (SupportedPIDs.Req20 value : map20.values())
                    {
                        collectPids(supportedPIDs, value.sourceAddress, value.pids);
                    }

                    Map<Integer, SupportedPIDs.Req40> map40 = mgr.requestPdu(SupportedPIDs.Req40.class, 1, TimeUnit.SECONDS);
                    for (SupportedPIDs.Req40 value : map40.values())
                    {
                        collectPids(supportedPIDs, value.sourceAddress, value.pids);
                    }

                    Map<Integer, SupportedPIDs.Req60> map60 = mgr.requestPdu(SupportedPIDs.Req60.class, 1, TimeUnit.SECONDS);
                    for (SupportedPIDs.Req60 value : map60.values())
                    {
                        collectPids(supportedPIDs, value.sourceAddress, value.pids);
                    }

                    Map<Integer, SupportedPIDs.Req80> map80 = mgr.requestPdu(SupportedPIDs.Req80.class, 1, TimeUnit.SECONDS);
                    for (SupportedPIDs.Req80 value : map80.values())
                    {
                        collectPids(supportedPIDs, value.sourceAddress, value.pids);
                    }

                    Map<Integer, SupportedPIDs.ReqA0> mapA0 = mgr.requestPdu(SupportedPIDs.ReqA0.class, 1, TimeUnit.SECONDS);
                    for (SupportedPIDs.ReqA0 value : mapA0.values())
                    {
                        collectPids(supportedPIDs, value.sourceAddress, value.pids);
                    }

                    if (!supportedPIDs.isEmpty())
                    {
                        for (SupportedPIDs pid : supportedPIDs.values())
                        {
                            m_manager.recordValue(pid);
                        }

                        m_obdii_SupportedPIDs = supportedPIDs;
                    }
                }

                if (m_obdii_VIN == null)
                {
                    m_obdii_VIN = fetchObdii(mgr, VIN.class);
                }

                {
                    Map<Integer, DtcStatus> mapStatus = fetchObdii(mgr, DtcStatus.class);
                    Map<Integer, DtcCheck>  mapCheck  = mgr.requestPdu(DtcCheck.class, 1, TimeUnit.SECONDS);
                    for (DtcCheck check : mapCheck.values())
                    {
                        if (mapStatus == null || !mapStatus.containsKey(check.sourceAddress))
                        {
                            DtcStatus status = new DtcStatus();
                            status.sourceAddress = check.sourceAddress;
                            m_manager.recordValue(status);
                        }
                    }
                }

                fetchObdii(mgr, CalculatedEngineLoad.class);
                fetchObdii(mgr, DistanceTraveledWithMalfunction.class);
                fetchObdii(mgr, EngineCoolantTemperature.class);
                fetchObdii(mgr, EngineHours.class);
                fetchObdii(mgr, EngineOilTemperature.class);
                fetchObdii(mgr, EngineRPM.class);
                fetchObdii(mgr, FuelPressure.class);
                fetchObdii(mgr, FuelSystemStatus.class);
                fetchObdii(mgr, IntakeAirTemperature.class);
                fetchObdii(mgr, IntakeManifoldAbsolutePressure.class);
                fetchObdii(mgr, MassAirFlowRate.class);
                fetchObdii(mgr, Odometer.class);
                fetchObdii(mgr, RunTimeSinceEngineStart.class);
                fetchObdii(mgr, TimeRunWithMalfunction.class);
                fetchObdii(mgr, TimingAdvance.class);
                fetchObdii(mgr, VehicleSpeed.class);
            }
            catch (Throwable t)
            {
                // Ignore failures.
            }

            queueObdiiPoll(10);
        }
    }

    private void collectPids(Map<Integer, SupportedPIDs> supportedPIDs,
                             int sourceAddress,
                             List<String> pids)
    {
        if (!pids.isEmpty())
        {
            SupportedPIDs obj = supportedPIDs.get(sourceAddress);
            if (obj == null)
            {
                obj = new SupportedPIDs();
                obj.sourceAddress = sourceAddress;
                obj.pids = Lists.newArrayList();

                supportedPIDs.put(sourceAddress, obj);
            }

            obj.pids.addAll(pids);
        }
    }

    private <T extends ObdiiObjectModel> Map<Integer, T> fetchObdii(ObdiiManager mgr,
                                                                    Class<T> clz)
    {
        Map<Integer, T> messages = mgr.requestPdu(clz, 1, TimeUnit.SECONDS);
        for (T value : messages.values())
        {
            m_manager.recordValue(value);
        }
        return messages.isEmpty() ? null : messages;
    }
}