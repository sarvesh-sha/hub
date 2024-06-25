/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.message;

import static java.util.Objects.requireNonNull;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.model.message.UserMessageGeneric;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "USER_MESSAGE_GENERIC")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "UserMessageGeneric", model = UserMessageGeneric.class, metamodel = UserMessageGenericRecord_.class)
public class UserMessageGenericRecord extends UserMessageRecord
{
    public UserMessageGenericRecord()
    {
    }

    //--//

    public static UserMessageGenericRecord newInstance(UserRecord user,
                                                       AlertRecord alert)
    {
        requireNonNull(user);

        UserMessageGenericRecord res = new UserMessageGenericRecord();
        res.setUser(user);
        return res;
    }
}
