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
import com.optio3.cloud.hub.model.message.UserMessageReport;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.report.ReportRecord;
import com.optio3.cloud.persistence.SessionHolder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "USER_MESSAGE_REPORT")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "UserMessageReport", model = UserMessageReport.class, metamodel = UserMessageReportRecord_.class)
public class UserMessageReportRecord extends UserMessageRecord
{
    @Optio3ControlNotifications(reason = "No change notification", direct = Notify.NEVER, reverse = Notify.NEVER)
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getReport")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "report", nullable = false, foreignKey = @ForeignKey(name = "USER_MESSAGE_REPORT__REPORT__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private ReportRecord report;

    //--//

    public UserMessageReportRecord()
    {
    }

    //--//

    public static UserMessageReportRecord newInstance(UserRecord user,
                                                      ReportRecord report)
    {
        requireNonNull(user);

        UserMessageReportRecord res = new UserMessageReportRecord();
        res.setUser(user);
        res.report = report;
        return res;
    }

    @Override
    public void populateFromDemo(SessionHolder holder,
                                 UserMessage msg)
    {
        super.populateFromDemo(holder, msg);

        UserMessageReport msg2 = (UserMessageReport) msg;
        this.report = holder.fromIdentity(msg2.report);
    }

    //--//

    public ReportRecord getReport()
    {
        return report;
    }
}
