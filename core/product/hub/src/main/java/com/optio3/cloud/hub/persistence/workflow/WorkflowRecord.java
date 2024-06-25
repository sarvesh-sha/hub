/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.workflow;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Tuple;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.SingularAttribute;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.cloud.hub.model.config.UsageFilterRequest;
import com.optio3.cloud.hub.model.workflow.Workflow;
import com.optio3.cloud.hub.model.workflow.WorkflowDetails;
import com.optio3.cloud.hub.model.workflow.WorkflowEventType;
import com.optio3.cloud.hub.model.workflow.WorkflowFilterRequest;
import com.optio3.cloud.hub.model.workflow.WorkflowPriority;
import com.optio3.cloud.hub.model.workflow.WorkflowStatus;
import com.optio3.cloud.hub.model.workflow.WorkflowType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.event.EventRecord;
import com.optio3.cloud.hub.persistence.message.UserMessageWorkflowRecord;
import com.optio3.cloud.model.PaginatedRecordIdentityList;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.search.Optio3QueryAnalyzerOverride;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "WORKFLOW", indexes = { @Index(name = "WORKFLOW__TYPE", columnList = "type"), @Index(name = "WORKFLOW__STATUS", columnList = "status") })
@DynamicUpdate // Due to HHH-11506
@Indexed
@Optio3QueryAnalyzerOverride("fuzzy_query")
@Optio3TableInfo(externalId = "Workflow", model = Workflow.class, metamodel = WorkflowRecord_.class)
public class WorkflowRecord extends EventRecord
{
    @Optio3ControlNotifications(reason = "Notify user of changes", direct = Optio3ControlNotifications.Notify.ALWAYS, reverse = Optio3ControlNotifications.Notify.NEVER, getter = "getCreatedBy")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getCreatedBy")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "createdBy", nullable = false, foreignKey = @ForeignKey(name = "WORKFLOW__CREATEDBY__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private UserRecord createdBy;

    @Optio3ControlNotifications(reason = "Notify user of changes", direct = Optio3ControlNotifications.Notify.ALWAYS, reverse = Optio3ControlNotifications.Notify.NEVER, getter = "getAssignedTo")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getAssignedTo", setter = "setAssignedTo")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "assignedTo", foreignKey = @ForeignKey(name = "WORKFLOW__ASSIGNEDTO__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private UserRecord assignedTo;

    //--//

    @Optio3UpgradeValue("active")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkflowStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private WorkflowType type;

    @Column(name = "priority")
    private int priority;

    //--//

    @Lob
    @Column(name = "details")
    private byte[] details;

    @Transient
    private final PersistAsJsonHelper<byte[], WorkflowDetails> m_detailsParser = new PersistAsJsonHelper<>(() -> details,
                                                                                                           (val) -> details = val,
                                                                                                           byte[].class,
                                                                                                           WorkflowDetails.class,
                                                                                                           ObjectMappers.SkipNulls);

    //--//

    @OneToMany(mappedBy = "workflow", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<WorkflowHistoryRecord> history;

    //--//

    public static WorkflowRecord newInstance(RecordHelper<WorkflowRecord> helper,
                                             UserRecord rec_user,
                                             AssetRecord rec_asset,
                                             WorkflowDetails details)
    {
        WorkflowRecord res = EventRecord.newInstance(helper, null, rec_asset);
        res.status    = WorkflowStatus.Active;
        res.type      = details.resolveToType();
        res.createdBy = rec_user;
        res.setDetails(details);

        return res;
    }

    public static int checkUsages(SessionProvider sessionProvider,
                                  UsageFilterRequest filters,
                                  TypedRecordIdentityList<WorkflowRecord> lst)
    {
        return filters.analyzeRecords(sessionProvider, lst, WorkflowRecord.class, (qh) ->
        {
            qh.addObject(WorkflowRecord_.details, byte[].class, (obj, val) ->
            {
                try
                {
                    var details = ObjectMappers.deserializeFromGzip(ObjectMappers.SkipNulls, val, WorkflowDetails.class);
                    obj.value1 = ObjectMappers.SkipNulls.writeValueAsString(details);
                }
                catch (Exception e)
                {
                    // Ignore failures.
                }
            });
        });
    }

    //--//

    public WorkflowHistoryRecord markAsProcessed(SessionHolder sessionHolder,
                                                 UserRecord rec_user)
    {
        switch (getStatus())
        {
            case Active:
            {
                WorkflowHistoryRecord rec = addHistoryEntry(sessionHolder, rec_user, WorkflowEventType.closed, "Workflow automatically approved");
                notifyUser(sessionHolder, rec_user, "Workflow resolved.");
                return rec;
            }

            case Disabling:
            {
                WorkflowHistoryRecord rec = addHistoryEntry(sessionHolder, rec_user, WorkflowEventType.disabled, "Workflow automatically disabled");
                notifyUser(sessionHolder, rec_user, "Workflow disabled.");
                return rec;
            }
        }
        return null;
    }

    public WorkflowHistoryRecord addHistoryEntry(SessionHolder sessionHolder,
                                                 UserRecord rec_user,
                                                 WorkflowEventType type,
                                                 String fmt,
                                                 Object... args)
    {
        RecordHelper<WorkflowHistoryRecord> helper = sessionHolder.createHelper(WorkflowHistoryRecord.class);

        return addHistoryEntry(helper, rec_user, type, fmt, args);
    }

    public WorkflowHistoryRecord addHistoryEntry(RecordHelper<WorkflowHistoryRecord> helper,
                                                 UserRecord rec_user,
                                                 WorkflowEventType type,
                                                 String fmt,
                                                 Object... args)
    {
        requireNonNull(fmt);

        WorkflowHistoryRecord rec_history = WorkflowHistoryRecord.newInstance(this, rec_user, type);

        rec_history.setText(String.format(fmt, args));

        helper.persist(rec_history);

        if (type == WorkflowEventType.closed)
        {
            setStatus(WorkflowStatus.Closed);
        }

        if (type == WorkflowEventType.disabled)
        {
            setStatus(WorkflowStatus.Disabled);
        }

        return rec_history;
    }

    public void updateLastHistoryText(RecordHelper<WorkflowHistoryRecord> helper,
                                      String text)
    {
        requireNonNull(text);

        RecordIdentity ri = CollectionUtils.firstElement(WorkflowHistoryRecord.listSorted(helper, this, null, null));

        WorkflowHistoryRecord rec_history = helper.getOrNull(ri != null ? ri.sysId : null);
        if (rec_history != null)
        {
            rec_history.setText(text);
        }
    }

    //--//

    public UserRecord getCreatedBy()
    {
        return createdBy;
    }

    public UserRecord getAssignedTo()
    {
        return assignedTo;
    }

    public void setAssignedTo(UserRecord assignedTo)
    {
        if (this.assignedTo != assignedTo)
        {
            this.assignedTo = assignedTo;
        }
    }

    public void assignToUser(SessionHolder sessionHolder,
                             UserRecord rec_user)
    {
        notifyUser(sessionHolder, rec_user, "Workflow assigned to you");

        if (rec_user != createdBy)
        {
            notifyUser(sessionHolder, createdBy, "Workflow has been assigned to fulfiller");
        }

        setAssignedTo(rec_user);
    }

    private void notifyUser(SessionHolder sessionHolder,
                            UserRecord rec_user,
                            String subject)
    {
        UserMessageWorkflowRecord rec_message = UserMessageWorkflowRecord.newInstance(rec_user, this);
        rec_message.setSubject(subject);
        rec_message.persist(sessionHolder);
    }

    //--//

    public WorkflowStatus getStatus()
    {
        return status;
    }

    public void setStatus(WorkflowStatus status)
    {
        this.status = requireNonNull(status);
    }

    public void updateStatus(RecordHelper<WorkflowHistoryRecord> helper,
                             UserRecord rec_user,
                             WorkflowStatus status,
                             String description)
    {
        switch (this.status)
        {
            case Active:
                switch (status)
                {
                    case Resolved:
                        addHistoryEntry(helper, rec_user, WorkflowEventType.resolved, BoxingUtils.get(description, WorkflowEventType.resolved.getDescription()));
                        this.status = WorkflowStatus.Resolved;
                        break;

                    case Closed:
                        addHistoryEntry(helper, rec_user, WorkflowEventType.closed, BoxingUtils.get(description, WorkflowEventType.closed.getDescription()));
                        this.status = WorkflowStatus.Closed;
                        break;
                }
                break;

            case Resolved:
                switch (status)
                {
                    case Active:
                        addHistoryEntry(helper, rec_user, WorkflowEventType.reopened, BoxingUtils.get(description, WorkflowEventType.reopened.getDescription()));
                        this.status = WorkflowStatus.Active;
                        break;

                    case Closed:
                        addHistoryEntry(helper, rec_user, WorkflowEventType.closed, BoxingUtils.get(description, WorkflowEventType.closed.getDescription()));
                        this.status = WorkflowStatus.Closed;
                        break;
                }
                break;

            case Closed:
                switch (status)
                {
                    case Active:
                        addHistoryEntry(helper, rec_user, WorkflowEventType.reopened, BoxingUtils.get(description, WorkflowEventType.reopened.getDescription()));
                        this.status = WorkflowStatus.Active;
                        break;

                    case Disabling:
                        addHistoryEntry(helper, rec_user, WorkflowEventType.disabling, BoxingUtils.get(description, WorkflowEventType.disabling.getDescription()));
                        this.status = WorkflowStatus.Disabling;
                        break;
                }
                break;

            case Disabling:
                switch (status)
                {
                    case Disabled:
                        addHistoryEntry(helper, rec_user, WorkflowEventType.disabled, BoxingUtils.get(description, WorkflowEventType.disabled.getDescription()));
                        this.status = WorkflowStatus.Disabled;
                        break;
                }

            case Disabled:
                switch (status)
                {
                    case Active:
                        addHistoryEntry(helper, rec_user, WorkflowEventType.reopened, BoxingUtils.get(description, WorkflowEventType.reopened.getDescription()));
                        this.status = status;
                        break;
                }
        }
    }

    public void updateExtendedDescription(RecordHelper<WorkflowHistoryRecord> helper,
                                          UserRecord rec_user,
                                          String oldExtendedDescription,
                                          String newExtendedDescription)
    {
        if (!StringUtils.equals(oldExtendedDescription, newExtendedDescription))
        {
            addHistoryEntry(helper, rec_user, WorkflowEventType.updatedWithNotes, "Workflow updated with notes");
        }
    }

    public WorkflowType getType()
    {
        return type;
    }

    public void setType(WorkflowType type)
    {
        this.type = requireNonNull(type);
    }

    public WorkflowPriority getPriority()
    {
        return WorkflowPriority.parse(priority);
    }

    public void setPriority(WorkflowPriority priority)
    {
        this.priority = requireNonNull(priority).getLevel();
    }

    //--//

    public WorkflowDetails getDetails()
    {
        return m_detailsParser.get();
    }

    public boolean setDetails(WorkflowDetails details)
    {
        return m_detailsParser.set(details);
    }

    //--//

    public List<WorkflowHistoryRecord> getHistory()
    {
        return CollectionUtils.asEmptyCollectionIfNull(history);
    }

    //--//

    private static class JoinHelper<T> extends EventRecord.JoinHelper<T, WorkflowRecord>
    {
        JoinHelper(RecordHelper<WorkflowRecord> helper,
                   Class<T> clz)
        {
            super(helper, clz);
        }

        //--//

        void applyFilters(WorkflowFilterRequest filters)
        {
            super.applyFilters(filters);

            if (filters.hasStatus())
            {
                filterByStatus(filters.workflowStatusIDs);
            }

            if (filters.hasTypes())
            {
                filterByTypes(filters.workflowTypeIDs);
            }

            if (filters.hasPriorities())
            {
                filterByPriorities(filters.workflowPriorityIDs);
            }

            if (filters.hasCreatedBy())
            {
                filterByCreatedBy(filters.createdByIDs);
            }

            if (filters.hasAssignedTo())
            {
                filterByAssignedTo(filters.assignedToIDs);
            }

            List<ParsedLike> likeFilters = ParsedLike.decode(filters.likeFilter);
            if (likeFilters != null)
            {
                addWhereClause(predicateForLike(likeFilters));
            }
        }

        protected Predicate predicateForLike(List<ParsedLike> likeFilters)
        {
            return predicateForLike(root, WorkflowRecord_.description, likeFilters);
        }

        @Override
        protected void handleSortCriteria(SortCriteria sort)
        {
            switch (sort.column)
            {
                case "status":
                {
                    addOrderBy(root, WorkflowRecord_.status, sort.ascending);
                    break;
                }

                case "type":
                {
                    addOrderBy(root, WorkflowRecord_.type, sort.ascending);
                    break;
                }

                case "priority":
                {
                    addOrderBy(root, WorkflowRecord_.priority, sort.ascending);
                    break;
                }

                case "createdBy":
                {
                    addUserSortExtension(WorkflowRecord_.createdBy, sort.ascending);
                    break;
                }

                case "assignedTo":
                {
                    addUserSortExtension(WorkflowRecord_.assignedTo, sort.ascending);
                    break;
                }

                default:
                    super.handleSortCriteria(sort);
            }
        }

        private void addUserSortExtension(SingularAttribute<WorkflowRecord, UserRecord> attribute,
                                          boolean ascending)
        {
            addSortExtension(new SortExtension<UserRecord>()
            {
                private Multimap<UserRecord, RecordIdentity> m_map = ArrayListMultimap.create();

                @Override
                public Path<UserRecord> getPath()
                {
                    return root.get(attribute);
                }

                @Override
                public void processValue(RecordIdentity ri,
                                         UserRecord rec_user)
                {
                    m_map.put(rec_user, ri);
                }

                @Override
                public void processResults(List<RecordIdentity> results)
                {
                    results.clear();

                    List<UserRecord> users = Lists.newArrayList(m_map.keySet());
                    users.sort((l, r) ->
                               {
                                   int diff = StringUtils.compareIgnoreCase(l.getFirstName(), r.getFirstName());

                                   if (diff == 0)
                                   {
                                       diff = StringUtils.compareIgnoreCase(l.getLastName(), r.getLastName());
                                   }

                                   return ascending ? diff : -diff;
                               });

                    for (UserRecord rec_user : users)
                    {
                        results.addAll(m_map.get(rec_user));
                    }
                }
            });
        }

        //--//

        void filterByStatus(List<WorkflowStatus> lst)
        {
            addWhereClauseIn(root, WorkflowRecord_.status, lst);
        }

        void filterByTypes(List<WorkflowType> lst)
        {
            addWhereClauseIn(root, WorkflowRecord_.type, lst);
        }

        void filterByPriorities(List<WorkflowPriority> lst)
        {
            addWhereClauseIn(root, WorkflowRecord_.priority, CollectionUtils.transformToList(lst, WorkflowPriority::getLevel));
        }

        void filterByCreatedBy(List<String> createdByIDs)
        {
            addWhereReferencingSysIds(root, WorkflowRecord_.createdBy, createdByIDs);
        }

        void filterByAssignedTo(List<String> assignedToIDs)
        {
            RecordHelper<UserRecord> userHelper = helper.wrapFor(UserRecord.class);
            List<UserRecord>         users      = QueryHelperWithCommonFields.getBatch(userHelper, assignedToIDs);
            addWhereClauseIn(root, WorkflowRecord_.assignedTo, users);
        }
    }

    //--//

    public static PaginatedRecordIdentityList filter(RecordHelper<WorkflowRecord> helper,
                                                     WorkflowFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        //--//

        return JoinHelper.returnFilterTuples(helper, jh);
    }

    public static long count(RecordHelper<WorkflowRecord> helper,
                             WorkflowFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return jh.count();
    }

    public static Map<String, Number> countWorkflowsByLocation(RecordHelper<WorkflowRecord> helper,
                                                               WorkflowFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            filters.sortBy = null; // Just in case, sanitize input.

            jh.applyFilters(filters);
        }

        return jh.countByField(jh.joinWithLocation(), RecordWithCommonFields_.sysId);
    }

    public static Map<WorkflowType, Number> countWorkflowsByType(RecordHelper<WorkflowRecord> helper,
                                                                 WorkflowFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            filters.sortBy = null; // Just in case, sanitize input.

            jh.applyFilters(filters);
        }

        return jh.countByField(jh.root, WorkflowRecord_.type);
    }

    public static Map<WorkflowPriority, Number> countWorkflowsByPriority(RecordHelper<WorkflowRecord> helper,
                                                                         WorkflowFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            filters.sortBy = null; // Just in case, sanitize input.

            jh.applyFilters(filters);
        }

        Map<Integer, Number>          map = jh.countByField(jh.root, WorkflowRecord_.priority);
        Map<WorkflowPriority, Number> res = Maps.newHashMap();

        for (Integer val : map.keySet())
        {
            Number count = map.get(val);

            res.put(WorkflowPriority.parse(val), count);
        }

        return res;
    }
}
