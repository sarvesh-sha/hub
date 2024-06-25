/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.protocol.model.ipn.IpnObjectPostProcess;
import com.optio3.protocol.model.ipn.IpnObjectsState;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.ConsumerWithException;
import org.apache.commons.lang3.StringUtils;

public abstract class IpnManager implements AutoCloseable,
                                            IpnObjectModel.INotifySample
{
    public static class State implements IpnObjectsState
    {
        public final TreeMap<String, IpnObjectModel> objects = new TreeMap<>();

        //--//

        @Override
        public int size()
        {
            return objects.size();
        }

        @Override
        public synchronized void enumerateValues(boolean sorted,
                                                 ConsumerWithException<IpnObjectModel> callback) throws
                                                                                                 Exception
        {
            Collection<String> names = objects.keySet();

            if (sorted)
            {
                List<String> namesSorted = Lists.newArrayList(names);

                namesSorted.sort(String::compareToIgnoreCase);

                names = namesSorted;
            }

            for (String name : names)
            {
                IpnObjectModel object = objects.get(name);
                callback.accept(object);
            }
        }

        @Override
        public synchronized IpnObjectModel set(IpnObjectModel object)
        {
            return objects.put(object.extractId(), object);
        }

        @Override
        public IpnObjectModel getById(IpnObjectModel object)
        {
            return objects.get(object.extractId());
        }

        @Override
        public synchronized <T extends IpnObjectModel> T getByClass(Class<T> clz)
        {
            for (IpnObjectModel object : objects.values())
            {
                T object2 = Reflection.as(object, clz);
                if (object2 != null)
                {
                    return object2;
                }
            }

            return null;
        }
    }

    //--//

    public static class ForValues
    {
    }

    public static final Logger LoggerInstance          = new Logger(IpnManager.class);
    public static final Logger LoggerInstanceForValues = LoggerInstance.createSubLogger(ForValues.class);

    //--//

    private State m_currentState = new State();

    private final ProtocolConfigForIpn m_cfg;
    private       MonotonousTime       m_discoveryLatency;

    private final List<IpnWorker> m_workers = Lists.newArrayList();

    //--//

    public IpnManager(ProtocolConfigForIpn cfg)
    {
        m_cfg = cfg;
    }

    public void start()
    {
        updateDiscoveryLatency(2, TimeUnit.SECONDS);

        ProtocolConfigForIpn cfg = m_cfg;

        if (cfg.simulate)
        {
            addWorker(new WorkerForSimulation(this, cfg.accelerometerFrequency, cfg.accelerometerRange, cfg.accelerometerThreshold));
        }
        else
        {
            if (cfg.accelerometerFrequency > 0)
            {
                addWorker(new WorkerForAccelerometer(this, cfg.accelerometerFrequency, cfg.accelerometerRange, cfg.accelerometerThreshold));
            }

            if (!cfg.i2cSensors.isEmpty())
            {
                addWorker(new WorkerForI2CSensors(this, cfg.i2cSensors));
            }

            if (StringUtils.isNotBlank(cfg.ipnPort))
            {
                addWorker(new WorkerForIpn(this, cfg.ipnPort, cfg.ipnBaudrate, cfg.ipnInvert));
            }

            if (StringUtils.isNotBlank(cfg.gpsPort))
            {
                addWorker(new WorkerForGPS(this, cfg.gpsPort));
            }

            if (StringUtils.isNotBlank(cfg.canPort))
            {
                addWorker(new WorkerForCAN(this, cfg.canPort, cfg.canFrequency, cfg.canNoTermination, cfg.canInvert));
            }

            if (StringUtils.isNotBlank(cfg.obdiiPort))
            {
                if (cfg.obdiiPort.startsWith("can"))
                {
                    addWorker(new WorkerForJ1939(this, cfg.obdiiPort, cfg.obdiiFrequency, cfg.obdiiInvert));
                }
                else
                {
                    addWorker(new WorkerForObdii(this, cfg.obdiiPort, cfg.obdiiFrequency));
                }
            }

            if (StringUtils.isNotBlank(cfg.epsolarPort))
            {
                addWorker(new WorkerForEpSolar(this, cfg.epsolarPort, cfg.epsolarInvert));
            }

            if (StringUtils.isNotBlank(cfg.holykellPort))
            {
                addWorker(new WorkerForHolykell(this, cfg.holykellPort, cfg.holykellInvert));
            }

            if (StringUtils.isNotBlank(m_cfg.argohytosPort))
            {
                addWorker(new WorkerForArgoHytos(this, cfg.argohytosPort));
            }

            if (StringUtils.isNotBlank(m_cfg.stealthpowerPort))
            {
                addWorker(new WorkerForStealthPower(this, cfg.stealthpowerPort));
            }

            if (StringUtils.isNotBlank(cfg.tristarPort))
            {
                addWorker(new WorkerForTristar(this, cfg.tristarPort));
            }

            if (StringUtils.isNotBlank(cfg.victronPort))
            {
                addWorker(new WorkerForVictron(this, cfg.victronPort));
            }

            if (StringUtils.isNotBlank(cfg.montageBluetoothGatewayPort))
            {
                addWorker(new WorkerForMontageBluetoothGateway(this, cfg.montageBluetoothGatewayPort));
            }
        }
    }

    private void addWorker(IpnWorker worker)
    {
        try
        {
            worker.startWorker();
            m_workers.add(worker);
        }
        catch (Throwable t)
        {
            Class<? extends IpnWorker> clz = worker.getClass();
            LoggerInstance.error("Failed to start %s, due to %s", clz.getSimpleName(), t);
        }
    }

    public void close() throws
                        Exception
    {
        for (IpnWorker worker : m_workers)
        {
            worker.stopWorker();
        }

        m_workers.clear();
    }

    //--//

    protected abstract void notifyTransport(String port,
                                            boolean opened,
                                            boolean closed);

    protected abstract void streamSamples(IpnObjectModel obj) throws
                                                              Exception;

    protected abstract void notifySamples(IpnObjectModel obj,
                                          String field);

    protected abstract byte[] detectedStealthPowerBootloader(byte bootloadVersion,
                                                             byte hardwareVersion,
                                                             byte hardwareRevision) throws
                                                                                    IOException;

    protected abstract void completedStealthPowerBootloader(int statusCode);

    protected <T> T accessSubManager(Class<T> clz)
    {
        for (IpnWorker worker : m_workers)
        {
            T res = worker.accessSubManager(clz);
            if (res != null)
            {
                return res;
            }
        }

        return null;
    }

    //--//

    void updateDiscoveryLatency(int amount,
                                TimeUnit unit)
    {
        MonotonousTime timeout = TimeUtils.computeTimeoutExpiration(amount, unit);

        if (m_discoveryLatency == null || m_discoveryLatency.isBefore(timeout))
        {
            m_discoveryLatency = timeout;
        }
    }

    //--//

    void recordValue(IpnObjectModel val)
    {
        recordValue(TimeUtils.nowEpochSeconds(), val);
    }

    void recordValue(double timestamp,
                     IpnObjectModel val)
    {
        if (LoggerInstanceForValues.isEnabled(Severity.DebugVerbose))
        {
            try
            {
                LoggerInstanceForValues.debugVerbose("Got %s: %s",
                                                     val.getClass()
                                                        .getName(),
                                                     val.serializeToJson());
            }
            catch (JsonProcessingException e)
            {
                // Ignore failures.
            }
        }

        if (val.shouldIgnoreSample())
        {
            return;
        }

        try
        {
            streamSamples(val);
        }
        catch (Exception e)
        {
            LoggerInstanceForValues.debug("Streaming callback failed due to %s", e);
        }

        IpnObjectModel trackingVal = m_currentState.getById(val);
        if (trackingVal == null)
        {
            trackingVal = BaseObjectModel.copySingleProperty(val, null);

            m_currentState.set(trackingVal);
        }

        @SuppressWarnings("unchecked") IpnObjectPostProcess<IpnObjectModel> itf = Reflection.as(val, IpnObjectPostProcess.class);
        if (itf != null)
        {
            try
            {
                itf.postProcess(m_currentState, trackingVal);
            }
            catch (Exception e)
            {
                LoggerInstance.debugVerbose("IpnObjectPostProcess failed due to %s", e);
            }
        }

        try
        {
            for (FieldModel fieldModel : val.getDescriptors())
            {
                String                    fieldName              = fieldModel.name;
                int                       minimumTemporalSpacing = fieldModel.debounceSeconds;
                IpnObjectModel.FieldState fieldState             = trackingVal.getFieldState(fieldName);

                Object oldVal = trackingVal.getField(fieldName);
                Object newVal = val.getField(fieldName);

                IpnObjectModel.FieldUpdateReason reason;
                boolean                          debounceFromLastValue = true;

                while (true)
                {
                    if (fieldModel.fixedRate)
                    {
                        // Don't emit immediately.
                        reason = IpnObjectModel.FieldUpdateReason.FixedRate;
                        break;
                    }

                    IpnObjectModel.FieldSample lastValue = fieldState.getLastValue();
                    if (lastValue == null)
                    {
                        // No previous value, send immediately.
                        reason                 = IpnObjectModel.FieldUpdateReason.Changed;
                        minimumTemporalSpacing = 0;
                        break;
                    }

                    if (Objects.deepEquals(oldVal, newVal))
                    {
                        //
                        // No changes => no event
                        //
                        reason = IpnObjectModel.FieldUpdateReason.Identical;
                        break;
                    }

                    if (fieldModel.isNumeric(val))
                    {
                        double oldValNum = toNumber(oldVal);
                        double newValNum = toNumber(newVal);

                        if (fieldModel.minimumDelta != 0.0)
                        {
                            if (Math.abs(oldValNum - newValNum) < fieldModel.minimumDelta)
                            {
                                //
                                // Change smaller than threshold => no event
                                //
                                reason = IpnObjectModel.FieldUpdateReason.BelowThreshold;
                                break;
                            }
                        }

                        //
                        // We have a change big enough to allow for an event generation.
                        //
                        // Should we debounce before sending it?
                        //
                        switch (fieldModel.stickyMode)
                        {
                            case None:
                                break;

                            case StickyActive:
                                if (newValNum > oldValNum)
                                {
                                    // Transition to active, send immediately.
                                    minimumTemporalSpacing = 0;
                                }
                                else
                                {
                                    // We want to debounce from now.
                                    debounceFromLastValue = false;
                                }
                                break;

                            case StickyInactive:
                                if (newValNum < oldValNum)
                                {
                                    // Transition to inactive, send immediately.
                                    minimumTemporalSpacing = 0;
                                }
                                else
                                {
                                    // We want to debounce from now.
                                    debounceFromLastValue = false;
                                }
                                break;

                            case StickyZero:
                                if (newValNum == 0.0)
                                {
                                    // Transition to zero, send immediately.
                                    minimumTemporalSpacing = 0;
                                }
                                else
                                {
                                    // We want to debounce from now.
                                    debounceFromLastValue = false;
                                }
                                break;

                            case StickyNonZero:
                                if (newValNum != 0.0)
                                {
                                    // Transition to non-zero, send immediately.
                                    minimumTemporalSpacing = 0;
                                }
                                else
                                {
                                    // We want to debounce from now.
                                    debounceFromLastValue = false;
                                }
                                break;
                        }
                    }

                    reason = IpnObjectModel.FieldUpdateReason.Changed;
                    break;
                }

                IpnObjectModel.FieldSample fieldSample = fieldState.allocateNewSample(reason,
                                                                                      timestamp,
                                                                                      fieldModel.temporalResolution,
                                                                                      minimumTemporalSpacing,
                                                                                      debounceFromLastValue,
                                                                                      newVal,
                                                                                      fieldModel.flushOnChange);
                fieldState.push(fieldSample, this);
            }
        }
        catch (Exception e)
        {
            LoggerInstance.debugVerbose("Streaming delta callback for %s failed due to %s", val.extractId(), e);
        }
    }

    private double toNumber(Object val)
    {
        return BoxingUtils.get(Reflection.coerceNumber(val, Double.class), 0.0);
    }

    //--//

    @Override
    public void acceptSample(IpnObjectModel obj,
                             String field)
    {
        try
        {
            notifySamples(obj, field);
        }
        catch (Exception e)
        {
            LoggerInstance.debugVerbose("Streaming delta callback for %s failed due to %s", obj.extractId(), e);
        }
    }

    public CompletableFuture<Void> getReadySignal() throws
                                                    Exception
    {
        Duration sleep = TimeUtils.remainingTime(m_discoveryLatency);
        if (sleep != null)
        {
            await(sleep(sleep.toMillis(), TimeUnit.MILLISECONDS));
        }

        return wrapAsync(null);
    }

    public State getState()
    {
        return m_currentState;
    }

    public State clearState()
    {
        State oldState = m_currentState;
        m_currentState = new State();
        return oldState;
    }
}
