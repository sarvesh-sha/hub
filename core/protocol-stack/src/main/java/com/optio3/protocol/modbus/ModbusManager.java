/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.collection.WeakLinkedList;
import com.optio3.concurrency.AsyncSemaphore;
import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned8;
import com.optio3.logging.Logger;
import com.optio3.logging.RedirectingLogger;
import com.optio3.protocol.modbus.model.pdu.ReadCoils;
import com.optio3.protocol.modbus.model.pdu.ReadDeviceIdentification;
import com.optio3.protocol.modbus.model.pdu.ReadDiscreteInputs;
import com.optio3.protocol.modbus.model.pdu.ReadHoldingRegisters;
import com.optio3.protocol.modbus.model.pdu.ReadInputRegisters;
import com.optio3.protocol.modbus.pdu.ApplicationPDU;
import com.optio3.protocol.modbus.transport.AbstractTransport;
import com.optio3.protocol.model.modbus.ModbusObjectIdentifier;
import com.optio3.protocol.model.modbus.ModbusObjectModelRaw;
import com.optio3.serialization.Reflection;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.function.AsyncBiConsumerWithException;
import com.optio3.util.function.BiConsumerWithException;

public final class ModbusManager extends RedirectingLogger implements AutoCloseable
{
    static final class Config
    {
        AbstractTransport transport;

        int maxParallelRequests = 1;

        int defaultRetries = 5;
        int defaultTimeout = 500;

        void close() throws
                     Exception
        {
            if (transport != null)
            {
                transport.close();
                transport = null;
            }
        }
    }

    //--//

    public static final Logger LoggerInstance = new Logger(ModbusManager.class);

    //--//

    private final Config m_config;

    private AtomicInteger m_transactionId = new AtomicInteger((int) (Math.random() * 65535));

    private final WeakLinkedList<TransactionIdListener> m_invokeListeners  = new WeakLinkedList<>();
    private final Map<Integer, AsyncSemaphore>          m_concurrentAccess = Maps.newHashMap();

    ModbusManager(Config config)
    {
        super(LoggerInstance);

        m_config = config;
    }

    public void start(BiConsumerWithException<Boolean, Boolean> notifyTransportState)
    {
        m_config.transport.start(this, notifyTransportState);
    }

    public void close() throws
                        Exception
    {
        m_config.close();
    }

    //--//

    public CompletableFuture<Map<Unsigned8, String>> getIds(Integer deviceIdentifier) throws
                                                                                      Exception
    {
        Map<Unsigned8, String> objects      = Maps.newHashMap();
        Unsigned8              nextObjectId = Unsigned8.box(0);

        while (true)
        {
            ReadDeviceIdentification req = new ReadDeviceIdentification();
            req.objectId = nextObjectId;

            ServiceRequestHandle<ReadDeviceIdentification, ReadDeviceIdentification.Response> handle = postNew(deviceIdentifier, req, ReadDeviceIdentification.Response.class);

            ReadDeviceIdentification.Response res = await(handle.result(), 1000, TimeUnit.MILLISECONDS);

            for (ReadDeviceIdentification.ObjectValue value : res.values)
            {
                objects.put(value.objectId, value.text);
            }

            if (res.moreFollows != 0xFF || Unsigned8.compare(nextObjectId, res.nextObjectId) >= 0)
            {
                break;
            }

            nextObjectId = res.nextObjectId;
        }

        return wrapAsync(objects);
    }

    public CompletableFuture<ModbusObjectModelRaw> read(Integer deviceIdentifier,
                                                        Collection<ModbusObjectIdentifier> selectors) throws
                                                                                                      Exception
    {
        List<ModbusObjectIdentifier> workList_DiscreteInput   = Lists.newArrayList();
        List<ModbusObjectIdentifier> workList_Coil            = Lists.newArrayList();
        List<ModbusObjectIdentifier> workList_HoldingRegister = Lists.newArrayList();
        List<ModbusObjectIdentifier> workList_InputRegister   = Lists.newArrayList();

        ModbusObjectModelRaw res = new ModbusObjectModelRaw();

        for (ModbusObjectIdentifier id : selectors)
        {
            switch (id.type)
            {
                case DiscreteInput:
                    workList_DiscreteInput.add(id);
                    break;

                case Coil:
                    workList_Coil.add(id);
                    break;

                case HoldingRegister:
                    workList_HoldingRegister.add(id);
                    break;

                case InputRegister:
                    workList_InputRegister.add(id);
                    break;
            }
        }

        await(forEachRange(workList_DiscreteInput, (start, count) -> readDiscreteInputs(deviceIdentifier, res, start, count)));
        await(forEachRange(workList_Coil, (start, count) -> readCoils(deviceIdentifier, res, start, count)));
        await(forEachRange(workList_HoldingRegister, (start, count) -> readHoldingRegisters(deviceIdentifier, res, start, count)));
        await(forEachRange(workList_InputRegister, (start, count) -> readInputRegisters(deviceIdentifier, res, start, count)));

        return wrapAsync(res);
    }

    //--//

    CompletableFuture<AsyncSemaphore.Holder> acquireAccessToTransport(Integer target)
    {
        AsyncSemaphore semaphore;

        synchronized (m_concurrentAccess)
        {
            semaphore = m_concurrentAccess.get(target);
            if (semaphore == null)
            {
                semaphore = new AsyncSemaphore(m_config.maxParallelRequests);
                m_concurrentAccess.put(target, semaphore);
            }
        }

        return semaphore.acquire();
    }

    //--//

    private CompletableFuture<Void> forEachRange(List<ModbusObjectIdentifier> list,
                                                 AsyncBiConsumerWithException<ModbusObjectIdentifier, ModbusObjectIdentifier> callback) throws
                                                                                                                                        Exception
    {
        final int maxCount = 125;

        ModbusObjectIdentifier first = null;
        ModbusObjectIdentifier last  = null;

        list.sort(ModbusObjectIdentifier::compareTo);

        int count = 0;

        for (ModbusObjectIdentifier id : list)
        {
            if (first == null)
            {
                first = id;
            }
            else if (last.number + 1 != id.number || count >= maxCount)
            {
                await(callback.accept(first, last));

                first = id;
                count = 0;
            }

            last = id;
            count++;
        }

        if (first != null)
        {
            await(callback.accept(first, last));
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> readDiscreteInputs(Integer deviceIdentifier,
                                                       ModbusObjectModelRaw obj,
                                                       ModbusObjectIdentifier start,
                                                       ModbusObjectIdentifier end) throws
                                                                                   Exception
    {
        int count = end.number - start.number + 1;

        ReadDiscreteInputs req = new ReadDiscreteInputs();
        req.startingAddress = Unsigned16.box(start.number);
        req.quantity = Unsigned16.box(count);

        ServiceRequestHandle<ReadDiscreteInputs, ReadDiscreteInputs.Response> handle = postNew(deviceIdentifier, req, ReadDiscreteInputs.Response.class);

        ReadDiscreteInputs.Response res  = await(handle.result(), 5, TimeUnit.SECONDS);
        BitSet                      bits = res.asBits();
        for (int i = 0; i < count; i++)
        {
            obj.setValue(start.type.allocateIdentifier(start.number + i), bits.get(i));
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> readCoils(Integer deviceIdentifier,
                                              ModbusObjectModelRaw obj,
                                              ModbusObjectIdentifier start,
                                              ModbusObjectIdentifier end) throws
                                                                          Exception
    {
        int count = end.number - start.number + 1;

        ReadCoils req = new ReadCoils();
        req.startingAddress = Unsigned16.box(start.number);
        req.quantity = Unsigned16.box(count);

        ServiceRequestHandle<ReadCoils, ReadCoils.Response> handle = postNew(deviceIdentifier, req, ReadCoils.Response.class);

        ReadCoils.Response res  = await(handle.result(), 5, TimeUnit.SECONDS);
        BitSet             bits = res.asBits();
        for (int i = 0; i < count; i++)
        {
            obj.setValue(start.type.allocateIdentifier(start.number + i), bits.get(i));
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> readHoldingRegisters(Integer deviceIdentifier,
                                                         ModbusObjectModelRaw obj,
                                                         ModbusObjectIdentifier start,
                                                         ModbusObjectIdentifier end) throws
                                                                                     Exception
    {
        int count = end.number - start.number + 1;

        ReadHoldingRegisters req = new ReadHoldingRegisters();
        req.startingAddress = Unsigned16.box(start.number);
        req.quantity = Unsigned16.box(count);

        ServiceRequestHandle<ReadHoldingRegisters, ReadHoldingRegisters.Response> handle = postNew(deviceIdentifier, req, ReadHoldingRegisters.Response.class);

        ReadHoldingRegisters.Response res = await(handle.result(), 5, TimeUnit.SECONDS);
        for (int i = 0; i < count; i++)
        {
            obj.setValue(start.type.allocateIdentifier(start.number + i), res.results[i]);
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> readInputRegisters(Integer deviceIdentifier,
                                                       ModbusObjectModelRaw obj,
                                                       ModbusObjectIdentifier start,
                                                       ModbusObjectIdentifier end) throws
                                                                                   Exception
    {
        int count = end.number - start.number + 1;

        ReadInputRegisters req = new ReadInputRegisters();
        req.startingAddress = Unsigned16.box(start.number);
        req.quantity = Unsigned16.box(count);

        ServiceRequestHandle<ReadInputRegisters, ReadInputRegisters.Response> handle = postNew(deviceIdentifier, req, ReadInputRegisters.Response.class);

        ReadInputRegisters.Response res = await(handle.result(), 5, TimeUnit.SECONDS);
        for (int i = 0; i < count; i++)
        {
            obj.setValue(start.type.allocateIdentifier(start.number + i), res.results[i]);
        }

        return wrapAsync(null);
    }

    private <T extends ApplicationPDU, U extends ApplicationPDU.Response> ServiceRequestHandle<T, U> postNew(Integer deviceIdentifier,
                                                                                                             T req,
                                                                                                             Class<U> clz)
    {
        return new ServiceRequestHandle<T, U>(this, deviceIdentifier, req, m_config.defaultRetries, m_config.defaultTimeout, TimeUnit.MILLISECONDS, clz);
    }

    //--//

    TransactionIdListener registerForTransactionId(Integer deviceIdentifier,
                                                   IApplicationPduListener listener)
    {
        int                   transactionId = m_transactionId.incrementAndGet() & 0xFFFF;
        TransactionIdListener handle        = new TransactionIdListener(this, deviceIdentifier, transactionId, listener);

        synchronized (m_invokeListeners)
        {
            m_invokeListeners.add(handle);
        }

        return handle;
    }

    void removeListener(TransactionIdListener handle)
    {
        synchronized (m_invokeListeners)
        {
            m_invokeListeners.remove(handle);
        }
    }

    IApplicationPduListener locateListener(int transactionId)
    {
        synchronized (m_invokeListeners)
        {
            for (TransactionIdListener v : m_invokeListeners)
            {
                if (v.getTransactionId() == transactionId)
                {
                    return v.getListener();
                }
            }
        }

        return null;
    }

    //--//

    @AsyncBackground
    CompletableFuture<Void> sendDirect(Integer destination,
                                       int transactionId,
                                       OutputBuffer ob)
    {
        try
        {
            await(m_config.transport.send(destination, transactionId, ob));
        }
        catch (Exception e)
        {
            LoggerInstance.debugVerbose("sendDirect failed due to %s", e);
        }

        return wrapAsync(null);
    }

    public void processResponse(int transactionId,
                                int deviceIdentifier,
                                ApplicationPDU apdu)
    {
        ApplicationPDU.Response apdu_resp = Reflection.as(apdu, ApplicationPDU.Response.class);
        if (apdu_resp != null)
        {
            IApplicationPduListener listener = locateListener(transactionId);
            if (listener != null)
            {
                listener.processResponse(deviceIdentifier, apdu_resp);
            }
        }
    }
}

