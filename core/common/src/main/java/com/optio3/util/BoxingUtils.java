/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.time.ZonedDateTime;

public class BoxingUtils
{
    public static boolean get(Boolean val)
    {
        return get(val, false);
    }

    public static boolean get(Boolean val,
                              boolean defaultValue)
    {
        return val != null ? val : defaultValue;
    }

    public static short get(Short val)
    {
        return get(val, (short) 0);
    }

    public static short get(Short val,
                            short defaultValue)
    {
        return val != null ? val : defaultValue;
    }

    public static int get(Integer val)
    {
        return get(val, (int) 0);
    }

    public static int get(Integer val,
                          int defaultValue)
    {
        return val != null ? val : defaultValue;
    }

    public static long get(Long val)
    {
        return get(val, (long) 0);
    }

    public static long get(Long val,
                           long defaultValue)
    {
        return val != null ? val : defaultValue;
    }

    public static float get(Float val)
    {
        return get(val, (float) 0);
    }

    public static float get(Float val,
                            float defaultValue)
    {
        return val != null ? val : defaultValue;
    }

    public static double get(Double val)
    {
        return get(val, (double) 0);
    }

    public static double get(Double val,
                             double defaultValue)
    {
        return val != null ? val : defaultValue;
    }

    public static String get(String val,
                             String defaultValue)
    {
        return val != null ? val : defaultValue;
    }

    public static ZonedDateTime get(ZonedDateTime val,
                                    ZonedDateTime defaultValue)
    {
        return val != null ? val : defaultValue;
    }

    public static <T extends Enum<T>> T get(T val,
                                            T defaultValue)
    {
        return val != null ? val : defaultValue;
    }

    public static double minWithNaN(double a,
                                    double b)
    {
        if (Double.isNaN(a))
        {
            return b;
        }

        if (Double.isNaN(b))
        {
            return a;
        }
        return Math.min(a, b);
    }

    public static double maxWithNaN(double a,
                                    double b)
    {
        if (Double.isNaN(a))
        {
            return b;
        }

        if (Double.isNaN(b))
        {
            return a;
        }
        return Math.max(a, b);
    }

    //--//

    public static int bound(int val,
                            int min,
                            int max)
    {
        if (val < min)
        {
            return min;
        }

        if (val > max)
        {
            return max;
        }

        return val;
    }

    public static long bound(long val,
                             long min,
                             long max)
    {
        if (val < min)
        {
            return min;
        }

        if (val > max)
        {
            return max;
        }

        return val;
    }

    public static float bound(float val,
                              float min,
                              float max)
    {
        if (Float.isNaN(val))
        {
            return val;
        }

        if (val < min)
        {
            return min;
        }

        if (val > max)
        {
            return max;
        }

        return val;
    }

    public static double bound(double val,
                               double min,
                               double max)
    {
        if (Double.isNaN(val))
        {
            return val;
        }

        if (val < min)
        {
            return min;
        }

        if (val > max)
        {
            return max;
        }

        return val;
    }
}
