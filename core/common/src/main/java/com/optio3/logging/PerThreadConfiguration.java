/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.logging;

import java.util.Map;

import com.google.common.collect.Maps;

class PerThreadConfiguration
{
    static final ThreadLocal<PerThreadConfiguration> s_perThreadConfiguration = new ThreadLocal<>();

    private final PerThreadConfiguration m_parent;
    private       String                 m_indent;

    private Map<Logger, Map<Severity, Boolean>> m_changes = Maps.newHashMap();

    private PerThreadConfiguration(PerThreadConfiguration parent)
    {
        m_parent = parent;
        m_indent = m_parent != null ? m_parent.m_indent : "";
    }

    void pop()
    {
        Map<Logger, Map<Severity, Boolean>> changes = m_changes;
        m_changes = null; // Block recursion.

        if (changes != null)
        {
            for (Logger logger : changes.keySet())
            {
                Map<Severity, Boolean> map = changes.get(logger);

                for (Severity sev : map.keySet())
                {
                    Boolean enabled = map.get(sev);
                    if (enabled != null)
                    {
                        if (enabled)
                        {
                            logger.enablePerThread(sev);
                        }
                        else
                        {
                            logger.disablePerThread(sev);
                        }
                    }
                    else
                    {
                        logger.inheritPerThread(sev);
                    }
                }
            }
        }

        s_perThreadConfiguration.set(m_parent);
    }

    void setIndent(String indent)
    {
        m_indent = indent;
    }

    static String getIndent()
    {
        PerThreadConfiguration config = s_perThreadConfiguration.get();
        return config != null ? config.m_indent : "";
    }

    static LoggerResource push()
    {
        PerThreadConfiguration oldConfig = s_perThreadConfiguration.get();
        PerThreadConfiguration newConfig = new PerThreadConfiguration(oldConfig);

        s_perThreadConfiguration.set(newConfig);

        return new LoggerResource(newConfig);
    }

    static LoggerResource indent(String prefix)
    {
        LoggerResource res = push();

        PerThreadConfiguration config = ensurePerThreadConfiguration();
        config.m_indent += prefix;

        return res;
    }

    static void recordState(Logger logger,
                            Severity sev,
                            Boolean enabled)
    {
        PerThreadConfiguration config = ensurePerThreadConfiguration();
        if (config.m_changes != null)
        {
            Map<Severity, Boolean> map = config.m_changes.computeIfAbsent(logger, (logger2) -> Maps.newHashMap());
            map.put(sev, enabled);
        }
    }

    private static PerThreadConfiguration ensurePerThreadConfiguration()
    {
        PerThreadConfiguration config = s_perThreadConfiguration.get();
        if (config == null)
        {
            config = new PerThreadConfiguration(null);
            s_perThreadConfiguration.set(config);
        }
        return config;
    }
}
