/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import io.dropwizard.logging.LoggingUtil;
import org.apache.commons.lang3.StringUtils;

class LegacyLogging
{
    public static final Logger LoggerInstance = new Logger(LegacyLogging.class);

    public static void configureLevels()
    {
        // Turn down logging for various libraries.
        {
            org.apache.log4j.Logger apacheRoot = org.apache.log4j.LogManager.getRootLogger();
            apacheRoot.setLevel(org.apache.log4j.Level.WARN);

            final ch.qos.logback.classic.Logger root = LoggingUtil.getLoggerContext()
                                                                  .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

            root.setLevel(ch.qos.logback.classic.Level.WARN);
        }
    }

    public static void redirect()
    {
        final ch.qos.logback.classic.Logger root = LoggingUtil.getLoggerContext()
                                                              .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        root.detachAndStopAllAppenders();

        root.addAppender(new Appender<ILoggingEvent>()
        {
            private String m_name = "Optio3 Legacy Bridge";
            private ch.qos.logback.core.Context m_context;

            @Override
            public String getName()
            {
                return m_name;
            }

            @Override
            public void setName(String s)
            {
                m_name = s;
            }

            @Override
            public void setContext(Context context)
            {
                m_context = context;
            }

            @Override
            public Context getContext()
            {
                return m_context;
            }

            @Override
            public void doAppend(ILoggingEvent iLoggingEvent) throws
                                                              LogbackException
            {
                Level    level            = iLoggingEvent.getLevel();
                String   loggerName       = iLoggingEvent.getLoggerName();
                String   formattedMessage = iLoggingEvent.getFormattedMessage();
                Severity severity;

                if (StringUtils.equals(loggerName, "org.eclipse.jetty.util.ssl.SslContextFactory.config"))
                {
                    // This logger is quiet verbose, whenever we have to reconnect. Downgrade it.
                    severity = Severity.Debug;
                }
                else if (StringUtils.equals(loggerName, "org.hibernate.engine.internal.StatefulPersistenceContext") && StringUtils.startsWith(formattedMessage, "HHH000179"))
                {
                    // Nuisance message from Hibernate. Downgrade it.
                    severity = Severity.Debug;
                }
                else if (StringUtils.startsWith(loggerName, "com.microsoft.azure.sdk.iot.device."))
                {
                    // Nuisance message from Azure. Downgrade it.
                    severity = Severity.Debug;
                }
                else if (StringUtils.equals(loggerName, "com.azure.identity.ClientSecretCredential"))
                {
                    // Nuisance message from Azure. Downgrade it.
                    severity = Severity.Debug;
                }
                else if (level.isGreaterOrEqual(Level.ERROR))
                {
                    severity = Severity.Error;
                }
                else if (level.isGreaterOrEqual(Level.WARN))
                {
                    severity = Severity.Warn;
                }
                else if (level.isGreaterOrEqual(Level.INFO))
                {
                    severity = Severity.Info;
                }
                else
                {
                    severity = Severity.Debug;
                }

                LoggerInstance.log(null, severity, null, null, "%s : %s", loggerName, formattedMessage);
            }

            @Override
            public void addStatus(Status status)
            {

            }

            @Override
            public void addInfo(String s)
            {

            }

            @Override
            public void addInfo(String s,
                                Throwable throwable)
            {

            }

            @Override
            public void addWarn(String s)
            {

            }

            @Override
            public void addWarn(String s,
                                Throwable throwable)
            {

            }

            @Override
            public void addError(String s)
            {

            }

            @Override
            public void addError(String s,
                                 Throwable throwable)
            {

            }

            @Override
            public void addFilter(Filter<ILoggingEvent> filter)
            {

            }

            @Override
            public void clearAllFilters()
            {

            }

            @Override
            public List<Filter<ILoggingEvent>> getCopyOfAttachedFiltersList()
            {
                return null;
            }

            @Override
            public FilterReply getFilterChainDecision(ILoggingEvent iLoggingEvent)
            {
                return null;
            }

            @Override
            public void start()
            {

            }

            @Override
            public void stop()
            {

            }

            @Override
            public boolean isStarted()
            {
                return true;
            }
        });
    }
}
