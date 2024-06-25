/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.metamodel.StaticMetamodel;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3AutoTrim;
import com.optio3.cloud.annotation.Optio3DontMap;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.annotation.Optio3MapToPersistence;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.TypedRecordIdentityMap;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;

/**
 * An helper class for converting from Hibernate entities to REST models and back.
 */
public final class ModelMapper
{
    // A sentinel we use to detect when to skip field writes.
    private final static Object s_skipMarker = new Object();

    public static <T> Class<T> getModelClass(Class<?> entity)
    {
        Optio3TableInfo anno = entity.getAnnotation(Optio3TableInfo.class);

        if (!anno.metamodel()
                 .isAnnotationPresent(StaticMetamodel.class))
        {
            throw Exceptions.newRuntimeException("Entity '%s' did not declare a correct metamodel: %s", entity, anno.metamodel());
        }

        @SuppressWarnings("unchecked") Class<T> clz = (Class<T>) anno.model();

        return clz;
    }

    public static <T extends ModelMapperTarget<?, ?>> void validateModel(Class<T> entityClass)
    {
        try
        {
            validateModel(entityClass, getModelClass(entityClass));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void validateModel(Class<?> entityClass,
                                      Class<?> modelClass) throws
                                                           IllegalAccessException,
                                                           IllegalArgumentException,
                                                           InvocationTargetException
    {
        // Make sure fields are not duplicated in type hierarchy.
        {
            Map<String, Class<?>> lookupDefinition = Maps.newHashMap();

            for (Class<?> clz2 = modelClass; clz2 != null; clz2 = clz2.getSuperclass())
            {
                for (Field f : clz2.getDeclaredFields())
                {
                    Class<?> clzPrevious = lookupDefinition.put(f.getName(), clz2);
                    if (clzPrevious != null)
                    {
                        throw Exceptions.newRuntimeException("Field '%s' declared multiple times: %s and %s", f.getName(), clz2.getSimpleName(), clzPrevious.getSimpleName());
                    }
                }
            }
        }

        for (Field f : Reflection.collectFields(modelClass)
                                 .values())
        {
            if (shouldSkipField(f))
            {
                continue;
            }

            Optio3MapToPersistence anno = f.getAnnotation(Optio3MapToPersistence.class);

            String prop = fixupPropertyName(f, anno);

            Method getter = Reflection.findGetter(entityClass, prop);
            if (getter == null)
            {
                throw Exceptions.newRuntimeException("Cannot find a property %s in %s to match model field %s", prop, entityClass, f);
            }

            validateValueToModel(getter.getGenericReturnType(), f.getGenericType(), getter, f);
        }
    }

    private static void validateValueToModel(Type entityType,
                                             Type modelType,
                                             Method contextEntity,
                                             Field contextModel) throws
                                                                 IllegalAccessException,
                                                                 IllegalArgumentException,
                                                                 InvocationTargetException
    {
        if (entityType == modelType)
        {
            return;
        }

        Class<?> entityClass = Reflection.getRawType(entityType);
        Class<?> modelClass  = Reflection.getRawType(modelType);

        if (entityClass.isInterface())
        {
            // Interfaces don't have fields.
            return;
        }

        boolean isEntitySimple = isSimpleType(entityClass);
        boolean isModelSimple  = isSimpleType(modelClass);
        if (isEntitySimple && isModelSimple)
        {
            if (entityClass != modelClass)
            {
                throw Exceptions.newRuntimeException("Cannot assign entity property %s to model field %s, incompatible types", contextEntity, contextModel);
            }

            return;
        }

        if (modelClass == String.class)
        {
            if (entityClass == EncryptedPayload.class)
            {
                // Special case.
                return;
            }

            throw Exceptions.newRuntimeException("Cannot assign entity property %s to model field %s, incompatible types", contextEntity, contextModel);
        }

        if (modelClass == TypedRecordIdentity.class)
        {
            Type     modelTypeSub  = Reflection.getTypeArgument(modelType, 0);
            Class<?> modelClassSub = Reflection.getRawType(modelTypeSub);

            if (modelClassSub != entityClass)
            {
                throw Exceptions.newRuntimeException("Can't assign entity property %s to model field %s, incompatible types", contextEntity, contextModel);
            }
            return;
        }

        if (Reflection.isSubclassOf(TypedRecordIdentityList.class, modelType))
        {
            if (!(Reflection.isSubclassOf(List.class, entityType) || Reflection.isSubclassOf(Set.class, entityType)))
            {
                throw Exceptions.newRuntimeException("Can't assign entity property %s to model field %s, incompatible collections", contextEntity, contextModel);
            }

            Type entityTypeSub = Reflection.getTypeArgument(entityType, 0);
            Type modelTypeSub  = Reflection.getTypeArgument(modelType, 0);

            if (entityTypeSub != modelTypeSub)
            {
                throw Exceptions.newRuntimeException("Can't assign entity property %s to model field %s, incompatible collections", contextEntity, contextModel);
            }
            return;
        }

        if (Reflection.isSubclassOf(List.class, modelType))
        {
            if (!(Reflection.isSubclassOf(List.class, entityType) || Reflection.isSubclassOf(Set.class, entityType)))
            {
                throw Exceptions.newRuntimeException("Can't assign entity property %s to model field %s, incompatible collections", contextEntity, contextModel);
            }

            Type entityTypeSub = Reflection.getTypeArgument(entityType, 0);
            Type modelTypeSub  = Reflection.getTypeArgument(modelType, 0);

            validateValueToModel(entityTypeSub, modelTypeSub, contextEntity, contextModel);
            return;
        }

        if (Reflection.isSubclassOf(Map.class, modelType))
        {
            if (!Reflection.isSubclassOf(Map.class, entityType))
            {
                throw Exceptions.newRuntimeException("Can't assign entity property %s to model field %s, incompatible collections", contextEntity, contextModel);
            }

            Type entityTypeKey   = Reflection.getTypeArgument(entityType, 0);
            Type entityTypeValue = Reflection.getTypeArgument(entityType, 1);

            Type modelTypeKey   = Reflection.getTypeArgument(modelType, 0);
            Type modelTypeValue = Reflection.getTypeArgument(modelType, 1);

            validateValueToModel(entityTypeKey, modelTypeKey, contextEntity, contextModel);
            validateValueToModel(entityTypeValue, modelTypeValue, contextEntity, contextModel);
            return;
        }

        if (modelClass == RecordIdentity.class)
        {
            if (entityClass != RecordIdentity.class)
            {
                throw Exceptions.newRuntimeException("Cannot assign entity property %s to model field %s, incompatible types", contextEntity, contextModel);
            }

            return;
        }

        validateModel(entityClass, modelClass);
    }

    //--//

    public static <R extends ModelMapperTarget<T, ?>, T> List<T> toModels(SessionHolder sessionHolder,
                                                                          ModelMapperPolicy policy,
                                                                          List<R> entities)
    {
        return CollectionUtils.transformToList(entities, (rec) -> toModel(sessionHolder, policy, rec));
    }

    public static <R extends ModelMapperTarget<T, ?>, T> T toModel(SessionProvider sessionProvider,
                                                                   ModelMapperPolicy policy,
                                                                   Class<R> entityClass,
                                                                   Serializable id)
    {
        if (id == null)
        {
            return null;
        }

        try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
        {
            R rec = sessionHolder.getEntity(entityClass, id);

            return ModelMapper.toModel(sessionHolder, policy, rec);
        }
    }

    public static <R extends ModelMapperTarget<T, ?>, T> T toModel(SessionHolder sessionHolder,
                                                                   ModelMapperPolicy policy,
                                                                   R entity)
    {
        try
        {
            if (entity == null)
            {
                return null;
            }

            Class<T> modelClass = ModelMapper.getModelClass(entity.getClass());
            T        model      = Reflection.newInstance(modelClass);

            copyToModel(policy, entity, model);

            return entity.toModelOverride(sessionHolder, policy, model);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void copyToModel(ModelMapperPolicy policy,
                                    Object entity,
                                    Object model) throws
                                                  Exception
    {
        entity = SessionHolder.unwrapProxy(entity);

        Class<?> entityClass = entity.getClass();

        for (Field f : Reflection.collectFields(model.getClass())
                                 .values())
        {
            if (shouldSkipField(f))
            {
                continue;
            }

            Optio3MapToPersistence anno = f.getAnnotation(Optio3MapToPersistence.class);

            String prop = fixupPropertyName(f, anno);

            Method getter = Reflection.findGetter(entityClass, prop);
            if (getter == null)
            {
                throw Exceptions.newRuntimeException("Cannot find a property %s in %s to match model field %s", prop, entityClass, f);
            }

            if (policy.canReadField(f))
            {
                Object val = getter.invoke(entity);

                Object valConverted = convertValueToModel(policy, val, f.getGenericType(), getter, f);

                if (Modifier.isFinal(f.getModifiers()))
                {
                    {
                        @SuppressWarnings("rawtypes") Collection dst = Reflection.as(f.get(model), Collection.class);
                        if (dst != null)
                        {
                            dst.clear();

                            @SuppressWarnings("rawtypes") Collection src = Reflection.as(val, Collection.class);
                            if (src != null)
                            {
                                dst.addAll(src);
                            }

                            continue;
                        }
                    }

                    {
                        @SuppressWarnings("rawtypes") Map dst = Reflection.as(f.get(model), Map.class);
                        if (dst != null)
                        {
                            dst.clear();

                            @SuppressWarnings("rawtypes") Map src = Reflection.as(val, Map.class);
                            if (src != null)
                            {
                                dst.putAll(src);
                            }

                            continue;
                        }
                    }
                }

                f.set(model, valConverted);
            }
        }
    }

    private static Object convertValueToModel(ModelMapperPolicy policy,
                                              Object entityValue,
                                              Type modelType,
                                              Method contextEntity,
                                              Field contextModel) throws
                                                                  Exception
    {
        if (entityValue == null)
        {
            return null;
        }

        Type entityType = entityValue.getClass();
        if (entityType == modelType)
        {
            return entityValue;
        }

        Class<?> entityClass = Reflection.getRawType(entityType);
        Class<?> modelClass  = Reflection.getRawType(modelType);

        boolean isEntitySimple = isSimpleType(entityClass);
        boolean isModelSimple  = isSimpleType(modelClass);
        if (isEntitySimple && isModelSimple)
        {
            return Reflection.coerceNumber(entityValue, modelClass);
        }

        if (modelClass == String.class)
        {
            if (entityClass == EncryptedPayload.class)
            {
                return policy.decryptField(contextModel, (EncryptedPayload) entityValue);
            }

            throw Exceptions.newRuntimeException("Cannot assign entity property %s to model field %s, incompatible collections", contextEntity, contextModel);
        }

        if (modelClass == TypedRecordIdentity.class)
        {
            if (!Reflection.isSubclassOf(RecordWithCommonFields.class, entityType))
            {
                throw Exceptions.newRuntimeException("Cannot assign entity property %s to model field %s, incompatible types", contextEntity, contextModel);
            }

            RecordWithCommonFields rec = (RecordWithCommonFields) entityValue;
            return RecordIdentity.newTypedInstance(rec);
        }

        if (Reflection.isSubclassOf(TypedRecordIdentityList.class, modelType))
        {
            if (Reflection.isSubclassOf(List.class, entityType))
            {
                TypedRecordIdentityList<RecordWithCommonFields> modelList = new TypedRecordIdentityList<>();

                List<?> entityList = (List<?>) entityValue;
                for (Object entitySub : entityList)
                {
                    RecordWithCommonFields rec = (RecordWithCommonFields) entitySub;
                    modelList.add(RecordIdentity.newTypedInstance(rec));
                }

                return modelList;
            }

            // Because there's no Set in Javascript, we map sets to lists.
            if (Reflection.isSubclassOf(Set.class, entityType))
            {
                TypedRecordIdentityList<RecordWithCommonFields> modelList = new TypedRecordIdentityList<>();

                Set<?> entitySet = (Set<?>) entityValue;
                for (Object entitySub : entitySet)
                {
                    RecordWithCommonFields rec = (RecordWithCommonFields) entitySub;
                    modelList.add(RecordIdentity.newTypedInstance(rec));
                }

                return modelList;
            }

            throw Exceptions.newRuntimeException("Can't assign entity property %s to model field %s, incompatible collections", contextEntity, contextModel);
        }

        if (Reflection.isSubclassOf(List.class, modelType))
        {
            if (Reflection.isSubclassOf(List.class, entityType))
            {
                List<Object> modelList    = new ArrayList<>();
                Type         modelTypeSub = Reflection.getTypeArgument(modelType, 0);

                List<?> entityList = (List<?>) entityValue;
                for (Object entitySub : entityList)
                {
                    Object modelSub = convertValueToModel(policy, entitySub, modelTypeSub, contextEntity, contextModel);
                    modelList.add(modelSub);
                }

                return modelList;
            }

            // Because there's no Set in Javascript, we map sets to lists.
            if (Reflection.isSubclassOf(Set.class, entityType))
            {
                List<Object> modelList    = new ArrayList<>();
                Type         modelTypeSub = Reflection.getTypeArgument(modelType, 0);

                Set<?> entitySet = (Set<?>) entityValue;
                for (Object entitySub : entitySet)
                {
                    Object modelSub = convertValueToModel(policy, entitySub, modelTypeSub, contextEntity, contextModel);
                    modelList.add(modelSub);
                }

                return modelList;
            }

            throw Exceptions.newRuntimeException("Can't assign entity property %s to model field %s, incompatible collections", contextEntity, contextModel);
        }

        if (Reflection.isSubclassOf(TypedRecordIdentityMap.class, modelType))
        {
            if (Reflection.isSubclassOf(Map.class, entityType))
            {
                TypedRecordIdentityMap<Object, ? extends RecordWithCommonFields> modelMap     = new TypedRecordIdentityMap<>();
                Type                                                             modelTypeKey = Reflection.getTypeArgument(modelType, 0);

                Map<?, ?> entityMap = (Map<?, ?>) entityValue;
                for (Object entityKey : entityMap.keySet())
                {
                    RecordWithCommonFields entitySub = (RecordWithCommonFields) entityMap.get(entityKey);

                    Object modelKey = convertValueToModel(policy, entityKey, modelTypeKey, contextEntity, contextModel);
                    modelMap.put(modelKey, RecordIdentity.newTypedInstance(entitySub));
                }

                return modelMap;
            }

            throw Exceptions.newRuntimeException("Can't assign entity property %s to model field %s, incompatible collections", contextEntity, contextModel);
        }

        if (Reflection.isSubclassOf(Map.class, modelType))
        {
            if (!Reflection.isSubclassOf(Map.class, entityType))
            {
                throw Exceptions.newRuntimeException("Can't assign entity property %s to model field %s, incompatible collections", contextEntity, contextModel);
            }

            Map<Object, Object> modelMap       = Maps.newHashMap();
            Type                modelTypeKey   = Reflection.getTypeArgument(modelType, 0);
            Type                modelTypeValue = Reflection.getTypeArgument(modelType, 1);

            Map<?, ?> entityMap = (Map<?, ?>) entityValue;
            for (Object entityKey : entityMap.keySet())
            {
                Object entitySub = entityMap.get(entityKey);

                Object modelKey = convertValueToModel(policy, entityKey, modelTypeKey, contextEntity, contextModel);
                Object modelSub = convertValueToModel(policy, entitySub, modelTypeValue, contextEntity, contextModel);

                modelMap.put(modelKey, modelSub);
            }

            return modelMap;
        }

        if (Reflection.isSubclassOf(Set.class, modelType))
        {
            if (!Reflection.isSubclassOf(Set.class, entityType))
            {
                throw Exceptions.newRuntimeException("Can't assign entity property %s to model field %s, incompatible collections", contextEntity, contextModel);
            }

            Set<Object> modelSet     = Sets.newHashSet();
            Type        modelTypeSub = Reflection.getTypeArgument(modelType, 0);

            Set<?> entitySet = (Set<?>) entityValue;
            for (Object entitySub : entitySet)
            {
                Object modelSub = convertValueToModel(policy, entitySub, modelTypeSub, contextEntity, contextModel);
                modelSet.add(modelSub);
            }

            return modelSet;
        }

        if (Reflection.isSubclassOf(modelClass, entityClass))
        {
            return entityValue;
        }

        Object modelSub = Reflection.newInstance(modelType);

        copyToModel(policy, entityValue, modelSub);

        return modelSub;
    }

    //--//

    public static void trimModel(Object model)
    {
        for (Field f : Reflection.collectFields(model.getClass())
                                 .values())
        {
            if (f.getAnnotation(Optio3AutoTrim.class) != null)
            {
                try
                {
                    String val = Reflection.as(f.get(model), String.class);
                    if (val != null)
                    {
                        f.set(model, StringUtils.trimToNull(val));
                    }
                }
                catch (IllegalAccessException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static <R extends ModelMapperTarget<T, ?>, T> void fromModel(SessionHolder sessionHolder,
                                                                        ModelMapperPolicy policy,
                                                                        T model,
                                                                        R entity)
    {
        try
        {
            entity.fromModelOverride(sessionHolder, policy, model);

            copyFromModel(sessionHolder, model, policy, entity);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void copyFromModel(SessionHolder sessionHolder,
                                      Object model,
                                      ModelMapperPolicy policy,
                                      Object entity) throws
                                                     Exception
    {
        entity = SessionHolder.unwrapProxy(entity);

        Class<?> entityClass = entity.getClass();

        for (Field f : Reflection.collectFields(model.getClass())
                                 .values())
        {
            if (shouldSkipField(f))
            {
                continue;
            }

            if (f.getAnnotation(Optio3MapAsReadOnly.class) != null)
            {
                if (!policy.canOverrideReadOnlyField(f))
                {
                    continue;
                }
            }

            if (policy.canWriteField(f))
            {
                Optio3MapToPersistence anno = f.getAnnotation(Optio3MapToPersistence.class);
                String                 prop = fixupPropertyName(f, anno);
                Object                 val  = f.get(model);

                if (val instanceof String)
                {
                    if (f.getAnnotation(Optio3AutoTrim.class) != null)
                    {
                        val = StringUtils.trimToNull((String) val);
                    }
                }

                if (anno != null && anno.useGetterForUpdate())
                {
                    Method getter = Reflection.findGetter(entityClass, prop);
                    if (getter == null)
                    {
                        throw Exceptions.newRuntimeException("Cannot find a property %s in %s to match model field %s", prop, entityClass, f);
                    }

                    Object valConverted = convertValueFromModel(sessionHolder, policy, val, getter.getGenericReturnType(), getter, f);
                    if (valConverted == s_skipMarker)
                    {
                        continue;
                    }

                    Object   coll      = getter.invoke(entity);
                    Class<?> collClass = coll.getClass();

                    if (Reflection.isSubclassOf(List.class, collClass))
                    {
                        @SuppressWarnings("unchecked") List<Object> list = (List<Object>) coll;

                        list.clear();

                        list.addAll((List<?>) valConverted);
                    }
                    else if (Reflection.isSubclassOf(Map.class, collClass))
                    {
                        @SuppressWarnings("unchecked") Map<Object, Object> map = (Map<Object, Object>) coll;

                        map.clear();

                        map.putAll((Map<?, ?>) valConverted);
                    }
                    else if (Reflection.isSubclassOf(Set.class, collClass))
                    {
                        @SuppressWarnings("unchecked") Set<Object> set = (Set<Object>) coll;

                        set.clear();

                        set.addAll((Set<?>) valConverted);
                    }
                    else
                    {
                        throw Exceptions.newRuntimeException("Unsupported collection type %s in %s to match model field %s", collClass, entityClass, f);
                    }
                }
                else
                {
                    Method setter = Reflection.findSetter(entityClass, prop, null);
                    if (setter == null)
                    {
                        throw Exceptions.newRuntimeException("Cannot find a property %s in %s to match model field %s", prop, entityClass, f);
                    }

                    Object valConverted = convertValueFromModel(sessionHolder, policy, val, setter.getGenericParameterTypes()[0], setter, f);
                    if (valConverted == s_skipMarker)
                    {
                        continue;
                    }

                    setter.invoke(entity, valConverted);
                }
            }
        }
    }

    private static Object convertValueFromModel(SessionHolder sessionHolder,
                                                ModelMapperPolicy policy,
                                                Object modelValue,
                                                Type entityType,
                                                Method contextEntity,
                                                Field contextModel) throws
                                                                    Exception
    {
        if (modelValue == null)
        {
            return null;
        }

        Type modelType = modelValue.getClass();
        if (modelType == entityType)
        {
            return modelValue;
        }

        Class<?> modelClass     = Reflection.getRawType(modelType);
        Class<?> entityClass    = Reflection.getRawType(entityType);
        boolean  isModelSimple  = isSimpleType(modelClass);
        boolean  isEntitySimple = isSimpleType(entityClass);
        if (isModelSimple && isEntitySimple)
        {
            return Reflection.coerceNumber(modelValue, entityClass);
        }

        if (modelClass == String.class)
        {
            if (entityClass == EncryptedPayload.class)
            {
                EncryptedPayload res = policy.encryptField(contextModel, (String) modelValue);
                return res != null ? res : s_skipMarker;
            }

            throw Exceptions.newRuntimeException("Cannot assign model field %s to entity property %s, incompatible collections", contextModel, contextEntity);
        }

        if (modelClass == TypedRecordIdentity.class)
        {
            if (!Reflection.isSubclassOf(RecordWithCommonFields.class, entityType))
            {
                throw Exceptions.newRuntimeException("Cannot assign model field %s to entity property %s, incompatible types", contextModel, contextEntity);
            }

            RecordIdentity ri = (RecordIdentity) modelValue;
            return sessionHolder.getEntity(entityClass, ri.sysId);
        }

        if (Reflection.isSubclassOf(TypedRecordIdentityList.class, modelType))
        {
            if (Reflection.isSubclassOf(List.class, entityType))
            {
                List<Object> entityList     = new ArrayList<>();
                Type         entityTypeSub  = Reflection.getTypeArgument(entityType, 0);
                Class<?>     entityClassSub = Reflection.getRawType(entityTypeSub);

                TypedRecordIdentityList<?> modelList = (TypedRecordIdentityList<?>) modelValue;
                for (RecordIdentity modelSub : modelList)
                {
                    Object rec = sessionHolder.getEntity(entityClassSub, modelSub.sysId);
                    entityList.add(rec);
                }

                return entityList;
            }

            // Because there's no Set in Javascript, we map sets to lists.
            if (Reflection.isSubclassOf(Set.class, entityType))
            {
                Set<Object> entitySet      = Sets.newHashSet();
                Type        entityTypeSub  = Reflection.getTypeArgument(entityType, 0);
                Class<?>    entityClassSub = Reflection.getRawType(entityTypeSub);

                TypedRecordIdentityList<?> modelList = (TypedRecordIdentityList<?>) modelValue;
                for (RecordIdentity modelSub : modelList)
                {
                    Object rec = sessionHolder.getEntity(entityClassSub, modelSub.sysId);
                    entitySet.add(rec);
                }

                return entitySet;
            }

            throw Exceptions.newRuntimeException("Can't assign model field %s to entity property %s, incompatible collections", contextModel, contextEntity);
        }

        if (Reflection.isSubclassOf(List.class, modelType))
        {
            if (Reflection.isSubclassOf(List.class, entityType))
            {
                List<Object> entityList    = new ArrayList<>();
                Type         entityTypeSub = Reflection.getTypeArgument(entityType, 0);

                List<?> modelList = (List<?>) modelValue;
                for (Object modelSub : modelList)
                {
                    Object entitySub = convertValueFromModel(sessionHolder, policy, modelSub, entityTypeSub, contextEntity, contextModel);
                    if (entitySub != s_skipMarker)
                    {
                        entityList.add(entitySub);
                    }
                }

                return entityList;
            }

            // Because there's no Set in Javascript, we map sets to lists.
            if (Reflection.isSubclassOf(Set.class, entityType))
            {
                Set<Object> entitySet     = Sets.newHashSet();
                Type        entityTypeSub = Reflection.getTypeArgument(entityType, 0);

                List<?> modelList = (List<?>) modelValue;
                for (Object modelSub : modelList)
                {
                    Object entitySub = convertValueFromModel(sessionHolder, policy, modelSub, entityTypeSub, contextEntity, contextModel);
                    if (entitySub != s_skipMarker)
                    {
                        entitySet.add(entitySub);
                    }
                }

                return entitySet;
            }

            throw Exceptions.newRuntimeException("Can't assign model field %s to entity property %s, incompatible collections", contextModel, contextEntity);
        }

        if (Reflection.isSubclassOf(TypedRecordIdentityMap.class, modelType))
        {
            if (Reflection.isSubclassOf(Map.class, entityType))
            {
                Map<Object, Object> entityMap        = Maps.newHashMap();
                Type                entityTypeValue  = Reflection.getTypeArgument(entityType, 1);
                Class<?>            entityClassValue = Reflection.getRawType(entityTypeValue);

                TypedRecordIdentityMap<?, ?> modelMap = (TypedRecordIdentityMap<?, ?>) modelValue;
                for (Object modelKey : modelMap.keySet())
                {
                    RecordIdentity modelSub = modelMap.get(modelKey);
                    Object         rec      = sessionHolder.getEntity(entityClassValue, modelSub.sysId);
                    entityMap.put(modelKey, rec);
                }

                return entityMap;
            }

            throw Exceptions.newRuntimeException("Can't assign model field %s to entity property %s, incompatible collections", contextModel, contextEntity);
        }

        if (Reflection.isSubclassOf(Map.class, modelType))
        {
            if (!Reflection.isSubclassOf(Map.class, entityType))
            {
                throw Exceptions.newRuntimeException("Can't assign model field %s to entity property %s, incompatible collections", contextModel, contextEntity);
            }

            Map<Object, Object> entityMap       = Maps.newHashMap();
            Type                entityTypeKey   = Reflection.getTypeArgument(entityType, 0);
            Type                entityTypeValue = Reflection.getTypeArgument(entityType, 1);

            Map<?, ?> modelMap = (Map<?, ?>) modelValue;
            for (Object modelKey : modelMap.keySet())
            {
                Object modelSub = modelMap.get(modelKey);

                Object entityKey = convertValueFromModel(sessionHolder, policy, modelKey, entityTypeKey, contextEntity, contextModel);
                Object entitySub = convertValueFromModel(sessionHolder, policy, modelSub, entityTypeValue, contextEntity, contextModel);

                if (entityKey != s_skipMarker && entitySub != s_skipMarker)
                {
                    entityMap.put(entityKey, entitySub);
                }
            }

            return entityMap;
        }

        if (Reflection.isSubclassOf(entityClass, modelClass))
        {
            return modelValue;
        }

        Object entitySub = Reflection.newInstance(entityType);

        copyFromModel(sessionHolder, modelValue, policy, entitySub);

        return entitySub;
    }

    //--//

    private static boolean shouldSkipField(Field f)
    {
        return f.getAnnotation(Optio3DontMap.class) != null || !Modifier.isPublic(f.getModifiers());
    }

    private static boolean isSimpleType(Class<?> clz)
    {
        if (clz == String.class)
        {
            return true;
        }

        if (clz.isEnum())
        {
            return true;
        }

        return Reflection.getDescriptor(clz) != null;
    }

    private static String fixupPropertyName(Field f,
                                            Optio3MapToPersistence anno)
    {
        String prop = f.getName();
        if (anno != null && anno.value()
                                .length() > 0)
        {
            prop = anno.value();
        }
        return prop;
    }
}
