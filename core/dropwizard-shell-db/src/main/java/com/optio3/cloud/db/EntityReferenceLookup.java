/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.db;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;

public class EntityReferenceLookup
{
    public static final Logger LoggerInstance = new Logger(EntityReferenceLookup.class);

    public static abstract class AttributeDetails
    {
        public Optio3ControlNotifications.Notify controlDirect  = Optio3ControlNotifications.Notify.NEVER;
        public Optio3ControlNotifications.Notify controlReverse = Optio3ControlNotifications.Notify.NEVER;
        public Optio3Cascade.Flavor              cascadeDelete;
        public Method                            getter;
        public Method                            setter;
        public boolean                           clearInMemory;
        public boolean                           isId;

        public final Attribute<?, ?> attr;
        public final String          entityName;
        public final String          attributeName;
        public final String          displayName;

        protected AttributeDetails(Attribute<?, ?> attr)
        {
            Member   m = attr.getJavaMember();
            Class<?> c = m.getDeclaringClass();

            this.attr = attr;

            entityName    = c.getSimpleName();
            attributeName = attr.getName();

            displayName = entityName + "." + attributeName;

            getter = Reflection.findGetter(c, attributeName);
            setter = Reflection.findSetter(c, attributeName, attr.getJavaType());
        }

        void ensureGetter(String annoName,
                          String getterName)
        {
            if (StringUtils.isEmpty(getterName))
            {
                throw Exceptions.newGenericException(HibernateException.class, "Missing getter for %s annotation on '%s'", annoName, attr.getJavaMember());
            }
            else
            {
                Member m = attr.getJavaMember();

                if (m instanceof Field)
                {
                    try
                    {
                        Class<?> sourceClass = m.getDeclaringClass();

                        getter = sourceClass.getMethod(getterName);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                else if (m instanceof Method)
                {
                    getter = (Method) m;
                }
                else
                {
                    getter = null;
                }

                if (getter == null)
                {
                    throw Exceptions.newGenericException(HibernateException.class, "Schema error: no getter for '%s'", m);
                }
            }
        }

        void ensureSetter(String annoName,
                          String setterName,
                          boolean mustHave)
        {
            if (this instanceof SingularAttributeDetails)
            {
                if (StringUtils.isEmpty(setterName))
                {
                    if (mustHave)
                    {
                        throw Exceptions.newGenericException(HibernateException.class, "Missing setter for %s annotation on '%s'", annoName, attr.getJavaMember());
                    }
                }
                else
                {
                    Member m = attr.getJavaMember();

                    if (m instanceof Field)
                    {
                        try
                        {
                            Class<?> sourceClass = m.getDeclaringClass();

                            setter = sourceClass.getMethod(setterName, attr.getJavaType());
                        }
                        catch (Exception e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                    else if (m instanceof Method)
                    {
                        setter = (Method) m;
                    }
                    else
                    {
                        setter = null;
                    }

                    if (setter == null)
                    {
                        throw Exceptions.newGenericException(HibernateException.class, "Schema error: no setter for '%s'", m);
                    }
                }
            }
            else
            {
                if (!StringUtils.isEmpty(setterName))
                {
                    throw Exceptions.newGenericException(HibernateException.class, "Collection attribute '%s' cannot have setter for %s annotation", attr.getJavaMember(), annoName);
                }
            }
        }

        //--//

        public abstract Type<?> getTargetType();

        public final Object readValue(Object entity)
        {
            if (getter == null)
            {
                throw Exceptions.newGenericException(HibernateException.class, "Schema error: no getter for '%s'", attr.getJavaMember());
            }

            try
            {
                return getter.invoke(Hibernate.unproxy(entity));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public final void writeValue(Object entity,
                                     Object value)
        {
            if (setter == null)
            {
                throw Exceptions.newGenericException(HibernateException.class, "Schema error: no setter for '%s'", attr.getJavaMember());
            }

            try
            {
                setter.invoke(SessionHolder.unwrapProxy(entity), value);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public boolean canPointTo(Object target)
        {
            return Reflection.isSubclassOf(getTargetType().getJavaType(), SessionHolder.getClassOfEntity(target));
        }
    }

    public static class SingularAttributeDetails extends AttributeDetails
    {
        SingularAttributeDetails(SingularAttribute<?, ?> attr)
        {
            super(attr);
        }

        @SuppressWarnings("unchecked")
        public <T> SingularAttribute<T, ?> getTypedAttr()
        {
            return (SingularAttribute<T, ?>) attr;
        }

        @Override
        public Type<?> getTargetType()
        {
            return getTypedAttr().getType();
        }
    }

    public static class PluralAttributeDetails extends AttributeDetails
    {
        PluralAttributeDetails(PluralAttribute<?, ?, ?> attr)
        {
            super(attr);
        }

        @SuppressWarnings("unchecked")
        public <T> PluralAttribute<T, ?, ?> getTypedAttr()
        {
            return (PluralAttribute<T, ?, ?>) attr;
        }

        @Override
        public Type<?> getTargetType()
        {
            return getTypedAttr().getElementType();
        }

        public Collection<Object> readValueAsCollection(Object entity,
                                                        boolean onlyIfInitialized)
        {
            Object value = readValue(entity);
            if (value == null)
            {
                return null;
            }

            if (onlyIfInitialized && value instanceof PersistentCollection)
            {
                PersistentCollection pc = (PersistentCollection) value;
                if (!pc.wasInitialized())
                {
                    //
                    // Calling 'postAction' clears the cached size and resets queued operations and dirty flag.
                    // This is harmless because the collection is uninitialized, so it cannot be dirty or have pending operations.
                    //
                    pc.postAction();

                    return null;
                }
            }

            if (value instanceof Collection)
            {
                @SuppressWarnings("unchecked") Collection<Object> coll = (Collection<Object>) value;
                return coll;
            }

            if (value instanceof Map)
            {
                @SuppressWarnings("unchecked") Map<Object, Object> coll = (Map<Object, Object>) value;
                return coll.values();
            }

            throw Exceptions.newGenericException(HibernateException.class,
                                                 "Schema error: unrecognized type for '%s': %s",
                                                 attr.getJavaMember(),
                                                 SessionHolder.getClassOfEntity(value)
                                                              .getName());
        }
    }

    public static class EntityDetails
    {
        public final List<AttributeDetails> fields = Lists.newArrayList();

        public SingularAttributeDetails idAttribute;

        private AttributeDetails findAttribute(Attribute<?, ?> attr)
        {
            for (AttributeDetails attributeDetails : fields)
            {
                if (attributeDetails.attr == attr)
                {
                    return attributeDetails;
                }
            }

            return null;
        }

        private AttributeDetails accessAttribute(Attribute<?, ?> attr)
        {
            AttributeDetails attributeDetails = findAttribute(attr);
            if (attributeDetails == null)
            {
                if (attr instanceof SingularAttribute<?, ?>)
                {
                    attributeDetails = new SingularAttributeDetails((SingularAttribute<?, ?>) attr);
                }
                else if (attr instanceof PluralAttribute<?, ?, ?>)
                {
                    attributeDetails = new PluralAttributeDetails((PluralAttribute<?, ?, ?>) attr);
                }
                else
                {
                    throw Exceptions.newGenericException(HibernateException.class, "Unexpected attribute of type %s", attr.getClass());
                }

                fields.add(attributeDetails);
            }

            return attributeDetails;
        }
    }

    //--//

    private final Map<Class<?>, ManagedType<?>> m_fromJavaClassToEntityClass = Maps.newHashMap();

    private final Map<String, Class<?>> m_fromEntityNameToJavaClass = Maps.newHashMap();

    //--//

    private final Map<Class<?>, EntityDetails>          m_fromEntityToDirectAssociations  = Maps.newHashMap();
    private final Map<Class<?>, List<AttributeDetails>> m_fromEntityToReverseAssociations = Maps.newHashMap();

    private final ConcurrentMap<Class<?>, EntityDetails>          m_fromEntityHierarchyToDirectAssociations  = Maps.newConcurrentMap();
    private final ConcurrentMap<Class<?>, List<AttributeDetails>> m_fromEntityHierarchyToReverseAssociations = Maps.newConcurrentMap();

    public EntityReferenceLookup(Metamodel metamodel)
    {
        for (ManagedType<?> entityType : metamodel.getManagedTypes())
        {
            Class<?> clz        = entityType.getJavaType();
            String   entityName = clz.getName();

            m_fromJavaClassToEntityClass.putIfAbsent(clz, entityType);

            m_fromEntityNameToJavaClass.putIfAbsent(entityName, clz);
        }

        //
        // First pass to extract Id attributes
        //
        for (EntityType<?> entity : metamodel.getEntities())
        {
            for (IdentifiableType<?> entityPtr = entity; entityPtr != null; entityPtr = entityPtr.getSupertype())
            {
                Class<?> entityClass = entityPtr.getJavaType();

                if (!m_fromEntityToDirectAssociations.containsKey(entityClass))
                {
                    EntityDetails entityDetails = ensureEntityDetails(entityClass);

                    for (SingularAttribute<?, ?> attr : entityPtr.getSingularAttributes())
                    {
                        if (attr.isId())
                        {
                            SingularAttributeDetails details = (SingularAttributeDetails) entityDetails.accessAttribute(attr);
                            details.isId = true;

                            boolean ignoreIdAttributes = false;

                            for (Class<?> itfClass : entityClass.getInterfaces())
                            {
                                if (itfClass == ICrossSessionIdentifier.class)
                                {
                                    ignoreIdAttributes = true;
                                    break;
                                }
                            }

                            if (!ignoreIdAttributes)
                            {
                                if (entityDetails.idAttribute != null)
                                {
                                    throw Exceptions.newGenericException(HibernateException.class, "Schema error: multi-field identifiers not supported for '%s'", attr.getJavaMember());
                                }

                                entityDetails.idAttribute = details;
                            }
                        }
                    }
                }
            }
        }

        for (EntityType<?> entity : metamodel.getEntities())
        {
            LoggerInstance.debug("EntityType: %s",
                                 entity.getJavaType()
                                       .getName());

            boolean disallowAssociations;

            switch (entity.getPersistenceType())
            {
                case EMBEDDABLE:
                    disallowAssociations = true;
                    break;

                default:
                    disallowAssociations = false;
                    break;
            }

            for (Attribute<?, ?> attr : entity.getAttributes())
            {
                Attribute.PersistentAttributeType attributeType = attr.getPersistentAttributeType();

                LoggerInstance.debug("    Attribute: %s.%s - %s",
                                     entity.getJavaType()
                                           .getName(),
                                     attr.getName(),
                                     attributeType);

                Member           m           = attr.getJavaMember();
                Class<?>         sourceClass = m.getDeclaringClass();
                AccessibleObject attrMember  = (AccessibleObject) m;

                Optio3Cascade annoCascade    = attrMember.getAnnotation(Optio3Cascade.class);
                OneToMany     annoOneToMany  = attrMember.getAnnotation(OneToMany.class);
                OneToOne      annoOneToOne   = attrMember.getAnnotation(OneToOne.class);
                ManyToMany    annoManyToMany = attrMember.getAnnotation(ManyToMany.class);
                ManyToOne     annoManyToOne  = attrMember.getAnnotation(ManyToOne.class);

                if (annoCascade != null)
                {
                    EntityDetails    entityDetails    = ensureEntityDetails(sourceClass);
                    AttributeDetails attributeDetails = ensureAttributeDetails(entityDetails, attr);

                    attributeDetails.cascadeDelete = annoCascade.mode();

                    attributeDetails.ensureGetter("Optio3Cascade", annoCascade.getter());
                    attributeDetails.ensureSetter("Optio3Cascade", annoCascade.setter(), annoCascade.mode() != Optio3Cascade.Flavor.DELETE);
                }

                if (annoOneToMany != null)
                {
                    // One To Many are handled from the "Many" side.
                    if (disallowAssociations)
                    {
                        throw Exceptions.newGenericException(HibernateException.class, "Schema error: @Embeddable types with associations not currently supported: '%s'", attr.getJavaMember());
                    }

                    if (annoOneToMany.fetch() == FetchType.LAZY)
                    {
                        LazyCollection annoLazyCollection = attrMember.getAnnotation(LazyCollection.class);
                        if (annoLazyCollection == null)
                        {
                            throw Exceptions.newGenericException(HibernateException.class, "Schema error: no @LazyCollection for lazy attribute '%s'", attr.getJavaMember());
                        }
                    }

                    Optio3ControlNotifications annoControl = attrMember.getAnnotation(Optio3ControlNotifications.class);
                    if (annoControl != null && annoControl.markerForLeftJoin())
                    {
                        // Synthetic field used only for left joins, skip.
                        continue;
                    }

                    //
                    // For @OneToMany attributes, we don't need to perform database queries, since these are synthetic collections.
                    // But we need to clear the in-memory values.
                    //
                    EntityDetails    entityDetails    = ensureEntityDetails(sourceClass);
                    AttributeDetails attributeDetails = ensureAttributeDetails(entityDetails, attr);

                    attributeDetails.clearInMemory = true;
                    continue;
                }

                if (annoManyToMany != null)
                {
                    LazyCollection annoLazyCollection = attrMember.getAnnotation(LazyCollection.class);

                    if (StringUtils.isNotEmpty(annoManyToMany.mappedBy()))
                    {
                        //
                        // For @ManyToMany attributes with mappedBy, we don't need to perform database queries, since these are synthetic collections.
                        // But we need to clear the in-memory values.
                        //
                        if (annoManyToMany.fetch() == FetchType.LAZY)
                        {
                            if (annoLazyCollection == null)
                            {
                                throw Exceptions.newGenericException(HibernateException.class, "Schema error: no @LazyCollection for lazy attribute '%s'", attr.getJavaMember());
                            }
                        }

                        EntityDetails    entityDetails    = ensureEntityDetails(sourceClass);
                        AttributeDetails attributeDetails = ensureAttributeDetails(entityDetails, attr);

                        attributeDetails.clearInMemory = true;
                        continue;
                    }
                    else
                    {
                        if (annoLazyCollection != null)
                        {
                            throw Exceptions.newGenericException(HibernateException.class, "Schema error: invalid @LazyCollection on @ManyToMany without mappedBy on '%s'", attr.getJavaMember());
                        }
                    }
                }

                switch (attributeType)
                {
                    case BASIC:
                    case ELEMENT_COLLECTION:
                    case EMBEDDED:
                        // Not a reference, nothing to do.
                        break;

                    case MANY_TO_MANY:
                    case MANY_TO_ONE:
                    case ONE_TO_ONE:
                        if (disallowAssociations)
                        {
                            throw Exceptions.newGenericException(HibernateException.class, "Schema error: @Embeddable types with associations not currently supported: '%s'", attr.getJavaMember());
                        }

                        if (attr instanceof SingularAttribute<?, ?>)
                        {
                            SingularAttribute attr2 = (SingularAttribute) attr;

                            if (attr2.isId())
                            {
                                // Skip identity fields.
                                continue;
                            }
                        }

                        EntityDetails entityDetails = ensureEntityDetails(sourceClass);
                        if (entityDetails.idAttribute == null)
                        {
                            throw Exceptions.newGenericException(HibernateException.class, "Schema error: no identifier for attribute '%s'", attr.getJavaMember());
                        }

                        Optio3ControlNotifications annoControl = attrMember.getAnnotation(Optio3ControlNotifications.class);
                        if (annoControl == null)
                        {
                            throw Exceptions.newGenericException(HibernateException.class, "Schema error: no @Optio3ControlNotifications for attribute '%s'", attr.getJavaMember());
                        }

                        if (annoControl.direct() != Optio3ControlNotifications.Notify.IGNORE)
                        {
                            AttributeDetails attributeDetails = ensureAttributeDetails(entityDetails, attr);
                            attributeDetails.controlDirect  = annoControl.direct();
                            attributeDetails.controlReverse = annoControl.reverse();

                            switch (annoControl.direct())
                            {
                                case ALWAYS:
                                case ON_ASSOCIATION_CHANGES:
                                    if (annoManyToMany != null && annoManyToMany.fetch() == FetchType.LAZY)
                                    {
                                        attributeDetails.ensureGetter("Optio3ControlNotifications", annoControl.getter());
                                    }

                                    if (annoManyToOne != null && annoManyToOne.fetch() == FetchType.LAZY)
                                    {
                                        attributeDetails.ensureGetter("Optio3ControlNotifications", annoControl.getter());
                                    }

                                    if (annoOneToOne != null && annoOneToOne.fetch() == FetchType.LAZY)
                                    {
                                        attributeDetails.ensureGetter("Optio3ControlNotifications", annoControl.getter());
                                    }
                                    break;
                            }
                        }
                        break;

                    default:
                        break;
                }
            }
        }
    }

    private EntityDetails ensureEntityDetails(Class<?> sourceClass)
    {
        return m_fromEntityToDirectAssociations.computeIfAbsent(sourceClass, k -> new EntityDetails());
    }

    private AttributeDetails ensureAttributeDetails(EntityDetails entityDetails,
                                                    Attribute<?, ?> attr)
    {
        AttributeDetails details = entityDetails.accessAttribute(attr);

        Type<?> t = details.getTargetType();

        if (!(t instanceof ManagedType))
        {
            throw Exceptions.newGenericException(HibernateException.class, "Non-managed type used in association for %s", attr.getJavaMember());
        }

        ManagedType<?> targetType  = (ManagedType<?>) t;
        Class<?>       targetClass = targetType.getJavaType();

        List<AttributeDetails> lstReverse = m_fromEntityToReverseAssociations.computeIfAbsent(targetClass, k -> Lists.newArrayList());
        CollectionUtils.addIfMissingAndNotNull(lstReverse, details);

        return details;
    }

    //--//

    public Object extractDisplayName(Object entity)
    {
        try
        {
            Class<?>      entityClass   = SessionHolder.getClassOfEntity(entity);
            EntityDetails entityDetails = fromEntityToDirectAssociations(entityClass);

            Object id = entityDetails.idAttribute.readValue(entity);

            return String.format("%s:%s", entityClass.getSimpleName(), id);
        }
        catch (Throwable t)
        {
            return entity;
        }
    }

    public ManagedType<?> getEntityTypeFromEntityClass(Class<?> entityClass)
    {
        while (entityClass != null)
        {
            ManagedType<?> entityType = m_fromJavaClassToEntityClass.get(entityClass);
            if (entityType != null)
            {
                return entityType;
            }

            entityClass = entityClass.getSuperclass();
        }

        return null;
    }

    public Class<?> getClassFromEntityName(String entityName)
    {
        return m_fromEntityNameToJavaClass.get(entityName);
    }

    /**
     * Helper method to check if an entity was deleted in the current context.
     *
     * @param eventSource The context
     * @param entity      The entity to check
     *
     * @return True if it was deleted
     */
    public static boolean wasEntityDeleted(EventSource eventSource,
                                           Object entity)
    {
        if (entity == null)
        {
            return true;
        }

        return wasEntityDeleted(eventSource.getPersistenceContext()
                                           .getEntry(entity));
    }

    public static boolean wasEntityDeleted(EntityEntry entry)
    {
        if (entry == null)
        {
            return true;
        }

        Status status = entry.getStatus();
        return status == null || status == Status.DELETED || status == Status.GONE;
    }

    //--//

    public EntityDetails fromEntityToDirectAssociations(String entityName)
    {
        Class<?> clz = getClassFromEntityName(entityName);

        return fromEntityToDirectAssociations(clz);
    }

    public EntityDetails fromEntityToDirectAssociations(Class<?> clz)
    {
        EntityDetails res = m_fromEntityHierarchyToDirectAssociations.get(clz);
        if (res == null)
        {
            res = new EntityDetails();

            for (Class<?> clzPtr = clz; clzPtr != null; clzPtr = clzPtr.getSuperclass())
            {
                EntityDetails entityPtr = m_fromEntityToDirectAssociations.get(clzPtr);
                if (entityPtr != null)
                {
                    res.fields.addAll(entityPtr.fields);

                    if (entityPtr.idAttribute != null)
                    {
                        res.idAttribute = entityPtr.idAttribute;
                    }
                }
            }

            m_fromEntityHierarchyToDirectAssociations.putIfAbsent(clz, res);
        }

        return res;
    }

    public List<AttributeDetails> fromEntityToReverseAssociations(String entityName)
    {
        Class<?> clz = getClassFromEntityName(entityName);

        return fromEntityToReverseAssociations(clz);
    }

    public List<AttributeDetails> fromEntityToReverseAssociations(Class<?> clz)
    {
        List<AttributeDetails> res = m_fromEntityHierarchyToReverseAssociations.get(clz);
        if (res == null)
        {
            res = Lists.newArrayList();

            for (Class<?> clzPtr = clz; clzPtr != null; clzPtr = clzPtr.getSuperclass())
            {
                List<AttributeDetails> lstPtr = m_fromEntityToReverseAssociations.get(clzPtr);
                if (lstPtr != null)
                {
                    res.addAll(lstPtr);
                }
            }

            res = Collections.unmodifiableList(res);
            m_fromEntityHierarchyToReverseAssociations.putIfAbsent(clz, res);
        }

        return res;
    }

    //--//

    public <T> SingularAttributeDetails getIdentifier(ManagedType<T> type)
    {
        for (Class<?> clzPtr = type.getJavaType(); clzPtr != null; clzPtr = clzPtr.getSuperclass())
        {
            EntityDetails entityDetails = m_fromEntityToDirectAssociations.get(clzPtr);
            if (entityDetails != null && entityDetails.idAttribute != null)
            {
                return entityDetails.idAttribute;
            }
        }

        return null;
    }

    //--//

    public void dump()
    {
        List<Class<?>> classes = Lists.newArrayList(m_fromEntityToDirectAssociations.keySet());
        classes.sort(Comparator.comparing(Class::getName));

        for (Class<?> clz : classes)
        {
            EntityDetails entityDetails = m_fromEntityToDirectAssociations.get(clz);

            for (AttributeDetails attr : sortForDump(entityDetails.fields))
            {
                String  kind;
                Type<?> target;

                if (attr instanceof SingularAttributeDetails)
                {
                    SingularAttributeDetails singularAttr = (SingularAttributeDetails) attr;

                    kind   = "SINGULAR";
                    target = singularAttr.getTypedAttr()
                                         .getType();
                }
                else
                {
                    PluralAttributeDetails pluralAttr = (PluralAttributeDetails) attr;

                    kind   = "PLURAL  ";
                    target = pluralAttr.getTypedAttr()
                                       .getElementType();
                }

                String targetText = target.getJavaType()
                                          .getSimpleName();

                if (attr.cascadeDelete == Optio3Cascade.Flavor.CLEAR)
                {
                    LoggerInstance.info("%-80s => CLEAR   %s %-50s => %s", clz.getName(), kind, attr.attributeName, targetText);
                }

                if (attr.cascadeDelete == Optio3Cascade.Flavor.DELETE)
                {
                    LoggerInstance.info("%-80s => DELETE  %s %-50s => %s", clz.getName(), kind, attr.attributeName, targetText);
                }

                if (attr.cascadeDelete == Optio3Cascade.Flavor.PREVENT)
                {
                    LoggerInstance.info("%-80s => PREVENT %s %-50s => %s", clz.getName(), kind, attr.attributeName, targetText);
                }

                if (attr.controlDirect == Optio3ControlNotifications.Notify.ALWAYS)
                {
                    LoggerInstance.info("%-80s => DIRECT  %s %-50s => %s", clz.getName(), kind, attr.attributeName, targetText);
                }

                if (attr.controlDirect == Optio3ControlNotifications.Notify.ON_ASSOCIATION_CHANGES)
                {
                    LoggerInstance.info("%-80s => CHANGE  %s %-50s => %s", clz.getName(), kind, attr.attributeName, targetText);
                }

                if (attr.controlReverse != Optio3ControlNotifications.Notify.NEVER)
                {
                    LoggerInstance.info("%-80s => REVERSE %s %-50s => %s", clz.getName(), kind, attr.attributeName, targetText);
                }
            }
        }
    }

    private Collection<AttributeDetails> sortForDump(Collection<AttributeDetails> attributes)
    {
        List<AttributeDetails> lst = Lists.newArrayList(attributes);
        lst.sort(Comparator.comparing((details) -> details.displayName));
        return lst;
    }
}
