/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.alert;

import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;
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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Tuple;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;

import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.cloud.hub.model.alert.Alert;
import com.optio3.cloud.hub.model.alert.AlertEventLevel;
import com.optio3.cloud.hub.model.alert.AlertEventType;
import com.optio3.cloud.hub.model.alert.AlertFilterRequest;
import com.optio3.cloud.hub.model.alert.AlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertStatus;
import com.optio3.cloud.hub.model.alert.AlertType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.event.EventRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.PaginatedRecordIdentityList;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.search.Optio3QueryAnalyzerOverride;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "ALERT", indexes = { @Index(name = "ALERT__TYPE", columnList = "type"), @Index(name = "ALERT__STATUS", columnList = "status") })
@DynamicUpdate // Due to HHH-11506
@Indexed
@Optio3QueryAnalyzerOverride("fuzzy_query")
@Optio3TableInfo(externalId = "Alert", model = Alert.class, metamodel = AlertRecord_.class)
public class AlertRecord extends EventRecord
{
    @Optio3ControlNotifications(reason = "Don't notify changes", direct = Notify.NEVER, reverse = Notify.NEVER)
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "alert_definition_version", nullable = true, foreignKey = @ForeignKey(name = "ALERT__ALERT_DEFINITION_VERSION__FK"))
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getAlertDefinitionVersion", setter = "setAlertDefinitionVersion")
    private AlertDefinitionVersionRecord alertDefinitionVersion;

    //--//

    @Optio3UpgradeValue("active")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AlertStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AlertType type;

    @Column(name = "severity")
    private int severity;

    //--//

    @OneToMany(mappedBy = "alert", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<AlertHistoryRecord> history;

    @Transient
    private AlertHistoryRecord m_lastHistory;

    //--//

    public static AlertRecord newInstance(RecordHelper<AlertRecord> helper,
                                          Integer sequenceNumber,
                                          AlertDefinitionVersionRecord rec_def,
                                          AssetRecord rec_asset,
                                          AlertType type)
    {
        requireNonNull(rec_asset);

        AlertRecord res = EventRecord.newInstance(helper, sequenceNumber, rec_asset);
        res.alertDefinitionVersion = rec_def;
        res.status                 = AlertStatus.active;
        res.type                   = type;

        return res;
    }

    public static AlertRecord newInstance(RecordHelper<AlertRecord> helper,
                                          Integer sequenceNumber,
                                          AlertDefinitionVersionRecord rec_def,
                                          LocationRecord rec_location,
                                          AlertType type)
    {
        requireNonNull(rec_location);

        AlertRecord res = EventRecord.newInstance(helper, sequenceNumber, rec_location);
        res.alertDefinitionVersion = rec_def;
        res.type                   = type;

        return res;
    }

    //--//

    public AlertHistoryRecord addHistoryEntry(SessionHolder sessionHolder,
                                              ZonedDateTime timestamp,
                                              AlertEventLevel level,
                                              AlertEventType type,
                                              String fmt,
                                              Object... args)
    {
        RecordHelper<AlertHistoryRecord> helper = sessionHolder.createHelper(AlertHistoryRecord.class);

        return addHistoryEntry(helper, timestamp, level, type, fmt, args);
    }

    public AlertHistoryRecord addHistoryEntry(RecordHelper<AlertHistoryRecord> helper,
                                              ZonedDateTime timestamp,
                                              AlertEventLevel level,
                                              AlertEventType type,
                                              String fmt,
                                              Object... args)
    {
        requireNonNull(fmt);

        AlertHistoryRecord rec_history = AlertHistoryRecord.newInstance(this, level, type);

        rec_history.setText(String.format(fmt, args));

        if (timestamp != null)
        {
            rec_history.setCreatedOn(timestamp);
            rec_history.setUpdatedOn(timestamp);
        }

        helper.persist(rec_history);

        if (type == AlertEventType.closed)
        {
            setStatus(AlertStatus.closed);
        }

        m_lastHistory = rec_history;

        return rec_history;
    }

    public void updateLastHistoryText(RecordHelper<AlertHistoryRecord> helper,
                                      String text)
    {
        requireNonNull(text);

        var rec_history = getLastHistory(helper);
        if (rec_history != null)
        {
            rec_history.setText(text);
        }
    }

    public AlertHistoryRecord getLastHistory(RecordHelper<AlertHistoryRecord> helper)
    {
        if (m_lastHistory == null)
        {
            RecordIdentity ri = CollectionUtils.firstElement(AlertHistoryRecord.listSorted(helper, this, null, null, 1));

            m_lastHistory = helper.getOrNull(ri != null ? ri.sysId : null);
        }

        return m_lastHistory;
    }

    //--//

    public AlertDefinitionVersionRecord getAlertDefinitionVersion()
    {
        return alertDefinitionVersion;
    }

    public void setAlertDefinitionVersion(AlertDefinitionVersionRecord alertDefinitionVersion)
    {
        if (this.alertDefinitionVersion != alertDefinitionVersion)
        {
            this.alertDefinitionVersion = alertDefinitionVersion;
        }
    }

    public AlertStatus getStatus()
    {
        return status;
    }

    public void setStatus(AlertStatus status)
    {
        this.status = requireNonNull(status);
    }

    public void updateStatus(RecordHelper<AlertHistoryRecord> helper,
                             ZonedDateTime timestamp,
                             AlertStatus status,
                             String statusText)
    {
        switch (this.status)
        {
            case active:
                switch (status)
                {
                    case muted:
                        addHistoryEntry(helper, timestamp, AlertEventLevel.info, AlertEventType.muted, BoxingUtils.get(statusText, "Alert muted"));
                        this.status = AlertStatus.muted;
                        break;

                    case resolved:
                        addHistoryEntry(helper, timestamp, AlertEventLevel.info, AlertEventType.resolved, BoxingUtils.get(statusText, "Alert resolved"));
                        this.status = AlertStatus.resolved;
                        break;

                    case closed:
                        addHistoryEntry(helper, timestamp, AlertEventLevel.info, AlertEventType.closed, BoxingUtils.get(statusText, "Alert closed"));
                        this.status = AlertStatus.closed;
                        break;
                }
                break;

            case muted:
                switch (status)
                {
                    case active:
                        addHistoryEntry(helper, timestamp, AlertEventLevel.info, AlertEventType.unmuted, BoxingUtils.get(statusText, "Alert unmuted"));
                        this.status = AlertStatus.active;
                        break;

                    case resolved:
                        addHistoryEntry(helper, timestamp, AlertEventLevel.info, AlertEventType.closed, BoxingUtils.get(statusText, "Alert resolved"));
                        this.status = AlertStatus.resolved;
                        break;

                    case closed:
                        addHistoryEntry(helper, timestamp, AlertEventLevel.info, AlertEventType.closed, BoxingUtils.get(statusText, "Alert closed"));
                        this.status = AlertStatus.closed;
                        break;
                }
                break;

            case resolved:
                switch (status)
                {
                    case active:
                        addHistoryEntry(helper, timestamp, AlertEventLevel.failure, AlertEventType.reopened, BoxingUtils.get(statusText, "Alert reactivated"));
                        this.status = AlertStatus.active;
                        break;

                    case closed:
                        addHistoryEntry(helper, timestamp, AlertEventLevel.info, AlertEventType.closed, BoxingUtils.get(statusText, "Alert closed"));
                        this.status = AlertStatus.closed;
                        break;
                }
                break;

            case closed:
                switch (status)
                {
                    case active:
                        addHistoryEntry(helper, timestamp, AlertEventLevel.failure, AlertEventType.reopened, BoxingUtils.get(statusText, "Alert reactivated"));
                        this.status = AlertStatus.active;
                        break;
                }
                break;
        }
    }

    public AlertType getType()
    {
        return type;
    }

    public void setType(AlertType type)
    {
        this.type = requireNonNull(type);
    }

    public AlertSeverity getSeverity()
    {
        return AlertSeverity.parse(severity);
    }

    public void setSeverity(AlertSeverity severity)
    {
        this.severity = requireNonNull(severity).getLevel();
    }

    public List<AlertHistoryRecord> getHistory()
    {
        return CollectionUtils.asEmptyCollectionIfNull(history);
    }

    //--//

    private static class JoinHelper<T> extends EventRecord.JoinHelper<T, AlertRecord>
    {
        Join<AlertRecord, AlertDefinitionVersionRecord> rootAlertDefinitionVersion;

        JoinHelper(RecordHelper<AlertRecord> helper,
                   Class<T> clz)
        {
            super(helper, clz);
        }

        public Join<AlertRecord, AlertDefinitionVersionRecord> joinWithAlertDefinitionVersion()
        {
            if (rootAlertDefinitionVersion == null)
            {
                rootAlertDefinitionVersion = root.join(AlertRecord_.alertDefinitionVersion);
            }

            return rootAlertDefinitionVersion;
        }

        //--//

        void applyFilters(AlertFilterRequest filters)
        {
            super.applyFilters(filters);

            if (filters.hasStatus())
            {
                filterByStatus(filters.alertStatusIDs);
            }

            if (filters.hasTypes())
            {
                filterByTypes(filters.alertTypeIDs);
            }

            if (filters.hasSeverities())
            {
                filterBySeverity(filters.alertSeverityIDs);
            }

            if (filters.hasRules())
            {
                filterByRules(filters.alertRules);
            }
        }

        @Override
        protected void handleSortCriteria(SortCriteria sort)
        {
            switch (sort.column)
            {
                case "status":
                {
                    addOrderBy(root, AlertRecord_.status, sort.ascending);
                    break;
                }

                case "type":
                {
                    addOrderBy(root, AlertRecord_.type, sort.ascending);
                    break;
                }

                case "severity":
                {
                    addOrderBy(root, AlertRecord_.severity, sort.ascending);
                    break;
                }

                default:
                    super.handleSortCriteria(sort);
            }
        }

        //--//

        private void filterByStatus(List<AlertStatus> lst)
        {
            addWhereClauseIn(root, AlertRecord_.status, lst);
        }

        private void filterByTypes(List<AlertType> lst)
        {
            addWhereClauseIn(root, AlertRecord_.type, lst);
        }

        private void filterBySeverity(List<AlertSeverity> lst)
        {
            addWhereClauseIn(root, AlertRecord_.severity, CollectionUtils.transformToList(lst, AlertSeverity::getLevel));
        }

        private void filterByRules(TypedRecordIdentityList<AlertDefinitionRecord> lst)
        {
            Path<AlertDefinitionRecord> rootDefinition = joinWithAlertDefinitionVersion().get(AlertDefinitionVersionRecord_.definition);

            addWhereClauseIn(rootDefinition, RecordWithCommonFields_.sysId, CollectionUtils.transformToListNoNulls(lst, (ri) -> ri.sysId));
        }
    }

    //--//

    public static PaginatedRecordIdentityList filter(RecordHelper<AlertRecord> helper,
                                                     AlertFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        //--//

        return JoinHelper.returnFilterTuples(helper, jh);
    }

    public static long count(RecordHelper<AlertRecord> helper,
                             AlertFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return jh.count();
    }

    public static Map<String, Number> countAlertsByLocation(RecordHelper<AlertRecord> helper,
                                                            AlertFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            filters.sortBy = null; // Just in case, sanitize input.

            jh.applyFilters(filters);
        }

        return jh.countByField(jh.joinWithLocation(), RecordWithCommonFields_.sysId);
    }

    public static Map<AlertType, Number> countAlertsByType(RecordHelper<AlertRecord> helper,
                                                           AlertFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            filters.sortBy = null; // Just in case, sanitize input.

            jh.applyFilters(filters);
        }

        return jh.countByField(jh.root, AlertRecord_.type);
    }

    public static Map<AlertStatus, Number> countAlertsByStatus(RecordHelper<AlertRecord> helper,
                                                               AlertFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            filters.sortBy = null; // Just in case, sanitize input.

            jh.applyFilters(filters);
        }

        return jh.countByField(jh.root, AlertRecord_.status);
    }

    public static Map<AlertDefinitionRecord, Number> countAlertsByRule(RecordHelper<AlertRecord> helper,
                                                                       AlertFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            filters.sortBy = null; // Just in case, sanitize input.

            jh.applyFilters(filters);
        }

        Join<AlertRecord, AlertDefinitionVersionRecord> join = jh.joinWithAlertDefinitionVersion();
        return jh.countByField(join, AlertDefinitionVersionRecord_.definition);
    }

    public static Map<AlertSeverity, Number> countAlertsBySeverity(RecordHelper<AlertRecord> helper,
                                                                   AlertFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            filters.sortBy = null; // Just in case, sanitize input.

            jh.applyFilters(filters);
        }

        Map<Integer, Number>       map = jh.countByField(jh.root, AlertRecord_.severity);
        Map<AlertSeverity, Number> res = Maps.newHashMap();

        for (Integer val : map.keySet())
        {
            Number count = map.get(val);

            res.put(AlertSeverity.parse(val), count);
        }

        return res;
    }
}
