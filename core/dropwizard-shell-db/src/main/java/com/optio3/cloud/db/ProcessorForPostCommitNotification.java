/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.db;

import java.util.Map;

import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.persistence.PostCommitNotificationReason;
import com.optio3.cloud.persistence.PostCommitNotificationState;
import com.optio3.cloud.persistence.PostCommitNotificationStateField;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.interceptor.AbstractLazyLoadInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.ServiceRegistryImplementor;

class ProcessorForPostCommitNotification
{
    private final AbstractApplicationWithDatabase<?> m_app;
    private final String                             m_databaseId;
    private final EntityReferenceLookup              m_lookup;
    private final MetamodelImplementor               m_metamodel;

    ProcessorForPostCommitNotification(AbstractApplicationWithDatabase<?> app,
                                       String databaseId,
                                       SessionFactoryImplementor sessionFactory,
                                       EntityReferenceLookup lookup)
    {
        m_app = app;
        m_databaseId = databaseId;
        m_metamodel = sessionFactory.getMetamodel();
        m_lookup = lookup;

        ServiceRegistryImplementor  serviceRegistry  = sessionFactory.getServiceRegistry();
        final EventListenerRegistry listenerRegistry = serviceRegistry.getService(EventListenerRegistry.class);

        //
        // In order for all the change notifications to work, we need to load all the lazy fields for updated or deleted entities.
        //
        listenerRegistry.prependListeners(EventType.FLUSH, new FlushEventListener()
        {
            @Override
            public void onFlush(FlushEvent event) throws
                                                  HibernateException
            {
                PersistenceContext ctx = event.getSession()
                                              .getPersistenceContext();

                for (Map.Entry<Object, EntityEntry> pair : ctx.reentrantSafeEntityEntries())
                {
                    Object      entity = pair.getKey();
                    EntityEntry entry  = pair.getValue();

                    Status status = entry.getStatus();
                    if (status == Status.DELETED)
                    {
                        loadAllLazyControlAttributes(entity);
                    }
                    else if (status == Status.MANAGED)
                    {
                        SelfDirtinessTracker tracker = Reflection.as(entity, SelfDirtinessTracker.class);
                        if (tracker != null && tracker.$$_hibernate_hasDirtyAttributes())
                        {
                            loadAllLazyControlAttributes(entity);
                        }
                    }
                }
            }
        });

        listenerRegistry.prependListeners(EventType.POST_COMMIT_INSERT, new PostCommitInsertEventListener()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean requiresPostCommitHanding(EntityPersister persister)
            {
                return true;
            }

            @Override
            public void onPostInsert(PostInsertEvent event)
            {
                EntityPersister persister = event.getPersister();
                String[]        names     = persister.getPropertyNames();
                Object[]        newState  = event.getState();

                PostCommitNotificationState notifyState = new PostCommitNotificationState();
                notifyState.fields = new PostCommitNotificationStateField[names.length];
                for (int i = 0; i < names.length; i++)
                {
                    PostCommitNotificationStateField field = new PostCommitNotificationStateField();
                    field.name = names[i];
                    field.currentValue = newState[i];

                    notifyState.fields[i] = field;
                }

                m_app.notifyPostCommit(m_databaseId, event.getEntity(), event.getId(), PostCommitNotificationReason.INSERT, notifyState);
            }

            @Override
            public void onPostInsertCommitFailed(PostInsertEvent event)
            {
            }
        });

        listenerRegistry.prependListeners(EventType.POST_COMMIT_UPDATE, new PostCommitUpdateEventListener()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean requiresPostCommitHanding(EntityPersister persister)
            {
                return true;
            }

            @Override
            public void onPostUpdate(PostUpdateEvent event)
            {
                EntityPersister persister = event.getPersister();
                String[]        names     = persister.getPropertyNames();
                Object[]        oldState  = event.getOldState();
                Object[]        newState  = event.getState();

                PostCommitNotificationState notifyState = new PostCommitNotificationState();
                notifyState.fields = new PostCommitNotificationStateField[names.length];
                for (int i = 0; i < names.length; i++)
                {
                    PostCommitNotificationStateField field = new PostCommitNotificationStateField();
                    field.name = names[i];
                    field.currentValue = newState[i];
                    field.previousValue = oldState != null ? oldState[i] : null;

                    notifyState.fields[i] = field;
                }

                for (int dirty : event.getDirtyProperties())
                {
                    PostCommitNotificationStateField field = notifyState.fields[dirty];
                    field.dirty = true;
                }

                m_app.notifyPostCommit(m_databaseId, event.getEntity(), event.getId(), PostCommitNotificationReason.UPDATE, notifyState);
            }

            @Override
            public void onPostUpdateCommitFailed(PostUpdateEvent event)
            {
            }
        });

        listenerRegistry.prependListeners(EventType.POST_COMMIT_DELETE, new PostCommitDeleteEventListener()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onPostDelete(PostDeleteEvent event)
            {
                EntityPersister persister = event.getPersister();
                String[]        names     = persister.getPropertyNames();
                Object[]        oldState  = event.getDeletedState();

                PostCommitNotificationState notifyState = new PostCommitNotificationState();
                notifyState.fields = new PostCommitNotificationStateField[names.length];
                for (int i = 0; i < names.length; i++)
                {
                    PostCommitNotificationStateField field = new PostCommitNotificationStateField();
                    field.name = names[i];
                    field.previousValue = oldState != null ? oldState[i] : null;

                    notifyState.fields[i] = field;
                }

                m_app.notifyPostCommit(m_databaseId, event.getEntity(), event.getId(), PostCommitNotificationReason.DELETE, notifyState);
            }

            @Override
            public boolean requiresPostCommitHanding(EntityPersister persister)
            {
                return true;
            }

            @Override
            public void onPostDeleteCommitFailed(PostDeleteEvent event)
            {
            }
        });

        listenerRegistry.prependListeners(EventType.POST_COLLECTION_UPDATE, new PostCollectionUpdateEventListener()
        {
            @Override
            public void onPostUpdateCollection(PostCollectionUpdateEvent event)
            {
                PersistentCollection coll  = event.getCollection();
                Object               owner = coll.getOwner();
                String               role  = coll.getRole();

                if (owner != null && role != null)
                {
                    int pos = role.lastIndexOf('.');
                    if (pos > 0)
                    {
                        CollectionPersister persister = m_metamodel.collectionPersister(role);

                        PostCommitNotificationStateField field = new PostCommitNotificationStateField();
                        field.name = role.substring(pos + 1);
                        field.dirty = true;
                        field.previousValue = coll.getStoredSnapshot();
                        field.currentValue = coll.getSnapshot(persister);

                        PostCommitNotificationState state = new PostCommitNotificationState();
                        state.fields = new PostCommitNotificationStateField[1];
                        state.fields[0] = field;

                        m_app.notifyPostCommit(m_databaseId, owner, coll.getKey(), PostCommitNotificationReason.UPDATE, state);
                    }
                }
            }
        });
    }

    private void loadAllLazyControlAttributes(Object entity)
    {
        PersistentAttributeInterceptable interceptable = Reflection.as(entity, PersistentAttributeInterceptable.class);
        if (interceptable != null)
        {
            EntityReferenceLookup.EntityDetails entityDetails = m_lookup.fromEntityToDirectAssociations(SessionHolder.getClassOfEntity(entity));
            for (EntityReferenceLookup.AttributeDetails details : entityDetails.fields)
            {
                if (details.isId)
                {
                    continue;
                }

                if (details instanceof EntityReferenceLookup.PluralAttributeDetails)
                {
                    //
                    // No need to load collections.
                    // Also, if we tried to load them, they would be flagged as 'unreachable', because we don't write back into the entities.
                    // That would cause them to be deleted.
                    //
                    continue;
                }

                switch (details.controlDirect)
                {
                    case ALWAYS:
                    case ON_ASSOCIATION_CHANGES:
                        AbstractLazyLoadInterceptor interceptor = Reflection.as(interceptable.$$_hibernate_getInterceptor(), AbstractLazyLoadInterceptor.class);
                        if (interceptor != null)
                        {
                            interceptor.readObject(entity, details.attributeName, null);
                        }
                        break;
                }
            }
        }
    }
}
