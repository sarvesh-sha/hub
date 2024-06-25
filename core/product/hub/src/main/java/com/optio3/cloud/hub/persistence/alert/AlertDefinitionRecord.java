/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.alert;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Tuple;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.model.alert.AlertDefinition;
import com.optio3.cloud.hub.model.alert.AlertDefinitionFilterRequest;
import com.optio3.cloud.hub.model.alert.AlertDefinitionPurpose;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogHandler;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "ALERT_DEFINITION")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "AlertDefinition", model = AlertDefinition.class, metamodel = AlertDefinitionRecord_.class)
public class AlertDefinitionRecord extends RecordWithCommonFields implements ModelMapperTarget<AlertDefinition, AlertDefinitionRecord_>,
                                                                             LogHandler.ILogHost<AlertDefinitionLogRecord>
{
    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Optio3UpgradeValue("Definition")
    @Column(name = "purpose", nullable = false)
    private AlertDefinitionPurpose purpose;

    //--//

    @Column(name = "last_output")
    private ZonedDateTime lastOutput;

    @Column(name = "last_offset", nullable = false)
    private int lastOffset;

    @Lob
    @Column(name = "log_ranges")
    private byte[] logRanges;

    //--//

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getHeadVersion")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "head_version", foreignKey = @ForeignKey(name = "ALERT_DEFINITION__HEAD_VERSION__FK"))
    private AlertDefinitionVersionRecord headVersion;

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getReleaseVersion")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "release_version", foreignKey = @ForeignKey(name = "ALERT_DEFINITION__RELEASE_VERSION__FK"))
    private AlertDefinitionVersionRecord releaseVersion;

    @OneToMany(mappedBy = "definition", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<AlertDefinitionVersionRecord> versions;

    //--//

    @Lob
    @Column(name = "execution_state")
    @Basic(fetch = FetchType.LAZY)
    private byte[] executionState;

    @Transient
    private final PersistAsJsonHelper<byte[], AlertEngineExecutionContext.State> m_executionStateHelper = new PersistAsJsonHelper<>(() -> executionState,
                                                                                                                                    (val) -> executionState = val,
                                                                                                                                    byte[].class,
                                                                                                                                    AlertEngineExecutionContext.State.class,
                                                                                                                                    ObjectMappers.SkipNulls,
                                                                                                                                    true);

    //--//

    public static AlertDefinitionRecord newInstance(AlertDefinitionPurpose purpose)
    {
        AlertDefinitionRecord record = new AlertDefinitionRecord();
        record.purpose = purpose;
        return record;
    }

    //--//

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public AlertDefinitionPurpose getPurpose()
    {
        return purpose;
    }

    //--//

    public ZonedDateTime getLastOutput()
    {
        return lastOutput;
    }

    public int getLastOffset()
    {
        return lastOffset;
    }

    @Override
    public byte[] getLogRanges()
    {
        return logRanges;
    }

    @Override
    public void setLogRanges(byte[] logRanges,
                             ZonedDateTime lastOutput,
                             int lastOffset)
    {
        if (!Arrays.equals(this.logRanges, logRanges))
        {
            this.logRanges  = logRanges;
            this.lastOutput = lastOutput;
            this.lastOffset = lastOffset;
        }
    }

    @Override
    public void refineLogQuery(LogHandler.JoinHelper<?, AlertDefinitionLogRecord> jh)
    {
        jh.addWhereClauseWithEqual(jh.rootLog, AlertDefinitionLogRecord_.owningDefinition, this);
    }

    @Override
    public AlertDefinitionLogRecord allocateNewLogInstance()
    {
        return AlertDefinitionLogRecord.newInstance(this);
    }

    public static LogHandler<AlertDefinitionRecord, AlertDefinitionLogRecord> allocateLogHandler(RecordLocked<AlertDefinitionRecord> lock)
    {
        return new LogHandler<>(lock, AlertDefinitionLogRecord.class);
    }

    public static LogHandler<AlertDefinitionRecord, AlertDefinitionLogRecord> allocateLogHandler(SessionHolder sessionHolder,
                                                                                                 AlertDefinitionRecord rec)
    {
        return new LogHandler<>(sessionHolder, rec, AlertDefinitionLogRecord.class);
    }

    //--//

    public List<AlertDefinitionVersionRecord> getVersions()
    {
        return CollectionUtils.asEmptyCollectionIfNull(versions);
    }

    public AlertDefinitionVersionRecord getHeadVersion()
    {
        return headVersion;
    }

    public boolean setHeadVersion(AlertDefinitionVersionRecord headVersion)
    {
        if (SessionHolder.sameEntity(this.headVersion, headVersion))
        {
            return false; // Nothing changed.
        }

        this.headVersion = headVersion;
        return true;
    }

    public AlertDefinitionVersionRecord getReleaseVersion()
    {
        return releaseVersion;
    }

    public boolean setReleaseVersion(AlertDefinitionVersionRecord releaseVersion)
    {
        if (SessionHolder.sameEntity(this.releaseVersion, releaseVersion))
        {
            return false; // Nothing changed.
        }

        this.releaseVersion = releaseVersion;
        return true;
    }

    //--//

    public AlertEngineExecutionContext.State getExecutionState()
    {
        return m_executionStateHelper.getNoCaching();
    }

    public boolean setExecutionState(AlertEngineExecutionContext.State state)
    {
        return m_executionStateHelper.set(state);
    }

    //--//

    public static List<AlertDefinitionRecord> getBatch(RecordHelper<AlertDefinitionRecord> helper,
                                                       List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static TypedRecordIdentityList<AlertDefinitionRecord> filter(RecordHelper<AlertDefinitionRecord> helper,
                                                                        AlertDefinitionFilterRequest filters)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            if (filters != null)
            {
                applyFilters(jh, filters);
            }
        });
    }

    private static void applyFilters(QueryHelperWithCommonFields<Tuple, AlertDefinitionRecord> jh,
                                     AlertDefinitionFilterRequest filters)
    {
        if (CollectionUtils.isNotEmpty(filters.purposes))
        {
            jh.addWhereClauseIn(jh.root, AlertDefinitionRecord_.purpose, filters.purposes);
        }

        if (filters.sortBy != null)
        {
            for (SortCriteria sort : filters.sortBy)
            {
                switch (sort.column)
                {
                    case "createdOn":
                    {
                        jh.addOrderBy(jh.root, RecordWithCommonFields_.createdOn, sort.ascending);
                        break;
                    }

                    case "updatedOn":
                    {
                        jh.addOrderBy(jh.root, RecordWithCommonFields_.updatedOn, sort.ascending);
                        break;
                    }

                    case "title":
                    {
                        jh.addOrderBy(jh.root, AlertDefinitionRecord_.title, sort.ascending);
                        break;
                    }
                }
            }
        }
    }

    //--//

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<AlertDefinitionRecord> helper) throws
                                                                   Exception
    {
        if (validation.canProceed())
        {
            removeInner(validation, helper);
        }
    }

    private void removeInner(ValidationResultsHolder validation,
                             RecordHelper<AlertDefinitionRecord> helper)
    {
        setReleaseVersion(null);
        setHeadVersion(null);

        final RecordHelper<AlertDefinitionVersionRecord> helper_version = helper.wrapFor(AlertDefinitionVersionRecord.class);
        for (AlertDefinitionVersionRecord rec_child : Lists.newArrayList(getVersions()))
        {
            rec_child.remove(validation, helper_version);
        }

        helper.delete(this);
    }
}
