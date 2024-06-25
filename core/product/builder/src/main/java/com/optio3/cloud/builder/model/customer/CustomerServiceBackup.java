/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.customer;

import java.util.List;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.customer.BackupKind;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.BaseModelWithMetadata;
import com.optio3.cloud.model.TypedRecordIdentity;

public class CustomerServiceBackup extends BaseModelWithMetadata
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<CustomerServiceRecord> customerService;

    //--//

    @Optio3MapAsReadOnly
    public String fileId;

    @Optio3MapAsReadOnly
    public long fileSize;

    @Optio3MapAsReadOnly
    public String fileIdOnAgent;

    @Optio3MapAsReadOnly
    public boolean pendingTransfer;

    @Optio3MapAsReadOnly
    public BackupKind trigger;

    @Optio3MapAsReadOnly
    public String extraConfigLines;

    @Optio3MapAsReadOnly
    public List<RoleAndArchitectureWithImage> roleImages;
}
