/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;
import static java.util.Objects.requireNonNull;

import java.lang.reflect.Array;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.validation.constraints.NotNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.concurrency.AsyncMutex;
import com.optio3.concurrency.CompletableFutureWithSafeTimeout;
import com.optio3.lang.Unsigned32;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.bacnet.model.enums.NetworkPriority;
import com.optio3.protocol.bacnet.model.pdu.application.ConfirmedRequestPDU;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ReadProperty;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ReadPropertyMultiple;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.WriteProperty;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.WhoIs;
import com.optio3.protocol.model.DeviceReachability;
import com.optio3.protocol.model.TransportPerformanceCounters;
import com.optio3.protocol.model.bacnet.BACnetAddress;
import com.optio3.protocol.model.bacnet.BACnetDeviceAddress;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.PropertyType;
import com.optio3.protocol.model.bacnet.constructed.BACnetAddressBinding;
import com.optio3.protocol.model.bacnet.constructed.BACnetError;
import com.optio3.protocol.model.bacnet.constructed.ReadAccessResult;
import com.optio3.protocol.model.bacnet.constructed.ReadAccessSpecification;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetRejectReason;
import com.optio3.protocol.model.bacnet.enums.BACnetSegmentation;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetServicesSupported;
import com.optio3.protocol.model.bacnet.error.BACnetAbortedException;
import com.optio3.protocol.model.bacnet.error.BACnetRejectedException;
import com.optio3.protocol.model.bacnet.error.BACnetSegmentationException;
import com.optio3.protocol.model.bacnet.objects.device;
import com.optio3.protocol.model.transport.TransportAddress;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;

public final class DeviceIdentity implements Comparable<DeviceIdentity>
{
    public static final Logger LoggerInstance          = BACnetManager.LoggerInstance.createSubLogger(DeviceIdentity.class);
    public static final Logger LoggerInstanceForValues = LoggerInstance.createSubLogger(ObjectDescriptor.class);

    // Parameter for the weighted sum used to smooth out typical response times.
    private static final int AveragingWindowMax = 10;

    //--//

    public static final Comparator<ObjectDescriptor> ObjectDescriptorComparatorInstance = (o1, o2) ->
    {
        if (o1 == o2)
        {
            return 0;
        }

        if (o1 == null)
        {
            return 1;
        }

        if (o2 == null)
        {
            return -1;
        }

        return BACnetObjectIdentifier.compare(o1.id, o2.id);
    };

    public class ObjectDescriptor implements Comparable<ObjectDescriptor>
    {
        public final BACnetObjectIdentifier id;

        private boolean official;

        private Set<BACnetPropertyIdentifierOrUnknown> m_properties;

        private Map<BACnetPropertyIdentifierOrUnknown, Integer> m_failuresScoreboard;

        //--//

        ObjectDescriptor(BACnetObjectIdentifier id)
        {
            this.id = id;
        }

        //--//

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }

            ObjectDescriptor that = Reflection.as(o, ObjectDescriptor.class);
            if (that == null)
            {
                return false;
            }

            return id.equals(that.id);
        }

        @Override
        public int hashCode()
        {
            return id.hashCode();
        }

        @Override
        public int compareTo(ObjectDescriptor o)
        {
            return ObjectDescriptorComparatorInstance.compare(this, o);
        }

        @Override
        public String toString()
        {
            return id.toString();
        }

        //--//

        public boolean hasKnownProperty(BACnetPropertyIdentifierOrUnknown propId)
        {
            return id.object_type.hasProperty(propId);
        }

        public CompletableFuture<Set<BACnetPropertyIdentifierOrUnknown>> getProperties() throws
                                                                                         Exception
        {
            if (m_properties == null)
            {
                Set<BACnetPropertyIdentifierOrUnknown> set = await(getPropertiesInner());

                // Make sure object_name is part of the set of properties.
                if (set.add(BACnetPropertyIdentifier.object_name.forRequest()))
                {
                    LoggerInstance.debug("getProperties [%s] Added 'object_name', not part of set of properties...", DeviceIdentity.this);
                }

                m_properties = m_mgr.memoizeProperties(set);
            }

            return wrapAsync(m_properties);
        }

        private CompletableFuture<Set<BACnetPropertyIdentifierOrUnknown>> getPropertiesInner() throws
                                                                                               Exception
        {
            Set<BACnetPropertyIdentifierOrUnknown> set         = Sets.newHashSet();
            TimeoutSpec                            timeoutSpec = m_mgr.getDefaultTimeout();

            try
            {
                if (!id.object_type.isUnknown())
                {
                    BACnetObjectModel obj = id.allocateNewObject();

                    await(updateArrayProperty(timeoutSpec, obj, BACnetPropertyIdentifier.property_list.forRequest(), null));

                    Object                              value = obj.getValue(BACnetPropertyIdentifier.property_list, null);
                    BACnetPropertyIdentifierOrUnknown[] array = Reflection.as(value, BACnetPropertyIdentifierOrUnknown[].class);
                    if (array != null)
                    {
                        set.addAll(Arrays.asList(array));
                        return wrapAsync(set);
                    }
                }
            }
            catch (Exception e)
            {
                LoggerInstance.debug("getProperties [%s] Reading 'property_list' failed with %s", DeviceIdentity.this, e);
            }

            //
            // Try fetching all the properties with 'all' wildcard property.
            //
            if (couldSupportReadMultiple())
            {
                ReadPropertyMultiple req = new ReadPropertyMultiple();
                req.add(id, BACnetPropertyIdentifier.all);

                ServiceRequestResult<ReadPropertyMultiple.Ack> ack = await(readPropertyMultipleRaw(timeoutSpec, null, null, Duration.of(1, ChronoUnit.SECONDS), req));
                if (ack.value != null)
                {
                    if (LoggerInstanceForValues.isEnabled(Severity.DebugObnoxious))
                    {
                        LoggerInstanceForValues.debugObnoxious("getProperties: %s", ObjectMappers.prettyPrintAsJson(ack.value));
                    }

                    for (ReadAccessResult v : ack.value.list_of_read_access_results)
                    {
                        for (ReadAccessResult.Values v2 : v.list_of_results)
                        {
                            set.add(v2.property_identifier);
                        }
                    }

                    return wrapAsync(set);
                }

                LoggerInstance.debug("getProperties [%s] Reading 'all' property failed with %s", DeviceIdentity.this, ack.failure);
            }

            //
            // Try loading all the known properties in a batch.
            //
            if (id.object_type.value != null)
            {
                BatchReader br = new BatchReader();

                Map<BACnetPropertyIdentifier, PropertyType> knownProperties = id.object_type.value.propertyTypes();

                for (BACnetPropertyIdentifier propId : knownProperties.keySet())
                {
                    br.add(this, propId.forRequest());
                }

                BatchReaderResult res    = await(br.execute(timeoutSpec));
                ObjectReadResult  result = res.values.get(this);

                for (BACnetPropertyIdentifier propId : knownProperties.keySet())
                {
                    if (result.getFailureDetails(propId.forRequest()) == null)
                    {
                        set.add(propId.forRequest());
                    }
                }
            }

            return wrapAsync(set);
        }

        public CompletableFuture<ObjectReadResult> readProperties(Collection<BACnetPropertyIdentifierOrUnknown> input) throws
                                                                                                                       Exception
        {
            BatchReader br = new BatchReader();

            for (BACnetPropertyIdentifierOrUnknown propId : input)
            {
                br.add(this, propId);
            }

            BatchReaderResult res = await(br.execute(m_mgr.getDefaultTimeout()));

            return wrapAsync(res.values.get(this));
        }

        //--//

        public int getFailureCount(BACnetPropertyIdentifierOrUnknown prop)
        {
            Map<BACnetPropertyIdentifierOrUnknown, Integer> map = m_failuresScoreboard;
            if (map == null)
            {
                return 0;
            }

            Integer consecutiveFailures = map.get(prop);
            return consecutiveFailures == null ? 0 : consecutiveFailures;
        }

        public int incrementFailureCount(BACnetPropertyIdentifierOrUnknown prop)
        {
            Map<BACnetPropertyIdentifierOrUnknown, Integer> map = m_failuresScoreboard;
            if (map == null)
            {
                m_failuresScoreboard = map = Maps.newHashMap();
            }

            Integer consecutiveFailures = map.get(prop);
            int     val                 = consecutiveFailures == null ? 0 : consecutiveFailures;

            val += 1;

            map.put(prop, val);

            return val;
        }

        public void resetFailureCount(BACnetPropertyIdentifierOrUnknown prop)
        {
            if (m_failuresScoreboard != null)
            {
                m_failuresScoreboard.remove(prop);

                if (m_failuresScoreboard.isEmpty())
                {
                    m_failuresScoreboard = null;
                }
            }
        }
    }

    public static class ObjectReadResult
    {
        public final BACnetObjectModel state;

        ObjectReadResult(ObjectDescriptor objDesc)
        {
            this.state = objDesc.id.allocateNewObject();
        }

        void setResult(BACnetObjectIdentifier deviceId,
                       BACnetPropertyIdentifierOrUnknown propId,
                       Optional<Unsigned32> arrayIndex,
                       Object value,
                       BACnetError error)
        {
            LoggerInstance.debug("BatchReader [%s] Got value for %s/%s", deviceId, state.getObjectIdentity(), propId);

            if (error != null)
            {
                setFailure(deviceId, propId, error);
            }
            else
            {
                state.setValueWithOptionalIndex(propId, arrayIndex, value);
            }
        }

        void setFailure(BACnetObjectIdentifier deviceId,
                        BACnetPropertyIdentifierOrUnknown propId,
                        Object failure)
        {
            state.setFailure(propId, failure);

            if (LoggerInstance.isEnabled(Severity.Debug))
            {
                BACnetObjectModel.FailureDetails details = state.getFailureDetails(propId);
                Exception                        e       = details.asException(state.getObjectIdentity(), propId);

                LoggerInstance.debug("BatchReader [%s] Failure: %s/%s: %s", deviceId, state.getObjectIdentity(), propId, e.getMessage());
            }
        }

        //--//

        public Set<BACnetPropertyIdentifierOrUnknown> getProperties()
        {
            return state.getAccessedProperties();
        }

        public BACnetObjectModel.FailureDetails getFailureDetails(BACnetPropertyIdentifierOrUnknown propId)
        {
            return state.getFailureDetails(propId);
        }

        public Object getProperty(BACnetPropertyIdentifierOrUnknown propId,
                                  Unsigned32 index) throws
                                                    Exception
        {
            BACnetObjectModel.FailureDetails failure = getFailureDetails(propId);
            if (failure != null)
            {
                throw failure.asException(state.getObjectIdentity(), propId);
            }

            return getPropertyNoThrow(propId, index);
        }

        public Object getPropertyNoThrow(BACnetPropertyIdentifierOrUnknown propId,
                                         Unsigned32 index)
        {
            if (getFailureDetails(propId) != null)
            {
                return null;
            }

            return state.getValue(propId, index);
        }
    }

    static class ObjectAndProperty implements Comparable<ObjectAndProperty>
    {
        final ObjectDescriptor                  objDesc;
        final BACnetPropertyIdentifierOrUnknown propId;

        ObjectAndProperty(ObjectDescriptor objDesc,
                          BACnetPropertyIdentifierOrUnknown propId)
        {
            requireNonNull(objDesc);
            requireNonNull(propId);

            this.objDesc = objDesc;
            this.propId  = propId;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }

            ObjectAndProperty that = Reflection.as(o, ObjectAndProperty.class);
            if (that == null)
            {
                return false;
            }

            return objDesc.id.equals(that.objDesc.id) && propId.equals(that.propId);
        }

        @Override
        public int hashCode()
        {
            int result = objDesc.id.hashCode();
            result = 31 * result + (propId != null ? propId.hashCode() : 0);
            return result;
        }

        @Override
        public int compareTo(ObjectAndProperty o)
        {
            int diff = this.objDesc.id.compareTo(o.objDesc.id);
            if (diff == 0)
            {
                diff = this.propId.compareTo(o.propId);
            }

            return diff;
        }

        @Override
        public String toString()
        {
            return objDesc.id + "/" + propId;
        }
    }

    public static class BatchReaderResult extends TransportPerformanceCounters
    {
        public final Map<ObjectDescriptor, ObjectReadResult> values = Maps.newHashMap();
    }

    public class BatchReader
    {
        private Set<ObjectAndProperty> m_inputs = Collections.emptySet();

        BatchReader()
        {
        }

        public void add(ObjectDescriptor objDesc,
                        BACnetPropertyIdentifierOrUnknown propId)
        {
            if (m_inputs.isEmpty())
            {
                m_inputs = Sets.newHashSet();
            }

            ObjectAndProperty op = new ObjectAndProperty(objDesc, propId);
            m_inputs.add(op);
        }

        public CompletableFuture<BatchReaderResult> execute(@NotNull TimeoutSpec timeoutSpec) throws
                                                                                              Exception
        {
            BatchReaderResult result = new BatchReaderResult();

            LoggerInstance.debug("BatchReader [%s] Start: %d entries to read", DeviceIdentity.this, m_inputs.size());

            List<ObjectAndProperty> toProcess      = Lists.newLinkedList();
            List<ObjectAndProperty> toProcessSlow  = Lists.newLinkedList();
            List<ObjectAndProperty> toProcessArray = Lists.newLinkedList();

            //
            // Distribute and randomize the set of properties.
            //
            {
                ObjectAndProperty[] inputs = m_inputs.toArray(new ObjectAndProperty[0]);
                m_inputs = null;

                Random rnd             = new Random();
                int    toProcessCursor = inputs.length;
                while (toProcessCursor > 0)
                {
                    int               nextPos = rnd.nextInt(toProcessCursor--);
                    ObjectAndProperty last    = inputs[toProcessCursor];

                    ObjectAndProperty op = inputs[nextPos]; // Pick random element in list.
                    inputs[nextPos] = last; // Move the last element in the slot left open.

                    ObjectDescriptor objDesc = op.objDesc;

                    if (objDesc.id.object_type.isArrayProperty(op.propId))
                    {
                        toProcessArray.add(op);
                    }
                    else
                    {
                        if (op.propId.value != BACnetPropertyIdentifier.present_value)
                        {
                            toProcessSlow.add(op);
                        }
                        else
                        {
                            toProcess.add(op);
                        }
                    }
                }

                // Move the less important properties to the back of the queue.
                toProcess.addAll(toProcessSlow);
            }

            //
            // If the device has been marked as unreachable,
            // we'll execute only a single request from the batch and then give up.
            //
            boolean isReachable = wasReachable();
            if (!isReachable)
            {
                isReachable = await(isReachable(null, timeoutSpec, true));
            }

            List<ObjectAndProperty> toProcessQueued     = Lists.newLinkedList();
            int                     maxSize             = (int) (m_maxApdu * 0.9);
            int                     round               = 0;
            int                     roundWithNoProgress = 0;

            //
            // First, read all the non-array properties, trying to fit them in the smallest number of packets possible.
            //
            while (!toProcess.isEmpty())
            {
                int pendingNumBefore = toProcess.size();

                LoggerInstance.debug("BatchReader [%s] Round %d: %d entries with batch size of %d", DeviceIdentity.this, ++round, pendingNumBefore, maxSize);

                if (timeoutSpec.isPastDeadline())
                {
                    LoggerInstance.debug("BatchReader [%s] Past deadline, cancelling remaining %d requests...", DeviceIdentity.this, toProcess.size());

                    for (ObjectAndProperty op : toProcess)
                    {
                        ObjectReadResult target = ensureObjectReadResult(result.values, op.objDesc);

                        target.setFailure(getDeviceId(), op.propId, timeoutSpec.deadlineException);
                    }

                    toProcess.clear();
                    break;
                }

                {
                    ReadPropertyMultiple req = new ReadPropertyMultiple();

                    toProcessQueued.clear();

                    if (isReachable && couldSupportReadMultiple())
                    {
                        //
                        // Add as many item as they'll fit in read request and response.
                        //
                        int sizeTx = 0;
                        int sizeRx = 0;

                        for (ObjectAndProperty op : toProcess)
                        {
                            BACnetManager.EstimatedPayloadSize estimatedSize = m_mgr.estimateSize(op.objDesc.id, op.propId);

                            if (sizeTx == 0)
                            {
                                sizeTx += estimatedSize.requestSizeOverhead;
                            }

                            if (sizeRx == 0 && estimatedSize.responseSizeOverhead > 0)
                            {
                                sizeRx += estimatedSize.responseSizeOverhead;
                            }

                            sizeTx += estimatedSize.requestSize;
                            sizeRx += estimatedSize.responseSize > 0 ? estimatedSize.responseSize : 32;

                            if (sizeTx > maxSize || sizeRx > maxSize)
                            {
                                break;
                            }

                            LoggerInstance.debug("BatchReader [%s] Adding %s to batch, sizeTx=%d sizeRx=%d", DeviceIdentity.this, op, sizeTx, sizeRx);
                            req.add(op.objDesc.id, op.propId);

                            toProcessQueued.add(op);
                        }
                    }

                    boolean madeProgress = false;

                    //
                    // If small batch, read properties one at a time.
                    //
                    if (toProcessQueued.size() < 2)
                    {
                        int propIndex;

                        if (isReachable)
                        {
                            propIndex = 0;
                        }
                        else
                        {
                            propIndex = new Random().nextInt(toProcess.size());
                        }

                        ObjectAndProperty op = toProcess.get(propIndex);
                        LoggerInstance.debug("BatchReader [%s] Calling readPropertyRaw for %s (%d to go)", DeviceIdentity.this, op, toProcess.size());

                        ServiceRequestResult<ReadProperty.Ack> ack = await(readPropertyRaw(timeoutSpec, op.objDesc.id, op.propId));
                        result.accumulate(ack);

                        ObjectReadResult target = ensureObjectReadResult(result.values, op.objDesc);
                        if (ack.value != null)
                        {
                            if (LoggerInstanceForValues.isEnabled(Severity.DebugObnoxious))
                            {
                                LoggerInstanceForValues.debugObnoxious("BatchReader [%s]: %s", DeviceIdentity.this, ObjectMappers.prettyPrintAsJson(ack.value));
                            }

                            ObjectAndProperty found = dispatchResult(result.values, toProcess, ack.value.object_identifier, ack.value.property_identifier, ack.value.property_value, null);
                            if (found != null)
                            {
                                isReachable         = true;
                                madeProgress        = true;
                                roundWithNoProgress = 0;
                            }

                            target.setResult(getDeviceId(), op.propId, Optional.empty(), ack.value.property_value, null);
                        }
                        else if (ack.failure != null)
                        {
                            target.setFailure(getDeviceId(), op.propId, ack.failure);

                            toProcess.remove(op);

                            isReachable         = true;
                            madeProgress        = true;
                            roundWithNoProgress = 0;
                        }
                    }
                    else
                    {
                        ServiceRequestResult<ReadPropertyMultiple.Ack> ack = await(readPropertyMultipleRaw(timeoutSpec, null, null, null, req));
                        result.accumulate(ack);

                        if (ack.value != null)
                        {
                            if (LoggerInstanceForValues.isEnabled(Severity.DebugObnoxious))
                            {
                                LoggerInstanceForValues.debugObnoxious("BatchReader [%s]: %s", DeviceIdentity.this, ObjectMappers.prettyPrintAsJson(ack.value));
                            }

                            for (ReadAccessResult accessResult : ack.value.list_of_read_access_results)
                            {
                                for (ReadAccessResult.Values value : accessResult.list_of_results)
                                {
                                    ObjectAndProperty found = dispatchResult(result.values,
                                                                             toProcess,
                                                                             accessResult.object_identifier,
                                                                             value.property_identifier,
                                                                             value.property_value,
                                                                             value.property_access_error);
                                    if (found != null)
                                    {
                                        isReachable         = true;
                                        madeProgress        = true;
                                        roundWithNoProgress = 0;
                                    }
                                }
                            }
                        }
                        else
                        {
                            if (ack.failure instanceof TimeoutException)
                            {
                                for (ObjectAndProperty op : toProcessQueued)
                                {
                                    LoggerInstance.debug("BatchReader [%s] Got timeout, failing %s", DeviceIdentity.this, op);

                                    ObjectReadResult target = ensureObjectReadResult(result.values, op.objDesc);

                                    target.setFailure(getDeviceId(), op.propId, ack.failure);
                                    toProcess.remove(op);
                                }

                                continue;
                            }

                            LoggerInstance.debug("BatchReader [%s] ReadPropertyMultiple error: %s", DeviceIdentity.this, ack.failure);

                            if (ack.failure instanceof BACnetSegmentationException)
                            {
                                maxSize -= 16;
                            }
                            else if (ack.failure instanceof BACnetAbortedException)
                            {
                                // For any abort, shrink the batch size.
                                maxSize = (int) (maxSize * 0.8);
                            }
                            else
                            {
                                if (ack.failure instanceof BACnetRejectedException)
                                {
                                    BACnetRejectedException e = (BACnetRejectedException) ack.failure;

                                    if (e.reason == BACnetRejectReason.unrecognized_service)
                                    {
                                        m_supportsReadMultiple = false;
                                    }
                                }

                                maxSize = 0;
                            }

                            continue;
                        }
                    }

                    if (!madeProgress && roundWithNoProgress++ < 3)
                    {
                        continue;
                    }
                }

                if (!isReachable)
                {
                    //
                    // We tried at least one property at random, it failed, consider the rest of the batch a failure as well.
                    //
                    Exception e = Exceptions.newRuntimeException("Device [%s] is unreachable", DeviceIdentity.this);

                    for (ObjectAndProperty op : toProcess)
                    {
                        ObjectReadResult target = ensureObjectReadResult(result.values, op.objDesc);

                        target.setFailure(getDeviceId(), op.propId, e);
                    }

                    for (ObjectAndProperty op : toProcessArray)
                    {
                        ObjectReadResult target = ensureObjectReadResult(result.values, op.objDesc);

                        target.setFailure(getDeviceId(), op.propId, e);
                    }

                    toProcess.clear();
                    toProcessArray.clear();
                }

                int pendingNumAfter = toProcess.size();
                if (pendingNumAfter == pendingNumBefore)
                {
                    //
                    // Something went wrong, no progress, just fail all the remaining items.
                    //
                    for (ObjectAndProperty op : toProcess)
                    {
                        LoggerInstance.debug("BatchReader [%s] No progress, failing %s", DeviceIdentity.this, op);

                        ObjectReadResult target = ensureObjectReadResult(result.values, op.objDesc);

                        target.setFailure(getDeviceId(), op.propId, new BACnetAbortedException("No forward progress"));
                    }

                    break;
                }
            }

            //
            // Then read the arrays.
            //
            for (ObjectAndProperty op : toProcessArray)
            {
                ObjectReadResult target = ensureObjectReadResult(result.values, op.objDesc);

                if (timeoutSpec.isPastDeadline())
                {
                    LoggerInstance.debug("BatchReader [%s] Past deadline, cancelling array request...", DeviceIdentity.this);

                    target.setFailure(getDeviceId(), op.propId, timeoutSpec.deadlineException);
                }
                else
                {
                    try
                    {
                        LoggerInstance.debug("BatchReader [%s] Calling updateArrayProperty for %s", DeviceIdentity.this, op);
                        await(updateArrayProperty(timeoutSpec, target.state, op.propId, result));
                    }
                    catch (Throwable t)
                    {
                        target.setFailure(getDeviceId(), op.propId, t);
                    }
                }
            }

            //--//

            return wrapAsync(result);
        }

        private ObjectReadResult ensureObjectReadResult(Map<ObjectDescriptor, ObjectReadResult> results,
                                                        ObjectDescriptor objDesc)
        {
            return results.computeIfAbsent(objDesc, ObjectReadResult::new);
        }

        private ObjectAndProperty dispatchResult(Map<ObjectDescriptor, ObjectReadResult> results,
                                                 List<ObjectAndProperty> toProcess,
                                                 BACnetObjectIdentifier objId,
                                                 BACnetPropertyIdentifierOrUnknown propId,
                                                 Object value,
                                                 BACnetError error)
        {
            for (Iterator<ObjectAndProperty> it = toProcess.iterator(); it.hasNext(); )
            {
                ObjectAndProperty op = it.next();

                if (!op.objDesc.id.equals(objId))
                {
                    continue;
                }

                if (!op.propId.equals(propId))
                {
                    continue;
                }

                it.remove();

                BACnetManager.EstimatedPayloadSize estimatedSize = m_mgr.estimateSize(op.objDesc.id, op.propId);
                estimatedSize.updateResponse(op.objDesc.id, op.propId, value);

                ObjectReadResult target = ensureObjectReadResult(results, op.objDesc);

                target.setResult(getDeviceId(), propId, Optional.empty(), value, error);

                return op;
            }

            LoggerInstance.debug("BatchReader [%s] Received an unexpected result for this object: %s / %s (Expected: %s)", DeviceIdentity.this, objId, propId, toProcess);
            return null;
        }
    }

    public static final Comparator<DeviceIdentity> ComparatorInstance = (o1, o2) ->
    {
        if (o1 == o2)
        {
            return 0;
        }

        if (o1 == null)
        {
            return 1;
        }

        if (o2 == null)
        {
            return -1;
        }

        int diff = BACnetDeviceAddress.compare(o1.m_address, o2.m_address);
        if (diff == 0)
        {
            if (o1.m_transportAddress != null && o2.m_transportAddress != null)
            {
                diff = StringUtils.compare(o1.m_transportAddress.toString(), o2.m_transportAddress.toString());
            }
        }

        return diff;
    };

    private final BACnetManager                                    m_mgr;
    private final CompletableFuture<DeviceIdentity>                m_resolution            = new CompletableFuture<>();
    private final CompletableFutureWithSafeTimeout<DeviceIdentity> m_cancellableResolution = new CompletableFutureWithSafeTimeout<>(m_resolution);
    private final TransportPerformanceCounters                     m_statistics            = new TransportPerformanceCounters();

    private final BACnetDeviceAddress m_address;
    private final ObjectDescriptor    m_deviceDesc;

    private TransportAddress m_transportAddress;
    private BACnetAddress    m_bacnetAddress;

    private BACnetSegmentation m_segmentation = BACnetSegmentation.no_segmentation;
    private int                m_maxApdu      = ConfirmedRequestPDU.MaxAPDU_480;

    //--//

    private final     AsyncMutex         m_objectsFetchMutex = new AsyncMutex();
    private           ObjectDescriptor[] m_objects;
    private transient boolean            m_isObjectListOfficial;

    private boolean m_protocolVersionChecked;
    private long    m_protocolVersion;

    private boolean m_protocolRevisionChecked;
    private long    m_protocolRevision;

    private boolean                 m_servicesChecked;
    private BACnetServicesSupported m_services;

    private transient Boolean m_supportsReadMultiple;

    private final DeviceReachability.State m_reachability    = new DeviceReachability.State(45 * 60);
    private       Duration                 m_responseTimeMax = Duration.ZERO;
    private       long                     m_responseTimeAverage;
    private       int                      m_responseTimeCount;

    //--//

    void setParameters(BACnetSegmentation segmentation,
                       int maxApdu)
    {
        this.m_segmentation = segmentation;
        this.m_maxApdu      = Math.max(128, maxApdu);
    }

    DeviceIdentity(BACnetManager mgr,
                   BACnetDeviceAddress address)
    {
        m_mgr = mgr;

        m_address             = address;
        m_deviceDesc          = new ObjectDescriptor(BACnetObjectType.device.asObjectIdentifier(address.instanceNumber));
        m_deviceDesc.official = true; // Assume the device's object descriptor is the official one.

        flushObjects();
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        DeviceIdentity that = Reflection.as(o, DeviceIdentity.class);
        if (that == null)
        {
            return false;
        }

        if (!Objects.equals(m_address, that.m_address))
        {
            return false;
        }

        if (m_transportAddress != null && that.m_transportAddress != null)
        {
            if (!m_transportAddress.equals(that.m_transportAddress))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return m_address.hashCode();
    }

    @Override
    public int compareTo(DeviceIdentity o)
    {
        return ComparatorInstance.compare(this, o);
    }

    //--//

    public TransportAddress getTransportAddress()
    {
        return m_transportAddress;
    }

    void setTransportAddress(TransportAddress address)
    {
        if (m_transportAddress == null)
        {
            m_transportAddress = address;

            m_resolution.complete(this);
        }
    }

    void setSourceAddress(BACnetAddress bacnetAddress)
    {
        m_bacnetAddress = bacnetAddress;
    }

    void setNetworkCapabilities(BACnetSegmentation segmentation,
                                int maxApdu)
    {
        requireNonNull(segmentation);

        setParameters(segmentation, maxApdu);
    }

    //--//

    public boolean hasAddress()
    {
        return m_transportAddress != null;
    }

    public BACnetDeviceAddress getNetworkAddress()
    {
        return m_address;
    }

    public int getNetworkNumber()
    {
        return m_address.networkNumber;
    }

    public int getInstanceNumber()
    {
        return m_address.instanceNumber;
    }

    public BACnetObjectIdentifier getDeviceId()
    {
        return m_deviceDesc.id;
    }

    public ObjectDescriptor getObjectDescriptor()
    {
        return m_deviceDesc;
    }

    public BACnetAddress getBACnetAddress()
    {
        return m_bacnetAddress;
    }

    public BACnetDeviceDescriptor extractIdentifier()
    {
        BACnetDeviceDescriptor desc = new BACnetDeviceDescriptor();
        desc.address = getNetworkAddress();

        desc.bacnetAddress = getBACnetAddress();

        if (m_transportAddress != null)
        {
            desc.transport = m_transportAddress;
        }

        desc.segmentation = m_segmentation;
        desc.maxAdpu      = m_maxApdu;

        return desc;
    }

    //--//

    public boolean supportsReceiveSegmentation()
    {
        switch (m_segmentation)
        {
            case segmented_both:
            case segmented_receive:
                return true;

            default:
                return false;
        }
    }

    public boolean supportsTransmitSegmentation()
    {
        switch (m_segmentation)
        {
            case segmented_both:
            case segmented_transmit:
                return true;

            default:
                return false;
        }
    }

    public int getMaxApdu()
    {
        return m_maxApdu;
    }

    public Duration getMaxResponseTime()
    {
        return m_responseTimeMax;
    }

    public TimeoutSpec getEstimatedResponseTime(int retries,
                                                float scale)
    {
        if (m_responseTimeCount == 0)
        {
            return m_mgr.getDefaultTimeout();
        }

        return TimeoutSpec.create(retries, Duration.ofNanos((long) (m_responseTimeAverage * scale)));
    }

    //--//

    void updateNetworkStatistics(TransportPerformanceCounters perf)
    {
        m_statistics.accumulate(perf);

        m_mgr.updateNetworkStatistics(this, perf);
    }

    public TransportPerformanceCounters getStatistics()
    {
        return m_statistics;
    }

    //--//

    public void startResolution()
    {
        if (!isResolved())
        {
            //
            // Issue a Who-Is, to try and resolve the device identity.
            //
            int   instanceNumber = getNetworkAddress().instanceNumber;
            WhoIs reg            = new WhoIs();
            reg.setRange(instanceNumber, instanceNumber);
            m_mgr.sendApplicationBroadcastRequest(reg, NetworkPriority.Normal);
        }
    }

    public CompletableFuture<DeviceIdentity> waitForResolution(long timeout,
                                                               TimeUnit unit)
    {
        try
        {
            startResolution();

            await(m_cancellableResolution.waitForCompletion(timeout, unit));

            return wrapAsync(this);
        }
        catch (Exception e)
        {
            return wrapAsync(null);
        }
    }

    public boolean hasValidInstanceNumber()
    {
        return m_address.hasValidInstanceNumber();
    }

    public boolean isResolved()
    {
        return m_transportAddress != null;
    }

    public boolean matchNetworkNumber(int networkNumber)
    {
        if (networkNumber == BACnetAddress.GlobalNetwork)
        {
            return true;
        }

        return m_address.networkNumber == networkNumber;
    }

    public boolean matchNetworkId(int networkNumber,
                                  int instanceNumber)
    {
        if (m_address.instanceNumber != instanceNumber)
        {
            return false;
        }

        return matchNetworkNumber(networkNumber);
    }

    public boolean matchTransport(TransportAddress transport)
    {
        return transport == null || transport.equals(this.getTransportAddress());
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%d:%d", m_address.networkNumber, m_address.instanceNumber));

        if (m_bacnetAddress != null)
        {
            sb.append(String.format(" / %s", m_bacnetAddress));
        }

        if (m_maxApdu > 0)
        {
            sb.append(String.format(" - APDU %d", m_maxApdu));
        }

        if (m_transportAddress != null)
        {
            sb.append(String.format(" at %s", m_transportAddress));
        }

        return sb.toString();
    }

    //--//

    public BatchReader createBatchReader()
    {
        return new BatchReader();
    }

    public CompletableFuture<ServiceRequestResult<ReadProperty.Ack>> readPropertyRaw(@NotNull TimeoutSpec timeoutSpec,
                                                                                     @NotNull BACnetObjectIdentifier objectId,
                                                                                     @NotNull BACnetPropertyIdentifier propId)
    {
        return readPropertyRaw(timeoutSpec, objectId, propId.forRequest());
    }

    public CompletableFuture<ServiceRequestResult<ReadProperty.Ack>> readPropertyRaw(@NotNull TimeoutSpec timeoutSpec,
                                                                                     @NotNull BACnetObjectIdentifier objectId,
                                                                                     @NotNull BACnetPropertyIdentifierOrUnknown propId)
    {
        ReadProperty                                         req    = ReadProperty.newInstance(objectId, propId);
        ServiceRequestHandle<ReadProperty, ReadProperty.Ack> handle = m_mgr.postNew(this, timeoutSpec, null, null, null, req, ReadProperty.Ack.class);

        return handle.result();
    }

    public CompletableFuture<ServiceRequestResult<ReadProperty.Ack>> readArrayPropertyRaw(@NotNull TimeoutSpec timeoutSpec,
                                                                                          @NotNull BACnetObjectIdentifier objectId,
                                                                                          @NotNull BACnetPropertyIdentifierOrUnknown propId,
                                                                                          int index)
    {
        ReadProperty                                         req    = ReadProperty.newInstance(objectId, propId, index);
        ServiceRequestHandle<ReadProperty, ReadProperty.Ack> handle = m_mgr.postNew(this, timeoutSpec, null, null, null, req, ReadProperty.Ack.class);

        return handle.result();
    }

    public <T extends BACnetObjectModel> CompletableFuture<T> readProperty(@NotNull TimeoutSpec timeoutSpec,
                                                                           @NotNull BACnetObjectIdentifier objectId,
                                                                           @NotNull BACnetPropertyIdentifierOrUnknown propId,
                                                                           @NotNull Class<T> clz)
    {
        try
        {
            ServiceRequestResult<ReadProperty.Ack> ack = await(readPropertyRaw(timeoutSpec, objectId, propId));
            if (ack.value != null)
            {
                if (LoggerInstanceForValues.isEnabled(Severity.DebugObnoxious))
                {
                    LoggerInstanceForValues.debugObnoxious("readProperty %s/%s: %s", objectId, propId, ObjectMappers.prettyPrintAsJson(ack.value));
                }

                return wrapAsync(ack.value.toObject(clz));
            }
        }
        catch (Exception e)
        {
            // Ignore failures.
        }

        return wrapAsync(null);
    }

    //--//

    public CompletableFuture<ServiceRequestResult<ReadPropertyMultiple.Ack>> readPropertyMultipleRaw(@NotNull TimeoutSpec timeoutSpec,
                                                                                                     Duration minTimeout,
                                                                                                     Duration maxTimeout,
                                                                                                     Duration maxTimeoutPerOutstandingRequest,
                                                                                                     @NotNull ReadPropertyMultiple req)
    {
        ServiceRequestHandle<ReadPropertyMultiple, ReadPropertyMultiple.Ack> handle = m_mgr.postNew(this,
                                                                                                    timeoutSpec,
                                                                                                    minTimeout,
                                                                                                    maxTimeout,
                                                                                                    maxTimeoutPerOutstandingRequest,
                                                                                                    req,
                                                                                                    ReadPropertyMultiple.Ack.class);

        return handle.result();
    }

    public CompletableFuture<ServiceRequestResult<ReadPropertyMultiple.Ack>> readPropertyMultipleRaw(@NotNull TimeoutSpec timeoutSpec,
                                                                                                     Duration minTimeout,
                                                                                                     Duration maxTimeout,
                                                                                                     Duration maxTimeoutPerOutstandingRequest,
                                                                                                     @NotNull BACnetObjectIdentifier objId,
                                                                                                     BACnetPropertyIdentifier... props)
    {
        ReadPropertyMultiple req = new ReadPropertyMultiple();

        for (BACnetPropertyIdentifier prop : props)
        {
            req.add(objId, prop);
        }

        return readPropertyMultipleRaw(timeoutSpec, minTimeout, maxTimeout, maxTimeoutPerOutstandingRequest, req);
    }

    //--//

    public <T> CompletableFuture<T[]> readArrayProperty(@NotNull TimeoutSpec timeoutSpec,
                                                        @NotNull BACnetObjectIdentifier objectId,
                                                        @NotNull BACnetPropertyIdentifier propId,
                                                        @NotNull Class<T[]> clz) throws
                                                                                 Exception
    {
        return readArrayProperty(timeoutSpec, objectId, propId.forRequest(), clz);
    }

    public <T> CompletableFuture<T[]> readArrayProperty(@NotNull TimeoutSpec timeoutSpec,
                                                        @NotNull BACnetObjectIdentifier objectId,
                                                        @NotNull BACnetPropertyIdentifierOrUnknown propId,
                                                        @NotNull Class<T[]> clz) throws
                                                                                 Exception
    {
        BACnetObjectModel obj = objectId.allocateNewObject();

        await(updateArrayProperty(timeoutSpec, obj, propId, null));

        Object value = obj.getValue(propId, null);
        return wrapAsync(clz.cast(value));
    }

    public CompletableFuture<Void> updateArrayProperty(@NotNull TimeoutSpec timeoutSpec,
                                                       @NotNull BACnetObjectModel obj,
                                                       @NotNull BACnetPropertyIdentifierOrUnknown propIdOrUnknown,
                                                       TransportPerformanceCounters statistics) throws
                                                                                                Exception
    {
        final BACnetPropertyIdentifier propId = propIdOrUnknown.value;
        if (propId == null)
        {
            // If it's not a known property, it won't have a field in the object.
            return wrapAsync(null);
        }

        final BACnetObjectIdentifier objId = obj.getObjectIdentity();

        switch (propId)
        {
            case object_list:
                // Don't try reading the object list in one shot, it can be very large.
                break;

            default:
            {
                ServiceRequestResult<ReadProperty.Ack> ack = await(readPropertyRaw(timeoutSpec, objId, propIdOrUnknown));
                if (statistics != null)
                {
                    statistics.accumulate(ack);
                }

                if (ack.value != null)
                {
                    if (LoggerInstanceForValues.isEnabled(Severity.DebugObnoxious))
                    {
                        LoggerInstanceForValues.debugObnoxious("updateArrayProperty %s/%s: %s", objId, propId, ObjectMappers.prettyPrintAsJson(ack.value));
                    }

                    ack.value.updateObject(LoggerInstance, obj);

                    return wrapAsync(null);
                }
            }
        }

        int length;

        // Read the length of the property.
        if (await(updateArrayPropertyWithRetries(timeoutSpec, 3, statistics, obj, propIdOrUnknown, 0)))
        {
            Object array = obj.getValue(propId, null);
            length = Array.getLength(array);

            LoggerInstance.debug("updateArrayProperty [%s]: %s/%s : array length = %d", this, obj, propIdOrUnknown, length);
        }
        else
        {
            length = Integer.MAX_VALUE;
        }

        int batchMax = Math.max(1, m_maxApdu / 16); // Give a rough estimate of the maximum number of reads we can fit in a packet.
        int index    = 0;

        //
        // Allow long timeouts when reading multiple entries from an array.
        //
        final Duration minTimeout = Duration.of(2, ChronoUnit.SECONDS);
        final Duration maxTimeout = Duration.of(4, ChronoUnit.SECONDS);

        outerLoop:
        while (index < length)
        {
            if (batchMax > 1 && couldSupportReadMultiple())
            {
                ReadPropertyMultiple          req      = new ReadPropertyMultiple();
                final ReadAccessSpecification readSpec = ReadAccessSpecification.newRequest(objId);
                req.list_of_read_access_specifications.add(readSpec);

                //
                // Add N items to the read request.
                //
                for (int batchIndex = 0; batchIndex + index < length && batchIndex < batchMax; batchIndex++)
                {
                    readSpec.add(propIdOrUnknown, index + batchIndex + 1);
                }

                ServiceRequestResult<ReadPropertyMultiple.Ack> ack = await(readPropertyMultipleRaw(timeoutSpec, minTimeout, maxTimeout, maxTimeout, req));
                if (statistics != null)
                {
                    statistics.accumulate(ack);
                }

                if (ack.value != null)
                {
                    if (LoggerInstanceForValues.isEnabled(Severity.DebugObnoxious))
                    {
                        LoggerInstanceForValues.debugObnoxious("updateArrayProperty %s/%s/%d: %s", objId, propId, index, ObjectMappers.prettyPrintAsJson(ack.value));
                    }

                    if (ack.value.list_of_read_access_results.size() != 1)
                    {
                        batchMax = 1;
                        continue;
                    }

                    ReadAccessResult res = ack.value.list_of_read_access_results.get(0);
                    for (ReadAccessResult.Values val : res.list_of_results)
                    {
                        if (val.property_access_error != null)
                        {
                            LoggerInstance.debug("updateArrayProperty [%s]: ReadPropertyMultiple %s/%s  got an error %s.%s at index %d",
                                                 this,
                                                 objId,
                                                 propIdOrUnknown,
                                                 val.property_access_error.error_class,
                                                 val.property_access_error.error_code,
                                                 index);

                            //
                            // If we failed to read the first element of the array and we didn't get a length, reset the array.
                            //
                            if (index == 0 && length == Integer.MAX_VALUE)
                            {
                                obj.setValue(propIdOrUnknown, null, null);
                            }

                            // Got an error, must be the last item, exit.
                            break outerLoop;
                        }

                        Long indexResult = Unsigned32.unboxOptional(val.property_array_index);
                        if (indexResult == null || indexResult != (index + 1))
                        {
                            // Got an error, must be the last item, exit.
                            break outerLoop;
                        }

                        obj.setValueWithOptionalIndex(val.property_identifier, val.property_array_index, val.property_value);

                        index++;
                    }
                }
                else
                {
                    if (ack.failure instanceof TimeoutException)
                    {
                        if (index == 0)
                        {
                            // For timeout for first index, drop to single value mode.
                            batchMax = 1;
                        }
                        else
                        {
                            // For any timeout, shrink the batch size.
                            batchMax /= 2;
                        }

                        continue;
                    }

                    LoggerInstance.debug("updateArrayProperty [%s]: ReadPropertyMultiple error at batch size of %d: %s", this, batchMax, ack.failure);

                    if (ack.failure instanceof BACnetAbortedException)
                    {
                        // For any abort, shrink the batch size.
                        batchMax /= 2;
                    }
                    else
                    {
                        batchMax = 1;
                    }
                }
            }
            else
            {
                if (!await(updateArrayPropertyWithRetries(timeoutSpec, 3, statistics, obj, propIdOrUnknown, index + 1)))
                {
                    break;
                }

                index++;
            }
        }

        Object array = obj.getValue(propId, null);
        if (array != null)
        {
            int arrayLength = Array.getLength(array);

            if (index < arrayLength)
            {
                //
                // For whatever reason, we got less values than the reported size of the array.
                // Let's truncate the array.
                //
                obj.setValue(propIdOrUnknown, Unsigned32.box(0), index);
                Object array2 = obj.getValue(propId, null);

                for (int index2 = 0; index2 < index; index2++)
                {
                    Array.set(array2, index2, Array.get(array, index2));
                }
            }
        }

        return wrapAsync(null);
    }

    public CompletableFuture<Boolean> updateArrayPropertyWithRetries(TimeoutSpec timeoutSpec,
                                                                     int retries,
                                                                     TransportPerformanceCounters statistics,
                                                                     BACnetObjectModel obj,
                                                                     BACnetPropertyIdentifierOrUnknown propId,
                                                                     int index) throws
                                                                                Exception
    {
        for (int i = 0; i < retries; i++)
        {
            boolean result = await(updateArrayProperty(timeoutSpec, statistics, obj, propId, index));
            if (result)
            {
                return wrapAsync(true);
            }
        }

        return wrapAsync(false);
    }

    public CompletableFuture<Boolean> updateArrayProperty(TimeoutSpec timeoutSpec,
                                                          TransportPerformanceCounters statistics,
                                                          BACnetObjectModel obj,
                                                          BACnetPropertyIdentifierOrUnknown propId,
                                                          int index) throws
                                                                     Exception
    {
        ServiceRequestResult<ReadProperty.Ack> ack = await(readArrayPropertyRaw(timeoutSpec, obj.getObjectIdentity(), propId, index));
        if (statistics != null)
        {
            statistics.accumulate(ack);
        }

        if (ack.value != null)
        {
            ack.value.updateObject(LoggerInstance, obj);

            return wrapAsync(true);
        }
        else
        {
            LoggerInstance.debug("updateArrayProperty [%s]: read for index %d on %s/%s failed with %s", this, index, obj.getObjectIdentity(), propId, ack.failure);

            return wrapAsync(false);
        }
    }

    //--//

    public CompletableFuture<ServiceRequestResult<WriteProperty.Ack>> writePropertyRaw(@NotNull TimeoutSpec timeoutSpec,
                                                                                       @NotNull BACnetObjectIdentifier objectId,
                                                                                       @NotNull BACnetPropertyIdentifier propId,
                                                                                       Object value)
    {
        return writePropertyRaw(timeoutSpec, objectId, propId.forRequest(), value);
    }

    public CompletableFuture<ServiceRequestResult<WriteProperty.Ack>> writePropertyRaw(@NotNull TimeoutSpec timeoutSpec,
                                                                                       @NotNull BACnetObjectIdentifier objectId,
                                                                                       @NotNull BACnetPropertyIdentifierOrUnknown propId,
                                                                                       Object value)
    {
        WriteProperty                                          req    = WriteProperty.newInstance(objectId, propId, value);
        ServiceRequestHandle<WriteProperty, WriteProperty.Ack> handle = m_mgr.postNew(this, timeoutSpec, null, null, null, req, WriteProperty.Ack.class);

        return handle.result();
    }

    public CompletableFuture<Boolean> writeProperty(@NotNull TimeoutSpec timeoutSpec,
                                                    @NotNull BACnetObjectIdentifier objectId,
                                                    @NotNull BACnetPropertyIdentifierOrUnknown propId,
                                                    Object value)
    {
        try
        {
            ServiceRequestResult<WriteProperty.Ack> ack = await(writePropertyRaw(timeoutSpec, objectId, propId, value));
            return AsyncRuntime.True;
        }
        catch (Exception e)
        {
            // Ignore failures.
            return AsyncRuntime.False;
        }
    }

    //--//

    void markReceivedPacket(TransportPerformanceCounters stats,
                            int packetLength)
    {
        stats.packetRx++;
        stats.packetRxBytes += packetLength;
    }

    public void reportReachabilityChange(DeviceReachability.ReachabilityCallback callback) throws
                                                                                           Exception
    {
        m_reachability.reportReachabilityChange(callback);
    }

    public void markAsReachable()
    {
        m_reachability.markAsReachable();
    }

    public void markAsUnreachable()
    {
        m_reachability.markAsUnreachable();
    }

    public boolean wasReachable()
    {
        return m_reachability.wasReachable();
    }

    public ZonedDateTime getLastReachable()
    {
        return m_reachability.getLastReachable();
    }

    //--//

    public CompletableFuture<Boolean> isReachable(ILogger logger,
                                                  TimeoutSpec timeoutSpec,
                                                  boolean checkOnlyName)
    {
        try
        {
            if (timeoutSpec == null)
            {
                timeoutSpec = m_mgr.getDefaultTimeout();
            }

            if (!checkOnlyName)
            {
                BACnetServicesSupported services = await(getSupportedServices(false, timeoutSpec));
                if (services != null)
                {
                    return wrapAsync(true);
                }

                if (logger != null)
                {
                    logger.warn("Failed to determine supported services of device %s, possibly unreachable...", this);
                }
            }

            device device = await(readProperty(timeoutSpec, getDeviceId(), BACnetPropertyIdentifier.object_name.forRequest(), device.class));
            if (device != null && StringUtils.isNotEmpty(device.object_name))
            {
                if (logger != null)
                {
                    logger.warn("Device %s reachable (even though it doesn't expose protocol version), got its name as '%s'...", this, device.object_name);
                }

                return wrapAsync(true);
            }

            if (logger != null)
            {
                logger.warn("Failed to get name of device %s, likely unreachable...", this);
            }
        }
        catch (Throwable t)
        {
            // Failure is like unreachable.
        }

        markAsUnreachable();

        return wrapAsync(false);
    }

    public CompletableFuture<Long> getProtocolVersion(boolean useCache,
                                                      TimeoutSpec timeoutSpec)
    {
        Long res;

        if (useCache && m_protocolVersionChecked)
        {
            res = m_protocolVersion;
        }
        else
        {
            try
            {
                device obj = await(readProperty(timeoutSpec, m_deviceDesc.id, BACnetPropertyIdentifier.protocol_version.forRequest(), device.class));

                res = obj.protocol_version;
            }
            catch (Exception e)
            {
                res = -1L;
            }

            m_protocolVersionChecked = true;
            m_protocolVersion        = res;
        }

        return wrapAsync(res);
    }

    public CompletableFuture<Long> getProtocolRevision(boolean useCache,
                                                       TimeoutSpec timeoutSpec)
    {
        Long res;

        if (useCache && m_protocolRevisionChecked)
        {
            res = m_protocolRevision;
        }
        else
        {
            try
            {
                device obj = await(readProperty(timeoutSpec, m_deviceDesc.id, BACnetPropertyIdentifier.protocol_revision.forRequest(), device.class));

                res = obj.protocol_revision;
            }
            catch (Exception e)
            {
                res = -1L;
            }

            m_protocolRevisionChecked = true;
            m_protocolRevision        = res;
        }

        return wrapAsync(res);
    }

    public CompletableFuture<BACnetServicesSupported> getSupportedServices(boolean useCache,
                                                                           TimeoutSpec timeoutSpec)
    {
        BACnetServicesSupported res;

        if (useCache && m_servicesChecked)
        {
            res = m_services;
        }
        else
        {
            try
            {
                device obj = await(readProperty(timeoutSpec, m_deviceDesc.id, BACnetPropertyIdentifier.protocol_services_supported.forRequest(), device.class));
                if (obj != null)
                {
                    res = obj.protocol_services_supported;
                    LoggerInstance.debug("Device '%s' supports: %s", this, m_services);
                }
                else
                {
                    res = null;
                }
            }
            catch (Exception e)
            {
                LoggerInstance.debug("Device '%s' failed to report supported service due to %s", this, e);
                res = null;
            }

            m_servicesChecked = true;
            m_services        = res;
        }

        return wrapAsync(res);
    }

    public boolean couldSupportReadMultiple()
    {
        return m_supportsReadMultiple == null || m_supportsReadMultiple;
    }

    //--//

    public CompletableFuture<List<BACnetAddressBinding>> getDeviceAddressBindings(TimeoutSpec timeoutSpec)
    {
        List<BACnetAddressBinding> res;

        try
        {
            device obj = await(readProperty(timeoutSpec, m_deviceDesc.id, BACnetPropertyIdentifier.device_address_binding.forRequest(), device.class));

            res = obj.device_address_binding;
            if (res == null)
            {
                res = Collections.emptyList();
            }
        }
        catch (Exception e)
        {
            res = null;
        }

        return wrapAsync(res);
    }

    public void flushObjects()
    {
        synchronized (m_objectsFetchMutex)
        {
            // Always have the device's object descriptor in the array.
            m_objects    = new ObjectDescriptor[1];
            m_objects[0] = m_deviceDesc;

            m_isObjectListOfficial = false;
        }
    }

    public CompletableFuture<TreeMap<BACnetObjectIdentifier, ObjectDescriptor>> getObjects() throws
                                                                                             Exception
    {
        if (!m_isObjectListOfficial)
        {
            try (AsyncMutex.Holder holder = await(m_objectsFetchMutex.acquire()))
            {
                if (!m_isObjectListOfficial) // Check again, under lock.
                {
                    BACnetObjectIdentifier[] objectList = await(readArrayProperty(m_mgr.getDefaultTimeout(), m_deviceDesc.id, BACnetPropertyIdentifier.object_list, BACnetObjectIdentifier[].class));

                    if (objectList != null)
                    {
                        synchronized (m_objectsFetchMutex)
                        {
                            TreeMap<BACnetObjectIdentifier, ObjectDescriptor> map = asMap();

                            for (int offset = 0; offset < objectList.length; offset++)
                            {
                                BACnetObjectIdentifier objId = objectList[offset];

                                if (objId == null || objId.object_type == null)
                                {
                                    LoggerInstance.warn("Got a null identifier for the object at offset %d", offset);
                                }
                                else if (objId.object_type.equals(BACnetObjectType.device) && !m_deviceDesc.id.equals(objId))
                                {
                                    // Skip device/0...
                                    LoggerInstance.warn("Got unexpected %s identifier for the object at offset %d", objId, offset);
                                }
                                else
                                {
                                    ObjectDescriptor objDesc = map.get(objId);
                                    if (objDesc == null)
                                    {
                                        objDesc = new ObjectDescriptor(m_mgr.memoize(objId));
                                        map.put(objDesc.id, objDesc);
                                    }

                                    objDesc.official = true;
                                }
                            }

                            ObjectDescriptor[] objects = new ObjectDescriptor[map.size()];
                            int                pos     = 0;

                            for (ObjectDescriptor objDesc : map.values())
                            {
                                objects[pos++] = objDesc;
                            }

                            validateSortingOrder(objects);

                            m_objects = objects;
                        }
                    }

                    m_isObjectListOfficial = true;
                }
            }
        }

        return wrapAsync(asMap());
    }

    public ObjectDescriptor ensureObjectDescriptor(BACnetObjectIdentifier objId)
    {
        if (m_deviceDesc.id.equals(objId))
        {
            return m_deviceDesc;
        }

        synchronized (m_objectsFetchMutex)
        {
            int pos = binarySearch(m_objects, objId);
            if (pos >= 0)
            {
                return m_objects[pos];
            }

            ObjectDescriptor   objDesc        = new ObjectDescriptor(m_mgr.memoize(objId));
            int                insertionPoint = ~pos;
            int                length         = m_objects.length;
            ObjectDescriptor[] objects        = new ObjectDescriptor[length + 1];

            System.arraycopy(m_objects, 0, objects, 0, insertionPoint);
            objects[insertionPoint] = objDesc;
            System.arraycopy(m_objects, insertionPoint, objects, insertionPoint + 1, length - insertionPoint);

            validateSortingOrder(objects);

            m_objects = objects;

            return objDesc;
        }
    }

    public CompletableFuture<DeviceIdentity.ObjectDescriptor> getObjectDescriptor(BACnetObjectIdentifier objId) throws
                                                                                                                Exception
    {
        ObjectDescriptor objDesc = ensureObjectDescriptor(objId);
        if (objDesc != null && objDesc.official)
        {
            return wrapAsync(objDesc);
        }

        TreeMap<BACnetObjectIdentifier, ObjectDescriptor> objects = await(getObjects());
        return wrapAsync(objects.get(objId));
    }

    //--//

    void updateTiming(Duration timing)
    {
        //
        // We take each request roundtrip time and use that to update the average response time.
        // To make it adaptive to current conditions, we use a weighted sum.
        //

        m_responseTimeAverage = ((m_responseTimeAverage * m_responseTimeCount) + timing.toNanos()) / (m_responseTimeCount + 1);
        m_responseTimeCount   = Math.min(m_responseTimeCount + 1, AveragingWindowMax);

        if (m_responseTimeMax.compareTo(timing) < 0)
        {
            m_responseTimeMax = timing;
            LoggerInstance.debug("New max request time for %s: %smsec", this, timing);
        }
    }

    private static int binarySearch(ObjectDescriptor[] descriptors,
                                    BACnetObjectIdentifier objId)
    {
        int low  = 0;
        int high = descriptors.length - 1;

        while (low <= high)
        {
            int              mid    = (low + high) >>> 1;
            ObjectDescriptor midVal = descriptors[mid];

            int cmp = BACnetObjectIdentifier.compare(midVal.id, objId);
            if (cmp < 0)
            {
                low = mid + 1;
            }
            else if (cmp > 0)
            {
                high = mid - 1;
            }
            else
            {
                return mid; // key found
            }
        }

        return ~low;  // key not found.
    }

    private TreeMap<BACnetObjectIdentifier, ObjectDescriptor> asMap()
    {
        TreeMap<BACnetObjectIdentifier, ObjectDescriptor> map = new TreeMap<>();

        for (ObjectDescriptor desc : m_objects)
        {
            map.put(desc.id, desc);
        }

        return map;
    }

    private void validateSortingOrder(ObjectDescriptor[] objects)
    {
        for (int i = 0; i < objects.length - 1; i++)
        {
            ObjectDescriptor left  = objects[i];
            ObjectDescriptor right = objects[i + 1];

            if (left.id.compareTo(right.id) >= 0)
            {
                throw Exceptions.newRuntimeException("INTERNAL ERROR: failed to sort objects: %s should be after %s...", left, right);
            }
        }
    }
}
