/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.remoting;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

public class RemoteExceptionResult
{
    public String typeId;
    public String message;

    public String[] declaringClass;
    public String[] methodName;
    public String[] fileName;
    public int[]    lineNumber;

    public RemoteExceptionResult cause;

    //--//

    public static RemoteExceptionResult encode(Throwable t)
    {
        if (t == null)
        {
            return null;
        }

        RemoteExceptionResult re = new RemoteExceptionResult();

        re.typeId = t.getClass()
                     .getName();
        re.message = t.getMessage();

        StackTraceElement[] stackTrace = t.getStackTrace();
        if (stackTrace != null)
        {
            int depth = stackTrace.length;
            re.declaringClass = new String[depth];
            re.methodName = new String[depth];
            re.fileName = new String[depth];
            re.lineNumber = new int[depth];

            for (int i = 0; i < depth; i++)
            {
                StackTraceElement ste = stackTrace[i];

                re.declaringClass[i] = ste.getClassName();
                re.methodName[i] = ste.getMethodName();
                re.fileName[i] = ste.getFileName();
                re.lineNumber[i] = ste.getLineNumber();
            }
        }

        re.cause = encode(t.getCause());

        return re;
    }

    public Throwable decode()
    {
        Throwable tCause = cause != null ? cause.decode() : null;

        StackTraceElement[] stackTrace;

        if (declaringClass != null)
        {
            int depth = declaringClass.length;

            stackTrace = new StackTraceElement[depth];

            for (int i = 0; i < depth; i++)
                stackTrace[i] = new StackTraceElement(declaringClass[i], methodName[i], fileName[i], lineNumber[i]);
        }
        else
        {
            stackTrace = null;
        }

        Throwable tNew = tryInstantiation(tCause, stackTrace);
        if (tNew != null)
        {
            return tNew;
        }

        return new RemotedException(typeId + ":" + message, tCause, stackTrace);
    }

    private Throwable tryInstantiation(Throwable cause,
                                       StackTraceElement[] stackTrace)
    {
        try
        {
            Class<?> clz = Class.forName(typeId);
            if (!Throwable.class.isAssignableFrom(clz))
            {
                return null;
            }

            Throwable tNew = null;

            if (cause != null)
            {
                tNew = tryToAllocateWithMessageAndCause(clz, message, cause);

                if (tNew == null)
                {
                    tNew = tryToAllocateWithCause(clz, cause);
                }
            }

            if (tNew == null)
            {
                tNew = tryToAllocateWithMessage(clz, message);
            }

            if (tNew == null)
            {
                tNew = tryToAllocate(clz);
            }

            if (tNew != null)
            {
                if (tNew.getCause() != cause)
                {
                    Field f = Reflection.findField(clz, "cause");
                    if (f != null)
                    {
                        f.setAccessible(true);
                        f.set(tNew, cause);
                    }
                }

                if (!StringUtils.equals(tNew.getMessage(), message))
                {
                    Field f = Reflection.findField(clz, "detailMessage");
                    if (f != null)
                    {
                        f.setAccessible(true);
                        f.set(tNew, message);
                    }
                }

                if (tNew.getStackTrace() != stackTrace)
                {
                    Field f = Reflection.findField(clz, "stackTrace");
                    if (f != null)
                    {
                        f.setAccessible(true);
                        f.set(tNew, stackTrace);
                    }
                }

                return tNew;
            }
        }
        catch (Throwable ex)
        {
        }

        return null;
    }

    private static Throwable tryToAllocateWithMessageAndCause(Class<?> clz,
                                                              String message,
                                                              Throwable cause)
    {
        try
        {
            for (Constructor<?> constructor : clz.getConstructors())
            {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length != 2)
                {
                    continue;
                }

                if (paramTypes[0] != String.class)
                {
                    continue;
                }

                if (!paramTypes[1].isInstance(cause))
                {
                    continue;
                }

                return (Throwable) constructor.newInstance(message, cause);
            }
        }
        catch (Throwable ex)
        {
        }

        return null;
    }

    private static Throwable tryToAllocateWithCause(Class<?> clz,
                                                    Throwable cause)
    {
        try
        {
            for (Constructor<?> constructor : clz.getConstructors())
            {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length != 1)
                {
                    continue;
                }

                if (!paramTypes[0].isInstance(cause))
                {
                    continue;
                }

                return (Throwable) constructor.newInstance(cause);
            }
        }
        catch (Throwable ex)
        {
        }

        return null;
    }

    private static Throwable tryToAllocateWithMessage(Class<?> clz,
                                                      String message)
    {
        try
        {
            for (Constructor<?> constructor : clz.getConstructors())
            {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length != 1)
                {
                    continue;
                }

                if (paramTypes[0] != String.class)
                {
                    continue;
                }

                return (Throwable) constructor.newInstance(message);
            }
        }
        catch (Throwable ex)
        {
        }

        return null;
    }

    private static Throwable tryToAllocate(Class<?> clz)
    {
        try
        {
            return (Throwable) Reflection.newInstance(clz);
        }
        catch (Throwable ex)
        {
            return null;
        }
    }

    //--//

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        toString(sb, true);

        return sb.toString();
    }

    public void toString(StringBuilder sb,
                         boolean includeValues)
    {
        sb.append(typeId);
        sb.append(":");
        sb.append(message);

        if (includeValues && declaringClass != null)
        {
            for (int i = 0; i < declaringClass.length; i++)
                sb.append(String.format("\n   at %s.%s(%s:%d)", declaringClass[i], methodName[i], fileName[i], lineNumber[i]));
        }

        if (cause != null)
        {
            sb.append("Inner exception:\n");
            cause.toString(sb, includeValues);
        }
    }
}
