/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.logging;

import java.util.function.Function;

public class RedirectingLogger implements ILogger
{
    private final ILogger m_logger;

    private boolean m_prefixFetched;
    private String  m_prefix;

    public RedirectingLogger(ILogger logger)
    {
        m_logger = logger;
    }

    @Override
    public boolean isEnabled(Severity level)
    {
        return m_logger.isEnabled(level);
    }

    @Override
    public boolean canForwardToRemote()
    {
        return m_logger.canForwardToRemote();
    }

    @Override
    public ILogger getInnerContext()
    {
        return m_logger;
    }

    @Override
    public void log(ILogger context,
                    Severity level,
                    String prefix,
                    String msg)
    {
        if (!isEnabled(level))
        {
            return;
        }

        m_logger.log(context != null ? context : m_logger, level, join(prefix), msg);
    }

    @Override
    public void log(ILogger context,
                    Severity level,
                    String prefix,
                    Function<Object, Object> argumentProcessor,
                    String fmt,
                    Object arg1)
    {
        if (!isEnabled(level))
        {
            return;
        }

        m_logger.log(context != null ? context : m_logger, level, join(prefix), join(argumentProcessor), fmt, arg1);
    }

    @Override
    public void log(ILogger context,
                    Severity level,
                    String prefix,
                    Function<Object, Object> argumentProcessor,
                    String fmt,
                    Object arg1,
                    Object arg2)
    {
        if (!isEnabled(level))
        {
            return;
        }

        m_logger.log(context != null ? context : m_logger, level, join(prefix), join(argumentProcessor), fmt, arg1, arg2);
    }

    @Override
    public void log(ILogger context,
                    Severity level,
                    String prefix,
                    Function<Object, Object> argumentProcessor,
                    String fmt,
                    Object arg1,
                    Object arg2,
                    Object arg3)
    {
        if (!isEnabled(level))
        {
            return;
        }

        m_logger.log(context != null ? context : m_logger, level, join(prefix), join(argumentProcessor), fmt, arg1, arg2, arg3);
    }

    @Override
    public void log(ILogger context,
                    Severity level,
                    String prefix,
                    Function<Object, Object> argumentProcessor,
                    String fmt,
                    Object arg1,
                    Object arg2,
                    Object arg3,
                    Object arg4)
    {
        if (!isEnabled(level))
        {
            return;
        }

        m_logger.log(context != null ? context : m_logger, level, join(prefix), join(argumentProcessor), fmt, arg1, arg2, arg3, arg4);
    }

    @Override
    public void log(ILogger context,
                    Severity level,
                    String prefix,
                    Function<Object, Object> argumentProcessor,
                    String fmt,
                    Object arg1,
                    Object arg2,
                    Object arg3,
                    Object arg4,
                    Object arg5)
    {
        if (!isEnabled(level))
        {
            return;
        }

        m_logger.log(context != null ? context : m_logger, level, join(prefix), join(argumentProcessor), fmt, arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public void log(ILogger context,
                    Severity level,
                    String prefix,
                    Function<Object, Object> argumentProcessor,
                    String fmt,
                    Object arg1,
                    Object arg2,
                    Object arg3,
                    Object arg4,
                    Object arg5,
                    Object arg6)
    {
        if (!isEnabled(level))
        {
            return;
        }

        m_logger.log(context != null ? context : m_logger, level, join(prefix), join(argumentProcessor), fmt, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public void log(ILogger context,
                    Severity level,
                    String prefix,
                    Function<Object, Object> argumentProcessor,
                    String fmt,
                    Object... args)
    {
        if (!isEnabled(level))
        {
            return;
        }

        m_logger.log(context != null ? context : m_logger, level, join(prefix), join(argumentProcessor), fmt, args);
    }

    //--//

    public void invalidatePrefix()
    {
        m_prefixFetched = false;
    }

    public String getPrefix()
    {
        return null;
    }

    protected Function<Object, Object> getArgumentProcessor()
    {
        return null;
    }

    //--//

    private String join(String prefix)
    {
        if (!m_prefixFetched)
        {
            m_prefix = getPrefix();
            m_prefixFetched = true;
        }

        if (m_prefix == null)
        {
            return prefix;
        }

        if (prefix == null)
        {
            return m_prefix;
        }

        return m_prefix + " " + prefix;
    }

    private Function<Object, Object> join(Function<Object, Object> argumentProcessor)
    {
        Function<Object, Object> ourProcessor = getArgumentProcessor();

        if (ourProcessor == null)
        {
            return argumentProcessor;
        }

        if (argumentProcessor == null)
        {
            return ourProcessor;
        }

        return (arg) ->
        {
            arg = argumentProcessor.apply(arg);
            arg = ourProcessor.apply(arg);
            return arg;
        };
    }
}
