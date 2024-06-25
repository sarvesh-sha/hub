/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class DeploymentAgentUpgrade
{
    public DeploymentAgentUpgradeAction                  action;
    public TypedRecordIdentity<CustomerRecord>           customer;
    public TypedRecordIdentity<CustomerServiceRecord>    service;
    public TypedRecordIdentityList<DeploymentHostRecord> hosts;
}
