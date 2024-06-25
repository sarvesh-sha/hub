/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.logging;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.collection.WeakLinkedList;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class Logger implements ILogger
{
    private static final Logger s_root = new Logger();

    private final Logger                               m_parentLogger;
    private final boolean                              m_dontForwardToRemote;
    private final List<Logger>                         m_childLoggers = Lists.newArrayList();
    private final String                               m_selector;
    private final String                               m_selectorFull;
    private final SeverityStatus[]                     m_statuses;
    private       Map<Class<?>, Logger>                m_lookupSubLoggers;
    private       WeakLinkedList<LoggerChangeDetector> m_changeDetectors;

    public Logger(Class<?> selector)
    {
        this(selector, s_root, false);
    }

    public Logger(Class<?> selector,
                  boolean dontForwardToRemote)
    {
        this(selector, s_root, dontForwardToRemote);
    }

    private Logger()
    {
        this(Logger.class, null, false);

        accessStatus(Severity.Info).enable();
        accessStatus(Severity.Warn).enable();
        accessStatus(Severity.Error).enable();

        recomputeLevels();
    }

    private Logger(Class<?> selector,
                   Logger parentLogger,
                   boolean dontForwardToRemote)
    {
        m_dontForwardToRemote = dontForwardToRemote;

        m_parentLogger = parentLogger;
        m_statuses     = SeverityStatus.newArray();

        for (Severity sev : Severity.values())
        {
            SeverityStatus status = new SeverityStatus(this, sev);
            m_statuses[sev.order()] = status;
        }

        if (selector == null)
        {
            m_selector     = null;
            m_selectorFull = null;
        }
        else
        {
            m_selectorFull = selector.getName();

            String shortSelector = null;

            while (true)
            {
                String simpleName = selector.getSimpleName();

                if (shortSelector != null)
                {
                    shortSelector = simpleName + "." + shortSelector;
                }
                else
                {
                    shortSelector = simpleName;
                }

                Class<?> selectorHost = selector.getNestHost();
                if (selectorHost == selector)
                {
                    break;
                }

                selector = selectorHost;
            }

            m_selector = shortSelector;
        }

        if (parentLogger != null)
        {
            LoggerFactory.execUnderLock(() -> parentLogger.m_childLoggers.add(this));
        }

        LoggerFactory.register(this);

        recomputeLevels();
    }

    public synchronized Logger createSubLogger(Class<?> selector)
    {
        if (m_lookupSubLoggers == null)
        {
            m_lookupSubLoggers = Maps.newHashMap();
        }

        Logger res = m_lookupSubLoggers.get(selector);
        if (res == null)
        {
            res = new Logger(selector, this, m_dontForwardToRemote);
            m_lookupSubLoggers.put(selector, res);
        }

        return res;
    }

    public Logger getParentLogger()
    {
        return m_parentLogger;
    }

    String getSelector()
    {
        return m_selector;
    }

    String getFullSelector()
    {
        return m_selectorFull;
    }

    //--//

    @Override
    public boolean canForwardToRemote()
    {
        return !m_dontForwardToRemote;
    }

    @Override
    public ILogger getInnerContext()
    {
        return null;
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

        logInner(context, level, prefix, msg);
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

        logInner(context, level, prefix, argumentProcessor, fmt, arg1);
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

        logInner(context, level, prefix, argumentProcessor, fmt, arg1, arg2);
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

        logInner(context, level, prefix, argumentProcessor, fmt, arg1, arg2, arg3);
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

        logInner(context, level, prefix, argumentProcessor, fmt, arg1, arg2, arg3, arg4);
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

        logInner(context, level, prefix, argumentProcessor, fmt, arg1, arg2, arg3, arg4, arg5);
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

        logInner(context, level, prefix, argumentProcessor, fmt, arg1, arg2, arg3, arg4, arg5, arg6);
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

        logInner(context, level, prefix, argumentProcessor, fmt, args);
    }

    //--//

    private Object[] processArgs(Object[] args,
                                 Function<Object, Object> argumentProcessor)
    {
        Object[] newArgs = null;

        for (int i = 0; i < args.length; i++)
        {
            Object o = args[i];

            if (o instanceof Throwable)
            {
                Throwable e = (Throwable) o;

                o = LoggerFactory.convertStackTraceToString(e);
            }

            if (argumentProcessor != null)
            {
                o = argumentProcessor.apply(o);
            }

            if (o instanceof AtomicInteger)
            {
                o = ((AtomicInteger) o).get();
            }
            else if (o instanceof AtomicLong)
            {
                o = ((AtomicLong) o).get();
            }

            if (o != args[i])
            {
                if (newArgs == null)
                {
                    newArgs = Arrays.copyOf(args, args.length);
                }

                newArgs[i] = o;
            }
        }

        return newArgs != null ? newArgs : args;
    }

    //--//

    public void enable(Severity level)
    {
        if (accessStatus(level).enable())
        {
            recomputeLevels();
        }
    }

    public void disable(Severity level)
    {
        if (accessStatus(level).disable())
        {
            recomputeLevels();
        }
    }

    public void inherit(Severity level)
    {
        if (accessStatus(level).inherit())
        {
            recomputeLevels();
        }
    }

    public void enablePerThread(Severity level)
    {
        accessStatus(level).enablePerThread();

        recomputeLevels();
    }

    public void disablePerThread(Severity level)
    {
        accessStatus(level).disablePerThread();

        recomputeLevels();
    }

    public void inheritPerThread(Severity level)
    {
        accessStatus(level).inheritPerThread();

        recomputeLevels();
    }

    public boolean isEnabled(Severity level)
    {
        return accessStatus(level).isEnabled();
    }

    public Boolean getLocalConfiguration(Severity level)
    {
        return accessStatus(level).getLocalConfiguration();
    }

    //--//

    void ensurePerThreadInChildren(Severity level,
                                   SeverityStatus.PerThread pt)
    {
        LoggerFactory.execUnderLock(() ->
                                    {
                                        for (Logger childLogger : m_childLoggers)
                                        {
                                            childLogger.ensurePerThread(level, pt);
                                        }
                                    });
    }

    private void ensurePerThread(Severity level,
                                 SeverityStatus.PerThread ptParent)
    {
        SeverityStatus.PerThread pt = accessStatus(level).ensurePerThread();
        ptParent.linkChild(pt);

        for (Logger childLogger : m_childLoggers)
        {
            childLogger.ensurePerThread(level, pt);
        }
    }

    //--//

    public Map<Severity, Boolean> fetchConfig()
    {
        Map<Severity, Boolean> cfg = Maps.newHashMap();

        for (Severity sev : Severity.values())
        {
            Boolean val = getLocalConfiguration(sev);
            if (val != null)
            {
                cfg.put(sev, val);
            }
        }

        return cfg;
    }

    public void applyConfig(Map<Severity, Boolean> cfg)
    {
        for (Severity sev : Severity.values())
        {
            Boolean val = cfg != null ? cfg.get(sev) : null;

            if (val == null)
            {
                inherit(sev);
            }
            else if (val)
            {
                enable(sev);
            }
            else
            {
                disable(sev);
            }
        }
    }

    //--//

    private void logInner(ILogger context,
                          Severity level,
                          String prefix,
                          Function<Object, Object> argumentProcessor,
                          String fmt,
                          Object... args)
    {
        args = processArgs(args, argumentProcessor);

        try
        {
            logInner(context, level, prefix, String.format(fmt, args));
        }
        catch (Throwable t)
        {
            logInner(context, Severity.Error, prefix, String.format("Failed to log '%s', due to %s", fmt, t.getMessage()));
        }
    }

    private void logInner(ILogger context,
                          Severity level,
                          String prefix,
                          String msg)
    {
        String indent     = PerThreadConfiguration.getIndent();
        int    currentPos = 0;
        int    maxPos     = msg.length();

        final ZonedDateTime now = TimeUtils.now();
        final String thread = Thread.currentThread()
                                    .getName();

        if (context == null)
        {
            context = this;
        }

        while (true)
        {
            int nextPosCR = msg.indexOf('\r', currentPos);
            int nextPosLF = msg.indexOf('\n', currentPos);

            if (nextPosCR < 0)
            {
                nextPosCR = maxPos;
            }

            if (nextPosLF < 0)
            {
                nextPosLF = maxPos;
            }

            int nextPos = Math.min(nextPosCR, nextPosLF);

            String subMsg = msg.substring(currentPos, nextPos);

            if (StringUtils.isNotEmpty(prefix))
            {
                subMsg = prefix + " " + subMsg;
            }

            if (indent.length() > 0)
            {
                subMsg = indent + " " + subMsg;
            }

            LoggerFactory.append(context, now, level, thread, getSelector(), subMsg);

            if (nextPosCR + 1 == nextPosLF)
            {
                // Skip \r\n as one line, not two.
                currentPos = nextPosLF + 1;
            }
            else
            {
                currentPos = nextPos + 1;
            }

            if (currentPos >= maxPos)
            {
                break;
            }
        }
    }

    //--//

    SeverityStatus accessStatus(Severity level)
    {
        return m_statuses[level.order()];
    }

    void recomputeLevels()
    {
        LoggerFactory.execUnderLock(this::recomputeLevelsInner);
    }

    private void recomputeLevelsInner()
    {
        for (SeverityStatus status : m_statuses)
        {
            status.recomputeLevel();
        }

        for (Logger childLogger : m_childLoggers)
        {
            childLogger.recomputeLevelsInner();
        }

        WeakLinkedList<LoggerChangeDetector> lst = m_changeDetectors;
        if (lst != null)
        {
            List<LoggerChangeDetector> activeList = Lists.newArrayList();

            synchronized (lst)
            {
                for (LoggerChangeDetector changeDetector : lst)
                {
                    activeList.add(changeDetector);
                }
            }

            for (LoggerChangeDetector changeDetector : activeList)
            {
                changeDetector.process();
            }
        }
    }

    void registerChangeDetector(LoggerChangeDetector detector)
    {
        WeakLinkedList<LoggerChangeDetector> lst = m_changeDetectors;
        if (lst == null)
        {
            lst               = new WeakLinkedList<>();
            m_changeDetectors = lst;
        }

        synchronized (lst)
        {
            lst.add(detector);
        }
    }
}
