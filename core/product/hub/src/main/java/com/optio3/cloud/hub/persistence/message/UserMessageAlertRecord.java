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
import com.optio3.cloud.hub.model.message.UserMessageAlert;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.persistence.SessionHolder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "USER_MESSAGE_ALERT")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "UserMessageAlert", model = UserMessageAlert.class, metamodel = UserMessageAlertRecord_.class)
public class UserMessageAlertRecord extends UserMessageRecord
{
    @Optio3ControlNotifications(reason = "No change notification", direct = Notify.NEVER, reverse = Notify.NEVER, getter = "getAlert")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getAlert")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "alert", nullable = false, foreignKey = @ForeignKey(name = "USER_MESSAGE_ALERT__ALERT__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private AlertRecord alert;

    //--//

    public UserMessageAlertRecord()
    {
    }

    //--//

    public static UserMessageAlertRecord newInstance(UserRecord user,
                                                     AlertRecord alert)
    {
        requireNonNull(user);

        UserMessageAlertRecord res = new UserMessageAlertRecord();
        res.setUser(user);
        res.alert = alert;
        return res;
    }

    @Override
    public void populateFromDemo(SessionHolder holder,
                                 UserMessage msg)
    {
        super.populateFromDemo(holder, msg);

        UserMessageAlert msg2 = (UserMessageAlert) msg;
        this.alert = holder.fromIdentity(msg2.alert);
    }

    //--//

    public AlertRecord getAlert()
    {
        return alert;
    }
}
