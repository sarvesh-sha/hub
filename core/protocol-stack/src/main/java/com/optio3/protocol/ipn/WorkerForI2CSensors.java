/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.concurrency.Executors;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.logging.Logger;
import com.optio3.protocol.common.ServiceWorker;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.config.I2CSensor;
import com.optio3.protocol.model.config.I2CSensor_MCP3428;
import com.optio3.protocol.model.config.I2CSensor_SHT3x;
import com.optio3.protocol.model.config.KnownI2C;
import com.optio3.protocol.model.ipn.objects.sensors.IpnCurrent;
import com.optio3.protocol.model.ipn.objects.sensors.IpnHumidity;
import com.optio3.protocol.model.ipn.objects.sensors.IpnI2CSensor;
import com.optio3.protocol.model.ipn.objects.sensors.IpnTemperature;
import com.optio3.protocol.model.ipn.objects.sensors.IpnVoltage;
import com.optio3.serialization.Reflection;
import com.optio3.stream.InputBuffer;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;

public final class WorkerForI2CSensors extends ServiceWorker implements IpnWorker
{
    static class MovingAverage
    {
        double accumulator;
        int    samplesCollected;

        public void add(double v)
        {
            accumulator += v;
            samplesCollected++;
        }

        public double getAndReset()
        {
            double value = samplesCollected > 0 ? accumulator / samplesCollected : Double.NaN;

            accumulator      = 0;
            samplesCollected = 0;

            return value;
        }
    }

    static abstract class SensorDetails<T extends I2CSensor>
    {
        private ScheduledFuture<?> m_delayedWarning;
        private boolean            m_warnedOfFailure;

        final T model;

        long nextSample;

        protected SensorDetails(T model)
        {
            this.model = model;
        }

        public void computeNextSamplingTime(long nowMilli)
        {
            int samplingPeriodMilli = (int) (model.samplingPeriod / Math.max(1, model.averagingSamples) * 1000);

            nextSample = TimeUtils.adjustGranularity(nowMilli + samplingPeriodMilli, samplingPeriodMilli);
        }

        public void processSample(FirmwareHelper helper,
                                  long nowMilli)
        {
            if (nowMilli > nextSample)
            {
                executeSample(helper);
                computeNextSamplingTime(nowMilli);
            }
        }

        protected abstract void executeSample(FirmwareHelper helper);

        protected synchronized void reportSamplingError(String fmt,
                                                        Object... args)
        {
            if (!m_warnedOfFailure && m_delayedWarning == null)
            {
                m_delayedWarning = Executors.scheduleOnDefaultPool(() ->
                                                                   {
                                                                       m_warnedOfFailure = true;
                                                                       LoggerInstance.error(fmt, args);
                                                                   }, (int) (10 * model.samplingPeriod), TimeUnit.SECONDS);
            }
        }

        protected synchronized void reportSamplingErrorResolution(String fmt,
                                                                  Object... args)
        {
            dismissSamplingError();

            if (m_warnedOfFailure)
            {
                LoggerInstance.info(fmt, args);
                m_warnedOfFailure = false;
            }
        }

        private synchronized void dismissSamplingError()
        {
            if (m_delayedWarning != null)
            {
                m_delayedWarning.cancel(false);
                m_delayedWarning = null;
            }
        }
    }

    class SensorDetails_SHT3x extends SensorDetails<I2CSensor_SHT3x>
    {
        private final MovingAverage m_temperature = new MovingAverage();
        private final MovingAverage m_humidity    = new MovingAverage();
        private       boolean       m_gotSample;

        SensorDetails_SHT3x(I2CSensor_SHT3x model)
        {
            super(model);
        }

        @Override
        protected void executeSample(FirmwareHelper helper)
        {
            if (helper.writeI2C(model.bus, KnownI2C.SHT3x.defaultAddress, 0, 0, new byte[] { 36, 0 }))
            {
                workerSleep(30);

                byte[] result = helper.readI2C(model.bus, KnownI2C.SHT3x.defaultAddress, 0, 0, 6);
                if (result != null && result.length == 6)
                {
                    try (InputBuffer ib = InputBuffer.createFrom(result))
                    {
                        ib.littleEndian = false;

                        int temperature = ib.read2BytesUnsigned();
                        ib.read1ByteUnsigned();
                        int humidity = ib.read2BytesUnsigned();

                        m_temperature.add(-45.0 + (175.0 * temperature / 65535));
                        m_humidity.add(100.0 * humidity / 65535);

                        if (m_temperature.samplesCollected >= model.averagingSamples)
                        {
                            var val = new IpnTemperature();
                            val.temperature      = (float) m_temperature.getAndReset();
                            val.bus              = model.bus;
                            val.equipmentClass   = model.equipmentClass;
                            val.instanceSelector = model.instanceSelector;
                            m_manager.recordValue(nextSample / 1000.0, val);
                        }

                        if (m_humidity.samplesCollected >= model.averagingSamples)
                        {
                            var val = new IpnHumidity();
                            val.humidity         = (float) m_humidity.getAndReset();
                            val.bus              = model.bus;
                            val.equipmentClass   = model.equipmentClass;
                            val.instanceSelector = model.instanceSelector;
                            m_manager.recordValue(nextSample / 1000.0, val);
                        }
                    }

                    m_gotSample = true;
                    reportSamplingErrorResolution("Reconnected SHT30x on bus %d!", model.bus);
                    return;
                }
            }

            if (!m_gotSample)
            {
                reportSamplingError("No SHT30x detected on bus %d...", model.bus);
            }
            else
            {
                reportSamplingError("No samples from SHT30x on bus %d...", model.bus);
            }
        }
    }

    class SensorDetails_MCP3428 extends SensorDetails<I2CSensor_MCP3428>
    {
        private final MovingAverage m_values = new MovingAverage();
        private final byte          m_cmd;
        private final float         m_conversionFactor;
        private       boolean       m_gotSample;

        SensorDetails_MCP3428(I2CSensor_MCP3428 model)
        {
            super(model);

            int   averagingSamples = Math.max(1, model.averagingSamples);
            float samplingPeriod   = model.samplingPeriod / averagingSamples;
            if (samplingPeriod < 0.5f) // Max 2 sample per second per channel.
            {
                model.samplingPeriod = 0.5f * averagingSamples;
            }

            // 128  =  1 in bit 7   = Start conversion.
            //   8  = 10 in bit 4-3 = 16 bits (15 Samples per second)
            //        xx in bit 6-5 = channel
            //        xx in bit 1-0 = gain.
            int cmd = 128 + 8 + ((model.channel & 0x3) << 5);

            float fullRange = (2.048f / 32768);

            if (model.gain <= 1)
            {
                m_conversionFactor = fullRange;
            }
            else if (model.gain <= 2)
            {
                m_conversionFactor = fullRange / 2;
                cmd |= 1;
            }
            else if (model.gain <= 4)
            {
                m_conversionFactor = fullRange / 4;
                cmd |= 2;
            }
            else
            {
                m_conversionFactor = fullRange / 8;
                cmd |= 3;
            }

            m_cmd = (byte) cmd;

            LoggerInstance.debug("SensorDetails_MCP3428: ch %d: cmd=%02x conversionFactor=%f", model.channel, m_cmd, m_conversionFactor);
        }

        @Override
        protected void executeSample(FirmwareHelper helper)
        {
            if (helper.writeI2C(-1, KnownI2C.MCP3428.defaultAddress, 0, 0, new byte[] { m_cmd }))
            {
                workerSleep(1000 / 14); // It converts at 15 samples per second, wait a bit longer.

                byte[] result = helper.readI2C(-1, KnownI2C.MCP3428.defaultAddress, 0, 0, 3);
                if (result != null && result.length == 3)
                {
                    try (InputBuffer ib = InputBuffer.createFrom(result))
                    {
                        ib.littleEndian = false;

                        int   rawValue = ib.read2BytesSigned();
                        float voltage  = rawValue * m_conversionFactor;
                        int   status   = ib.read1ByteUnsigned();

                        if ((status & 128) == 0)
                        {
                            float value = ((voltage - model.conversionOffsetPre) / model.conversionScale) + model.conversionOffsetPost;
                            LoggerInstance.debugVerbose("SensorDetails_MCP3428: ch %d: raw=%d voltage=%fV value=%f", model.channel, rawValue, voltage, value);

                            m_values.add(value);

                            if (m_values.samplesCollected >= model.averagingSamples)
                            {
                                IpnI2CSensor obj;

                                if (WellKnownPointClass.SensorCurrent.asWrapped()
                                                                     .equals(model.pointClass))
                                {
                                    var val = new IpnCurrent();
                                    val.current = (float) m_values.getAndReset();
                                    obj         = val;
                                }
                                else if (WellKnownPointClass.SensorVoltage.asWrapped()
                                                                          .equals(model.pointClass))
                                {
                                    var val = new IpnVoltage();
                                    val.voltage = (float) m_values.getAndReset();
                                    obj         = val;
                                }
                                else if (WellKnownPointClass.SensorTemperature.asWrapped()
                                                                              .equals(model.pointClass))
                                {
                                    var val = new IpnTemperature();
                                    val.temperature = (float) m_values.getAndReset();
                                    obj             = val;
                                }
                                else if (WellKnownPointClass.SensorHumidity.asWrapped()
                                                                           .equals(model.pointClass))
                                {
                                    var val = new IpnHumidity();
                                    val.humidity = (float) m_values.getAndReset();
                                    obj          = val;
                                }
                                else
                                {
                                    obj = null;
                                }

                                if (obj != null)
                                {
                                    obj.channel          = model.channel;
                                    obj.equipmentClass   = model.equipmentClass;
                                    obj.instanceSelector = model.instanceSelector;
                                    m_manager.recordValue(nextSample / 1000.0, obj);
                                }
                            }
                        }
                    }

                    m_gotSample = true;
                    reportSamplingErrorResolution("Reconnected MCP3428 on channel %d!", model.channel);
                    return;
                }
            }

            if (!m_gotSample)
            {
                reportSamplingError("No MCP3428 detected...");
            }
            else
            {
                reportSamplingError("No samples from MCP3428 on channel %d...", model.channel);
            }
        }
    }

    public static final Logger LoggerInstance = new Logger(WorkerForI2CSensors.class);

    private final IpnManager      m_manager;
    public final  List<I2CSensor> m_i2cSensors;

    public WorkerForI2CSensors(IpnManager manager,
                               List<I2CSensor> i2cSensors)
    {
        super(IpnManager.LoggerInstance, "I2C Sensor", 15, 2000);

        m_manager    = manager;
        m_i2cSensors = i2cSensors;
    }

    @Override
    public void startWorker()
    {
        start();

        m_manager.updateDiscoveryLatency(15, TimeUnit.SECONDS);
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

        if (helper.supportsI2C())
        {
            if (helper.supportsI2Cmultiplex())
            {
                LoggerInstance.info("Found I2C multiplexer");

                {
                    int[]  scan = helper.scanI2C(Integer.MAX_VALUE);
                    String line = formatScan(helper, scan);
                    if (line != null)
                    {
                        LoggerInstance.info("Found %s on main bus", line);
                    }

                    if (!canContinue())
                    {
                        return;
                    }
                }

                for (int bus = 0; bus < 8; bus++)
                {
                    int[]  scan = helper.scanI2C(bus);
                    String line = formatScan(helper, scan);
                    if (line != null)
                    {
                        LoggerInstance.info("Found %s on bus %d", line, bus);
                    }

                    if (!canContinue())
                    {
                        return;
                    }
                }
            }

            //--//

            var sensors = CollectionUtils.transformToListNoNulls(m_i2cSensors, (model) ->
            {
                var model_SHT3x = Reflection.as(model, I2CSensor_SHT3x.class);
                if (model_SHT3x != null)
                {
                    return new SensorDetails_SHT3x(model_SHT3x);
                }

                var model_MCP3428 = Reflection.as(model, I2CSensor_MCP3428.class);
                if (model_MCP3428 != null)
                {
                    return new SensorDetails_MCP3428(model_MCP3428);
                }

                return null;
            });

            long now = TimeUtils.nowMilliUtc();
            for (var sensor : sensors)
            {
                sensor.computeNextSamplingTime(now);
            }

            while (canContinue())
            {
                now = TimeUtils.nowMilliUtc();

                long nextSample = Long.MAX_VALUE;
                for (var sensor : sensors)
                {
                    sensor.processSample(helper, now);
                    nextSample = Math.min(nextSample, sensor.nextSample);
                }

                now = TimeUtils.nowMilliUtc();
                if (nextSample > now)
                {
                    workerSleep((int) Math.max(1_000, nextSample - now));
                }
            }
        }
    }

    private static String formatScan(FirmwareHelper helper,
                                     int[] scan)
    {
        StringBuilder sb = new StringBuilder();

        if (scan != null)
        {
            for (int address : scan)
            {
                if (address != 0)
                {
                    com.optio3.infra.waypoint.KnownI2C deviceModel = helper.resolveI2C(Integer.MAX_VALUE, address, true);

                    if (sb.length() > 0)
                    {
                        sb.append(", ");
                    }

                    sb.append(String.format("%s(%02x)", deviceModel, address));
                }
            }
        }

        return sb.length() > 0 ? sb.toString() : null;
    }
}
