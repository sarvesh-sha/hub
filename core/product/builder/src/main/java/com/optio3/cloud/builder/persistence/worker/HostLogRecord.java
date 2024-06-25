/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.worker;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3Cascade.Flavor;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.persistence.CommonLogRecord;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "HOST_LOG")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "HostLog", model = BaseModel.class, metamodel = HostLogRecord_.class, defragmentOnBoot = true)
public class HostLogRecord extends CommonLogRecord
{
    /**
     * Bound to this job step.
     */
    @Optio3ControlNotifications(reason = "The host is already touched when we change this record", direct = Notify.NEVER, reverse = Notify.NEVER)
    @Optio3Cascade(mode = Flavor.DELETE, getter = "getOwningHost")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "owning_host", nullable = false, foreignKey = @ForeignKey(name = "HOST_LOG__OWNING_HOST__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private HostRecord owningHost;

    public static HostLogRecord newInstance(HostRecord rec_host)
    {
        HostLogRecord rec_log = new HostLogRecord();
        rec_log.owningHost = rec_host;

        return rec_log;
    }

    //--//

    public HostRecord getOwningHost()
    {
        return owningHost;
    }
}
