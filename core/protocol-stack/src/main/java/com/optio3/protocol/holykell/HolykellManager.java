/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.holykell;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.asyncawait.AsyncDelay;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.logging.Logger;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.Severity;
import com.optio3.protocol.modbus.ModbusManager;
import com.optio3.protocol.modbus.ModbusManagerBuilder;
import com.optio3.protocol.modbus.transport.SerialTransport;
import com.optio3.protocol.modbus.transport.SerialTransportBuilder;
import com.optio3.protocol.model.ipn.objects.holykell.HolykellModel;
import com.optio3.protocol.model.modbus.ModbusObjectIdentifier;
import com.optio3.protocol.model.modbus.ModbusObjectModelRaw;
import com.optio3.protocol.model.modbus.ModbusObjectType;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypeDescriptor;

public abstract class HolykellManager implements AutoCloseable
{
    public static final Logger LoggerInstance = new Logger(HolykellManager.class);

    //--//

    private enum State
    {
        Idle,
        Success,
        Failure,
    }

    //--//

    private static TypeDescriptor               c_convert          = Reflection.getDescriptor(float.class);
    private static ModbusObjectIdentifier       s_idLevel_hi       = ModbusObjectType.HoldingRegister.allocateIdentifier(0);
    private static ModbusObjectIdentifier       s_idLevel_lo       = ModbusObjectType.HoldingRegister.allocateIdentifier(1);
    private static ModbusObjectIdentifier       s_idTemperature_hi = ModbusObjectType.HoldingRegister.allocateIdentifier(2);
    private static ModbusObjectIdentifier       s_idTemperature_lo = ModbusObjectType.HoldingRegister.allocateIdentifier(3);
    private static List<ModbusObjectIdentifier> s_ids              = Lists.newArrayList(s_idLevel_hi, s_idLevel_lo, s_idTemperature_hi, s_idTemperature_lo);

    private final String        m_modbusPort;
    private final boolean       m_modbusInvert;
    private       ModbusManager m_modbusManager;

    private int           m_pollingPeriod;
    private State         m_currentState = State.Idle;
    private List<Integer> m_deviceIds;

    public HolykellManager(String port,
                           boolean invert)
    {
        m_modbusPort = port;
        m_modbusInvert = invert;
    }

    public void close() throws
                        Exception
    {
        stop();
    }

    //--//

    protected abstract void notifyTransport(String port,
                                            boolean opened,
                                            boolean closed);

    //--//

    public synchronized boolean start()
    {
        if (m_modbusManager == null)
        {
            try
            {
                SerialTransportBuilder transportBuilder = SerialTransportBuilder.newBuilder();
                transportBuilder.setPort(m_modbusPort);
                transportBuilder.setBaudRate(9600);

                SerialTransport transport = transportBuilder.build();

                ModbusManagerBuilder builder = ModbusManagerBuilder.newBuilder();
                builder.setTransport(transport);

                FirmwareHelper f = FirmwareHelper.get();
                f.selectPort(m_modbusPort, FirmwareHelper.PortFlavor.RS485, m_modbusInvert, false);

                m_modbusManager = builder.build();

                m_modbusManager.start((opened, closed) -> notifyTransport(m_modbusPort, opened, closed));
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to start Holykell, due to %s", t);
                return false;
            }
        }

        return true;
    }

    private synchronized void stop() throws
                                     Exception
    {
        if (m_modbusManager != null)
        {
            m_modbusManager.close();
            m_modbusManager = null;
        }
    }

    public synchronized void schedulePolling(int period,
                                             Consumer<HolykellModel> notify)
    {
        if (m_modbusManager != null)
        {
            m_pollingPeriod = period;

            // First time through, schedule immediately.
            executePolling(notify, 0, TimeUnit.SECONDS);
        }
    }

    @AsyncBackground
    private CompletableFuture<Void> executePolling(Consumer<HolykellModel> notify,
                                                   @AsyncDelay long delay,
                                                   @AsyncDelay TimeUnit delayUnit)
    {
        if (m_modbusManager != null)
        {
            try
            {
                if (m_deviceIds == null)
                {
                    List<Integer> deviceIds = Lists.newArrayList();

                    for (int deviceId = 1; deviceId < 10; deviceId++)
                    {
                        try
                        {
                            HolykellModel obj = await(read(deviceId));

                            notify.accept(obj);

                            deviceIds.add(deviceId);
                        }
                        catch (Throwable t)
                        {
                            // Device not present
                        }
                    }

                    m_deviceIds = deviceIds;
                }
                else
                {
                    for (Integer deviceId : m_deviceIds)
                    {
                        HolykellModel obj = await(read(deviceId));

                        notify.accept(obj);
                    }
                }

                transitionToSuccess();
            }
            catch (Throwable t)
            {
                transitionToFailure(t);
            }

            if (m_modbusManager != null)
            {
                executePolling(notify, m_pollingPeriod, TimeUnit.SECONDS);
            }
        }

        return wrapAsync(null);
    }

    private void transitionToSuccess()
    {
        if (m_modbusManager != null)
        {
            switch (m_currentState)
            {
                case Failure:
                    LoggerInstance.info("Holykell polling resumed!");
                    // Fallthrough

                case Idle:
                    m_currentState = State.Success;
                    break;
            }
        }
    }

    private void transitionToFailure(Throwable t)
    {
        if (m_modbusManager != null)
        {
            switch (m_currentState)
            {
                case Idle:
                case Success:
                    if (LoggerInstance.isEnabled(Severity.Debug))
                    {
                        LoggerInstance.debug("Holykell polling failed: %s", t);
                    }
                    else
                    {
                        LoggerInstance.error("Holykell polling failed: %s", LoggerFactory.extractExceptionDescription(t));
                    }

                    m_currentState = State.Failure;
                    break;
            }
        }
    }

    //--//

    public CompletableFuture<HolykellModel> read(int deviceId) throws
                                                               Exception
    {
        HolykellModel obj = new HolykellModel();

        ModbusObjectModelRaw raw = await(m_modbusManager.read(deviceId, s_ids));

        obj.level = validateRange(toFloat(raw, s_idLevel_hi, s_idLevel_lo), 0, 2);
        obj.temperature = validateRange(toFloat(raw, s_idTemperature_hi, s_idTemperature_lo), -50, 100);
        obj.unitId = deviceId;

        return wrapAsync(obj);
    }

    private static float toFloat(ModbusObjectModelRaw raw,
                                 ModbusObjectIdentifier hi,
                                 ModbusObjectIdentifier lo)
    {
        int val = (raw.getIntegerValueUnsigned(hi) << 16) | raw.getIntegerValueUnsigned(lo);

        return (float) c_convert.fromLongValue(val);
    }

    private static float validateRange(float val,
                                       float min,
                                       float max)
    {
        return min <= val && val <= max ? val : Float.NaN;
    }
}