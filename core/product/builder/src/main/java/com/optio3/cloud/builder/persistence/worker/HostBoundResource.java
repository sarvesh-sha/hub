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

import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.model.BaseModel;
import com.optio3.serialization.Reflection;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "RESOURCES_HOSTBOUND")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "ResourceHostBound", model = BaseModel.class, metamodel = HostBoundResource_.class)
public abstract class HostBoundResource extends TrackedRecordWithResources
{
    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getOwningHost")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "owning_host", foreignKey = @ForeignKey(name = "OWNING_HOST__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private HostRecord owningHost;

    //--//

    public HostRecord getOwningHost()
    {
        return owningHost;
    }

    protected void setOwningHost(HostRecord owningHost)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (this.owningHost != owningHost)
        {
            this.owningHost = owningHost;
        }
    }

    //--//

    protected HostRecord getRequiredHost()
    {
        HostRecord requiredHost = owningHost;

        if (requiredHost == null)
        {
            requiredHost = Reflection.as(this, HostRecord.class);
        }

        return requiredHost;
    }
}
