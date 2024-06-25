/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.optio3.cloud.AbstractApplication;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.persistence.RecordWithDistributedLocking;

@Entity
@Table(name = "DISTRIBUTED_LOCKING")
@Optio3TableInfo(externalId = "DistributedLocking", model = BaseModel.class, metamodel = DistributedLockingRecord_.class)
public class DistributedLockingRecord extends RecordWithDistributedLocking
{
    public static void registerAsLockProvider(AbstractApplication app,
                                              List<Class<?>> entities)
    {
        register(app, DistributedLockingRecord.class, entities);
    }
}
