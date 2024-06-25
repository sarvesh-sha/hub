/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.tristar;

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
import com.optio3.lang.Unsigned8;
import com.optio3.logging.Logger;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.Severity;
import com.optio3.protocol.modbus.ModbusManager;
import com.optio3.protocol.modbus.ModbusManagerBuilder;
import com.optio3.protocol.modbus.transport.SerialTransport;
import com.optio3.protocol.modbus.transport.SerialTransportBuilder;
import com.optio3.protocol.model.ipn.objects.morningstar.BaseTriStarModel;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStarFieldModel;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStar_Charger;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStar_EEPROM;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStar_FilteredADC;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStar_MPPT;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStar_Status;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStar_Temperatures;
import com.optio3.protocol.model.modbus.ModbusObjectIdentifier;
import com.optio3.protocol.model.modbus.ModbusObjectModelRaw;
import com.optio3.protocol.model.modbus.ModbusObjectType;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypedBitSet;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public abstract class TriStarManager implements AutoCloseable
{
    public static final Logger LoggerInstance = new Logger(TriStarManager.class);

    //--//

    private enum State
    {
        Idle,
        Success,
        Failure,
    }

    //--//

    private final String                  m_modbusPort;
    private       ModbusManager           m_modbusManager;
    private       CompletableFuture<Void> m_modbusReady;

    private float m_voltageScaling;
    private float m_currentSpacing;

    private int            m_pollingPeriod;
    private State          m_currentState = State.Idle;
    private MonotonousTime m_deadlineForTransportError;

    public TriStarManager(String port)
    {
        m_modbusPort = port;
    }

    public void close() throws
                        Exception
    {
        stop();
    }

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
                f.selectPort(m_modbusPort, FirmwareHelper.PortFlavor.RS232, false, false);

                m_modbusManager = builder.build();

                m_modbusManager.start((opened, closed) -> notifyTransport(m_modbusPort, opened, closed));

                ensureInitialized();

                notifyTransport(m_modbusPort, true, false);
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to start TriStar, due to %s", t);
                return false;
            }
        }

        return true;
    }

    private CompletableFuture<Void> ensureInitialized() throws
                                                        Exception
    {
        if (m_modbusReady != null && m_modbusReady.isCompletedExceptionally())
        {
            m_modbusReady = null;
        }

        if (m_modbusReady == null)
        {
            m_modbusReady = initialize();
        }

        return m_modbusReady;
    }

    private CompletableFuture<Void> initialize() throws
                                                 Exception
    {
        try
        {
            Map<Unsigned8, String> ids = await(m_modbusManager.getIds(1));

            for (Unsigned8 id : ids.keySet())
            {
                LoggerInstance.info("  ID: %s = %s", id, ids.get(id));
            }
        }
        catch (Throwable t)
        {
            // Ignore failures.
        }

        List<ModbusObjectIdentifier> registers = Lists.newArrayList();

        final ModbusObjectIdentifier V_PU_hi = ModbusObjectType.HoldingRegister.allocateIdentifier(0x0000);
        final ModbusObjectIdentifier V_PU_lo = ModbusObjectType.HoldingRegister.allocateIdentifier(0x0001);
        final ModbusObjectIdentifier V_CU_hi = ModbusObjectType.HoldingRegister.allocateIdentifier(0x0002);
        final ModbusObjectIdentifier V_CU_lo = ModbusObjectType.HoldingRegister.allocateIdentifier(0x0003);
        final ModbusObjectIdentifier ver_sw  = ModbusObjectType.HoldingRegister.allocateIdentifier(0x0004);

        registers.add(V_PU_hi);
        registers.add(V_PU_lo);
        registers.add(V_CU_hi);
        registers.add(V_CU_lo);
        registers.add(ver_sw);

        ModbusObjectModelRaw raw = await(m_modbusManager.read(1, registers));

        m_voltageScaling = raw.getIntegerValueUnsigned(V_PU_hi) + (raw.getIntegerValueUnsigned(V_PU_lo) / 65536.0f);
        m_currentSpacing = raw.getIntegerValueUnsigned(V_CU_hi) + (raw.getIntegerValueUnsigned(V_CU_lo) / 65536.0f);

        return wrapAsync(null);
    }

    private synchronized void stop() throws
                                     Exception
    {
        if (m_modbusManager != null)
        {
            m_modbusManager.close();
            m_modbusManager = null;
        }

        m_modbusReady = null;
    }

    //--//

    protected abstract void notifyTransport(String port,
                                            boolean opened,
                                            boolean closed);

    //--//

    public synchronized void schedulePolling(int period,
                                             Consumer<BaseTriStarModel> notify)
    {
        if (m_modbusManager != null)
        {
            m_pollingPeriod = period;

            // First time through, schedule immediately.
            executePolling(notify, 0, TimeUnit.SECONDS);
        }
    }

    @AsyncBackground
    private CompletableFuture<Void> executePolling(Consumer<BaseTriStarModel> notify,
                                                   @AsyncDelay long delay,
                                                   @AsyncDelay TimeUnit delayUnit)
    {
        if (m_modbusManager != null)
        {
            try
            {
                {
                    TriStar_FilteredADC obj = await(read(TriStar_FilteredADC.class));

                    notify.accept(obj);
                }

                {
                    TriStar_Temperatures obj = await(read(TriStar_Temperatures.class));

                    notify.accept(obj);
                }

                {
                    TriStar_Status obj = await(read(TriStar_Status.class));

                    notify.accept(obj);
                }

                {
                    TriStar_Charger obj = await(read(TriStar_Charger.class));

                    notify.accept(obj);
                }

                {
                    TriStar_MPPT obj = await(read(TriStar_MPPT.class));

                    notify.accept(obj);
                }

                {
                    TriStar_EEPROM obj = await(read(TriStar_EEPROM.class));

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
                    LoggerInstance.info("TriStar polling resumed!");
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
                            LoggerInstance.debug("TriStar polling failed: %s", t);
                        }
                        else
                        {
                            LoggerInstance.error("TriStar polling failed: %s", LoggerFactory.extractExceptionDescription(t));
                        }

                        m_currentState = State.Failure;
                    }
                    break;
            }
        }
    }

    //--//

    public <T extends BaseTriStarModel> CompletableFuture<T> read(Class<T> clz) throws
                                                                                Exception
    {
        await(ensureInitialized());

        T instance = Reflection.newInstance(clz);

        Map<String, TriStarFieldModel> registers = BaseTriStarModel.collectRegisters(clz);

        List<ModbusObjectIdentifier> ids = Lists.newArrayList();
        for (TriStarFieldModel fieldModel : registers.values())
        {
            ids.add(fieldModel.pdu);

            if (fieldModel.pduLow != null)
            {
                ids.add(fieldModel.pduLow);
            }
        }

        ModbusObjectModelRaw raw = await(m_modbusManager.read(1, ids));

        for (String field : registers.keySet())
        {
            TriStarFieldModel fieldModel = registers.get(field);

            if (fieldModel.pdu.type == ModbusObjectType.HoldingRegister)
            {
                long value;

                if (fieldModel.signed)
                {
                    if (fieldModel.pduLow != null)
                    {
                        int high = raw.getIntegerValueSigned(fieldModel.pdu);
                        int low  = raw.getIntegerValueUnsigned(fieldModel.pduLow);

                        value = high * 65536 + low;
                    }
                    else
                    {
                        value = raw.getIntegerValueSigned(fieldModel.pdu);
                    }
                }
                else
                {
                    if (fieldModel.pduLow != null)
                    {
                        int high = raw.getIntegerValueUnsigned(fieldModel.pdu);
                        int low  = raw.getIntegerValueUnsigned(fieldModel.pduLow);

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

                    if (fieldModel.voltageScaling)
                    {
                        value2 *= m_voltageScaling;
                    }

                    if (fieldModel.currentScaling)
                    {
                        value2 *= m_currentSpacing;
                    }

                    instance.setField(field, Reflection.coerceNumber(value2, Reflection.getRawType(fieldModel.type)));
                }
                else
                {
                    instance.setField(field, Reflection.coerceNumber(value, Reflection.getRawType(fieldModel.type)));
                }
            }
            else
            {
                instance.setField(field, raw.getBooleanValue(fieldModel.pdu));
            }
        }

        return wrapAsync(instance);
    }
}