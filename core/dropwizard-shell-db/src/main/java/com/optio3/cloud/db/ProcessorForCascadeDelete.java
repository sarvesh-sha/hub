/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.db;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.event.spi.DeleteEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * This class registers with Hibernate to process cascading of deletes.
 * <br>
 * It scans the entities {@link Optio3Cascade} annotations, and reacts to deletes, either by clearing the reference or by deleting the consumer records.
 */
class ProcessorForCascadeDelete
{
    public static final Logger LoggerInstance = new Logger(ProcessorForCascadeDelete.class);

    static
    {
        // Debugging helper to easily flip logging levels.
        LoggerInstance.disable(Severity.Debug);
    }

    private final String m_databaseId;

    private final EntityReferenceLookup m_lookup;

    ProcessorForCascadeDelete(String databaseId,
                              SessionFactoryImplementor sessionFactory,
                              EntityReferenceLookup lookup)
    {
        m_databaseId = databaseId;
        m_lookup     = lookup;

        ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();

        // Do the registrations
        final EventListenerRegistry listenerRegistry = serviceRegistry.getService(EventListenerRegistry.class);

        listenerRegistry.prependListeners(EventType.DELETE, new DeleteEventListener()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onDelete(DeleteEvent event) throws
                                                    HibernateException
            {
                processCascades(event.getSession(), event.getObject());
            }

            @Override
            public void onDelete(DeleteEvent event,
                                 @SuppressWarnings("rawtypes") Set transientEntities) throws
                                                                                      HibernateException
            {
                processCascades(event.getSession(), event.getObject());
            }
        });
    }

    //--//

    private void processCascades(EventSource eventSource,
                                 Object deletedObject)
    {
        deletedObject = SessionHolder.unwrapProxy(deletedObject);

        PersistenceContext ctx   = eventSource.getPersistenceContext();
        EntityEntry        entry = ctx.getEntry(deletedObject);
        if (entry == null)
        {
            // The object is not associated with the current persistence context. Just exit.
            return;
        }

        if (LoggerInstance.isEnabled(Severity.Debug))
        {
            LoggerInstance.debug("processCascades: %s", m_lookup.extractDisplayName(deletedObject));
        }

        // Notify the object that it's about to be deleted.
        IRemoveNotification callback = Reflection.as(deletedObject, IRemoveNotification.class);
        if (callback != null)
        {
            callback.onRemove(eventSource);
        }

        Multimap<Class<?>, Serializable> deleteCandidates = null;
        Entry<Object, EntityEntry>[]     entriesInContext = ctx.reentrantSafeEntityEntries(); // To track entities in the session but not flushed to DB.
        String                           entityName       = entry.getEntityName();

        // Try and load lazy fields before the entity gets deleted.
        for (EntityReferenceLookup.AttributeDetails attributeDetails : m_lookup.fromEntityToDirectAssociations(entityName).fields)
        {
            if (attributeDetails.getter != null)
            {
                attributeDetails.readValue(deletedObject);
            }
        }

        List<EntityReferenceLookup.AttributeDetails> attributes = m_lookup.fromEntityToReverseAssociations(entityName);
        if (!attributes.isEmpty())
        {
            //
            // Search for all the entities *pointing* to the deleted object.
            //

            //
            // We have to use a temporary session, otherwise Hibernate would try and flush the session *before* the query.
            //
            try (Session subSession = eventSource.getSessionFactory()
                                                 .openTemporarySession())
            {
                for (EntityReferenceLookup.AttributeDetails details : attributes)
                {
                    if (details.cascadeDelete == null)
                    {
                        continue;
                    }

                    EntityReferenceLookup.SingularAttributeDetails singularDetails = Reflection.as(details, EntityReferenceLookup.SingularAttributeDetails.class);
                    if (singularDetails != null)
                    {
                        Class<?> entityType = singularDetails.attr.getDeclaringType()
                                                                  .getJavaType();

                        // SELECT x FROM X x WHERE x.<attr> == <deleted record>
                        CriteriaBuilder  cb       = subSession.getCriteriaBuilder();
                        CriteriaQuery<?> qdef     = cb.createQuery(entityType);
                        Root<?>          rootPath = qdef.from(entityType);
                        qdef.where(cb.equal(rootPath.get(details.attributeName), deletedObject));

                        List<?> list = subSession.createQuery(qdef)
                                                 .getResultList();
                        for (Object o : list)
                        {
                            Serializable matchId = getCrossSessionIdentifier(eventSource.getSession(), subSession, o);

                            Object consumer = eventSource.byId(entityType)
                                                         .load(matchId);
                            if (consumer != null && handleSingular(singularDetails, deletedObject, consumer))
                            {
                                deleteCandidates = addCandidate(deleteCandidates, entityType, matchId);
                            }
                        }
                    }

                    EntityReferenceLookup.PluralAttributeDetails pluralDetails = Reflection.as(details, EntityReferenceLookup.PluralAttributeDetails.class);
                    if (pluralDetails != null)
                    {
                        Class<?> entityType = pluralDetails.attr.getDeclaringType()
                                                                .getJavaType();

                        // SELECT x FROM X x WHERE x.<attr>.contains(<deleted record>)
                        CriteriaBuilder  cb       = subSession.getCriteriaBuilder();
                        CriteriaQuery<?> qdef     = cb.createQuery(entityType);
                        Root<?>          rootPath = qdef.from(entityType);
                        qdef.where(cb.isMember(deletedObject, rootPath.get(details.attributeName)));

                        List<?> list = subSession.createQuery(qdef)
                                                 .getResultList();
                        for (Object o : list)
                        {
                            Serializable matchId = getCrossSessionIdentifier(eventSource.getSession(), subSession, o);

                            Object consumer = eventSource.byId(entityType)
                                                         .load(matchId);
                            if (consumer != null && handlePlural(pluralDetails, deletedObject, consumer))
                            {
                                deleteCandidates = addCandidate(deleteCandidates, entityType, matchId);
                            }
                        }
                    }
                }
            }
        }

        //--//

        //
        // Post-process entities already in the session, to make sure we don't miss pending changes.
        //
        if (entriesInContext != null)
        {
            for (Entry<Object, EntityEntry> pair : entriesInContext)
            {
                Object      consumer      = pair.getKey();
                EntityEntry consumerEntry = pair.getValue();

                if (EntityReferenceLookup.wasEntityDeleted(consumerEntry))
                {
                    continue;
                }

                Class<?>     consumerClz = SessionHolder.getClassOfEntity(consumer);
                Serializable consumerId  = consumerEntry.getId();

                //
                // Look for entities that have a direct association with the deleted object.
                //
                EntityReferenceLookup.EntityDetails entityDetails = m_lookup.fromEntityToDirectAssociations(consumerClz);
                for (EntityReferenceLookup.AttributeDetails details : entityDetails.fields)
                {
                    if (details.isId)
                    {
                        continue;
                    }

                    if (!details.canPointTo(deletedObject))
                    {
                        continue;
                    }

                    EntityReferenceLookup.SingularAttributeDetails singularDetails = Reflection.as(details, EntityReferenceLookup.SingularAttributeDetails.class);
                    if (singularDetails != null)
                    {
                        if (checkSingularPointToTarget(singularDetails, deletedObject, consumer) && handleSingular(singularDetails, deletedObject, consumer))
                        {
                            deleteCandidates = addCandidate(deleteCandidates, consumerClz, consumerId);
                        }
                    }

                    EntityReferenceLookup.PluralAttributeDetails pluralDetails = Reflection.as(details, EntityReferenceLookup.PluralAttributeDetails.class);
                    if (pluralDetails != null)
                    {
                        if (checkPluralPointToTarget(pluralDetails, deletedObject, consumer) && handlePlural(pluralDetails, deletedObject, consumer))
                        {
                            deleteCandidates = addCandidate(deleteCandidates, consumerClz, consumerId);
                        }
                    }
                }
            }
        }

        if (deleteCandidates != null)
        {
            for (Class<?> clz : deleteCandidates.keySet())
            {
                Collection<Serializable> list = deleteCandidates.get(clz);
                for (Serializable id : list)
                {
                    Object consumer = eventSource.byId(clz)
                                                 .load(id);
                    if (consumer != null)
                    {
                        LoggerInstance.debug("Cascade Delete: %s:%s", clz.getSimpleName(), id);
                        eventSource.delete(consumer);
                    }
                }
            }
        }
    }

    private Serializable getCrossSessionIdentifier(Session targetSession,
                                                   Session sourceSession,
                                                   Object o)
    {
        Serializable            id  = sourceSession.getIdentifier(o);
        ICrossSessionIdentifier id2 = Reflection.as(id, ICrossSessionIdentifier.class);
        if (id2 != null)
        {
            id = id2.remapToSession(targetSession);
        }

        return id;
    }

    private Multimap<Class<?>, Serializable> addCandidate(Multimap<Class<?>, Serializable> deleteCandidates,
                                                          Class<?> entityType,
                                                          Serializable matchId)
    {
        if (deleteCandidates == null)
        {
            deleteCandidates = HashMultimap.create();
        }

        if (!deleteCandidates.containsEntry(entityType, matchId))
        {
            deleteCandidates.put(entityType, matchId);
        }

        return deleteCandidates;
    }

    private boolean handleSingular(EntityReferenceLookup.SingularAttributeDetails details,
                                   Object target,
                                   Object consumer)
    {
        Optio3Cascade.Flavor cascadeDelete = normalizeCascade(details);
        switch (cascadeDelete)
        {
            case DELETE:
                return true;

            case PREVENT:
                throw Exceptions.newGenericException(HibernateException.class, "Optio3Cascade annotation on '%s' prevents deletes", details.displayName);

            case CLEAR:
            default:
                //
                // Check to see if the property still points to the target object.
                // It could be that it got updated but not flushed yet.
                //
                Object oldValue = details.readValue(consumer);
                if (oldValue == target)
                {
                    if (LoggerInstance.isEnabled(Severity.Debug))
                    {
                        LoggerInstance.debug("Cascade Clear: %s : %s : %s", m_lookup.extractDisplayName(consumer), details.displayName, m_lookup.extractDisplayName(target));
                    }

                    details.writeValue(consumer, null);
                }
                return false;
        }
    }

    private boolean checkSingularPointToTarget(EntityReferenceLookup.SingularAttributeDetails details,
                                               Object target,
                                               Object consumer)
    {
        //
        // Check to see if the property still points to the target object.
        // It could be that it got updated but not flushed yet.
        //
        Object oldValue = details.readValue(consumer);
        return oldValue == target;
    }

    private boolean handlePlural(EntityReferenceLookup.PluralAttributeDetails details,
                                 Object target,
                                 Object consumer)
    {
        Optio3Cascade.Flavor cascadeDelete = normalizeCascade(details);
        switch (cascadeDelete)
        {
            case DELETE:
                return true;

            case PREVENT:
                throw Exceptions.newGenericException(HibernateException.class, "Optio3Cascade annotation on '%s' prevents deletes", details.displayName);

            case CLEAR:
            default:
                //
                // Check to see if the collection still points to the target object.
                // It could be that it got updated but not flushed yet.
                //
                Collection<?> coll = details.readValueAsCollection(consumer, true);
                if (coll != null && coll.contains(target))
                {
                    if (LoggerInstance.isEnabled(Severity.Debug))
                    {
                        LoggerInstance.debug("Cascade Clear Coll: %s : %s : %s", m_lookup.extractDisplayName(consumer), details.displayName, m_lookup.extractDisplayName(target));
                    }

                    coll.remove(target);
                }

                return false;
        }
    }

    private boolean checkPluralPointToTarget(EntityReferenceLookup.PluralAttributeDetails details,
                                             Object target,
                                             Object consumer)
    {
        //
        // Check to see if the collection still points to the target object.
        // It could be that it got updated but not flushed yet.
        //
        Collection<?> coll = details.readValueAsCollection(consumer, true);
        return coll != null && coll.contains(target);
    }

    //--//

    private static Optio3Cascade.Flavor normalizeCascade(EntityReferenceLookup.AttributeDetails details)
    {
        Optio3Cascade.Flavor cascadeDelete = details.cascadeDelete;
        if (cascadeDelete == null || details.clearInMemory)
        {
            cascadeDelete = Optio3Cascade.Flavor.CLEAR;
        }
        return cascadeDelete;
    }
}
