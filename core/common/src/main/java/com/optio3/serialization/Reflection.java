/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.serialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.JavaType;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned32;
import com.optio3.lang.Unsigned8;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;

public class Reflection
{
    private static final Object[]   c_emptyArgs     = new Object[0];
    private static final Class<?>[] c_emptyArgTypes = new Class<?>[0];

    public static class FieldAccessor
    {
        private final Field m_field;

        public FieldAccessor(Field f)
        {
            m_field = f;

            f.setAccessible(true);
        }

        public FieldAccessor(Class<?> clz,
                             String name)
        {
            Field f = findField(clz, name);
            if (f == null)
            {
                throw Exceptions.newIllegalArgumentException("No field '%s' in class '%s'", name, clz);
            }

            m_field = f;

            f.setAccessible(true);
        }

        //--//

        public String getName()
        {
            return m_field.getName();
        }

        public <T extends Annotation> T getAnnotation(Class<T> clz)
        {
            return m_field.getAnnotation(clz);
        }

        public Class<?> getDeclaringClass()
        {
            return m_field.getDeclaringClass();
        }

        public Field getNative()
        {
            return m_field;
        }

        public Class<?> getNativeType()
        {
            return m_field.getType();
        }

        //--//

        public Object get(Object target)
        {
            try
            {
                return m_field.get(target);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public void set(Object target,
                        Object value)
        {
            try
            {
                m_field.set(target, coerceNumber(value, m_field.getType()));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    //--//

    private static final ConcurrentMap<Class<?>, Map<String, Field>>                                     s_classToFields       = Maps.newConcurrentMap();
    private static final ConcurrentMap<Class<?>, Multimap<String, Method>>                               s_classToMethods      = Maps.newConcurrentMap();
    private static final ConcurrentMap<Class<?>, Set<Class<?>>>                                          s_classToJsonSubTypes = Maps.newConcurrentMap();
    private static final ConcurrentMap<Class<?>, List<Enum<?>>>                                          s_classToEnumValues   = Maps.newConcurrentMap();
    private static final ConcurrentMap<Class<?>, TypeDescriptor>                                         s_classToDescriptor   = Maps.newConcurrentMap();
    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Method>>                          s_propertyToGetter    = Maps.newConcurrentMap();
    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, ConcurrentMap<Class<?>, Method>>> s_propertyToSetter    = Maps.newConcurrentMap();

    private static final TypeDescriptor s_nullSentinel = new TypeDescriptor(null, 0, TypeDescriptorKind.integerSigned)
    {

        @Override
        public Object fromLongValue(long value)
        {
            throw new RuntimeException("Internal error: this should never be used");
        }

        @Override
        public long asLongValue(Object value)
        {
            throw new RuntimeException("Internal error: this should never be used");
        }
    };

    static
    {
        addDescriptor(Boolean.class, new TypeDescriptor(Boolean.TYPE, 8, TypeDescriptorKind.integerUnsigned)
        {

            @Override
            public Object fromLongValue(long value)
            {
                return value != 0;
            }

            @Override
            public long asLongValue(Object value)
            {
                boolean v = (boolean) value;

                return v ? 1 : 0;
            }
        });

        addDescriptor(Character.class, new TypeDescriptor(Character.TYPE, 16, TypeDescriptorKind.integerUnsigned)
        {

            @Override
            public Object fromLongValue(long value)
            {
                return (char) value;
            }

            @Override
            public long asLongValue(Object value)
            {
                char v = (char) value;

                return v;
            }
        });

        addDescriptor(Byte.class, new TypeDescriptor(Byte.TYPE, 8, TypeDescriptorKind.integerUnsigned)
        {

            @Override
            public Object fromLongValue(long value)
            {
                return (byte) value;
            }

            @Override
            public long asLongValue(Object value)
            {
                byte v = (byte) value;

                return v;
            }
        });

        addDescriptor(Short.class, new TypeDescriptor(Short.TYPE, 16, TypeDescriptorKind.integerSigned)
        {

            @Override
            public Object fromLongValue(long value)
            {
                return (short) value;
            }

            @Override
            public long asLongValue(Object value)
            {
                short v = (short) value;

                return v;
            }
        });

        addDescriptor(Integer.class, new TypeDescriptor(Integer.TYPE, 32, TypeDescriptorKind.integerSigned)
        {

            @Override
            public Object fromLongValue(long value)
            {
                return (int) value;
            }

            @Override
            public long asLongValue(Object value)
            {
                int v = (int) value;

                return v;
            }
        });

        addDescriptor(Long.class, new TypeDescriptor(Long.TYPE, 64, TypeDescriptorKind.integerSigned)
        {

            @Override
            public Object fromLongValue(long value)
            {
                return value;
            }

            @Override
            public long asLongValue(Object value)
            {
                long v = (long) value;

                return v;
            }
        });

        addDescriptor(Float.class, new TypeDescriptor(Float.TYPE, 32, TypeDescriptorKind.floatingPoint)
        {

            @Override
            public Object fromLongValue(long value)
            {
                return Float.intBitsToFloat((int) value);
            }

            @Override
            public long asLongValue(Object value)
            {
                Float v = (Float) value;

                return Float.floatToIntBits(v);
            }
        });

        addDescriptor(Double.class, new TypeDescriptor(Double.TYPE, 64, TypeDescriptorKind.floatingPoint)
        {

            @Override
            public Object fromLongValue(long value)
            {
                return Double.longBitsToDouble(value);
            }

            @Override
            public long asLongValue(Object value)
            {
                Double v = (Double) value;

                return Double.doubleToLongBits(v);
            }
        });

        addDescriptor(Unsigned8.class, new TypeDescriptor(Unsigned8.class, 8, TypeDescriptorKind.integerUnsigned)
        {

            @Override
            public Object fromLongValue(long value)
            {
                return Unsigned8.box(value);
            }

            @Override
            public long asLongValue(Object value)
            {
                Unsigned8 v = (Unsigned8) value;

                return v.unbox();
            }
        });

        addDescriptor(Unsigned16.class, new TypeDescriptor(Unsigned16.class, 16, TypeDescriptorKind.integerUnsigned)
        {

            @Override
            public Object fromLongValue(long value)
            {
                return Unsigned16.box(value);
            }

            @Override
            public long asLongValue(Object value)
            {
                Unsigned16 v = (Unsigned16) value;

                return v.unbox();
            }
        });

        addDescriptor(Unsigned32.class, new TypeDescriptor(Unsigned32.class, 32, TypeDescriptorKind.integerUnsigned)
        {

            @Override
            public Object fromLongValue(long value)
            {
                return Unsigned32.box(value);
            }

            @Override
            public long asLongValue(Object value)
            {
                Unsigned32 v = (Unsigned32) value;

                return v.unbox();
            }
        });
    }

    static void addDescriptor(Class<?> boxedClz,
                              TypeDescriptor td)
    {
        s_classToDescriptor.putIfAbsent(td.clz, td);

        if (boxedClz != null)
        {
            td.setBoxedType(boxedClz);
            s_classToDescriptor.putIfAbsent(boxedClz, td);
        }
    }

    public static TypeDescriptor getDescriptor(Type type)
    {
        return getDescriptor(getRawType(type));
    }

    public static TypeDescriptor getDescriptor(Class<?> clz)
    {
        TypeDescriptor td = s_classToDescriptor.get(clz);
        if (td != null)
        {
            return td == s_nullSentinel ? null : td;
        }

        CustomTypeDescriptor ctd = clz.getAnnotation(CustomTypeDescriptor.class);
        if (ctd != null)
        {
            td = Reflection.newInstance(ctd.factory())
                           .create(clz);
            addDescriptor(null, td);
            return td;
        }

        Method encoding = null;
        Method decoding = null;

        for (Method md : collectMethods(clz).values())
        {
            if (encoding == null && md.isAnnotationPresent(HandlerForEncoding.class))
            {
                if (md.getParameterTypes().length == 0)
                {
                    encoding = md;
                }
            }

            if (decoding == null && md.isAnnotationPresent(HandlerForDecoding.class))
            {
                if (md.getParameterTypes().length == 1)
                {
                    decoding = md;
                }
            }
        }

        final Method encoding2 = encoding;
        final Method decoding2 = decoding;

        if (encoding2 != null && decoding2 != null)
        {
            Class<?> clzRet = encoding2.getReturnType();
            if (!clzRet.isPrimitive())
            {
                throw Exceptions.newRuntimeException("Invalid return type for HandleForEncoding on %s.%s(): %s", clz.getName(), encoding2.getName(), clzRet.getName());
            }

            Class<?> clzArg = decoding2.getParameterTypes()[0];
            if (!clzArg.isPrimitive())
            {
                throw Exceptions.newRuntimeException("Invalid return type for HandleForDecoding on %s.%s(): %s", clz.getName(), decoding2.getName(), clzArg.getName());
            }

            TypeDescriptor tdReturn = getDescriptor(clzRet);

            td = new TypeDescriptor(clz, tdReturn.size, TypeDescriptorKind.integerUnsigned)
            {

                @Override
                public Object fromLongValue(long value)
                {
                    try
                    {
                        Object arg;

                        if (clzArg == byte.class)
                        {
                            arg = (byte) value;
                        }
                        else if (clzArg == short.class)
                        {
                            arg = (short) value;
                        }
                        else if (clzArg == int.class)
                        {
                            arg = (int) value;
                        }
                        else
                        {
                            arg = value;
                        }

                        return decoding2.invoke(null, arg);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public long asLongValue(Object value)
                {
                    try
                    {
                        Number res = (Number) encoding2.invoke(value);

                        return res.longValue();
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            };
            addDescriptor(null, td);
            return td;
        }

        s_classToDescriptor.putIfAbsent(clz, s_nullSentinel);
        return null;
    }

    public static boolean canAssignTo(Class<?> targetType,
                                      Type typeToCheck)
    {
        Class<?> sourceType = Reflection.getRawType(typeToCheck);

        if (targetType.isPrimitive() || sourceType.isPrimitive())
        {
            TypeDescriptor tdSource = Reflection.getDescriptor(sourceType);
            TypeDescriptor tdTarget = Reflection.getDescriptor(targetType);

            return tdSource == tdTarget;
        }

        return Reflection.isSubclassOf(targetType, typeToCheck);
    }

    @FunctionalInterface
    public interface CoerceFromDouble<T>
    {
        T coerce(double val);
    }

    @SuppressWarnings("unchecked")
    public static <O> CoerceFromDouble<O> buildNumberCoercerFromDouble(Class<O> clzTarget)
    {
        clzTarget = getBoxedType(clzTarget);

        TypeDescriptor tdTarget = Reflection.getDescriptor(clzTarget);
        if (tdTarget == null)
        {
            return clzTarget::cast;
        }

        @SuppressWarnings("unchecked") Class<O> boxedType = (Class<O>) tdTarget.getBoxedType();
        if (boxedType != null)
        {
            clzTarget = boxedType;
        }

        Class<O> finalClzTarget = clzTarget;

        if (tdTarget.isFloatingType())
        {
            if (tdTarget.size == 32)
            {
                return (i) ->
                {
                    return finalClzTarget.cast((float) i);
                };
            }
            else
            {
                return (i) ->
                {
                    return finalClzTarget.cast(i);
                };
            }
        }
        else
        {
            return (i) ->
            {
                long o = (long) i;

                return finalClzTarget.cast(tdTarget.fromLongValue(o));
            };
        }
    }

    @SuppressWarnings("unchecked")
    public static <O> Function<Object, O> buildNumberCoercer(Class<?> clzSource,
                                                             Class<O> clzTarget)
    {
        clzTarget = getBoxedType(clzTarget);

        if (clzTarget.isAssignableFrom(clzSource))
        {
            return clzTarget::cast;
        }

        TypeDescriptor tdSource = Reflection.getDescriptor(clzSource);
        TypeDescriptor tdTarget = Reflection.getDescriptor(clzTarget);

        if (tdSource == null || tdTarget == null)
        {
            return clzTarget::cast;
        }

        @SuppressWarnings("unchecked") Class<O> boxedType = (Class<O>) tdTarget.getBoxedType();
        if (boxedType != null)
        {
            clzTarget = boxedType;
        }

        Class<O> finalClzTarget = clzTarget;

        if (tdTarget.isFloatingType())
        {
            //
            // Don't use ?: syntax, it would convert floats to doubles!!
            //
            if (tdTarget.size == 32)
            {
                if (Number.class.isAssignableFrom(clzSource))
                {
                    return (i) ->
                    {
                        if (i == null)
                        {
                            return null;
                        }

                        Number num = (Number) i;

                        return finalClzTarget.cast(num.floatValue());
                    };
                }
                else
                {
                    return (i) ->
                    {
                        if (i == null)
                        {
                            return null;
                        }

                        Number num = tdSource.asLongValue(i);

                        return finalClzTarget.cast(num.floatValue());
                    };
                }
            }
            else
            {
                if (Number.class.isAssignableFrom(clzSource))
                {
                    return (i) ->
                    {
                        if (i == null)
                        {
                            return null;
                        }

                        Number num = (Number) i;

                        return finalClzTarget.cast(num.doubleValue());
                    };
                }
                else
                {
                    return (i) ->
                    {
                        if (i == null)
                        {
                            return null;
                        }

                        Number num = tdSource.asLongValue(i);

                        return finalClzTarget.cast(num.doubleValue());
                    };
                }
            }
        }
        else
        {
            long v;

            if (tdSource.isFloatingType())
            {
                return (i) ->
                {
                    if (i == null)
                    {
                        return null;
                    }

                    Number num = (Number) i;

                    long o = (long) num.doubleValue();

                    return finalClzTarget.cast(tdTarget.fromLongValue(o));
                };
            }
            else
            {
                return (i) ->
                {
                    if (i == null)
                    {
                        return null;
                    }

                    long o = tdSource.asLongValue(i);

                    return finalClzTarget.cast(tdTarget.fromLongValue(o));
                };
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T coerceNumber(Object value,
                                     Class<T> clzTarget)
    {
        if (value == null)
        {
            return null;
        }

        clzTarget = getBoxedType(clzTarget);

        Class<?> clzSource = value.getClass();

        if (!clzTarget.isAssignableFrom(clzSource))
        {
            TypeDescriptor tdSource = Reflection.getDescriptor(clzSource);
            TypeDescriptor tdTarget = Reflection.getDescriptor(clzTarget);

            if (tdSource != null && tdTarget != null)
            {
                if (tdTarget.isFloatingType())
                {
                    if (!(value instanceof Number))
                    {
                        value = tdSource.asLongValue(value);
                    }

                    Number num = (Number) value;

                    //
                    // Don't use ?: syntax, it would convert floats to doubles!!
                    //
                    if (tdTarget.size == 32)
                    {
                        value = num.floatValue();
                    }
                    else
                    {
                        value = num.doubleValue();
                    }
                }
                else
                {
                    long v;

                    if (tdSource.isFloatingType())
                    {
                        Number num = (Number) value;

                        v = (long) num.doubleValue();
                    }
                    else
                    {
                        v = tdSource.asLongValue(value);
                    }

                    value = tdTarget.fromLongValue(v);
                }

                @SuppressWarnings("unchecked") Class<T> boxedType = (Class<T>) tdTarget.getBoxedType();
                if (boxedType != null)
                {
                    clzTarget = boxedType;
                }
            }
        }

        return clzTarget.cast(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getBoxedType(Class<T> clz)
    {
        if (clz.isPrimitive())
        {
            TypeDescriptor td = Reflection.getDescriptor(clz);
            return (Class<T>) td.getBoxedType();
        }

        return clz;
    }

    //--//

    public static Map<String, Field> collectFields(Class<?> clz)
    {
        Map<String, Field> res = s_classToFields.get(clz);
        if (res != null)
        {
            return res;
        }

        res = Maps.newHashMap();

        for (Class<?> clz2 = clz; clz2 != null; clz2 = clz2.getSuperclass())
        {
            for (Field f : clz2.getDeclaredFields())
            {
                res.put(f.getName(), f);
            }
        }

        res = Collections.unmodifiableMap(res);

        Map<String, Field> oldRes = s_classToFields.putIfAbsent(clz, res);
        return oldRes != null ? oldRes : res;
    }

    public static Multimap<String, Method> collectMethods(Class<?> clz)
    {
        Multimap<String, Method> res = s_classToMethods.get(clz);
        if (res != null)
        {
            return res;
        }

        ImmutableListMultimap.Builder<String, Method> builder = ImmutableListMultimap.builder();

        for (Class<?> clz2 = clz; clz2 != null; clz2 = clz2.getSuperclass())
        {
            for (Method md : clz2.getDeclaredMethods())
            {
                builder.put(md.getName(), md);
            }
        }

        res = builder.build();

        Multimap<String, Method> oldRes = s_classToMethods.putIfAbsent(clz, res);
        return oldRes != null ? oldRes : res;
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<Class<? extends T>> collectJsonSubTypes(Class<T> clz)
    {
        Set<Class<?>>           setUntyped = s_classToJsonSubTypes.get(clz);
        Set<Class<? extends T>> setTyped   = (Set<Class<? extends T>>) (Set<?>) setUntyped;
        if (setTyped != null)
        {
            return setTyped;
        }

        setTyped = Sets.newHashSet();
        collectJsonSubTypes(setTyped, clz, clz);

        setTyped = Collections.unmodifiableSet(setTyped);
        setUntyped = (Set<Class<?>>) (Set<?>) setTyped;

        Set<Class<?>> oldSetUntyped = s_classToJsonSubTypes.putIfAbsent(clz, setUntyped);
        if (oldSetUntyped != null)
        {
            return (Set<Class<? extends T>>) (Set<?>) oldSetUntyped;
        }

        return setTyped;
    }

    @SuppressWarnings("unchecked")
    private static <T> void collectJsonSubTypes(Set<Class<? extends T>> set,
                                                Class<? extends T> clz,
                                                Class<T> clzRoot)
    {
        if (set.contains(clz))
        {
            return;
        }

        if (!isAbstractClass(clz))
        {
            set.add(clz);
        }

        JsonSubTypes anno = clz.getAnnotation(JsonSubTypes.class);
        if (anno != null)
        {
            for (JsonSubTypes.Type type : anno.value())
            {
                Class<?> subClz = type.value();

                if (!Reflection.isSubclassOf(clzRoot, subClz))
                {
                    throw Exceptions.newIllegalArgumentException("Type '%s' is not a subtype of '%s', check @JsonSubTypes annotations", subClz, clzRoot);
                }

                collectJsonSubTypes(set, (Class<? extends T>) subClz, clzRoot);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> List<T> collectEnumValues(Class<T> clz)
    {
        List<T> res = (List<T>) s_classToEnumValues.get(clz);
        if (res == null)
        {
            res = CollectionUtils.fromArray(clz, clz.getEnumConstants());

            s_classToEnumValues.put(clz, (List<Enum<?>>) res);
        }

        return res;
    }

    //--//

    public static Optional<Object> dereference(Object src,
                                               String... path)
    {
        try
        {
            for (String fieldName : path)
            {
                if (src == null)
                {
                    return Optional.empty();
                }

                Field f = findField(src.getClass(), fieldName);
                if (f == null)
                {
                    return Optional.empty();
                }

                f.setAccessible(true);
                src = f.get(src);
            }

            return Optional.of(src);
        }
        catch (Throwable t)
        {
            return Optional.empty();
        }
    }

    public static Field findField(Class<?> clz,
                                  String name)
    {
        return collectFields(clz).get(name);
    }

    public static Method findGetter(Class<?> clz,
                                    String name)
    {
        ConcurrentMap<String, Method> map     = s_propertyToGetter.computeIfAbsent(clz, (key) -> Maps.newConcurrentMap());
        Method                        mCached = map.get(name);
        if (mCached != null)
        {
            return mCached;
        }

        Method mGet = null;
        Method mIs  = null;
        Method mHas = null;

        for (Method m : collectMethods(clz).values())
        {
            if (m.getParameterCount() != 0)
            {
                continue;
            }

            if (matchAccessorName(name, m.getName(), "get"))
            {
                mGet = m;
                continue;
            }

            if (matchAccessorName(name, m.getName(), "is"))
            {
                mIs = m;
                continue;
            }

            if (matchAccessorName(name, m.getName(), "has"))
            {
                mHas = m;
                continue;
            }
        }

        //
        // Make sure we give 'get' priority over 'is', and 'is priority over 'has'.
        //
        if (mGet != null)
        {
            map.putIfAbsent(name, mGet);
            return mGet;
        }

        if (mIs != null)
        {
            map.putIfAbsent(name, mIs);
            return mIs;
        }

        if (mHas != null)
        {
            map.putIfAbsent(name, mHas);
            return mHas;
        }

        return null;
    }

    public static Method findSetter(Class<?> clz,
                                    String name,
                                    Class<?> valueType)
    {
        ConcurrentMap<String, ConcurrentMap<Class<?>, Method>> map                = s_propertyToSetter.computeIfAbsent(clz, (key) -> Maps.newConcurrentMap());
        ConcurrentMap<Class<?>, Method>                        map2               = map.computeIfAbsent(name, (key) -> Maps.newConcurrentMap());
        Class<?>                                               effectiveValueType = valueType != null ? valueType : Void.class; // Concurrent map doesn't like nulls.
        Method                                                 mCached            = map2.get(effectiveValueType);
        if (mCached != null)
        {
            return mCached;
        }

        for (Method m : collectMethods(clz).values())
        {
            if (m.getParameterCount() != 1)
            {
                continue;
            }

            if (valueType != null)
            {
                Class<?> methodArg = m.getParameterTypes()[0];
                if (!methodArg.isAssignableFrom(valueType))
                {
                    continue;
                }
            }

            if (matchAccessorName(name, m.getName(), "set"))
            {
                map2.putIfAbsent(effectiveValueType, m);
                return m;
            }
        }

        return null;
    }

    private static boolean matchAccessorName(String target,
                                             String methodName,
                                             String prefix)
    {
        int prefixLen = prefix.length();
        int len       = target.length();

        if (methodName.length() != len + prefixLen)
        {
            return false;
        }

        if (!methodName.startsWith(prefix))
        {
            return false;
        }

        for (int pos = 0; pos < len; pos++)
        {
            char targetC = target.charAt(pos);
            char methodC = methodName.charAt(pos + prefixLen);

            if (pos == 0)
            {
                targetC = Character.toLowerCase(targetC);
                methodC = Character.toLowerCase(methodC);
            }

            if (targetC != methodC)
            {
                return false;
            }
        }

        return true;
    }

    //--//

    public static Type resolveGenericType(Type context,
                                          Type type)
    {
        if (type == null)
        {
            return null;
        }

        if (type instanceof Class<?>)
        {
            return type;
        }

        if (type instanceof ParameterizedType)
        {
            return type;
        }

        if (type instanceof TypeVariable<?>)
        {
            TypeVariable<?> type2 = (TypeVariable<?>) type;

            Object genDecl = type2.getGenericDeclaration();
            if (genDecl instanceof Class<?>)
            {
                Class<?> genDecl2 = (Class<?>) genDecl;

                TypeVariable<?>[] typeParams = genDecl2.getTypeParameters();
                for (int i = 0; i < typeParams.length; i++)
                {
                    if (typeParams[i].equals(type2))
                    {
                        if (context instanceof ParameterizedType)
                        {
                            ParameterizedType parameterizedType = (ParameterizedType) context;

                            Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
                            if (i < actualTypeArgs.length)
                            {
                                return actualTypeArgs[i];
                            }
                        }

                        if (context instanceof JavaType)
                        {
                            JavaType jsonType = (JavaType) context;

                            JavaType jsonTypeParam = jsonType.getBindings()
                                                             .getBoundType(i);
                            if (jsonTypeParam != null)
                            {
                                return jsonTypeParam;
                            }
                        }
                    }
                }
            }

            if (genDecl instanceof Method)
            {
                Method genDecl2 = (Method) genDecl;

                TypeVariable<?>[] typeParams = genDecl2.getTypeParameters();
                for (int i = 0; i < typeParams.length; i++)
                {
                    if (typeParams[i].equals(type2))
                    {
                        if (context instanceof ParameterizedType)
                        {
                            ParameterizedType parameterizedType = (ParameterizedType) context;

                            Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
                            if (i < actualTypeArgs.length)
                            {
                                return actualTypeArgs[i];
                            }
                        }

                        if (context instanceof JavaType)
                        {
                            JavaType jsonType = (JavaType) context;

                            JavaType jsonTypeParam = jsonType.getBindings()
                                                             .getBoundType(i);
                            if (jsonTypeParam != null)
                            {
                                return jsonTypeParam;
                            }
                        }
                    }
                }
            }

            Type[] bounds = type2.getBounds();
            if (bounds == null)
            {
                return Object.class;
            }

            if (bounds.length == 1)
            {
                return bounds[0];
            }
        }

        if (type instanceof JavaType)
        {
            return type;
        }

        if (context != null)
        {
            throw Exceptions.newRuntimeException("Can't determine real type of %s [in context: %s]", type, context);
        }
        else
        {
            throw Exceptions.newRuntimeException("Can't determine real type of %s", type);
        }
    }

    public static <T> Class<T> getRawType(Type context,
                                          Type type)
    {
        if (type == null)
        {
            return null;
        }

        type = resolveGenericType(context, type);

        if (type instanceof Class<?>)
        {
            @SuppressWarnings("unchecked") Class<T> result = (Class<T>) type;
            return result;
        }

        if (type instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            @SuppressWarnings("unchecked") Class<T> result = (Class<T>) parameterizedType.getRawType();
            return result;
        }

        if (type instanceof JavaType)
        {
            JavaType jsonType = (JavaType) type;

            @SuppressWarnings("unchecked") Class<T> result = (Class<T>) jsonType.getRawClass();
            return result;
        }

        if (context != null)
        {
            throw Exceptions.newRuntimeException("Can't determine real type of %s [in context: %s]", type, context);
        }
        else
        {
            throw Exceptions.newRuntimeException("Can't determine real type of %s", type);
        }
    }

    public static <T> Class<T> getRawType(Type type)
    {
        return getRawType(null, type);
    }

    public static Type getTypeArgument(Type type,
                                       int i)
    {
        Type typeArg = getTypeArgumentOrNull(type, i);
        if (typeArg == null)
        {
            throw Exceptions.newRuntimeException("Can't get type argument %d for type '%s'", i, type);
        }

        return typeArg;
    }

    public static Type getTypeArgumentOrNull(Type type,
                                             int i)
    {
        if (type instanceof ParameterizedType)
        {
            ParameterizedType paramType = (ParameterizedType) type;
            Type[]            args      = paramType.getActualTypeArguments();

            if (args != null && i >= 0 && i < args.length)
            {
                return args[i];
            }
        }

        if (type instanceof JavaType)
        {
            JavaType jsonType = (JavaType) type;

            return jsonType.getBindings()
                           .getBoundType(i);
        }

        return null;
    }

    public static <T> Class<T> searchTypeArgument(Type target,
                                                  Object instance,
                                                  int argIndex)
    {
        return searchTypeArgument(target, instance.getClass(), argIndex);
    }

    public static <T> Class<T> searchTypeArgument(Type target,
                                                  Class<?> input,
                                                  int argIndex)
    {
        return getRawType(searchTypeArgument(target, input, argIndex, null));
    }

    private static Class<?> searchTypeArgument(Type target,
                                               Class<?> clz,
                                               int argIndex,
                                               Class<?>[] argsActual)
    {
        if (clz == null || clz == target)
        {
            if (argsActual == null || argIndex >= argsActual.length)
            {
                return null;
            }

            return argsActual[argIndex];
        }

        Type superClass = clz.getGenericSuperclass();
        if (superClass instanceof ParameterizedType)
        {
            ParameterizedType superType        = (ParameterizedType) superClass;
            Type[]            superArgsGeneric = superType.getActualTypeArguments();
            Class<?>[]        superArgsActual  = new Class<?>[superArgsGeneric.length];

            for (int superArgIndex = 0; superArgIndex < superArgsGeneric.length; superArgIndex++)
            {
                Type arg = superArgsGeneric[superArgIndex];
                if (arg instanceof Class<?>)
                {
                    Class<?> argClz = (Class<?>) arg;
                    superArgsActual[superArgIndex] = argClz;
                }
                else if (arg instanceof TypeVariable<?>)
                {
                    //
                    // This is the case of a generic class extending another generic class and using one of its Type Arguments as the parameter for the extension.
                    // Match the definition with the use to find the actual class.
                    //
                    TypeVariable<?>[] argsGenericDef = clz.getTypeParameters();
                    for (int thisArgIndex = 0; thisArgIndex < argsGenericDef.length; thisArgIndex++)
                    {
                        if (argsGenericDef[thisArgIndex] == arg)
                        {
                            superArgsActual[superArgIndex] = argsActual[thisArgIndex];
                            break;
                        }
                    }
                }
            }

            argsActual = superArgsActual;
        }
        else
        {
            argsActual = null;
        }

        return searchTypeArgument(target, clz.getSuperclass(), argIndex, argsActual);
    }

    //--//

    public static Method findMethodOfFunctionalInterface(Type itf)
    {
        Class<?> clz = Reflection.getRawType(itf);
        if (clz.isAnnotationPresent(FunctionalInterface.class))
        {
            for (Method m : collectMethods(clz).values())
            {
                if (isAbstractMethod(m))
                {
                    return m;
                }
            }
        }

        return null;
    }

    //--//

    @SuppressWarnings("unchecked")
    public static <T> T as(Object val,
                           Class<T> clz)
    {
        return clz.isInstance(val) ? (T) val : null;
    }

    public static <T> T newInstance(Type type)
    {
        return newInstance(getRawType(type));
    }

    public static <T> T newInstance(Class<T> clz)
    {
        try
        {
            Constructor<T> constr = clz.getConstructor(c_emptyArgTypes);
            return constr.newInstance(c_emptyArgs);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static <T> T newInstance(Class<T> clz,
                                    Object... args)
    {
        try
        {
            Class<?>[] argClz = new Class<?>[args.length];
            for (int i = 0; i < argClz.length; i++)
            {
                Object arg = args[i];
                argClz[i] = arg != null ? arg.getClass() : Object.class;
            }

            Constructor<T> constr = clz.getConstructor(argClz);
            return constr.newInstance(args);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static boolean isSubclassOf(Class<?> targetType,
                                       Type typeToCheck)
    {
        return targetType.isAssignableFrom(getRawType(typeToCheck));
    }

    //--//

    public static boolean isAbstractClass(Class<?> clz)
    {
        return Modifier.isAbstract(clz.getModifiers());
    }

    public static boolean isAbstractMethod(Method m)
    {
        return Modifier.isAbstract(m.getModifiers());
    }

    public static boolean isStaticMethod(Method m)
    {
        return Modifier.isStatic(m.getModifiers());
    }

    public static boolean isMethodReturningAPromise(Method m)
    {
        return m != null && Reflection.canAssignTo(CompletableFuture.class, m.getGenericReturnType());
    }

    public static boolean isMethodReturningAPromise(Type type,
                                                    Method m)
    {
        if (m != null)
        {
            Type returnType = m.getGenericReturnType();

            Class<?> returnClz = getRawType(type, returnType);
            if (returnClz != null)
            {
                return Reflection.canAssignTo(CompletableFuture.class, returnClz);
            }
        }

        return false;
    }
}
