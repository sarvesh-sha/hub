/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.workflow;

import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;
import java.util.List;

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
import javax.persistence.Table;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.cloud.hub.model.workflow.WorkflowEventType;
import com.optio3.cloud.hub.model.workflow.WorkflowHistory;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@DynamicUpdate // Due to HHH-11506
@Table(name = "WORKFLOW_HISTORY", indexes = { @Index(name = "WORKFLOW_HISTORY__OCCURRED", columnList = "sys_created_on") })
@Optio3TableInfo(externalId = "WorkflowHistory", model = WorkflowHistory.class, metamodel = WorkflowHistoryRecord_.class)
public class WorkflowHistoryRecord extends RecordWithCommonFields implements ModelMapperTarget<WorkflowHistory, WorkflowHistoryRecord_>
{
    @Optio3ControlNotifications(reason = "Only notify workflow of history's changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getWorkflow")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getWorkflow")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "workflow", nullable = false, foreignKey = @ForeignKey(name = "WORKFLOW__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private WorkflowRecord workflow;

    @Optio3UpgradeValue("created")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private WorkflowEventType type;

    @Lob
    @Column(name = "text", length = 8192)
    private String text;

    @Optio3ControlNotifications(reason = "Notify User", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getUser")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getUser", setter = "setUser")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "user", foreignKey = @ForeignKey(name = "WORKFLOW_HISTORY_USER__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private UserRecord user;

    public WorkflowHistoryRecord()
    {
    }

    public static WorkflowHistoryRecord newInstance(WorkflowRecord rec_workflow,
                                                    UserRecord rec_user,
                                                    WorkflowEventType type)

    {
        requireNonNull(rec_workflow);

        WorkflowHistoryRecord res = new WorkflowHistoryRecord();
        res.workflow = rec_workflow;
        res.type     = type;
        res.user     = rec_user;

        return res;
    }

    //--//

    public WorkflowRecord getWorkflow()
    {
        return workflow;
    }

    public WorkflowEventType getType()
    {
        return type;
    }

    public void setType(WorkflowEventType type)
    {
        this.type = type;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public UserRecord getUser()
    {
        return user;
    }

    public void setUser(UserRecord user)
    {
        if (user != this.user)
        {
            this.user = user;
        }
    }

    //--//

    public static TypedRecordIdentityList<WorkflowHistoryRecord> listSorted(RecordHelper<WorkflowHistoryRecord> helper,
                                                                            WorkflowRecord rec_workflow,
                                                                            ZonedDateTime rangeStart,
                                                                            ZonedDateTime rangeEnd)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            Root<WorkflowHistoryRecord> rootWorkflowHistory = jh.root;

            if (rec_workflow != null)
            {
                jh.addWhereClauseWithEqual(rootWorkflowHistory, WorkflowHistoryRecord_.workflow, rec_workflow);
            }

            jh.filterTimestampsCoveredByTargetRange(rootWorkflowHistory, RecordWithCommonFields_.createdOn, rangeStart, rangeEnd);

            jh.addOrderBy(rootWorkflowHistory, RecordWithCommonFields_.createdOn, false);
        });
    }

    public static TypedRecordIdentityList<WorkflowHistoryRecord> listSorted(RecordHelper<WorkflowHistoryRecord> helper,
                                                                            AssetRecord rec_asset,
                                                                            ZonedDateTime rangeStart,
                                                                            ZonedDateTime rangeEnd)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            Root<WorkflowHistoryRecord>                 rootWorkflowHistory = jh.root;
            Join<WorkflowHistoryRecord, WorkflowRecord> rootWorkflow        = rootWorkflowHistory.join(WorkflowHistoryRecord_.workflow);

            jh.addWhereClauseWithEqual(rootWorkflow, WorkflowRecord_.asset, rec_asset);

            jh.filterTimestampsCoveredByTargetRange(rootWorkflowHistory, RecordWithCommonFields_.createdOn, rangeStart, rangeEnd);

            jh.addOrderBy(rootWorkflowHistory, RecordWithCommonFields_.createdOn, false);
        });
    }

    public static List<WorkflowHistoryRecord> getBatch(RecordHelper<WorkflowHistoryRecord> helper,
                                                       List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }
}
