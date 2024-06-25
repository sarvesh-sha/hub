/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.logging;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

class SeverityStatus
{
    static class PerThread
    {
        WeakReference<Thread> m_weakThread;
        Boolean               m_enabled;

        PerThread       m_parent;
        List<PerThread> m_children;

        PerThread(Thread t)
        {
            m_weakThread = new WeakReference<>(t);
        }

        void linkChild(PerThread child)
        {
            if (child.m_parent != this)
            {
                child.unlinkFromParent();

                if (m_children == null)
                {
                    m_children = Lists.newArrayList();
                }

                m_children.add(child);
                child.m_parent = this;
            }
        }

        void unlinkFromParent()
        {
            if (m_parent != null)
            {
                List<PerThread> children = m_parent.m_children;

                m_parent = null;

                children.remove(this);
                if (children.isEmpty())
                {
                    m_parent.m_children = null;
                }
            }
        }
    }

    private static final int c_maxSeverityLevels = 6;

    private final Logger   m_owner;
    private final Severity m_sev;
    private       boolean  m_localEnabled;
    private       boolean  m_localDisabled;

    private boolean         m_enabled;
    private List<PerThread> m_perThread;

    SeverityStatus(Logger owner,
                   Severity sev)
    {
        m_owner = owner;
        m_sev = sev;
    }

    //--//

    boolean enable()
    {
        if (!m_localEnabled)
        {
            m_localEnabled = true;
            m_localDisabled = false;

            return true;
        }

        return false; // Not changed
    }

    boolean disable()
    {
        if (!m_localDisabled)
        {
            m_localEnabled = false;
            m_localDisabled = true;

            return true;
        }

        return false; // Not changed
    }

    boolean inherit()
    {
        if (m_localEnabled || m_localDisabled)
        {
            m_localEnabled = false;
            m_localDisabled = false;

            return true;
        }

        return false; // Not changed
    }

    void enablePerThread()
    {
        setPerThread(true);
    }

    void disablePerThread()
    {
        setPerThread(false);
    }

    private void setPerThread(Boolean state)
    {
        PerThreadConfiguration.recordState(m_owner, m_sev, state);

        PerThread pt = ensurePerThread();
        pt.m_enabled = state;

        m_owner.ensurePerThreadInChildren(m_sev, pt);
    }

    void inheritPerThread()
    {
        PerThreadConfiguration.recordState(m_owner, m_sev, null);

        PerThread pt = getPerThread();
        if (pt == null)
        {
            return; // Already not per-thread, nothing to do.
        }

        pt.m_enabled = null;

        if (pt.m_parent == null)
        {
            removePerThread();
        }
    }

    @Nonnull
    PerThread ensurePerThread()
    {
        return getPerThread(true);
    }

    private PerThread getPerThread()
    {
        return getPerThread(false);
    }

    @Nonnull
    private PerThread getPerThread(boolean createIfMissing)
    {
        synchronized (this)
        {
            Thread t = Thread.currentThread();

            PerThread pt = findPerThread(t, false);
            if (pt == null && createIfMissing)
            {
                if (m_perThread == null)
                {
                    m_perThread = Lists.newArrayList();
                }

                pt = new PerThread(t);
                m_perThread.add(pt);
            }

            return pt;
        }
    }

    Boolean getLocalConfiguration()
    {
        if (m_localEnabled)
        {
            return true;
        }

        if (m_localDisabled)
        {
            return false;
        }

        return null;
    }

    boolean isEnabled()
    {
        return m_perThread == null ? m_enabled : isEnabledSlow();
    }

    private boolean isEnabledSlow()
    {
        for (PerThread pt = getPerThread(); pt != null; pt = pt.m_parent)
        {
            if (pt.m_enabled != null)
            {
                return pt.m_enabled;
            }
        }

        return m_enabled;
    }

    //--//

    public Severity getTarget()
    {
        return m_sev;
    }

    //--//

    static SeverityStatus[] newArray()
    {
        return new SeverityStatus[c_maxSeverityLevels];
    }

    void recomputeLevel()
    {
        if (m_localEnabled || m_localDisabled)
        {
            m_enabled = m_localEnabled;
        }
        else
        {
            Logger parent = m_owner.getParentLogger();
            if (parent != null)
            {
                m_enabled = parent.accessStatus(m_sev).m_enabled;
            }
            else
            {
                m_enabled = false;
            }
        }

        if (m_perThread != null)
        {
            synchronized (this)
            {
                Iterator<PerThread> it = m_perThread.iterator();
                while (it.hasNext())
                {
                    PerThread pt = it.next();

                    Thread t2 = pt.m_weakThread.get();
                    if (t2 == null)
                    {
                        it.remove();
                    }
                }

                if (m_perThread.isEmpty())
                {
                    m_perThread = null;
                }
            }
        }
    }

    private PerThread removePerThread()
    {
        synchronized (this)
        {
            Thread t = Thread.currentThread();

            return findPerThread(t, true);
        }
    }

    private PerThread findPerThread(Thread t,
                                    boolean removeIfFound)
    {
        if (m_perThread != null)
        {
            Iterator<PerThread> it = m_perThread.iterator();
            while (it.hasNext())
            {
                PerThread pt = it.next();

                Thread t2 = pt.m_weakThread.get();
                if (t2 == null || (removeIfFound && t2 == t))
                {
                    it.remove();

                    if (m_perThread.isEmpty())
                    {
                        m_perThread = null;
                    }
                }

                if (t2 == t)
                {
                    return pt;
                }
            }
        }

        return null;
    }
}
