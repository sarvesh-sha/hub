/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.logging;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.serialization.Reflection;
import com.optio3.util.Encryption;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class LoggerFactory
{
    private static final Map<String, Logger>                  s_map         = Maps.newConcurrentMap();
    private static final ConcurrentMap<String, ZonedDateTime> s_stackTraces = Maps.newConcurrentMap();

    private static final CopyOnWriteArrayList<ILoggerAppender> s_appenders = new CopyOnWriteArrayList<>();

    static
    {
        registerAppender(new ConsoleAppender());
    }

    static void execUnderLock(Runnable callback)
    {
        synchronized (s_map)
        {
            callback.run();
        }
    }

    static void register(Logger logger)
    {
        execUnderLock(() -> s_map.put(logger.getFullSelector(), logger));
    }

    //--//

    public static Map<String, Logger> getLoggers()
    {
        synchronized (s_map)
        {
            return Maps.newHashMap(s_map);
        }
    }

    public static List<LoggerConfiguration> getLoggersConfiguration()
    {
        List<LoggerConfiguration> results = Lists.newArrayList();

        Map<String, Logger> loggers = getLoggers();
        Map<Logger, String> reverse = Maps.newHashMap();

        for (String name : loggers.keySet())
        {
            Logger logger = loggers.get(name);

            reverse.put(logger, name);
        }

        for (String name : loggers.keySet())
        {
            Logger logger = loggers.get(name);

            LoggerConfiguration lc = new LoggerConfiguration();
            lc.name = name;

            Logger parentLogger = logger.getParentLogger();
            if (parentLogger != null)
            {
                lc.parent = reverse.get(parentLogger);
            }

            lc.levels = logger.fetchConfig();

            results.add(lc);
        }

        return results;
    }

    public static LoggerConfiguration setLoggerConfiguration(LoggerConfiguration cfg)
    {
        Map<String, Logger> loggers = getLoggers();

        Logger logger = loggers.get(cfg.name);
        if (logger != null)
        {
            logger.applyConfig(cfg.levels);

            cfg.levels = logger.fetchConfig();
        }

        return cfg;
    }

    //--//

    public static void registerAppender(ILoggerAppender appender)
    {
        // Add new appenders at the start of the list.
        s_appenders.add(0, appender);
    }

    public static void unregisterAppender(ILoggerAppender appender)
    {
        s_appenders.remove(appender);
    }

    public static void resetAppenders()
    {
        s_appenders.clear();
    }

    public static void append(ILogger context,
                              ZonedDateTime timestamp,
                              Severity level,
                              String thread,
                              String selector,
                              String msg)
    {
        for (ILoggerAppender appender : s_appenders)
        {
            try
            {
                if (appender.append(context, timestamp, level, thread, selector, msg))
                {
                    return;
                }
            }
            catch (Exception e)
            {
                // Ignore exceptions in appenders.
            }
        }
    }

    public static LoggerResource pushPerThreadConfig()
    {
        return PerThreadConfiguration.push();
    }

    public static LoggerResource indent(String prefix)
    {
        return PerThreadConfiguration.indent(prefix);
    }

    public static Object convertStackTraceToString(Throwable e)
    {
        String s = Exceptions.convertStackTraceToString(e);

        String hash = Encryption.computeSha1AsText(s);

        ZonedDateTime now  = TimeUtils.now();
        ZonedDateTime when = s_stackTraces.get(hash);
        if (when != null)
        {
            final ZonedDateTime threshold = now.minus(1, ChronoUnit.DAYS);

            if (when.isAfter(threshold))
            {
                return String.format("%s <Stack trace seen at %s>", e, when);
            }
        }

        s_stackTraces.put(hash, now);

        return s;
    }

    public static Object extractExceptionDescription(Throwable t)
    {
        t = Exceptions.unwrapException(t);
        if (t instanceof TimeoutException)
        {
            return "timeout";
        }

        String msg = t.getMessage();

        Class<? extends Throwable> clz     = t.getClass();
        String                     clzName = clz.getSimpleName();

        if (StringUtils.isNotBlank(msg))
        {
            if (t instanceof RuntimeException)
            {
                return msg;
            }

            return clzName + " (" + msg + ")";
        }

        return clzName;
    }

    public static <T> T getService(ILogger context,
                                   Class<T> clz)
    {
        while (context != null)
        {
            T svc = Reflection.as(context, clz);
            if (svc != null)
            {
                return svc;
            }

            context = context.getInnerContext();
        }

        return null;
    }
}
