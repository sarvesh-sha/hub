/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3Cascade.Flavor;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.scheduler.BackgroundActivity;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.model.scheduler.BackgroundActivityFilterRequest;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.util.function.FunctionWithException;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "BACKGROUND_ACTIVITY",
       indexes = { @Index(name = "BACKGROUND_ACTIVITY__NEXTACTIVATION", columnList = "next_activation"), @Index(name = "BACKGROUND_ACTIVITY__HANDLER_KEY", columnList = "handler_key") })
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "BackgroundActivity", model = BackgroundActivity.class, metamodel = BackgroundActivityRecord_.class, defragmentOnBoot = true)
public class BackgroundActivityRecord extends RecordForBackgroundActivity<BackgroundActivityRecord, BackgroundActivityChunkRecord, HostRecord> implements ModelMapperTarget<BackgroundActivity, BackgroundActivityRecord_>
{
    /**
     * List of all the various instances belonging to this customer service role.
     */
    @Optio3ControlNotifications(reason = "Report changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getSubActivities")
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "TASK_TO_SUBTASK", joinColumns = @JoinColumn(name = "task"), inverseJoinColumns = @JoinColumn(name = "sub_task"))
    private Set<BackgroundActivityRecord> subActivities = Sets.newHashSet();

    /**
     * The roles that are associated with this host.
     */
    @ManyToMany(mappedBy = "subActivities", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private Set<BackgroundActivityRecord> waitingActivities = Sets.newHashSet();

    //--//

    @Optio3ControlNotifications(reason = "Don't report changes", direct = Notify.NEVER, reverse = Notify.NEVER)
    @Optio3Cascade(mode = Flavor.DELETE, getter = "getHostAffinity")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "host_affinity", foreignKey = @ForeignKey(name = "HOST_AFFINITY__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private HostRecord hostAffinity;

    //--//

    public BackgroundActivityRecord()
    {
    }

    @Override
    public Set<BackgroundActivityRecord> getWaitingActivities()
    {
        return waitingActivities;
    }

    @Override
    public Set<BackgroundActivityRecord> getSubActivities()
    {
        return subActivities;
    }

    // Due to Java's Type Erasure, the overridden methods don't carry the proper types. We need to create new methods.
    public Set<BackgroundActivityRecord> getWaitingActivitiesForModel()
    {
        return getWaitingActivities();
    }

    // Due to Java's Type Erasure, the overridden methods don't carry the proper types. We need to create new methods.
    public Set<BackgroundActivityRecord> getSubActivitiesForModel()
    {
        return getSubActivities();
    }

    //--//

    @Override
    public HostRecord getWorker()
    {
        return getHostAffinity();
    }

    @Override
    public void setWorker(HostRecord worker)
    {
        setHostAffinity(worker);
    }

    public HostRecord getHostAffinity()
    {
        return hostAffinity;
    }

    public void setHostAffinity(HostRecord hostAffinity)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (this.hostAffinity != hostAffinity)
        {
            this.hostAffinity = hostAffinity;
        }
    }

    public static BackgroundActivityRecord schedule(SessionHolder sessionHolder,
                                                    BackgroundActivityHandler<BackgroundActivityRecord, BackgroundActivityChunkRecord, HostRecord> handler,
                                                    ZonedDateTime nextActivation) throws
                                                                                  Exception
    {
        return newInstance(sessionHolder, handler, nextActivation, BackgroundActivityRecord.class);
    }

    public static RecordLocator<BackgroundActivityRecord> wrapTask(SessionProvider sessionProvider,
                                                                   FunctionWithException<SessionHolder, BackgroundActivityRecord> callback) throws
                                                                                                                                            Exception
    {
        try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
        {
            BackgroundActivityRecord rec_task = callback.apply(sessionHolder);

            sessionHolder.commit();

            return sessionHolder.createLocator(rec_task);
        }
    }

    //--//

    public static ZonedDateTime findNextActivation(RecordHelper<BackgroundActivityRecord> helper,
                                                   HostRecord hostAffinity)
    {
        return RecordForBackgroundActivity.findNextActivation(helper, (jh) ->
        {
            addHostAffinity(jh, hostAffinity);
        });
    }

    public static <H extends BackgroundActivityHandler<BackgroundActivityRecord, BackgroundActivityChunkRecord, HostRecord>> TypedRecordIdentityList<BackgroundActivityRecord> findHandlers(SessionHolder sessionHolder,
                                                                                                                                                                                            boolean onlyDone,
                                                                                                                                                                                            boolean onlyNotDone,
                                                                                                                                                                                            Class<H> clzHandler,
                                                                                                                                                                                            RecordLocator<?> handlerContext)
    {
        return findHandlers(sessionHolder, onlyDone, onlyNotDone, BackgroundActivityRecord.class, clzHandler, handlerContext);
    }

    public static TypedRecordIdentityList<BackgroundActivityRecord> list(RecordHelper<BackgroundActivityRecord> helper,
                                                                         HostRecord hostAffinity,
                                                                         BackgroundActivityFilterRequest filters)
    {
        return RecordForBackgroundActivity.list(helper, filters, (jh) ->
        {
            addHostAffinity(jh, hostAffinity);
        });
    }

    public static List<BackgroundActivityRecord> getBatch(RecordHelper<BackgroundActivityRecord> helper,
                                                          List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public void checkRemoveConditions(ValidationResultsHolder validation)
    {
        if (!getStatus().isDone())
        {
            validation.addFailure("status", "Activity is still running");
        }
    }

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<BackgroundActivityRecord> helper)
    {
        checkRemoveConditions(validation);

        if (validation.canProceed())
        {
            helper.delete(this);
        }
    }

    //--//

    private static void addHostAffinity(QueryHelperWithCommonFields<?, BackgroundActivityRecord> jh,
                                        HostRecord hostAffinity)
    {
        if (hostAffinity != null)
        {
            jh.addWhereClauseWithEqual(jh.root, BackgroundActivityRecord_.hostAffinity, hostAffinity);
        }
    }
}
