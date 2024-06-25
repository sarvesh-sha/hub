/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.epsolar;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
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
import com.optio3.protocol.model.ipn.objects.epsolar.BaseEpSolarModel;
import com.optio3.protocol.model.ipn.objects.epsolar.EpSolarFieldModel;
import com.optio3.protocol.model.ipn.objects.epsolar.EpSolar_BatteryParameters;
import com.optio3.protocol.model.ipn.objects.epsolar.EpSolar_LoadParameters;
import com.optio3.protocol.model.ipn.objects.epsolar.EpSolar_RealTimeData;
import com.optio3.protocol.model.modbus.ModbusObjectIdentifier;
import com.optio3.protocol.model.modbus.ModbusObjectModelRaw;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypedBitSet;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public abstract class EpSolarManager implements AutoCloseable
{
    public static final Logger LoggerInstance = new Logger(EpSolarManager.class);

    //--//

    private enum State
    {
        Idle,
        Success,
        Failure,
    }

    //--//

    private final String        m_modbusPort;
    private final boolean       m_modbusInvert;
    private       ModbusManager m_modbusManager;

    private int            m_pollingPeriod;
    private State          m_currentState = State.Idle;
    private MonotonousTime m_deadlineForTransportError;

    public EpSolarManager(String port,
                          boolean invert)
    {
        m_modbusPort   = port;
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
                transportBuilder.setBaudRate(115200);

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
                LoggerInstance.error("Failed to start EpSolar, due to %s", t);
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
                                             Consumer<BaseEpSolarModel> notify)
    {
        if (m_modbusManager != null)
        {
            m_pollingPeriod = period;

            // First time through, schedule immediately.
            executePolling(notify, 0, TimeUnit.SECONDS);
        }
    }

    @AsyncBackground
    private CompletableFuture<Void> executePolling(Consumer<BaseEpSolarModel> notify,
                                                   @AsyncDelay long delay,
                                                   @AsyncDelay TimeUnit delayUnit)
    {
        if (m_modbusManager != null)
        {
            try
            {
                {
                    EpSolar_RealTimeData obj = await(read(1, EpSolar_RealTimeData.class));

                    notify.accept(obj);
                }

                {
                    EpSolar_BatteryParameters obj = await(read(1, EpSolar_BatteryParameters.class));

                    notify.accept(obj);
                }

                {
                    EpSolar_LoadParameters obj = await(read(1, EpSolar_LoadParameters.class));

                    notify.accept(obj);
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
            m_deadlineForTransportError = null;

            switch (m_currentState)
            {
                case Failure:
                    LoggerInstance.info("EpSolar polling resumed!");
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
                    if (m_deadlineForTransportError == null)
                    {
                        m_deadlineForTransportError = TimeUtils.computeTimeoutExpiration(2, TimeUnit.MINUTES);
                    }

                    if (TimeUtils.isTimeoutExpired(m_deadlineForTransportError))
                    {
                        if (LoggerInstance.isEnabled(Severity.Debug))
                        {
                            LoggerInstance.debug("EpSolar polling failed: %s", t);
                        }
                        else
                        {
                            LoggerInstance.error("EpSolar polling failed: %s", LoggerFactory.extractExceptionDescription(t));
                        }

                        m_currentState = State.Failure;
                    }
                    break;
            }
        }
    }

    //--//

    public <T extends BaseEpSolarModel> CompletableFuture<T> read(int deviceId,
                                                                  Class<T> clz) throws
                                                                                Exception
    {
        T instance = Reflection.newInstance(clz);
        instance.unitId = deviceId;

        Map<String, EpSolarFieldModel> registers = BaseEpSolarModel.collectRegisters(clz);

        List<ModbusObjectIdentifier> ids = Lists.newArrayList();
        for (EpSolarFieldModel fieldModel : registers.values())
        {
            ids.add(fieldModel.pdu);

            if (fieldModel.pduHigh != null)
            {
                ids.add(fieldModel.pduHigh);
            }
        }

        ModbusObjectModelRaw raw = await(m_modbusManager.read(deviceId, ids));

        for (String field : registers.keySet())
        {
            EpSolarFieldModel fieldModel = registers.get(field);

            switch (fieldModel.pdu.type)
            {
                case HoldingRegister:
                case InputRegister:
                {
                    long value;

                    if (fieldModel.signed)
                    {
                        if (fieldModel.pduHigh != null)
                        {
                            int high = raw.getIntegerValueUnsigned(fieldModel.pduHigh);
                            int low  = raw.getIntegerValueSigned(fieldModel.pdu);

                            value = high * 65536 + low;
                        }
                        else
                        {
                            value = raw.getIntegerValueSigned(fieldModel.pdu);
                        }
                    }
                    else
                    {
                        if (fieldModel.pduHigh != null)
                        {
                            int high = raw.getIntegerValueUnsigned(fieldModel.pduHigh);
                            int low  = raw.getIntegerValueUnsigned(fieldModel.pdu);

                            value = high * 65536 + low;
                        }
                        else
                        {
                            value = raw.getIntegerValueUnsigned(fieldModel.pdu);
                        }
                    }

                    if (fieldModel.bitsetClass != null)
                    {
                        BitSet bs = new BitSet();

                        int pos = 0;

                        while (value != 0)
                        {
                            if ((value & 1) != 0)
                            {
                                bs.set(pos);
                            }

                            pos++;
                            value >>>= 1;
                        }

                        TypedBitSet<?> typedBs = Reflection.newInstance(fieldModel.bitsetClass, bs);

                        instance.setField(field, typedBs);
                    }
                    else if (fieldModel.enumClass != null)
                    {
                        instance.setField(field, Reflection.coerceNumber(value, fieldModel.enumClass));
                    }
                    else if (fieldModel.fixedScaling != 1.0)
                    {
                        double value2 = value;

                        value2 *= fieldModel.fixedScaling;

                        instance.setField(field, Reflection.coerceNumber(value2, Reflection.getRawType(fieldModel.type)));
                    }
                    else
                    {
                        instance.setField(field, Reflection.coerceNumber(value, Reflection.getRawType(fieldModel.type)));
                    }

                    break;
                }

                case DiscreteInput:
                case Coil:
                {
                    instance.setField(field, raw.getBooleanValue(fieldModel.pdu));
                    break;
                }
            }
        }

        instance.postProcess();

        return wrapAsync(instance);
    }
}