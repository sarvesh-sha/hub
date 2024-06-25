/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.logging;

import java.util.function.Function;

public interface ILogger
{
    boolean isEnabled(Severity level);

    boolean canForwardToRemote();

    ILogger getInnerContext();

    void log(ILogger context,
             Severity level,
             String prefix,
             String msg);

    void log(ILogger context,
             Severity level,
             String prefix,
             Function<Object, Object> argumentProcessor,
             String fmt,
             Object arg1);

    void log(ILogger context,
             Severity level,
             String prefix,
             Function<Object, Object> argumentProcessor,
             String fmt,
             Object arg1,
             Object arg2);

    void log(ILogger context,
             Severity level,
             String prefix,
             Function<Object, Object> argumentProcessor,
             String fmt,
             Object arg1,
             Object arg2,
             Object arg3);

    void log(ILogger context,
             Severity level,
             String prefix,
             Function<Object, Object> argumentProcessor,
             String fmt,
             Object arg1,
             Object arg2,
             Object arg3,
             Object arg4);

    void log(ILogger context,
             Severity level,
             String prefix,
             Function<Object, Object> argumentProcessor,
             String fmt,
             Object arg1,
             Object arg2,
             Object arg3,
             Object arg4,
             Object arg5);

    void log(ILogger context,
             Severity level,
             String prefix,
             Function<Object, Object> argumentProcessor,
             String fmt,
             Object arg1,
             Object arg2,
             Object arg3,
             Object arg4,
             Object arg5,
             Object arg6);

    void log(ILogger context,
             Severity level,
             String prefix,
             Function<Object, Object> argumentProcessor,
             String fmt,
             Object... args);

    //--//

    default void info(String msg)
    {
        log(this, Severity.Info, null, msg);
    }

    default void info(String fmt,
                      Object arg1)
    {
        log(this, Severity.Info, null, null, fmt, arg1);
    }

    default void info(String fmt,
                      Object arg1,
                      Object arg2)
    {
        log(this, Severity.Info, null, null, fmt, arg1, arg2);
    }

    default void info(String fmt,
                      Object arg1,
                      Object arg2,
                      Object arg3)
    {
        log(this, Severity.Info, null, null, fmt, arg1, arg2, arg3);
    }

    default void info(String fmt,
                      Object arg1,
                      Object arg2,
                      Object arg3,
                      Object arg4)
    {
        log(this, Severity.Info, null, null, fmt, arg1, arg2, arg3, arg4);
    }

    default void info(String fmt,
                      Object arg1,
                      Object arg2,
                      Object arg3,
                      Object arg4,
                      Object arg5)
    {
        log(this, Severity.Info, null, null, fmt, arg1, arg2, arg3, arg4, arg5);
    }

    default void info(String fmt,
                      Object arg1,
                      Object arg2,
                      Object arg3,
                      Object arg4,
                      Object arg5,
                      Object arg6)
    {
        log(this, Severity.Info, null, null, fmt, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    default void info(String fmt,
                      Object... args)
    {
        log(this, Severity.Info, null, null, fmt, args);
    }

    //--//

    default void warn(String msg)
    {
        log(this, Severity.Warn, null, msg);
    }

    default void warn(String fmt,
                      Object arg1)
    {
        log(this, Severity.Warn, null, null, fmt, arg1);
    }

    default void warn(String fmt,
                      Object arg1,
                      Object arg2)
    {
        log(this, Severity.Warn, null, null, fmt, arg1, arg2);
    }

    default void warn(String fmt,
                      Object arg1,
                      Object arg2,
                      Object arg3)
    {
        log(this, Severity.Warn, null, null, fmt, arg1, arg2, arg3);
    }

    default void warn(String fmt,
                      Object arg1,
                      Object arg2,
                      Object arg3,
                      Object arg4)
    {
        log(this, Severity.Warn, null, null, fmt, arg1, arg2, arg3, arg4);
    }

    default void warn(String fmt,
                      Object arg1,
                      Object arg2,
                      Object arg3,
                      Object arg4,
                      Object arg5)
    {
        log(this, Severity.Warn, null, null, fmt, arg1, arg2, arg3, arg4, arg5);
    }

    default void warn(String fmt,
                      Object arg1,
                      Object arg2,
                      Object arg3,
                      Object arg4,
                      Object arg5,
                      Object arg6)
    {
        log(this, Severity.Warn, null, null, fmt, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    default void warn(String fmt,
                      Object... args)
    {
        log(this, Severity.Warn, null, null, fmt, args);
    }

    //--//

    default void error(String msg)
    {
        log(this, Severity.Error, null, msg);
    }

    default void error(String fmt,
                       Object arg1)
    {
        log(this, Severity.Error, null, null, fmt, arg1);
    }

    default void error(String fmt,
                       Object arg1,
                       Object arg2)
    {
        log(this, Severity.Error, null, null, fmt, arg1, arg2);
    }

    default void error(String fmt,
                       Object arg1,
                       Object arg2,
                       Object arg3)
    {
        log(this, Severity.Error, null, null, fmt, arg1, arg2, arg3);
    }

    default void error(String fmt,
                       Object arg1,
                       Object arg2,
                       Object arg3,
                       Object arg4)
    {
        log(this, Severity.Error, null, null, fmt, arg1, arg2, arg3, arg4);
    }

    default void error(String fmt,
                       Object arg1,
                       Object arg2,
                       Object arg3,
                       Object arg4,
                       Object arg5)
    {
        log(this, Severity.Error, null, null, fmt, arg1, arg2, arg3, arg4, arg5);
    }

    default void error(String fmt,
                       Object arg1,
                       Object arg2,
                       Object arg3,
                       Object arg4,
                       Object arg5,
                       Object arg6)
    {
        log(this, Severity.Error, null, null, fmt, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    default void error(String fmt,
                       Object... args)
    {
        log(this, Severity.Error, null, null, fmt, args);
    }

    //--//

    default void debug(String msg)
    {
        log(this, Severity.Debug, null, msg);
    }

    default void debug(String fmt,
                       Object arg1)
    {
        log(this, Severity.Debug, null, null, fmt, arg1);
    }

    default void debug(String fmt,
                       Object arg1,
                       Object arg2)
    {
        log(this, Severity.Debug, null, null, fmt, arg1, arg2);
    }

    default void debug(String fmt,
                       Object arg1,
                       Object arg2,
                       Object arg3)
    {
        log(this, Severity.Debug, null, null, fmt, arg1, arg2, arg3);
    }

    default void debug(String fmt,
                       Object arg1,
                       Object arg2,
                       Object arg3,
                       Object arg4)
    {
        log(this, Severity.Debug, null, null, fmt, arg1, arg2, arg3, arg4);
    }

    default void debug(String fmt,
                       Object arg1,
                       Object arg2,
                       Object arg3,
                       Object arg4,
                       Object arg5)
    {
        log(this, Severity.Debug, null, null, fmt, arg1, arg2, arg3, arg4, arg5);
    }

    default void debug(String fmt,
                       Object arg1,
                       Object arg2,
                       Object arg3,
                       Object arg4,
                       Object arg5,
                       Object arg6)
    {
        log(this, Severity.Debug, null, null, fmt, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    default void debug(String fmt,
                       Object... args)
    {
        log(this, Severity.Debug, null, null, fmt, args);
    }

    //--//

    default void debugVerbose(String msg)
    {
        log(this, Severity.DebugVerbose, null, msg);
    }

    default void debugVerbose(String fmt,
                              Object arg1)
    {
        log(this, Severity.DebugVerbose, null, null, fmt, arg1);
    }

    default void debugVerbose(String fmt,
                              Object arg1,
                              Object arg2)
    {
        log(this, Severity.DebugVerbose, null, null, fmt, arg1, arg2);
    }

    default void debugVerbose(String fmt,
                              Object arg1,
                              Object arg2,
                              Object arg3)
    {
        log(this, Severity.DebugVerbose, null, null, fmt, arg1, arg2, arg3);
    }

    default void debugVerbose(String fmt,
                              Object arg1,
                              Object arg2,
                              Object arg3,
                              Object arg4)
    {
        log(this, Severity.DebugVerbose, null, null, fmt, arg1, arg2, arg3, arg4);
    }

    default void debugVerbose(String fmt,
                              Object arg1,
                              Object arg2,
                              Object arg3,
                              Object arg4,
                              Object arg5)
    {
        log(this, Severity.DebugVerbose, null, null, fmt, arg1, arg2, arg3, arg4, arg5);
    }

    default void debugVerbose(String fmt,
                              Object arg1,
                              Object arg2,
                              Object arg3,
                              Object arg4,
                              Object arg5,
                              Object arg6)
    {
        log(this, Severity.DebugVerbose, null, null, fmt, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    default void debugVerbose(String fmt,
                              Object... args)
    {
        log(this, Severity.DebugVerbose, null, null, fmt, args);
    }

    //--//

    default void debugObnoxious(String msg)
    {
        log(this, Severity.DebugObnoxious, null, msg);
    }

    default void debugObnoxious(String fmt,
                                Object arg1)
    {
        log(this, Severity.DebugObnoxious, null, null, fmt, arg1);
    }

    default void debugObnoxious(String fmt,
                                Object arg1,
                                Object arg2)
    {
        log(this, Severity.DebugObnoxious, null, null, fmt, arg1, arg2);
    }

    default void debugObnoxious(String fmt,
                                Object arg1,
                                Object arg2,
                                Object arg3)
    {
        log(this, Severity.DebugObnoxious, null, null, fmt, arg1, arg2, arg3);
    }

    default void debugObnoxious(String fmt,
                                Object arg1,
                                Object arg2,
                                Object arg3,
                                Object arg4)
    {
        log(this, Severity.DebugObnoxious, null, null, fmt, arg1, arg2, arg3, arg4);
    }

    default void debugObnoxious(String fmt,
                                Object arg1,
                                Object arg2,
                                Object arg3,
                                Object arg4,
                                Object arg5)
    {
        log(this, Severity.DebugObnoxious, null, null, fmt, arg1, arg2, arg3, arg4, arg5);
    }

    default void debugObnoxious(String fmt,
                                Object arg1,
                                Object arg2,
                                Object arg3,
                                Object arg4,
                                Object arg5,
                                Object arg6)
    {
        log(this, Severity.DebugObnoxious, null, null, fmt, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    default void debugObnoxious(String fmt,
                                Object... args)
    {
        log(this, Severity.DebugObnoxious, null, null, fmt, args);
    }
}