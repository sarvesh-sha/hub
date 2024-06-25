/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3Sanitize;
import com.optio3.cloud.annotation.Optio3SanitizeRecordReference;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import com.optio3.util.function.CallableWithoutException;

public abstract class ModelSanitizerContext
{
    public static class Simple extends ModelSanitizerContext
    {
        private final SessionHolder m_sessionHolder;

        public Simple(SessionHolder sessionHolder)
        {
            m_sessionHolder = sessionHolder;
        }

        @Override
        public <T> T getService(Class<T> serviceClass)
        {
            if (serviceClass == SessionHolder.class)
            {
                return serviceClass.cast(m_sessionHolder);
            }

            return super.getService(serviceClass);
        }

        @Override
        protected boolean assumeRecordsExist()
        {
            return m_sessionHolder == null;
        }
    }

    public static class SimpleLazy extends ModelSanitizerContext implements AutoCloseable
    {
        private final CallableWithoutException<SessionHolder> m_sessionHolderProducer;
        private       SessionHolder                           m_sessionHolder;

        public SimpleLazy(CallableWithoutException<SessionHolder> sessionHolderProducer)
        {
            m_sessionHolderProducer = sessionHolderProducer;
        }

        @Override
        public void close()
        {
            if (m_sessionHolder != null)
            {
                m_sessionHolder.close();
                m_sessionHolder = null;
            }
        }

        @Override
        public <T> T getService(Class<T> serviceClass)
        {
            if (serviceClass == SessionHolder.class)
            {
                if (m_sessionHolder == null)
                {
                    m_sessionHolder = m_sessionHolderProducer.call();
                }

                return serviceClass.cast(m_sessionHolder);
            }

            return super.getService(serviceClass);
        }

        @Override
        protected boolean assumeRecordsExist()
        {
            return m_sessionHolderProducer == null;
        }
    }

    static class FieldHandler
    {
        final Reflection.FieldAccessor accessor;

        Type                  collectionType;
        Type                  mapType;
        ModelSanitizerHandler handler;

        FieldHandler(Field field)
        {
            this.accessor = new Reflection.FieldAccessor(field);
        }
    }

    private static final ConcurrentMap<Class<?>, FieldHandler[]> s_fieldsPerClass = Maps.newConcurrentMap();

    private final LinkedList<Object>                        m_visitStack = new LinkedList<>();
    private final Map<Object, ModelSanitizerHandler.Target> m_visited    = Maps.newHashMap();

    private int m_countRemoved;
    private int m_countReplaced;

    //--//

    public boolean wasModified()
    {
        return m_countRemoved != 0 || m_countReplaced != 0;
    }

    public int getCountRemoved()
    {
        return m_countRemoved;
    }

    public int getCountReplaced()
    {
        return m_countReplaced;
    }

    protected abstract boolean assumeRecordsExist();

    public <T> T getService(Class<T> serviceClass)
    {
        throw Exceptions.newRuntimeException("Unknown service %s", serviceClass);
    }

    public <T> T fetchEntity(Class<T> clz,
                             String id)
    {
        SessionHolder sessionHolder = getService(SessionHolder.class);
        return sessionHolder.getEntityOrNull(clz, id);
    }

    public <T> boolean isValidEntity(Class<T> clz,
                                     String id)
    {
        return assumeRecordsExist() || fetchEntity(clz, id) != null;
    }

    public <T extends RecordWithCommonFields> T fetchIdentity(TypedRecordIdentity<T> ri)
    {
        SessionHolder sessionHolder = getService(SessionHolder.class);
        return sessionHolder.fromIdentityOrNull(ri);
    }

    public <T extends RecordWithCommonFields> boolean isValidIdentity(TypedRecordIdentity<T> ri)
    {
        return assumeRecordsExist() || fetchIdentity(ri) != null;
    }

    public <T> T findContainer(Class<T> clz)
    {
        for (var it = m_visitStack.descendingIterator(); it.hasNext(); )
        {
            T obj = Reflection.as(it.next(), clz);
            if (obj != null)
            {
                return obj;
            }
        }

        return null;
    }

    @SuppressWarnings({ "unchecked" })
    public <T> T processTyped(T obj)
    {
        if (obj == null)
        {
            return null;
        }

        ModelSanitizerHandler.Target target = process(obj);
        return target.castTo((Class<? extends T>) obj.getClass());
    }

    public final ModelSanitizerHandler.Target process(Object obj)
    {
        return processInner(obj, null);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected ModelSanitizerHandler.Target processInner(Object obj,
                                                        ModelSanitizerHandler handler)
    {
        if (obj == null)
        {
            return ModelSanitizerHandler.Target.Null;
        }

        ModelSanitizerHandler.Target target = m_visited.get(obj);
        if (target != null)
        {
            return target;
        }

        target = new ModelSanitizerHandler.Target(obj);

        Class<?> objClass = obj.getClass();
        if (objClass == String.class || objClass.isPrimitive() || objClass.isEnum())
        {
            return target;
        }

        // Block recursion.
        m_visited.put(obj, target);

        try
        {
            m_visitStack.add(obj);

            {
                List lst = Reflection.as(obj, List.class);
                if (lst != null)
                {
                    processList(lst, handler);

                    return target;
                }
            }

            {
                Set set = Reflection.as(obj, Set.class);
                if (set != null)
                {
                    processSet(set, handler);

                    return target;
                }
            }

            if (checkIfMap(objClass) != null)
            {
                processMap((Map<String, Object>) obj, handler);

                return target;
            }

            if (Reflection.isSubclassOf(TypedRecordIdentity.class, objClass))
            {
                TypedRecordIdentity<?> ri = (TypedRecordIdentity<?>) obj;

                if (ri.getEntityClass() == null)
                {
                    Class<?> entityClass = handler != null ? handler.getEntityClassHint() : null;

                    if (entityClass == null)
                    {
                        target.remove = true;
                        return target;
                    }

                    m_countReplaced++;
                    ri.setEntityClass(entityClass);
                }

                target.remove = !isValidIdentity(ri);

                return target;
            }

            for (FieldHandler fh : collectFields(objClass))
            {
                Object fieldValue = fh.accessor.get(obj);
                if (fieldValue == null)
                {
                    continue;
                }

                if (fh.collectionType != null)
                {
                    Collection<?> coll = (Collection<?>) fieldValue;

                    List lst = Reflection.as(coll, List.class);
                    if (lst != null)
                    {
                        processList(lst, fh.handler);
                        continue;
                    }

                    Set set = Reflection.as(target, Set.class);
                    if (set != null)
                    {
                        processSet(set, fh.handler);
                        continue;
                    }
                }
                else if (fh.mapType != null)
                {
                    processMap((Map<String, Object>) fieldValue, fh.handler);
                }
                else
                {
                    var res = process(fieldValue);

                    if (res.remove)
                    {
                        m_countRemoved++;
                        fh.accessor.set(obj, null);
                    }
                    else if (res.replace)
                    {
                        m_countReplaced++;
                        fh.accessor.set(obj, res.value);
                    }
                }
            }

            return target;
        }
        finally
        {
            m_visitStack.remove(obj);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void processList(List lst,
                             ModelSanitizerHandler handler)
    {
        for (int i = 0; i < lst.size(); i++)
        {
            var res = processInner(lst.get(i), handler);

            if (res.remove)
            {
                m_countRemoved++;
                lst.remove(i);
                i--;
            }
            else if (res.replace)
            {
                m_countReplaced++;
                lst.set(i, res.value);
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void processSet(Set set,
                            ModelSanitizerHandler handler)
    {
        for (Object el : Sets.newHashSet(set))
        {
            var res = processInner(el, handler);

            if (res.remove)
            {
                m_countRemoved++;
                set.remove(el);
            }
            else if (res.replace)
            {
                m_countReplaced++;
                set.remove(el);
                set.add(res.value);
            }
        }
    }

    private void processMap(Map<String, Object> map,
                            ModelSanitizerHandler handler)
    {
        Set<String> keys = Sets.newHashSet(map.keySet());

        for (String key : keys)
        {
            var res = processInner(map.get(key), handler);

            if (res.remove)
            {
                m_countRemoved++;
                map.remove(key);
            }
            else if (res.replace)
            {
                m_countReplaced++;
                map.put(key, res.value);
            }
        }
    }

    private static FieldHandler[] collectFields(Class<?> clz)
    {
        return s_fieldsPerClass.computeIfAbsent(clz, (clz2) ->
        {
            List<FieldHandler> lst = Lists.newArrayList();

            Map<String, Field> fields = Reflection.collectFields(clz);
            for (Field field : fields.values())
            {
                if (!Modifier.isPublic(field.getModifiers()))
                {
                    continue;
                }

                if (field.getAnnotation(JsonIgnore.class) != null)
                {
                    continue;
                }

                Type               type = field.getGenericType();
                final FieldHandler fh   = new FieldHandler(field);

                Optio3SanitizeRecordReference annoRef = field.getAnnotation(Optio3SanitizeRecordReference.class);
                if (annoRef != null)
                {
                    final Class<?> entityClass = annoRef.entityClass();

                    fh.handler = new ModelSanitizerHandler()
                    {
                        @Override
                        public void visit(ModelSanitizerContext context,
                                          Target target)
                        {
                            target.remove = !context.isValidEntity(entityClass, (String) target.value);
                        }

                        @Override
                        public Class<?> getEntityClassHint()
                        {
                            return entityClass;
                        }
                    };

                    if (Reflection.isSubclassOf(String.class, type))
                    {
                        lst.add(fh);
                        continue;
                    }

                    fh.collectionType = checkIfCollectionOf(type, String.class);
                    if (fh.collectionType != null)
                    {
                        lst.add(fh);
                        continue;
                    }

                    throw Exceptions.newIllegalArgumentException("@Optio3SanitizeRecordReference used on '%s', not a string or collection of strings field", field);
                }

                Optio3Sanitize anno = field.getAnnotation(Optio3Sanitize.class);
                if (anno != null)
                {
                    fh.collectionType = checkIfCollection(type);
                    fh.mapType        = checkIfCollection(type);
                    fh.handler        = Reflection.newInstance(anno.handler());

                    lst.add(fh);
                    continue;
                }

                //--//

                Class<?> rawType = Reflection.getRawType(type);
                if (rawType == String.class || rawType.isPrimitive() || rawType.isEnum())
                {
                    continue;
                }

                fh.collectionType = checkIfCollection(type);
                fh.mapType        = checkIfMap(type);

                final Class<?> entityClass = extractEntityClass(type);

                fh.handler = new ModelSanitizerHandler()
                {
                    @Override
                    public void visit(ModelSanitizerContext context,
                                      Target target)
                    {
                        var target2 = context.process(target.value);
                        target.copyFrom(target2);
                    }

                    @Override
                    public Class<?> getEntityClassHint()
                    {
                        return entityClass;
                    }
                };

                lst.add(fh);
            }

            var res = new FieldHandler[lst.size()];
            lst.toArray(res);
            return res;
        });
    }

    private static Type checkIfCollection(Type t)
    {
        if (Reflection.isSubclassOf(Collection.class, t))
        {
            return Reflection.getTypeArgument(t, 0);
        }

        return null;
    }

    private static Type checkIfCollectionOf(Type t,
                                            Class<?> elementsClz)
    {
        Type elementsType = checkIfCollection(t);
        if (elementsType != null)
        {
            if (Reflection.isSubclassOf(elementsClz, elementsType))
            {
                return elementsType;
            }
        }

        return null;
    }

    private static Type checkIfMap(Type t)
    {
        if (Reflection.isSubclassOf(Map.class, t))
        {
            if (Reflection.getTypeArgument(t, 0) == String.class) // Limit for Maps with string keys.
            {
                return Reflection.getTypeArgument(t, 1);
            }
        }

        return null;
    }

    private static Class<?> extractEntityClass(Type type)
    {
        if (Reflection.isSubclassOf(TypedRecordIdentity.class, type))
        {
            return Reflection.getRawType(Reflection.getTypeArgument(type, 0));
        }

        if (Reflection.isSubclassOf(TypedRecordIdentityList.class, type))
        {
            return Reflection.getRawType(Reflection.getTypeArgument(type, 0));
        }

        var coll = checkIfCollection(type);
        if (coll != null)
        {
            return extractEntityClass(coll);
        }

        var map = checkIfMap(type);
        if (map != null)
        {
            return extractEntityClass(map);
        }

        return null;
    }
}