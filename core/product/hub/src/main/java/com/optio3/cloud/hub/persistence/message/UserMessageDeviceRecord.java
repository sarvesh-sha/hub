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
import com.optio3.cloud.hub.model.message.UserMessageDevice;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.persistence.SessionHolder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "USER_MESSAGE_DEVICE")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "UserMessageDevice", model = UserMessageDevice.class, metamodel = UserMessageDeviceRecord_.class)
public class UserMessageDeviceRecord extends UserMessageRecord
{
    @Optio3ControlNotifications(reason = "No change notification", direct = Notify.NEVER, reverse = Notify.NEVER, getter = "getDevice")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getDevice")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "device", nullable = false, foreignKey = @ForeignKey(name = "USER_MESSAGE_DEVICE__DEVICE__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private DeviceRecord device;

    //--//

    public UserMessageDeviceRecord()
    {
    }

    //--//

    public static UserMessageDeviceRecord newInstance(UserRecord user,
                                                      DeviceRecord device)
    {
        requireNonNull(user);

        UserMessageDeviceRecord res = new UserMessageDeviceRecord();
        res.setUser(user);
        res.device = device;
        return res;
    }

    @Override
    public void populateFromDemo(SessionHolder holder,
                                 UserMessage msg)
    {
        super.populateFromDemo(holder, msg);

        UserMessageDevice msg2 = (UserMessageDevice) msg;
        this.device = holder.fromIdentity(msg2.device);
    }

    //--//

    public DeviceRecord getDevice()
    {
        return device;
    }
}
