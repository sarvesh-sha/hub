/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.optio3.serialization.Reflection;

public class Exceptions
{
    public static RuntimeException newRuntimeException(String fmt,
                                                       Object... args)
    {
        return new RuntimeException(String.format(fmt, args));
    }

    public static RuntimeException newIllegalArgumentException(String fmt,
                                                               Object... args)
    {
        return new IllegalArgumentException(String.format(fmt, args));
    }

    public static TimeoutException newTimeoutException(String fmt,
                                                       Object... args)
    {
        return new TimeoutException(String.format(fmt, args));
    }

    public static <T extends Exception> T newGenericException(Class<T> clz,
                                                              String fmt,
                                                              Object... args)
    {
        return Reflection.newInstance(clz, String.format(fmt, args));
    }

    public static <T extends Exception> T newGenericException(Class<T> clz,
                                                              Throwable nested,
                                                              String fmt,
                                                              Object... args)
    {
        return Reflection.newInstance(clz, String.format(fmt, args), nested);
    }

    public static <T, E extends Exception> T requireNotNull(T target,
                                                            Class<E> clz,
                                                            String fmt,
                                                            Object... args) throws
                                                                            Exception
    {
        if (target == null)
        {
            throw newGenericException(clz, fmt, args);
        }

        return target;
    }

    public static <E extends Exception> void requireTrue(boolean condition,
                                                         Class<E> clz,
                                                         String fmt,
                                                         Object... args) throws
                                                                         Exception
    {
        if (!condition)
        {
            throw newGenericException(clz, fmt, args);
        }
    }

    public static RuntimeException wrapAsRuntimeException(Exception e)
    {
        if (e instanceof RuntimeException)
        {
            return (RuntimeException) e;
        }

        return new RuntimeException(e);
    }

    public static <T> T getAndUnwrapException(CompletableFuture<T> future) throws
                                                                           Exception
    {
        try
        {
            return future.get();
        }
        catch (Exception e)
        {
            throw (Exception) unwrapException(e);
        }
    }

    public static <T> T getAndUnwrapException(CompletableFuture<T> future,
                                              long timeout,
                                              TimeUnit unit) throws
                                                             Exception
    {
        try
        {
            return future.get(timeout, unit);
        }
        catch (Exception e)
        {
            throw (Exception) unwrapException(e);
        }
    }

    public static Throwable unwrapException(Throwable t)
    {
        if (t instanceof InvocationTargetException)
        {
            final InvocationTargetException t2 = (InvocationTargetException) t;

            Throwable t3 = t2.getTargetException();
            if (t3 != null)
            {
                return unwrapException(t3);
            }
        }

        if (t instanceof ExecutionException)
        {
            final ExecutionException t2 = (ExecutionException) t;

            Throwable t3 = t2.getCause();
            if (t3 != null)
            {
                return unwrapException(t3);
            }
        }

        if (t instanceof RuntimeException)
        {
            final RuntimeException t2 = (RuntimeException) t;

            if (t2.getMessage() == null)
            {
                Throwable t3 = t2.getCause();
                if (t3 != null)
                {
                    return unwrapException(t3);
                }
            }
        }

        return t;
    }

    public static String convertStackTraceToString(Throwable t)
    {
        StringWriter sw = new StringWriter();
        PrintWriter  pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}
