/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import java.lang.reflect.Array;
import java.time.ZoneId;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.collection.ExpandableArrayOfDoubles;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsConverter;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.EngineeringUnitsValidator;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypedBitSet;
import com.optio3.util.BitSets;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;

public class TimeSeriesExtract<T> implements AutoCloseable
{
    public static class ConversionContext
    {
        public final TimeSeriesPropertyType    propType;
        public final EngineeringUnitsFactors   convertFrom;
        public final EngineeringUnitsFactors   convertTo;
        public final EngineeringUnitsConverter converter;
        public final EngineeringUnitsValidator validator;

        public ConversionContext(TimeSeriesPropertyType propType,
                                 EngineeringUnitsFactors convertTo)
        {
            this.propType    = propType;
            this.convertFrom = propType != null ? propType.unitsFactors : null;
            this.convertTo   = convertTo != null ? convertTo : convertFrom;

            EngineeringUnits                  unit             = convertFrom != null ? convertFrom.getPrimary() : null;
            EngineeringUnitsValidator.Factory validatorFactory = unit != null ? unit.getValidator() : null;

            converter = EngineeringUnits.buildConverter(this.convertFrom, this.convertTo);
            validator = validatorFactory != null ? validatorFactory.build(this.convertFrom, this.convertTo) : EngineeringUnitsValidator.NoOp;
        }

        public boolean shouldProcess()
        {
            return !converter.isIdentityTransformation() || !validator.isIdentityTransformation();
        }

        public double process(double value)
        {
            value = converter.convert(value);
            value = validator.validate(value);

            return value;
        }
    }

    //--//

    public static class Batch implements AutoCloseable
    {
        private final ExpandableArrayOfDoubles.Batch m_batchTimestamps;
        private final ExpandableArrayOfDoubles.Batch m_batchValues;

        public final int      length;
        public final double[] tempTimestamps;
        public final double[] tempValues;
        public       int      tempCursor;

        Batch(TimeSeriesExtract<?> owner)
        {
            m_batchTimestamps = owner.timestamps.prepareBatch();
            m_batchValues     = owner.values.prepareBatch();

            tempTimestamps = m_batchTimestamps.tempBuffer;
            tempValues     = m_batchValues.tempBuffer;
            length         = m_batchTimestamps.tempBuffer.length;
        }

        @Override
        public void close()
        {
            m_batchTimestamps.close();
            m_batchValues.close();
        }

        public void getRange(int arrayOffset,
                             int batchOffset,
                             int length)
        {
            m_batchTimestamps.getRange(arrayOffset, batchOffset, length);
            m_batchValues.getRange(arrayOffset, batchOffset, length);
        }

        public void setRange(int arrayOffset,
                             int batchOffset,
                             int length)
        {
            m_batchTimestamps.setRange(batchOffset, arrayOffset, length);
            m_batchValues.setRange(batchOffset, arrayOffset, length);
        }

        public void addRange(int batchOffset,
                             int length)
        {
            m_batchTimestamps.addRange(batchOffset, length);
            m_batchValues.addRange(batchOffset, length);
        }

        public void add(double timestamp,
                        double value)
        {
            int cursor = tempCursor;

            tempTimestamps[cursor] = timestamp;
            tempValues[cursor]     = value;

            tempCursor = ++cursor;
            if (cursor == length)
            {
                flush();
            }
        }

        public void flush()
        {
            if (tempCursor > 0)
            {
                addRange(0, tempCursor);
                tempCursor = 0;
            }
        }
    }

    //--//

    public final Class<T>                 clzExpected;
    public final ExpandableArrayOfDoubles timestamps;
    public final ExpandableArrayOfDoubles values;

    private List<String> m_enums;
    private Object[]     m_enumsResolved;
    private Class<?>     m_enumsResolvedForClass;

    private List<String[]>       m_enumSets;
    private Map<BitSet, Integer> m_enumSetLookups;
    private BitSet               m_enumSetBuilder;

    private List<BitSet>         m_bitsets;
    private Map<BitSet, Integer> m_bitsetLookups;

    public boolean unknownProperty;
    public ZoneId  timeZone;
    public double  nextTimestamp = Double.NaN;

    private TimeSeriesPropertyType m_propertySchema;
    private Boolean                m_shouldInterpolate;

    //--//

    public TimeSeriesExtract(Class<T> clzExpected)
    {
        this.clzExpected = clzExpected;
        this.timestamps  = ExpandableArrayOfDoubles.create();
        this.values      = ExpandableArrayOfDoubles.create();
    }

    @Override
    public void close()
    {
        timestamps.close();
        values.close();
    }

    public Batch prepareBatch()
    {
        return new Batch(this);
    }

    public TimeSeriesExtract<T> copyMetadata()
    {
        var copy = new TimeSeriesExtract<>(clzExpected);

        if (m_enums != null)
        {
            copy.m_enums = Lists.newArrayList(m_enums);
        }

        if (m_enumSets != null)
        {
            copy.m_enumSets       = Lists.newArrayList(m_enumSets);
            copy.m_enumSetLookups = Maps.newHashMap(m_enumSetLookups);
            copy.m_enumSetBuilder = new BitSet();
        }

        if (m_bitsets != null)
        {
            copy.m_bitsets       = Lists.newArrayList(m_bitsets);
            copy.m_bitsetLookups = Maps.newHashMap(m_bitsetLookups);
        }

        return copy;
    }

    //--//

    public int size()
    {
        return timestamps.size();
    }

    public int segmentSize()
    {
        return timestamps.segmentSize();
    }

    public int locateTimestamp(double timestamp)
    {
        return timestamps.binarySearch(timestamp);
    }

    public double getFirstTimestamp()
    {
        return getNthTimestamp(0);
    }

    public double getLastTimestamp()
    {
        return getNthTimestamp(size() - 1);
    }

    public double getNthTimestamp(int position)
    {
        return timestamps.get(position, TimeUtils.maxEpochSeconds());
    }

    public boolean setNthTimestamp(int position,
                                   double timestamp)
    {
        return timestamps.set(position, timestamp);
    }

    public double getNthValueRaw(int position)
    {
        return values.get(position, Double.NaN);
    }

    public boolean setNthValueRaw(int position,
                                  double value)
    {
        return values.set(position, value);
    }

    public void prepareForGrowth(int extraValues)
    {
        timestamps.prepareForGrowth(extraValues);
        values.prepareForGrowth(extraValues);
    }

    public void add(double timestamp,
                    double value)
    {
        int pos = size();
        if (pos > 0 && getLastTimestamp() >= timestamp)
        {
            //
            // The new timestamp is oldest than the last entry, need to insert it.
            //
            int insertAt = locateTimestamp(timestamp);
            if (insertAt < 0)
            {
                insertAt = ~insertAt;

                timestamps.insert(insertAt, timestamp);
                values.insert(insertAt, value);
            }
            else
            {
                //
                // Timestamp already exists, just overwrite it.
                //
                values.set(insertAt, value);
            }
        }
        else
        {
            timestamps.add(timestamp);
            values.add(value);
        }
    }

    public void remove(int pos)
    {
        timestamps.remove(pos, false);
        values.remove(pos, false);
    }

    public T getValue(int pos)
    {
        double value = getNthValueRaw(pos);
        if (Double.isNaN(value))
        {
            return null;
        }

        if (m_bitsets != null)
        {
            int valueInt = (int) value;

            return clzExpected.cast(CollectionUtils.getNthElement(m_bitsets, valueInt));
        }

        if (m_enumSets != null)
        {
            int valueInt = (int) value;

            return clzExpected.cast(CollectionUtils.getNthElement(m_enumSets, valueInt));
        }

        if (m_enums != null)
        {
            int valueInt = (int) value;

            if (clzExpected.isEnum())
            {
                var enums = resolveEnums(clzExpected);
                return clzExpected.cast(enums[valueInt]);
            }

            return clzExpected.cast(m_enums.get(valueInt));
        }

        return Reflection.coerceNumber(value, clzExpected);
    }

    //--//

    public boolean isEnum()
    {
        // Enum sets are built on top of enums, so ignore call.
        return m_enums != null && !isEnumSet();
    }

    public boolean isEnumSet()
    {
        return m_enumSets != null;
    }

    public boolean isBitset()
    {
        return m_bitsets != null;
    }

    //--//

    public String[] extractEnumLookup()
    {
        return isEnum() ? m_enums.toArray(new String[m_enums.size()]) : null;
    }

    public String[][] extractEnumSetLookup()
    {
        if (m_enumSets == null)
        {
            return null;
        }

        return m_enumSets.toArray(new String[m_enumSets.size()][]);
    }

    public int addEnumValue(String enumValue)
    {
        if (m_enums == null)
        {
            m_enums = Lists.newArrayList();
        }

        int index = m_enums.indexOf(enumValue);
        if (index < 0)
        {
            index = m_enums.size();
            m_enums.add(enumValue);
            m_enumsResolved = null;
        }

        return index;
    }

    public int[] prepareEnumLookup(List<String> enumLookups)
    {
        int[] output = new int[enumLookups.size()];

        for (int i = 0; i < output.length; i++)
        {
            output[i] = addEnumValue(enumLookups.get(i));
        }

        return output;
    }

    public int mapEnumSet(int[] lookup,
                          BitSet input)
    {
        if (m_enumSets == null)
        {
            m_enumSets       = Lists.newArrayList();
            m_enumSetLookups = Maps.newHashMap();
            m_enumSetBuilder = new BitSet();
        }

        m_enumSetBuilder.clear();

        int cardinality = 0;

        for (int indexOfMatch = input.nextSetBit(0); indexOfMatch >= 0; indexOfMatch = input.nextSetBit(indexOfMatch + 1))
        {
            int mappedEnumPos = lookup[indexOfMatch];
            m_enumSetBuilder.set(mappedEnumPos);

            cardinality++;
        }

        Integer finalIndex = m_enumSetLookups.get(m_enumSetBuilder);
        if (finalIndex == null)
        {
            String[] newSet = new String[cardinality];

            cardinality = 0;

            for (int indexOfMatch = m_enumSetBuilder.nextSetBit(0); indexOfMatch >= 0; indexOfMatch = m_enumSetBuilder.nextSetBit(indexOfMatch + 1))
            {
                newSet[cardinality++] = m_enums.get(indexOfMatch);
            }

            finalIndex = m_enumSets.size();
            m_enumSets.add(newSet);
            m_enumSetLookups.put(m_enumSetBuilder, finalIndex);
            m_enumSetBuilder = new BitSet();
        }

        return finalIndex;
    }

    public int mapBitset(BitSet bs)
    {
        if (m_bitsets == null)
        {
            m_bitsets       = Lists.newArrayList();
            m_bitsetLookups = Maps.newHashMap();
        }

        Integer finalIndex = m_bitsetLookups.get(bs);
        if (finalIndex == null)
        {
            Object value;

            if (Reflection.isSubclassOf(TypedBitSet.class, clzExpected))
            {
                value = Reflection.newInstance(clzExpected, bs);
            }
            else
            {
                value = bs;
            }

            String text = value.toString();

            bs = BitSets.copy(bs);

            finalIndex = addEnumValue(text);
            m_bitsetLookups.put(bs, finalIndex);
            m_bitsets.add(bs);
        }

        return finalIndex;
    }

    //--//

    public void convertIfNeeded(ConversionContext ctx)
    {
        if (ctx.shouldProcess())
        {
            TypeDescriptor tdTarget = Reflection.getDescriptor(clzExpected);
            if (tdTarget != null && tdTarget.isFloatingType())
            {
                for (int i = 0; i < size(); i++)
                {
                    double val = getNthValueRaw(i);
                    if (!Double.isNaN(val))
                    {
                        setNthValueRaw(i, ctx.process(val));
                    }
                }
            }
            else
            {
                Reflection.CoerceFromDouble<T> coercer = Reflection.buildNumberCoercerFromDouble(clzExpected);

                for (int i = 0; i < size(); i++)
                {
                    double val = getNthValueRaw(i);
                    if (!Double.isNaN(val))
                    {
                        double convertedNum = ctx.process(val);
                        Object coercedRaw   = coercer.coerce(convertedNum);
                        Number coercedNum   = Reflection.as(coercedRaw, Number.class);

                        setNthValueRaw(i, coercedNum != null ? coercedNum.doubleValue() : Double.NaN);
                    }
                }
            }
        }
    }

    //--//

    public TimeSeriesExtract<T> filterSamples(int maxSamples,
                                              int maxGapBetweenSamples,
                                              boolean treatWideGapAsMissing,
                                              int debounceSeconds,
                                              boolean dryRun)
    {
        if (maxSamples == 0)
        {
            maxSamples = Integer.MAX_VALUE;
        }

        double  timestampEpochSecondsMinus1 = 0;
        double  timestampEpochSecondsMinus2 = 0;
        double  previousValue               = Double.NaN;
        boolean lastTwoSamplesSameValue     = false;
        boolean insertedMissingValue        = true; // To avoid pushing a bogus sample in front of all the other samples.
        int     gapWidth                    = treatWideGapAsMissing ? 3600 : 0;

        int inputSize      = size();
        int outputPosition = 0;

        TimeSeriesExtract<T> dst   = dryRun ? null : copyMetadata();
        Batch                batch = dryRun ? null : dst.prepareBatch();

        for (int srcPosition = 0; srcPosition < inputSize; srcPosition++)
        {
            double timestampEpochSeconds = getNthTimestamp(srcPosition);
            double value                 = getNthValueRaw(srcPosition);

            if (outputPosition >= maxSamples)
            {
                if (batch != null)
                {
                    dst.nextTimestamp = timestampEpochSeconds;

                    batch.flush();
                    batch.close();
                }

                return dst;
            }

            if (gapWidth > 0 && !insertedMissingValue)
            {
                if (timestampEpochSeconds > 0 && (timestampEpochSeconds - timestampEpochSecondsMinus1) > gapWidth)
                {
                    if (batch == null)
                    {
                        // Not generating output, just report we need to mutate the extract.
                        return null;
                    }

                    timestampEpochSecondsMinus2 = timestampEpochSecondsMinus1;
                    timestampEpochSecondsMinus1 = timestampEpochSecondsMinus1 + 1;
                    batch.add(timestampEpochSecondsMinus1, Double.NaN);

                    outputPosition++;

                    previousValue        = Double.NaN;
                    insertedMissingValue = true;
                }
            }

            if (maxGapBetweenSamples > 0 && outputPosition >= 2)
            {
                if (value == previousValue)
                {
                    if (lastTwoSamplesSameValue)
                    {
                        double delta = timestampEpochSeconds - timestampEpochSecondsMinus2;

                        if (delta < maxGapBetweenSamples)
                        {
                            //
                            // If the last three samples have the same value and the distance between first and third is less than the maximum,
                            // grow the delta, instead of adding a new sample.
                            //
                            if (batch == null)
                            {
                                // Not generating output, just report we need to mutate the extract.
                                return null;
                            }

                            batch.flush();

                            timestampEpochSecondsMinus1 = timestampEpochSeconds;
                            dst.setNthTimestamp(outputPosition - 1, timestampEpochSecondsMinus1);
                            continue;
                        }
                    }

                    lastTwoSamplesSameValue = true;
                }
                else
                {
                    lastTwoSamplesSameValue = false;
                }
            }

            //
            // If we have a gap 3 times wider than the debounce value, insert an extra sample, to more accurately follow the sensor's values.
            //
            if (debounceSeconds != 0 && timestampEpochSecondsMinus1 != 0 && (timestampEpochSeconds - timestampEpochSecondsMinus1) > 3 * debounceSeconds)
            {
                if (batch == null)
                {
                    // Not generating output, just report we need to mutate the extract.
                    return null;
                }

                timestampEpochSecondsMinus2 = timestampEpochSecondsMinus1;
                timestampEpochSecondsMinus1 = timestampEpochSeconds - debounceSeconds;
                batch.add(timestampEpochSecondsMinus1, previousValue);
                outputPosition++;
            }

            if (batch != null)
            {
                batch.add(timestampEpochSeconds, value);
            }

            outputPosition++;

            timestampEpochSecondsMinus2 = timestampEpochSecondsMinus1;
            timestampEpochSecondsMinus1 = timestampEpochSeconds;
            previousValue               = value;
            insertedMissingValue        = false;
        }

        if (batch != null)
        {
            batch.flush();
            batch.close();
        }

        return dryRun ? this : dst;
    }

    //--//

    @SuppressWarnings("unchecked")
    private <E extends Enum<E>> E[] resolveEnums(Class<?> clz)
    {
        if (m_enumsResolvedForClass == clz && m_enumsResolved != null)
        {
            return (E[]) m_enumsResolved;
        }

        Class<E> clzEnum = (Class<E>) clz;

        int num = CollectionUtils.size(m_enums);
        E[] res = (E[]) Array.newInstance(clz, num);

        for (int i = 0; i < num; i++)
        {
            res[i] = Enum.valueOf(clzEnum, m_enums.get(i));
        }

        m_enumsResolved         = res;
        m_enumsResolvedForClass = clz;

        return res;
    }
}
