/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.io.Serializable;
import java.util.Set;

import com.google.common.collect.Sets;
import com.optio3.serialization.Reflection;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.type.Type;

/**
 * Low-level class used to interact with Hibernate, just before records are sent or after they are retrieved from the database.
 * <p>
 * This allows last-minute tweaks to the state of records, like updating the modification timestamp, or bumping a version number.
 */
public abstract class Interceptor extends EmptyInterceptor
{
    private static final long serialVersionUID = 1L;

    @Override
    public boolean onSave(Object entity,
                          Serializable id,
                          Object[] state,
                          String[] propertyNames,
                          Type[] types) throws
                                        CallbackException
    {
        InterceptorState is = new InterceptorState(this, entity, id, state, null, propertyNames, types, null);
        return is.onSave();
    }

    @Override
    public boolean onLoad(Object entity,
                          Serializable id,
                          Object[] state,
                          String[] propertyNames,
                          Type[] types) throws
                                        CallbackException
    {
        InterceptorState is = new InterceptorState(this, entity, id, state, null, propertyNames, types, null);
        return is.onLoad();
    }

    @Override
    public int[] findDirty(Object entity,
                           Serializable id,
                           Object[] currentState,
                           Object[] previousState,
                           String[] propertyNames,
                           Type[] types)
    {
        SelfDirtinessTracker tracker = Reflection.as(entity, SelfDirtinessTracker.class);
        if (tracker != null && tracker.$$_hibernate_hasDirtyAttributes())
        {
            InterceptorState is = new InterceptorState(this, entity, id, currentState, previousState, propertyNames, types, tracker.$$_hibernate_getDirtyAttributes());
            if (is.onFlushDirty())
            {
                Set<String> dirtyProperties = is.getChangeSet();

                Set<Integer> dirtyIndices = Sets.newHashSet();

                for (String prop : dirtyProperties)
                {
                    int index = findIndex(propertyNames, prop);
                    if (index >= 0)
                    {
                        dirtyIndices.add(index);
                    }
                }

                if (!dirtyIndices.isEmpty())
                {
                    int[] result = new int[dirtyIndices.size()];
                    int   pos    = 0;

                    for (int idx : dirtyIndices)
                        result[pos++] = idx;

                    return result;
                }
            }
        }

        return null;
    }

    private int findIndex(String[] array,
                          String target)
    {
        for (int i = 0; i < array.length; i++)
        {
            if (array[i].equals(target))
            {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void onDelete(Object entity,
                         Serializable id,
                         Object[] state,
                         String[] propertyNames,
                         Type[] types) throws
                                       CallbackException
    {
        InterceptorState is = new InterceptorState(this, entity, id, state, null, propertyNames, types, null);
        is.onDelete();
    }

    //--//

    public abstract SessionFactory getSessionFactory();
}
