/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.persistence.RecordForBackgroundActivityChunk;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "BACKGROUND_ACTIVITY_CHUNK")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "BackgroundActivityChunk", model = BaseModel.class, metamodel = BackgroundActivityChunkRecord_.class, defragmentOnBoot = true)
public class BackgroundActivityChunkRecord extends RecordForBackgroundActivityChunk<BackgroundActivityRecord, BackgroundActivityChunkRecord, HostAssetRecord>
{
    @Optio3ControlNotifications(reason = "Don't report changes", direct = Optio3ControlNotifications.Notify.NEVER, reverse = Optio3ControlNotifications.Notify.NEVER)
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getOwningActivity")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "owning_activity", nullable = false, foreignKey = @ForeignKey(name = "CHUNK__OWNING_ACTIVITY__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private BackgroundActivityRecord owningActivity;

    //--//

    @Override
    public BackgroundActivityRecord getOwningActivity()
    {
        return this.owningActivity;
    }

    @Override
    public void setOwningActivity(BackgroundActivityRecord owningActivity)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (this.owningActivity != owningActivity)
        {
            this.owningActivity = owningActivity;
        }
    }
}
