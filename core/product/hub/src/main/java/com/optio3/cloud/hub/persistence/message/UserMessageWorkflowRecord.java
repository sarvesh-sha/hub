/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.message;

import static java.util.Objects.requireNonNull;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.model.message.UserMessage;
import com.optio3.cloud.hub.model.message.UserMessageWorkflow;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowRecord;
import com.optio3.cloud.persistence.SessionHolder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "USER_MESSAGE_WORKFLOW")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "UserMessageWorkflow", model = UserMessageWorkflow.class, metamodel = UserMessageWorkflowRecord_.class)
public class UserMessageWorkflowRecord extends UserMessageRecord
{
    @Optio3ControlNotifications(reason = "No change notification", direct = Notify.NEVER, reverse = Notify.NEVER, getter = "getWorkflow")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getWorkflow")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "workflow", nullable = false, foreignKey = @ForeignKey(name = "USER_MESSAGE_WORKFLOW__WORKFLOW__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private WorkflowRecord workflow;

    //--//

    public UserMessageWorkflowRecord()
    {
    }

    //--//

    public static UserMessageWorkflowRecord newInstance(UserRecord user,
                                                        WorkflowRecord workflow)
    {
        requireNonNull(user);

        UserMessageWorkflowRecord res = new UserMessageWorkflowRecord();
        res.setUser(user);
        res.workflow = workflow;
        return res;
    }

    @Override
    public void populateFromDemo(SessionHolder holder,
                                 UserMessage msg)
    {
        super.populateFromDemo(holder, msg);

        UserMessageWorkflow msg2 = (UserMessageWorkflow) msg;
        this.workflow = holder.fromIdentity(msg2.workflow);
    }

    //--//

    public WorkflowRecord getWorkflow()
    {
        return workflow;
    }
}
