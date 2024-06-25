/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.customer;

import java.time.ZonedDateTime;

import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

public class CustomerServiceUpgradeBlocker
{
    public TypedRecordIdentity<UserRecord> user;
    public ZonedDateTime                   until;
}
