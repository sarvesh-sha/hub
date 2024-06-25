/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.customer;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.builder.persistence.customer.BackupKind;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.RoleAndArchitecture;
import com.optio3.cloud.model.TypedRecordIdentity;

public class CustomerServiceDesiredState
{
    public TypedRecordIdentity<CustomerServiceBackupRecord> fromBackup;
    public BackupKind                                       createBackup;

    public final List<CustomerServiceDesiredStateRole> roles = Lists.newArrayList();

    //--//

    public CustomerServiceDesiredStateRole lookup(RoleAndArchitecture key)
    {
        for (CustomerServiceDesiredStateRole stateRole : roles)
        {
            if (stateRole.role == key.getRole() && stateRole.architecture == key.getArchitecture())
            {
                return stateRole;
            }
        }

        return null;
    }
}
