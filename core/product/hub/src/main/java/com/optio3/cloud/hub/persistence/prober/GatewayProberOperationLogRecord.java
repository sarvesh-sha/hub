/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.prober;

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
@Table(name = "GATEWAY_PROBER_OPERATION_LOG")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "DeploymentProberOperationLog", model = BaseModel.class, metamodel = GatewayProberOperationLogRecord_.class)
public class GatewayProberOperationLogRecord extends CommonLogRecord
{
    /**
     * Bound to this job step.
     */
    @Optio3ControlNotifications(reason = "The task is already touched when we change this record", direct = Notify.NEVER, reverse = Notify.NEVER)
    @Optio3Cascade(mode = Flavor.DELETE, getter = "getOwningOp")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "owning_op", nullable = false, foreignKey = @ForeignKey(name = "OWNING_OP__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private GatewayProberOperationRecord owningOp;

    public static GatewayProberOperationLogRecord newInstance(GatewayProberOperationRecord rec_op)
    {
        GatewayProberOperationLogRecord rec_log = new GatewayProberOperationLogRecord();
        rec_log.owningOp = rec_op;

        return rec_log;
    }

    //--//

    public GatewayProberOperationRecord getOwningOp()
    {
        return owningOp;
    }
}
