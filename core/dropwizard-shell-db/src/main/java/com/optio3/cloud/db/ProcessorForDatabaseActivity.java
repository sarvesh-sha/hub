/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.db;

import java.io.Serializable;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.AbstractConfigurationWithDatabase;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.DatabaseActivity;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.persistence.DbAction;
import com.optio3.cloud.persistence.DbEvent;
import com.optio3.cloud.persistence.PostCommitNotificationReason;
import com.optio3.cloud.persistence.PostCommitNotificationState;
import com.optio3.cloud.persistence.PostCommitNotificationStateField;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.Reflection;
import com.optio3.util.TimeUtils;
import org.hibernate.Session;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * This class registers with Hibernate to generate cache invalidation messages.
 */
class ProcessorForDatabaseActivity<T extends AbstractConfigurationWithDatabase>
{
    public static class ForQueue
    {
    }

    public static class ForPost
    {
    }

    public static final Logger LoggerInstance         = new Logger(ProcessorForDatabaseActivity.class);
    public static final Logger LoggerInstanceForQueue = new Logger(ForQueue.class);
    public static final Logger LoggerInstanceForPost  = new Logger(ForPost.class);
    public static final Logger LoggerInstanceStats    = LoggerInstance.createSubLogger(Statistics.class);

    static
    {
        // Debugging helper to easily flip logging levels.
        LoggerInstance.disable(Severity.Debug);
        LoggerInstanceForQueue.disable(Severity.Debug);
        LoggerInstanceForPost.disable(Severity.Debug);
    }

    //--//

    static class Statistics
    {
        private final Map<Attribute<?, ?>, AtomicInteger> m_directAssociationStats  = Maps.newHashMap();
        private final Map<Attribute<?, ?>, AtomicInteger> m_reverseAssociationStats = Maps.newHashMap();

        public void update(Serializable id,
                           Attribute<?, ?> attr,
                           boolean reverse)
        {
            Map<Attribute<?, ?>, AtomicInteger> associationStats = reverse ? m_reverseAssociationStats : m_directAssociationStats;

            synchronized (this)
            {
                AtomicInteger counter = associationStats.get(attr);
                if (counter == null)
                {
                    counter = new AtomicInteger();
                    associationStats.put(attr, counter);
                }

                int count = counter.incrementAndGet();

                Severity level;

                if (LoggerInstanceStats.isEnabled(Severity.DebugVerbose))
                {
                    level = Severity.DebugVerbose;
                }
                else if (LoggerInstanceStats.isEnabled(Severity.Debug))
                {
                    if ((count % 100) != 0)
                    {
                        return;
                    }

                    level = Severity.Debug;
                }
                else
                {
                    return;
                }

                Type<?> targetType;

                if (attr instanceof PluralAttribute<?, ?, ?>)
                {
                    PluralAttribute<?, ?, ?> attr2 = (PluralAttribute<?, ?, ?>) attr;
                    targetType = attr2.getElementType();
                }
                else if (attr instanceof SingularAttribute<?, ?>)
                {
                    SingularAttribute<?, ?> attr2 = (SingularAttribute<?, ?>) attr;
                    targetType = attr2.getType();
                }
                else
                {
                    return;
                }

                String source = toText(attr.getDeclaringType()) + "." + attr.getName();

                switch (level)
                {
                    case Debug:
                        LoggerInstanceStats.debug("%s -> %s : count:%d : %s", source, toText(targetType), count, reverse ? "<reverse>" : "<direct>");
                        break;

                    case DebugVerbose:
                        LoggerInstanceStats.debugVerbose("%s : %s -> %s : count:%d : %s", id, source, toText(targetType), count, reverse ? "<reverse>" : "<direct>");
                        break;
                }
            }
        }
    }

    //--//

    private final AbstractApplicationWithDatabase<?> m_app;
    private final String                             m_databaseId;
    private final SessionFactoryImplementor          m_sessionFactory;
    private final EntityReferenceLookup              m_lookup;
    private final Object                             m_flushLock = new Object();

    private DatabaseActivity m_channel;

    @SuppressWarnings("unused") // Just to keep the reference alive.
    private final AbstractApplicationWithDatabase<T>.DatabaseChangeRegistration m_reg;

    private Map<String, DbEvent> m_pendingEvents;

    private final Statistics m_stats = new Statistics();

    //--//

    ProcessorForDatabaseActivity(AbstractApplicationWithDatabase<T> app,
                                 String databaseId,
                                 SessionFactoryImplementor sessionFactory,
                                 EntityReferenceLookup lookup)
    {
        m_app = app;
        m_databaseId = databaseId;
        m_sessionFactory = sessionFactory;
        m_lookup = lookup;

        //
        // For INSERTs and UPDATEs, we can use the PostCommit notifications.
        // For DELETEs, we rely on ProcessorForCascadeDelete and constraints enforced by AbstractApplication on Hibernate annotations.
        //
        m_reg = app.registerLocalDatabaseChangeNotification(m_databaseId, null, this::processNotification);
    }

    //--//

    private void processNotification(String databaseId,
                                     Object entity,
                                     Serializable id,
                                     PostCommitNotificationReason action,
                                     PostCommitNotificationState state)
    {
        if (LoggerInstance.isEnabled(Severity.Debug))
        {
            LoggerInstance.debug("processNotification: %s - %s", m_lookup.extractDisplayName(entity), action);
        }

        Class<?> entityClass = SessionHolder.getClassOfEntity(entity);

        switch (action)
        {
            case INSERT:
            {
                processDirectAssociations(entity, id, state, m_lookup.fromEntityToDirectAssociations(entityClass), true, false, false);
                break;
            }

            case UPDATE:
            {
                processDirectAssociations(entity, id, state, m_lookup.fromEntityToDirectAssociations(entityClass), true, false, true);

                processReverseAssociations(entity, id);
                break;
            }

            case DELETE:
            {
                processDirectAssociations(entity, id, state, m_lookup.fromEntityToDirectAssociations(entityClass), false, true, false);
                break;
            }
        }

        switch (action)
        {
            case DELETE:
                fireEventFromObject(entity, DbAction.DELETE, null);
                break;

            case INSERT:
                fireEventFromObject(entity, DbAction.INSERT, null);
                break;

            case UPDATE:
                fireEventFromObject(entity, DbAction.UPDATE_DIRECT, null);
                break;

            default:
                break;
        }
    }

    //--//

    private void processDirectAssociations(Object entity,
                                           Serializable id,
                                           PostCommitNotificationState state,
                                           EntityReferenceLookup.EntityDetails entityDetails,
                                           boolean useCurrent,
                                           boolean usePrevious,
                                           boolean onlyIfChanged)
    {
        for (EntityReferenceLookup.AttributeDetails attributeDetails : entityDetails.fields)
        {
            if (attributeDetails.controlDirect == Optio3ControlNotifications.Notify.NEVER)
            {
                continue;
            }

            PostCommitNotificationStateField field = state.findField(attributeDetails.attributeName, false);
            if (field == null)
            {
                continue;
            }

            if (attributeDetails instanceof EntityReferenceLookup.SingularAttributeDetails)
            {
                Object currentValue  = field.currentValue;
                Object previousValue = field.previousValue;

                if (attributeDetails.controlDirect == Optio3ControlNotifications.Notify.ON_ASSOCIATION_CHANGES && onlyIfChanged)
                {
                    if (!field.dirty)
                    {
                        LoggerInstance.debug("Referencing %s:%s => %s : NOT DIRTY", toText(entity), id, attributeDetails.displayName);
                        continue;
                    }

                    if (previousValue == currentValue)
                    {
                        LoggerInstance.debug("Referencing %s:%s => %s : SAME VALUE", toText(entity), id, attributeDetails.displayName);
                        continue;
                    }
                }

                if (usePrevious || onlyIfChanged)
                {
                    fireIndirectUpdateEventIfNotNull("", "previous ", entity, id, attributeDetails, previousValue);
                }

                if (useCurrent)
                {
                    fireIndirectUpdateEventIfNotNull("", "current ", entity, id, attributeDetails, currentValue);
                }
            }
            else if (attributeDetails instanceof EntityReferenceLookup.PluralAttributeDetails)
            {
                Collection<?> currentReferencedEntities  = asCollection(field.currentValue, usePrevious);
                Collection<?> previousReferencedEntities = asCollection(field.previousValue, usePrevious);

                if (attributeDetails.controlDirect == Optio3ControlNotifications.Notify.ON_ASSOCIATION_CHANGES && onlyIfChanged)
                {
                    if (!field.dirty)
                    {
                        LoggerInstance.debug("Referencing %s:%s => %s : NOT DIRTY", toText(entity), id, attributeDetails.displayName);
                        continue;
                    }

                    Set<?> currentSet  = Sets.newHashSet(currentReferencedEntities);
                    Set<?> previousSet = Sets.newHashSet(previousReferencedEntities);

                    for (Object referencedEntity : previousReferencedEntities)
                    {
                        if (currentSet.contains(referencedEntity))
                        {
                            continue;
                        }

                        fireIndirectUpdateEventIfNotNull("collection ", "previous ", entity, id, attributeDetails, referencedEntity);
                    }

                    for (Object referencedEntity : currentReferencedEntities)
                    {
                        if (previousSet.contains(referencedEntity))
                        {
                            continue;
                        }

                        fireIndirectUpdateEventIfNotNull("collection ", "current ", entity, id, attributeDetails, referencedEntity);
                    }
                }
                else
                {
                    if (usePrevious)
                    {
                        for (Object referencedEntity : previousReferencedEntities)
                        {
                            fireIndirectUpdateEventIfNotNull("collection ", "previous ", entity, id, attributeDetails, referencedEntity);
                        }
                    }

                    if (useCurrent)
                    {
                        for (Object referencedEntity : currentReferencedEntities)
                        {
                            fireIndirectUpdateEventIfNotNull("collection ", "current ", entity, id, attributeDetails, referencedEntity);
                        }
                    }
                }
            }
        }
    }

    private static Collection<?> asCollection(Object value,
                                              boolean forPrevious)
    {
        if (forPrevious && value instanceof PersistentCollection)
        {
            PersistentCollection pc = (PersistentCollection) value;
            if (!pc.wasInitialized())
            {
                // Skip non-initialized collections, we won't be able to load them.
                return Collections.emptyList();
            }
        }

        if (value instanceof Collection<?>)
        {
            return (Collection<?>) value;
        }

        return Collections.emptyList();
    }

    private void fireIndirectUpdateEventIfNotNull(String modeText,
                                                  String versionText,
                                                  Object entity,
                                                  Serializable id,
                                                  EntityReferenceLookup.AttributeDetails attributeDetails,
                                                  Object value)
    {
        if (value != null)
        {
            if (LoggerInstance.isEnabled(Severity.Debug))
            {
                LoggerInstance.debug("Referencing %s%s:%s => %s%s from %s", modeText, toText(entity), id, versionText, m_lookup.extractDisplayName(value), attributeDetails.displayName);
            }

            fireEventFromObject(value, DbAction.UPDATE_INDIRECT, attributeDetails.attr);
        }
    }

    private void processReverseAssociations(Object entity,
                                            Serializable id)
    {
        List<EntityReferenceLookup.AttributeDetails> attributes = m_lookup.fromEntityToReverseAssociations(SessionHolder.getClassOfEntity(entity));

        //
        // Quick check for attributes with reverse associations, to avoid creating a session when not needed.
        //
        boolean anyReverseAssocations = false;

        for (EntityReferenceLookup.AttributeDetails details : attributes)
        {
            if (details.controlReverse != Optio3ControlNotifications.Notify.NEVER)
            {
                anyReverseAssocations = true;
                break;
            }
        }

        if (anyReverseAssocations)
        {
            //
            // We have to use a temporary session, we don't want to affect the current Hibernate context.
            //
            try (Session subSession = m_sessionFactory.openTemporarySession())
            {
                for (EntityReferenceLookup.AttributeDetails details : attributes)
                {
                    if (details.controlReverse == Optio3ControlNotifications.Notify.NEVER)
                    {
                        continue;
                    }

                    LoggerInstance.debugVerbose("Checking %s...", details.displayName);

                    List<Serializable> referencingObjects = findReferencingObjects(subSession, entity, details.attr);
                    for (Serializable referencingId : referencingObjects)
                    {
                        LoggerInstance.debug("Referenced %s:%s <= %s from %s", toText(entity), id, referencingId, details.displayName);

                        fireEventFromType(details.attr.getDeclaringType(), referencingId, DbAction.UPDATE_INDIRECT, null, null, details.attr);
                    }
                }
            }
        }
    }

    //--//

    private void fireEventFromObject(Object entity,
                                     DbAction action,
                                     Attribute<?, ?> reachedThroughDirectLink)
    {
        Class<?>       entityClass = SessionHolder.getClassOfEntity(entity);
        ManagedType<?> managedType = m_lookup.getEntityTypeFromEntityClass(entityClass);
        if (managedType != null)
        {
            EntityReferenceLookup.SingularAttributeDetails idAttr = m_lookup.getIdentifier(managedType);
            if (idAttr != null)
            {
                Serializable id = (Serializable) idAttr.readValue(entity);
                if (id != null)
                {
                    RecordWithCommonFields rec        = Reflection.as(entity, RecordWithCommonFields.class);
                    ZonedDateTime          lastUpdate = RecordWithCommonFields.getUpdatedOnSafe(rec, false);

                    fireEventFromType(managedType, id, action, lastUpdate, reachedThroughDirectLink, null);
                }
            }
        }
    }

    private void fireEventFromType(ManagedType<?> managedType,
                                   Serializable id,
                                   DbAction action,
                                   ZonedDateTime lastUpdate,
                                   Attribute<?, ?> reachedThroughDirectLink,
                                   Attribute<?, ?> reachedThroughReverseLink)
    {
        if (lastUpdate == null)
        {
            lastUpdate = TimeUtils.now();
        }

        if (reachedThroughDirectLink != null)
        {
            m_stats.update(id, reachedThroughDirectLink, false);
        }

        if (reachedThroughReverseLink != null)
        {
            m_stats.update(id, reachedThroughReverseLink, true);
        }

        postHierarchicalEvent(managedType, id, action, lastUpdate);
    }

    void postHierarchicalEvent(Class<?> entityClass,
                               Serializable id,
                               DbAction action,
                               ZonedDateTime lastUpdate)
    {
        ManagedType<?> managedType = m_lookup.getEntityTypeFromEntityClass(entityClass);
        postHierarchicalEvent(managedType, id, action, lastUpdate);
    }

    private void postHierarchicalEvent(ManagedType<?> managedType,
                                       Serializable id,
                                       DbAction action,
                                       ZonedDateTime lastUpdate)
    {
        while (managedType instanceof EntityType)
        {
            EntityType<?> entityType = (EntityType<?>) managedType;

            postEvent(entityType.getJavaType(), id, action, lastUpdate);

            managedType = entityType.getSupertype();
        }
    }

    private void postEvent(Class<?> entityClass,
                           Serializable id,
                           DbAction action,
                           ZonedDateTime lastUpdate)
    {
        RecordIdentity ri = RecordIdentity.newInstance(entityClass, id.toString());
        ri.lastUpdate = lastUpdate;

        DbEvent event = new DbEvent();
        event.context = ri;
        event.action = action;

        String table = ri.getTable();
        String sysId = event.context.sysId;

        if (LoggerInstanceForQueue.isEnabled(Severity.DebugObnoxious))
        {
            LoggerInstanceForQueue.debugObnoxious("%s:%s - %s: %s", table, sysId, event.action, new Exception());
        }
        else
        {
            LoggerInstanceForQueue.debug("%s:%s - %s", table, sysId, event.action);
        }

        synchronized (m_lookup)
        {
            if (m_pendingEvents == null)
            {
                m_pendingEvents = Maps.newHashMap();

                Executors.scheduleOnDefaultPool(this::flushPendingEvents, 100, TimeUnit.MILLISECONDS);
            }

            String  key      = table + "/" + sysId;
            DbEvent oldEvent = m_pendingEvents.get(key);
            if (oldEvent != null)
            {
                //
                // We might generate multiple events for the same entity.
                // Only keep the most accurate one (i.e. Deletes over Updates, newer over older).
                //
                if (oldEvent.compareTo(event) <= 0)
                {
                    if (LoggerInstanceForQueue.isEnabled(Severity.Debug))
                    {
                        Duration distance = Duration.between(oldEvent.context.lastUpdate, event.context.lastUpdate);

                        LoggerInstanceForQueue.debug("%s:%s - %s skipped because of %s - Distance: %fsecs", table, sysId, event.action, oldEvent.action, distance.toNanos() / 1E9);
                    }

                    return;
                }

                if (LoggerInstanceForQueue.isEnabled(Severity.Debug))
                {
                    Duration distance = Duration.between(oldEvent.context.lastUpdate, event.context.lastUpdate);

                    LoggerInstanceForQueue.debug("%s:%s - %s replacing %s - Distance: %fsecs", table, sysId, event.action, oldEvent.action, distance.toNanos() / 1E9);
                }
            }

            m_pendingEvents.put(key, event);
        }
    }

    void drainDatabaseEvents()
    {
        flushPendingEvents();
    }

    private void flushPendingEvents()
    {
        synchronized (m_flushLock) // Grab a lock to ensure we send messages out in order.
        {
            Map<String, DbEvent> pendingEvents;

            synchronized (m_lookup)
            {
                pendingEvents = m_pendingEvents;
                m_pendingEvents = null;
            }

            if (pendingEvents != null)
            {
                LoggerInstanceForPost.debugVerbose("flushPendingEvents: Starting...");

                fireEvents(pendingEvents.values());

                LoggerInstanceForPost.debugVerbose("flushPendingEvents: Done");
            }
        }
    }

    private void fireEvents(Collection<DbEvent> events)
    {
        if (m_channel == null)
        {
            MessageBusBroker broker = m_app.getServiceNonNull(MessageBusBroker.class);
            m_channel = broker.getChannelProvider(DatabaseActivity.class);
        }

        LoggerInstanceForPost.debugVerbose("Firing %d events", events.size());

        if (LoggerInstanceForPost.isEnabled(Severity.Debug))
        {
            for (DbEvent event : events)
            {
                LoggerInstanceForPost.debug("%s:%s - %s - %s", event.context.getTable(), event.context.sysId, event.action, event.context.lastUpdate);
            }
        }

        if (m_channel != null)
        {
            m_channel.fireEvents(events);
        }
    }

    private <T2> List<Serializable> findReferencingObjects(Session session,
                                                           Object entity,
                                                           Attribute<T2, ?> attr)
    {
        ManagedType<T2> targetType = attr.getDeclaringType();
        Class<T2>       entityType = targetType.getJavaType();

        EntityReferenceLookup.SingularAttributeDetails idAttr = m_lookup.getIdentifier(targetType);
        if (idAttr == null)
        {
            return Collections.emptyList();
        }

        //
        // For Singular: SELECT x FROM X x WHERE x.<attr> == <updated record>
        // For Plural  : SELECT x FROM X x WHERE x.<attr>.contains(<updated record>)
        //
        CriteriaBuilder      cb       = session.getCriteriaBuilder();
        CriteriaQuery<Tuple> qdef     = cb.createQuery(Tuple.class);
        Root<T2>             rootPath = qdef.from(entityType);
        qdef.multiselect(rootPath.get(idAttr.getTypedAttr()));

        if (attr instanceof SingularAttribute<?, ?>)
        {
            qdef.where(cb.equal(rootPath.get(attr.getName()), entity));
        }
        else
        {
            qdef.where(cb.isMember(entity, rootPath.get(attr.getName())));
        }

        List<Serializable> res = Lists.newArrayList();
        List<Tuple> list = session.createQuery(qdef)
                                  .getResultList();
        for (Tuple o : list)
        {
            res.add((Serializable) o.get(0));
        }

        return res;
    }

    //--//

    private static String toText(Type<?> type)
    {
        return type.getJavaType()
                   .getSimpleName();
    }

    private static String toText(Object object)
    {
        return toText(SessionHolder.getClassOfEntity(object));
    }

    private static String toText(Class<?> clz)
    {
        return clz.getSimpleName();
    }
}
