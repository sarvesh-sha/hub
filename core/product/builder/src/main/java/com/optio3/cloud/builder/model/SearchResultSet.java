/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model;

import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class SearchResultSet
{
    public int                                 totalUsers;
    public TypedRecordIdentityList<UserRecord> users = new TypedRecordIdentityList<>();

    public int                                     totalCustomers;
    public TypedRecordIdentityList<CustomerRecord> customers = new TypedRecordIdentityList<>();

    public int                                            totalCustomerServices;
    public TypedRecordIdentityList<CustomerServiceRecord> customerServices = new TypedRecordIdentityList<>();

    public int                                           totalDeploymentHosts;
    public TypedRecordIdentityList<DeploymentHostRecord> deploymentHosts = new TypedRecordIdentityList<>();
}
