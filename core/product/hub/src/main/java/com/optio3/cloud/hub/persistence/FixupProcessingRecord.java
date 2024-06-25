/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.persistence.RecordForFixupProcessing;
import com.optio3.service.IServiceProvider;
import com.optio3.util.CollectionUtils;

@Entity
@Table(name = "FIXUP_PROCESSING")
@Optio3TableInfo(externalId = "FixupProcessing", model = BaseModel.class, metamodel = FixupProcessingRecord_.class)
public class FixupProcessingRecord extends RecordForFixupProcessing<FixupProcessingRecord>
{
    public FixupProcessingRecord()
    {
    }

    public static void executeAllHandlers(IServiceProvider serviceProvider) throws
                                                                            Exception
    {
        executeAllHandlers(serviceProvider, "com.optio3.cloud.hub.", FixupProcessingRecord.class);
    }

    public static List<String> listExecutedHandlers(IServiceProvider serviceProvider)
    {
        return CollectionUtils.transformToList(listExecutedHandlers(serviceProvider, FixupProcessingRecord.class), Class::getName);
    }
}
