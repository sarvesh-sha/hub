/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.SingularAttribute;

import com.google.common.collect.Sets;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.type.Type;

/**
 * Abstraction of the low-level Interceptor values, to expose the state through Metamodel attributes, to make it easier for clients to access the state without knowing the details.
 */
public class InterceptorState
{
    private final Interceptor  m_ctx;
    private final Object       m_entity;
    private final Serializable m_id;
    private final Object[]     m_currentState;
    private final Object[]     m_previousState;
    private final String[]     m_propertyNames;
    private final Type[]       m_types;
    private final String[]     m_dirtyAttributesFromTracker;
    private       boolean[]    m_changed;

    private boolean m_stateUpdated;

    InterceptorState(Interceptor ctx,
                     Object entity,
                     Serializable id,
                     Object[] currentState,
                     Object[] previousState,
                     String[] propertyNames,
                     Type[] types,
                     String[] dirtyAttributesFromTracker)
    {
        m_ctx                        = ctx;
        m_entity                     = entity;
        m_id                         = id;
        m_currentState               = currentState;
        m_previousState              = previousState;
        m_propertyNames              = propertyNames;
        m_types                      = types;
        m_dirtyAttributesFromTracker = dirtyAttributesFromTracker;
    }

    boolean onSave()
    {
        Optio3Lifecycle lc = castAs();
        if (lc != null)
        {
            lc.onSave(this);
        }

        return m_stateUpdated;
    }

    boolean onLoad()
    {
        Optio3Lifecycle lc = castAs();
        if (lc != null)
        {
            lc.onLoad(this);
        }

        return m_stateUpdated;
    }

    boolean onFlushDirty()
    {
        Optio3Lifecycle lc = castAs();
        if (lc != null)
        {
            lc.onFlushDirty(this);
        }

        return m_stateUpdated;
    }

    void onDelete()
    {
        Optio3Lifecycle lc = castAs();
        if (lc != null)
        {
            lc.onDelete(this);
        }
    }

    //--//

    public Class<?> getEntityClass()
    {
        return SessionHolder.getClassOfEntity(m_entity);
    }

    public Serializable getId()
    {
        return m_id;
    }

    public Type[] getTypes()
    {
        return m_types;
    }

    public <T> boolean hasChanged(SingularAttribute<?, T> attr)
    {
        int idx = findProperty(attr);
        if (idx < 0)
        {
            throw allocateHibernateException(attr);
        }

        if (m_changed != null && m_changed[idx])
        {
            return true;
        }

        if (m_previousState != null)
        {
            return m_currentState[idx] != m_previousState[idx];
        }

        String name = m_propertyNames[idx];
        if (m_dirtyAttributesFromTracker != null)
        {
            for (String dirtyName : m_dirtyAttributesFromTracker)
            {
                if (dirtyName.equals(name))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public <T> T getValue(SingularAttribute<?, T> attr)
    {
        int idx = findProperty(attr);
        if (idx < 0)
        {
            throw allocateHibernateException(attr);
        }

        return attr.getJavaType()
                   .cast(m_currentState[idx]);
    }

    public <T> T getPreviousValue(SingularAttribute<?, T> attr)
    {
        int idx = findProperty(attr);
        if (idx < 0)
        {
            throw allocateHibernateException(attr);
        }

        return attr.getJavaType()
                   .cast(m_previousState[idx]);
    }

    public <T> void setValue(SingularAttribute<?, T> attr,
                             T value)
    {
        int idx = findProperty(attr);
        if (idx < 0)
        {
            throw allocateHibernateException(attr);
        }

        if (m_changed == null)
        {
            m_changed = new boolean[m_currentState.length];
        }

        m_changed[idx]      = true;
        m_currentState[idx] = attr.getJavaType()
                                  .cast(value);
        m_stateUpdated      = true;
    }

    public Set<String> getChangeSet()
    {
        Set<String> res = Sets.newHashSet();

        if (m_changed != null)
        {
            for (int i = 0; i < m_changed.length; i++)
            {
                if (m_changed[i])
                {
                    res.add(m_propertyNames[i]);
                }
            }
        }

        if (m_dirtyAttributesFromTracker != null)
        {
            for (String name : m_dirtyAttributesFromTracker)
            {
                res.add(name);
            }
        }

        return res;
    }

    //--//

    public SessionFactory getSessionFactory()
    {
        return m_ctx.getSessionFactory();
    }

    //--//

    private Optio3Lifecycle castAs()
    {
        return Reflection.as(m_entity, Optio3Lifecycle.class);
    }

    private int findProperty(Attribute<?, ?> attr)
    {
        String name = attr.getName();
        for (int i = 0; i < m_propertyNames.length; i++)
        {
            if (m_propertyNames[i].equals(name))
            {
                return i;
            }
        }

        return -1;
    }

    private <T> HibernateException allocateHibernateException(SingularAttribute<?, T> attr)
    {
        return Exceptions.newGenericException(HibernateException.class,
                                              "Invalid attribute '%s', not applicable to '%s'",
                                              attr.getJavaMember(),
                                              SessionHolder.getClassOfEntity(m_entity)
                                                           .getSimpleName());
    }
}
