/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment;

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
@Table(name = "DEPLOYMENT_HOST_IMAGE_PULL_LOG")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "DeploymentHostImagePullLog", model = BaseModel.class, metamodel = DeploymentHostImagePullLogRecord_.class)
public class DeploymentHostImagePullLogRecord extends CommonLogRecord
{
    /**
     * Bound to this job step.
     */
    @Optio3ControlNotifications(reason = "The pull is already touched when we change this record", direct = Notify.NEVER, reverse = Notify.NEVER)
    @Optio3Cascade(mode = Flavor.DELETE, getter = "getOwningImagePull")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "owning_image_pull", nullable = false, foreignKey = @ForeignKey(name = "OWNING_PULL__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private DeploymentHostImagePullRecord owningImagePull;

    public static DeploymentHostImagePullLogRecord newInstance(DeploymentHostImagePullRecord rec_pull)
    {
        DeploymentHostImagePullLogRecord rec_log = new DeploymentHostImagePullLogRecord();
        rec_log.owningImagePull = rec_pull;

        return rec_log;
    }

    //--//

    public DeploymentHostImagePullRecord getOwningImagePull()
    {
        return owningImagePull;
    }
}
