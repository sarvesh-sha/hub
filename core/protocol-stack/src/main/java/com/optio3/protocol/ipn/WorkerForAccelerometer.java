/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.logging.Logger;
import com.optio3.protocol.common.ServiceWorker;
import com.optio3.protocol.model.ipn.objects.sensors.IpnAccelerometer;

public final class WorkerForAccelerometer extends ServiceWorker implements IpnWorker
{
    public static final Logger LoggerInstance = new Logger(WorkerForAccelerometer.class);

    private final IpnManager m_manager;
    private final float      m_accelerometerFrequency;
    private final float      m_accelerometerRange;
    private final float      m_accelerometerThreshold;
    private       boolean    m_gotData;

    public WorkerForAccelerometer(IpnManager manager,
                                  float accelerometerFrequency,
                                  float accelerometerRange,
                                  float accelerometerThreshold)
    {
        super(IpnManager.LoggerInstance, "Accelerometer", 5, 2000);

        m_manager = manager;
        m_accelerometerFrequency = accelerometerFrequency;
        m_accelerometerRange = accelerometerRange;
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
        FirmwareHelper helper = FirmwareHelper.get();

        if (helper.supportsAccelerometer())
        {
            helper.configureAccelerometer(m_accelerometerFrequency, m_accelerometerRange);

            int sleepFor10Samples = (int) (10 * (1000.0 / m_accelerometerFrequency));

            IpnAccelerometer accel = new IpnAccelerometer();

            while (canContinue())
            {
                workerSleep(sleepFor10Samples);

                helper.streamAccelerometerSamples((timestamp, x, y, z) ->
                                                  {
                                                      if (hasChanged(accel, x, y, z))
                                                      {
                                                          accel.x = x;
                                                          accel.y = y;
                                                          accel.z = z;
                                                      }

                                                      m_manager.recordValue(timestamp, accel);

                                                      m_gotData = true;
                                                  });

                if (m_gotData)
                {
                    reportErrorResolution("Reconnected to Accelerometer!");

                    m_gotData = false;
                }
                else
                {
                    reportError("No samples from Accelerometer...");
                }
            }
        }
    }

    private boolean hasChanged(IpnAccelerometer accel,
                               int x,
                               int y,
                               int z)
    {
        double magnitude1 = Math.sqrt(x * x + y * y + z * z);
        double magnitude2 = Math.sqrt(accel.x * accel.x + accel.y * accel.y + accel.z * accel.z);

        if (magnitude1 > 0.1 && magnitude2 > 0.1)
        {
            double percentMagnitudeDifference = 100 * Math.abs(magnitude1 - magnitude2) / Math.max(magnitude1, magnitude2);
            double dotProduct                 = (accel.x * x + accel.y * y + accel.z * z) / (magnitude1 * magnitude2);
            double percentAngleDifference     = 100 * (Math.abs(dotProduct) < 1 ? Math.acos(dotProduct) : 0) / Math.PI;

            LoggerInstance.debug("Magnitude: %f <-> %f  dot: %f mag: %f angle: %f", magnitude1, magnitude2, dotProduct, percentMagnitudeDifference, percentAngleDifference);

            if (percentMagnitudeDifference < m_accelerometerThreshold && percentAngleDifference < m_accelerometerThreshold)
            {
                return false;
            }
        }

        return true;
    }
}
