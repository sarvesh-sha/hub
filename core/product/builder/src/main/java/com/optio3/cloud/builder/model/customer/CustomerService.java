/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.customer;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3DontMap;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentTask;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentInstance;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceSecretRecord;
import com.optio3.cloud.builder.persistence.customer.DatabaseMode;
import com.optio3.cloud.client.deployer.model.DeployerShutdownConfiguration;
import com.optio3.cloud.model.BaseModelWithMetadata;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class CustomerService extends BaseModelWithMetadata
{
    public static class AlertThresholds
    {
        public static final TypeReference<List<AlertThresholds>> s_typeRef = new TypeReference<>()
        {
        };

        public DeploymentRole role;
        public int            warningThreshold;
        public int            alertThreshold;
    }

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<CustomerRecord> customer;

    //--//

    public String name;

    public String url;

    public DeploymentOperationalStatus operationalStatus;

    public String instanceAccount;

    public DeploymentInstance instanceType;

    @Optio3MapAsReadOnly // Read-only because we need to make sure the instance type is set before setting the region...
    public String instanceRegion;

    public CustomerVertical vertical;

    public Integer diskSize;

    public boolean useDemoData;
    public boolean relaunchAlways;

    public boolean disableServiceWorker;
    public boolean disableEmails;
    public boolean disableTexts;
    public boolean useTestReporter;

    @Optio3MapAsReadOnly // Read-only because we need to make sure the instance type is set before setting the region...
    public boolean certificateWarning;

    public String extraConfigLines;

    public Set<DeploymentRole> purposes = Sets.newHashSet();

    @Optio3MapAsReadOnly
    public DatabaseMode dbMode;

    @Optio3MapAsReadOnly
    public boolean heapStatusAbnormal;

    //--//

    @Optio3MapAsReadOnly
    public DeployerShutdownConfiguration batteryThresholds;

    @Optio3MapAsReadOnly
    public List<CustomerService.AlertThresholds> alertThresholds;

    //--//

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<BackgroundActivityRecord> currentActivity;

    @Optio3MapAsReadOnly
    public CustomerServiceUpgradeBlockers upgradeBlockers;

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<CustomerServiceBackupRecord> backups = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<CustomerServiceSecretRecord> secrets = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public List<RoleAndArchitectureWithImage> roleImages;

    @Optio3MapAsReadOnly
    public String roleOrigin;

    //--//

    @Optio3DontMap
    @JsonIgnore
    public Customer rawCustomer;

    @Optio3DontMap
    @JsonIgnore
    public DeploymentHost[] rawHosts;

    @Optio3DontMap
    @JsonIgnore
    public CustomerServiceBackup[] rawBackups;

    //--//

    @Optio3MapAsReadOnly
    public ZonedDateTime lastOutput;

    @Optio3MapAsReadOnly
    public int lastOffset;

    //--//

    public List<DeploymentHost> findHostsForRole(DeploymentStatus desiredState,
                                                 DeploymentRole desiredRole)
    {
        List<DeploymentHost> res = Lists.newArrayList();

        for (DeploymentHost host : rawHosts)
        {
            if (desiredRole != null && !host.hasRole(desiredRole))
            {
                continue;
            }

            if (desiredState != null && host.status != desiredState)
            {
                continue;
            }

            res.add(host);
        }

        return res;
    }

    public List<DeploymentTask> findTasksForRole(DeploymentStatus desiredState,
                                                 DeploymentRole desiredRole,
                                                 String imageSHA,
                                                 Boolean withContainer)
    {
        List<DeploymentTask> res = Lists.newArrayList();

        for (DeploymentHost host : rawHosts)
        {
            host.collectTasksForRole(res, desiredState, desiredRole, imageSHA, withContainer);
        }

        return res;
    }

    public boolean hasAnyTasksForRole(DeploymentStatus desiredState,
                                      DeploymentRole desiredRole,
                                      String imageSHA,
                                      Boolean withContainer)
    {
        List<DeploymentTask> tasks = findTasksForRole(desiredState, desiredRole, imageSHA, withContainer);
        return !tasks.isEmpty();
    }
}
