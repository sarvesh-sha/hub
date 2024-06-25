/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.DeviceReachability;
import com.optio3.protocol.model.FieldTemporalResolution;
import com.optio3.protocol.model.can.CanObjectModel;
import com.optio3.protocol.model.ipn.objects.IpnLocation;
import com.optio3.protocol.model.ipn.objects.argohytos.stealthpower.BaseArgoHytosModel;
import com.optio3.protocol.model.ipn.objects.bluesky.BaseBlueSkyObjectModel;
import com.optio3.protocol.model.ipn.objects.digineous.BaseDigineousModel;
import com.optio3.protocol.model.ipn.objects.digitalmatter.BaseDigitalMatterObjectModel;
import com.optio3.protocol.model.ipn.objects.epsolar.BaseEpSolarModel;
import com.optio3.protocol.model.ipn.objects.holykell.HolykellModel;
import com.optio3.protocol.model.ipn.objects.montage.BaseBluetoothGatewayObjectModel;
import com.optio3.protocol.model.ipn.objects.morningstar.BaseTriStarModel;
import com.optio3.protocol.model.ipn.objects.sensors.IpnAccelerometer;
import com.optio3.protocol.model.ipn.objects.sensors.IpnI2CSensor;
import com.optio3.protocol.model.ipn.objects.stealthpower.BaseStealthPowerModel;
import com.optio3.protocol.model.ipn.objects.victron.BaseVictronModel;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type_ipn")
@JsonSubTypes({ @JsonSubTypes.Type(value = BaseArgoHytosModel.class),
                @JsonSubTypes.Type(value = BaseBlueSkyObjectModel.class),
                @JsonSubTypes.Type(value = BaseBluetoothGatewayObjectModel.class),
                @JsonSubTypes.Type(value = BaseDigineousModel.class),
                @JsonSubTypes.Type(value = BaseDigitalMatterObjectModel.class),
                @JsonSubTypes.Type(value = BaseEpSolarModel.class),
                @JsonSubTypes.Type(value = BaseStealthPowerModel.class),
                @JsonSubTypes.Type(value = BaseTriStarModel.class),
                @JsonSubTypes.Type(value = BaseVictronModel.class),
                @JsonSubTypes.Type(value = CanObjectModel.class),
                @JsonSubTypes.Type(value = HolykellModel.class),
                @JsonSubTypes.Type(value = IpnAccelerometer.class),
                @JsonSubTypes.Type(value = IpnI2CSensor.class),
                @JsonSubTypes.Type(value = IpnLocation.class) })
public abstract class IpnObjectModel extends BaseObjectModel
{
    public static final Logger LoggerInstance = new Logger(IpnObjectModel.class);

    private static Map<String, Class<? extends IpnObjectModel>> s_lookupBaseId;

    //--//

    public interface INotifySample
    {
        void acceptSample(IpnObjectModel obj,
                          String field);
    }

    public static class FieldSample
    {
        public final int                     sequence;
        public final FieldUpdateReason       reason;
        public final double                  timestampEpochSeconds;
        public final FieldTemporalResolution temporalResolution;
        public final Object                  value;
        public final boolean                 flushOnChange;
        public       boolean                 emitted;

        FieldSample(int sequence,
                    FieldUpdateReason reason,
                    double timestampEpochSeconds,
                    FieldTemporalResolution temporalResolution,
                    Object value,
                    boolean flushOnChange)
        {
            this.sequence              = sequence;
            this.reason                = reason;
            this.timestampEpochSeconds = timestampEpochSeconds;
            this.temporalResolution    = temporalResolution;
            this.value                 = value;
            this.flushOnChange         = flushOnChange;
        }

        public boolean isReady(double nowEpochSeconds)
        {
            return timestampEpochSeconds <= nowEpochSeconds;
        }

        public static double now()
        {
            return TimeUtils.nowMilliUtc() * (1.0 / 1000);
        }

        @Override
        public String toString()
        {
            return "FieldSample{" + "timestampEpochSeconds=" + timestampEpochSeconds + ", value=" + value + ", flushOnChange=" + flushOnChange + '}';
        }
    }

    public enum FieldUpdateReason
    {
        FixedRate,
        Identical,
        BelowThreshold,
        Changed
    }

    public class FieldState
    {
        public final String             objectId;
        public final String             fieldName;
        private      int                m_sequence;
        private      double             m_lastEmittedTimestamp;
        private      FieldSample        m_lastValue;
        private      ScheduledFuture<?> m_futureValue;

        FieldState(String field)
        {
            objectId  = extractId();
            fieldName = field;
        }

        //--//

        public FieldSample getLastValue()
        {
            return m_lastValue;
        }

        public FieldSample allocateNewSample(FieldUpdateReason reason,
                                             double timestampEpochSeconds,
                                             FieldTemporalResolution temporalResolution,
                                             int minimumTemporalSpacing,
                                             boolean debounceFromLastValue,
                                             Object value,
                                             boolean flushOnChange)
        {
            double truncateTimestamp = temporalResolution.truncateTimestamp(timestampEpochSeconds);

            double debouncedTimestamp;

            if (debounceFromLastValue)
            {
                // Delay the timestamp to debounce from the last values.
                debouncedTimestamp = Math.max(m_lastEmittedTimestamp + minimumTemporalSpacing, truncateTimestamp);
            }
            else
            {
                // Delay the timestamp to debounce from now.
                debouncedTimestamp = truncateTimestamp + minimumTemporalSpacing;
            }

            // Move the timestamp a bit if it collides with the previous one, based on precision.
            double adjustedTimestamp = temporalResolution.adjustTimestamp(m_lastEmittedTimestamp, debouncedTimestamp);

            return new FieldSample(m_sequence++, reason, adjustedTimestamp, temporalResolution, value, flushOnChange);
        }

        public Object getCurrentValue()
        {
            return getField(fieldName);
        }

        public synchronized FieldSample pop(double nowEpochSeconds)
        {
            FieldSample sample = m_lastValue;
            if (sample != null)
            {
                if (!sample.emitted && sample.isReady(nowEpochSeconds))
                {
                    sample.emitted         = true;
                    m_lastEmittedTimestamp = sample.timestampEpochSeconds;

                    setField(fieldName, sample.value);

                    logSample(Severity.Debug, sample, "Pop");

                    return sample;
                }
            }

            return null;
        }

        public synchronized void push(FieldSample sample,
                                      INotifySample callback)
        {
            m_lastValue = sample;

            if (m_futureValue != null)
            {
                m_futureValue.cancel(false);
                m_futureValue = null;
            }

            if (sample.reason == FieldUpdateReason.Changed)
            {
                double nowEpochSeconds = FieldSample.now();
                double delay           = sample.timestampEpochSeconds - nowEpochSeconds;

                if (delay <= 0)
                {
                    notify(callback);
                }
                else
                {
                    logSample(Severity.DebugObnoxious, sample, "Debounce");

                    m_futureValue = Executors.scheduleOnDefaultPool(() ->
                                                                    {
                                                                        logSample(Severity.DebugObnoxious, sample, "Debounced");
                                                                        notify(callback);
                                                                    }, (long) (delay * 1000), TimeUnit.MILLISECONDS);
                }
            }
            else
            {
                logSample(Severity.DebugVerbose, sample, "Debounced");
            }
        }

        private void notify(INotifySample callback)
        {
            FieldSample sample = m_lastValue;
            if (!sample.emitted)
            {
                logSample(Severity.DebugVerbose, sample, "Notify");
                callback.acceptSample(IpnObjectModel.this, fieldName);
            }
        }

        private void logSample(Severity severity,
                               FieldSample sample,
                               String context)
        {
            if (LoggerInstance.isEnabled(severity))
            {
                LoggerInstance.log(null,
                                   severity,
                                   null,
                                   null,
                                   "[%s#%s at %.3f/%s # %-10d] %-10s %-14s %s",
                                   objectId,
                                   fieldName,
                                   sample.timestampEpochSeconds,
                                   sample.temporalResolution,
                                   sample.sequence,
                                   context,
                                   sample.reason,
                                   sample.value);
            }
        }

        @Override
        public String toString()
        {
            return String.format("FieldState{m_fieldName='%s', m_lastTimestamp='%.3f'}", fieldName, m_lastEmittedTimestamp);
        }
    }

    @JsonIgnore
    private Map<String, FieldState> m_sampleState;

    private final DeviceReachability.State m_reachability = new DeviceReachability.State(2 * 60);

    //--//

    public boolean shouldIncludeObject()
    {
        return true;
    }

    public boolean shouldIncludeProperty(String prop)
    {
        return true;
    }

    //--//

    public static IpnObjectModel allocateFromDescriptor(IpnDeviceDescriptor desc)
    {
        try
        {
            if (s_lookupBaseId == null)
            {
                Map<String, Class<? extends IpnObjectModel>> lookupBaseId = Maps.newHashMap();
                Set<Class<? extends IpnObjectModel>>         subTypes     = Reflection.collectJsonSubTypes(IpnObjectModel.class);

                for (Class<? extends IpnObjectModel> subType : subTypes)
                {
                    IpnObjectModel obj = Reflection.newInstance(subType);
                    lookupBaseId.put(obj.extractBaseId(), subType);
                }

                s_lookupBaseId = lookupBaseId;
            }

            String[]                        parts = StringUtils.split(desc.name, '/');
            Class<? extends IpnObjectModel> clz   = s_lookupBaseId.get(parts[0]);
            if (clz != null)
            {
                IpnObjectModel obj = Reflection.newInstance(clz);

                if (obj.parseId(desc.name))
                {
                    return obj;
                }
            }
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to allocate IPN object from descriptor %s: %s", desc, t);
        }

        return null;
    }

    public String extractBaseId()
    {
        return extractId();
    }

    public String extractUnitId()
    {
        return null;
    }

    public final String extractId()
    {
        String id = extractBaseId();

        String unitId = extractUnitId();
        if (unitId != null)
        {
            id = id + "/" + unitId;
        }

        return id;
    }

    public boolean parseId(String id)
    {
        return StringUtils.equals(extractId(), id);
    }

    public synchronized FieldState getFieldState(String field)
    {
        if (m_sampleState == null)
        {
            m_sampleState = Maps.newHashMap();
        }

        FieldState res = m_sampleState.get(field);
        if (res == null)
        {
            res = new FieldState(field);
            m_sampleState.put(field, res);
        }

        return res;
    }

    //--//

    public void reportReachabilityChange(DeviceReachability.ReachabilityCallback callback) throws
                                                                                           Exception
    {
        m_reachability.reportReachabilityChange(callback);
    }

    public abstract boolean shouldCommitReachabilityChange(boolean isReachable,
                                                           ZonedDateTime lastReachable);

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

    @JsonIgnore
    public ZonedDateTime getLastReachable()
    {
        return m_reachability.getLastReachable();
    }

    // TODO: UPGRADE PATCH: we have to drain some messages with "lastReachable" values.
    public void setLastReachable(ZonedDateTime value)
    {
    }

    //--//

    public static <T extends IpnObjectModel> T deserializeFromJson(Class<T> clz,
                                                                   String json) throws
                                                                                IOException
    {
        return deserializeInner(getObjectMapper(), clz, json);
    }

    @Override
    public ObjectMapper getObjectMapperForInstance()
    {
        return getObjectMapper();
    }

    public static ObjectMapper getObjectMapper()
    {
        return ObjectMappers.SkipDefaults;
    }
}
