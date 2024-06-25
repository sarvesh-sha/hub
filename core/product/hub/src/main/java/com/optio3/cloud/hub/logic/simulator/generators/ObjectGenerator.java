/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.generators;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.enums.BACnetEngineeringUnits;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;

public abstract class ObjectGenerator<T extends BACnetObjectModel>
{
    public class ObjectReader
    {
        private List<BACnetPropertyIdentifierOrUnknown> m_properties = Lists.newArrayList();

        public void add(BACnetPropertyIdentifierOrUnknown prop)
        {
            m_properties.add(prop);
        }

        public BACnetObjectModel read(ZonedDateTime now,
                                      boolean justSamples)
        {
            updateState();

            if (m_skipSamplesCount > 0)
            {
                m_skipSamplesCount--;

                if (m_skippedSamplesAreNull)
                {
                    return null;
                }
                else
                {
                    throw Exceptions.newRuntimeException("Skipping sample.");
                }
            }

            if (m_faultSamplesCount > 0)
            {
                m_faultSamplesCount--;
            }

            return readAllProperties(now, justSamples);
        }

        public String getPropsList()
        {
            return String.join(",", CollectionUtils.transformToList(m_properties, BACnetPropertyIdentifierOrUnknown::toJsonValue));
        }
    }

    private final String                 m_name;
    private final int                    m_instanceNumber;
    private final BACnetObjectType       m_objectType;
    private final Class<T>               m_clz;
    private final BACnetEngineeringUnits m_units;
    private final Random                 m_random = new Random();

    private double  m_skipSamplesRate;
    private int     m_maxSkippedSamples;
    private int     m_skipSamplesCount;
    private boolean m_skippedSamplesAreNull;

    private double m_faultSamplesRate;
    private int    m_maxFaultSamples;
    private int    m_faultSamplesCount;

    protected ObjectGenerator(String name,
                              int instanceNumber,
                              BACnetObjectType objectType,
                              BACnetEngineeringUnits units,
                              Class<T> clz)
    {
        m_name           = name;
        m_instanceNumber = instanceNumber;
        m_objectType     = objectType;
        m_units          = units;
        m_clz            = clz;
    }

    ObjectGenerator<T> configureSkipSamples(double skipSampleRate,
                                            int maxSkippedSamples)
    {
        m_skipSamplesRate   = skipSampleRate;
        m_maxSkippedSamples = maxSkippedSamples;
        return this;
    }

    ObjectGenerator<T> configureFaults(double faultSamplesRate,
                                       int maxFaultSamples)
    {
        m_faultSamplesRate = faultSamplesRate;
        m_maxFaultSamples  = maxFaultSamples;
        return this;
    }

    public abstract void readSamples(ZonedDateTime now,
                                     T model);

    public BACnetObjectModel readAllProperties(ZonedDateTime now,
                                               boolean justSamples)
    {
        T model = createObject();
        if (!justSamples)
        {
            model.setValue(BACnetPropertyIdentifier.object_name, getName());
            if (m_units != null)
            {
                model.setValue(BACnetPropertyIdentifier.units, m_units);
            }
        }

        readSamples(now, model);

        return model;
    }

    DeviceElementRecord persistObject(RecordHelper<AssetRecord> helper,
                                      BACnetDeviceRecord rec_device) throws
                                                                     Exception
    {
        DeviceElementRecord rec_object = new DeviceElementRecord();
        rec_object.setPhysicalName(getName());
        rec_object.setIdentifier(getIdentifier().toJsonValue());

        BACnetObjectModel obj = m_objectType.allocateNewObject(getIdentifier());
        obj.setValue(BACnetPropertyIdentifier.object_name, getName());
        rec_object.setContents(BACnetObjectModel.getObjectMapper(), obj);

        helper.persist(rec_object);
        helper.flush();

        rec_object.linkToParent(helper, rec_device);

        return rec_object;
    }

    public BACnetObjectIdentifier getIdentifier()
    {
        return new BACnetObjectIdentifier(m_objectType, m_instanceNumber);
    }

    public String getName()
    {
        return m_name;
    }

    protected boolean isInFault()
    {
        return m_faultSamplesCount > 0;
    }

    private void updateState()
    {
        double rand = m_random.nextDouble();

        if (m_skipSamplesCount <= 0 && rand < m_skipSamplesRate)
        {
            m_skipSamplesCount      = m_random.nextInt(m_maxSkippedSamples) + 1;
            m_skippedSamplesAreNull = m_random.nextBoolean();
        }

        rand = m_random.nextDouble();

        if (m_faultSamplesCount <= 0 && rand < m_faultSamplesRate)
        {
            m_faultSamplesCount = m_random.nextInt(m_maxFaultSamples) + 1;
        }
    }

    protected BACnetEventState getEventState()
    {
        if (isInFault())
        {
            return BACnetEventState.fault;
        }

        return BACnetEventState.normal;
    }

    protected boolean getOutOfService()
    {
        return isInFault();
    }

    protected BACnetStatusFlags getStatusFlags()
    {
        BACnetStatusFlags flags = new BACnetStatusFlags();
        if (isInFault())
        {
            flags.set(BACnetStatusFlags.Values.fault);
            flags.set(BACnetStatusFlags.Values.in_alarm);
        }
        return flags;
    }

    protected T createObject()
    {
        T obj = Reflection.newInstance(m_clz);
        obj.setObjectIdentity(getIdentifier());
        return obj;
    }

    protected ZonedDateTime getPSTTime(ZonedDateTime time)
    {
        return time.withZoneSameInstant(ZoneId.of("America/Los_Angeles"));
    }

    protected boolean isWeekend(ZonedDateTime time)
    {
        time = getPSTTime(time);
        return time.getDayOfWeek() == DayOfWeek.SATURDAY || time.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    protected boolean isDuringBusinessHours(ZonedDateTime time)
    {
        time = getPSTTime(time);

        return !isWeekend(time) && time.getHour() >= 6 && time.getHour() < 18;
    }

    protected double getPercentOfBusinessDay(ZonedDateTime time)
    {
        time = getPSTTime(time);
        ZonedDateTime startOfDay = time.truncatedTo(ChronoUnit.HOURS)
                                       .minusHours(time.getHour() - 6);

        return getPercentage(startOfDay, time, 12 * 60 * 60);
    }

    protected double getPercentOfWeekend(ZonedDateTime time)
    {
        time = getPSTTime(time);
        if (!isWeekend(time))
        {
            return 0;
        }

        int days = 0;
        if (time.getDayOfWeek() == DayOfWeek.SUNDAY)
        {
            days = 1;
        }

        ZonedDateTime startOfWeekend = time.truncatedTo(ChronoUnit.DAYS)
                                           .minusDays(days);

        return getPercentage(startOfWeekend, time, 48 * 60 * 60);
    }

    protected double getPercentage(ZonedDateTime beginning,
                                   ZonedDateTime now,
                                   long totalSeconds)
    {
        beginning = getPSTTime(beginning);
        now       = getPSTTime(now);
        if (now.isBefore(beginning))
        {
            return 0;
        }
        long currentSeconds = beginning.until(now, ChronoUnit.SECONDS);
        return Math.min(1, 1.0 * currentSeconds / totalSeconds);
    }

    public ObjectReader newReader()
    {
        return new ObjectReader();
    }
}
