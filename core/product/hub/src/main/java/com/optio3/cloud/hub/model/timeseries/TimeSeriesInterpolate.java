/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.util.TimeUtils;

public class TimeSeriesInterpolate
{
    public static class TimestampsMerger
    {
        private static final double[] s_empty = new double[0];

        private double[] m_current = s_empty;
        private int      m_currentLength;

        private double[] m_next;

        public double[] toArray()
        {
            return m_currentLength == m_current.length ? m_current : Arrays.copyOf(m_current, m_currentLength);
        }

        public void push(double[] timestamps)
        {
            int timestampsLength = timestamps.length;
            int currentLength    = m_currentLength;

            if (m_currentLength == 0)
            {
                m_current       = timestamps;
                m_currentLength = timestampsLength;
            }
            else
            {
                double[] current = m_current;
                double[] next    = m_next;

                if (next == null || next.length < timestampsLength + currentLength)
                {
                    next = new double[timestampsLength + currentLength + 512];
                }

                int timestampsPos = 0;
                int currentPos    = 0;
                int nextPos       = 0;

                while (timestampsPos < timestampsLength && currentPos < currentLength)
                {
                    double timestampsValue = timestamps[timestampsPos];
                    double currentValue    = current[currentPos];
                    double diff            = timestampsValue - currentValue;

                    if (Math.abs(diff) <= 0.001) // Smash together timestamps less than 1msec apart.
                    {
                        next[nextPos++] = timestampsValue;
                        timestampsPos++;
                        currentPos++;
                    }
                    else if (diff < 0) // active later than timestamps
                    {
                        next[nextPos++] = timestampsValue;
                        timestampsPos++;
                    }
                    else if (currentValue < timestampsValue)
                    {
                        next[nextPos++] = currentValue;
                        currentPos++;
                    }
                }

                int timestampsRemaining = timestampsLength - timestampsPos;
                if (timestampsRemaining > 0)
                {
                    System.arraycopy(timestamps, timestampsPos, next, nextPos, timestampsRemaining);
                    nextPos += timestampsRemaining;
                }

                int activeRemaining = currentLength - currentPos;
                if (activeRemaining > 0)
                {
                    System.arraycopy(m_current, currentPos, next, nextPos, activeRemaining);
                    nextPos += activeRemaining;
                }

                m_next          = m_current;
                m_current       = next;
                m_currentLength = nextPos;
            }
        }
    }

    public static TimeSeriesMultiPropertyResponse execute(double maxInterpolationGap,
                                                          List<TimeSeriesPropertyResponse> inputs)
    {
        TimestampsMerger merger = new TimestampsMerger();

        for (TimeSeriesPropertyResponse input : inputs)
        {
            merger.push(input.timestamps);
        }

        double[] finalTimestamps = merger.toArray();

        TimeSeriesMultiPropertyResponse res = new TimeSeriesMultiPropertyResponse();
        res.timestamps = finalTimestamps;
        res.results    = new TimeSeriesPropertyResponse[inputs.size()];

        for (int i = 0; i < res.results.length; i++)
        {
            res.results[i] = executeSingle(inputs.get(i), finalTimestamps, maxInterpolationGap);
        }

        return res;
    }

    public static TimeSeriesPropertyResponse[] execute(double maxInterpolationGap,
                                                       TimeSeriesPropertyResponse... inputs)
    {
        TimeSeriesMultiPropertyResponse results = execute(maxInterpolationGap, Lists.newArrayList(inputs));

        return results.results;
    }

    public static TimeSeriesPropertyResponse expandTimestamps(TimeSeriesPropertyResponse input,
                                                              double maxInterpolationGap,
                                                              double[]... extraTimestampsSet)
    {
        TimestampsMerger merger = new TimestampsMerger();

        merger.push(input.timestamps);

        for (double[] extraTimestamps : extraTimestampsSet)
        {
            merger.push(extraTimestamps);
        }

        double[] finalTimestamps = merger.toArray();

        return executeSingle(input, finalTimestamps, maxInterpolationGap);
    }

    private static TimeSeriesPropertyResponse executeSingle(TimeSeriesPropertyResponse specResultsIn,
                                                            double[] timestamps,
                                                            double maxInterpolationGap)
    {
        int numSamples = timestamps.length;

        TimeSeriesPropertyResponse specResultsOut = new TimeSeriesPropertyResponse();
        specResultsOut.values         = new double[numSamples];
        specResultsOut.enumLookup     = specResultsIn.enumLookup;
        specResultsOut.expectedType   = specResultsIn.expectedType;
        specResultsOut.timestamps     = timestamps;
        specResultsOut.timeZone       = specResultsIn.timeZone;
        specResultsOut.propertySchema = specResultsIn.propertySchema;

        double[] timestampsIn = specResultsIn.timestamps;
        double[] valuesIn     = specResultsIn.values;
        double[] valuesOut    = specResultsOut.values;

        double  timestampBefore = TimeUtils.minEpochSeconds();
        double  valueBefore     = Double.NaN;
        boolean dontInterpolate = !specResultsIn.shouldInterpolate();
        boolean ignoreGap       = true; // We need two samples to look at the gap width.

        if (maxInterpolationGap == 0)
        {
            maxInterpolationGap = 3600;
        }

        for (int posIn = 0, posOut = 0; posOut < numSamples; posOut++)
        {
            double targetTimestamp = timestamps[posOut];

            while (posIn < timestampsIn.length && timestampsIn[posIn] < targetTimestamp)
            {
                ignoreGap       = false;
                timestampBefore = timestampsIn[posIn];
                valueBefore     = valuesIn[posIn];
                posIn++;
            }

            double timestampAfter;
            double valueAfter;

            if (posIn < timestampsIn.length)
            {
                timestampAfter = timestampsIn[posIn];
                valueAfter     = valuesIn[posIn];
            }
            else
            {
                timestampAfter = TimeUtils.maxEpochSeconds();
                valueAfter     = Double.NaN;
                ignoreGap      = true;
            }

            double diff = targetTimestamp - timestampAfter;
            double valueOut;

            if (Math.abs(diff) <= 0.001) // Smash together timestamps less than 1msec apart.
            {
                valueOut = valueAfter;
            }
            else if (Double.isNaN(valueBefore) || Double.isNaN(valueAfter))
            {
                //
                // If any of the values is NaN, use the previous value, since timestamp is guaranteed to be before the next sample.
                //
                valueOut = valueBefore;
            }
            else if (dontInterpolate)
            {
                //
                // The type of the series is discrete, we should not interpolate.
                //
                valueOut = valueBefore;
            }
            else
            {
                double timeDiff = timestampAfter - timestampBefore;
                if (timeDiff == 0)
                {
                    // Should only happen on the first sample...
                    valueOut = valueAfter;
                }
                else if (!ignoreGap && timeDiff > maxInterpolationGap) // Gap between samples too wide, assume missing.
                {
                    valueOut = Double.NaN;
                }
                else
                {
                    double valueDiff = valueAfter - valueBefore;

                    valueOut = valueBefore + (valueDiff / timeDiff) * (targetTimestamp - timestampBefore);
                }
            }

            valuesOut[posOut] = valueOut;
        }

        return specResultsOut;
    }
}
