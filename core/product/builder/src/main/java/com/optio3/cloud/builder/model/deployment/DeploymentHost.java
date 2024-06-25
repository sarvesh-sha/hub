/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3DontMap;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.annotation.Optio3MapToPersistence;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentInstance;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.model.BaseModelWithMetadata;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.MetadataMap;
import org.apache.commons.lang3.StringUtils;

public class DeploymentHost extends BaseModelWithMetadata
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<CustomerServiceRecord> customerService;

    @Optio3MapAsReadOnly
    public Set<DeploymentRole> roles;

    //--//

    @Optio3MapAsReadOnly
    public String hostId;

    public String hostName;

    @Optio3MapAsReadOnly
    public String displayName;

    @Optio3MapAsReadOnly
    public String remoteName;

    @Optio3MapAsReadOnly
    public String dnsName;

    public int warningThreshold;

    @Optio3MapAsReadOnly
    public DeploymentStatus status;

    @Optio3MapAsReadOnly
    public DeploymentOperationalResponsiveness operationalResponsiveness;

    public DeploymentOperationalStatus operationalStatus;

    @Optio3MapAsReadOnly
    public DockerImageArchitecture architecture;

    @Optio3MapAsReadOnly
    public DeploymentInstance instanceType;

    @Optio3MapAsReadOnly
    public String instanceRegion;

    //--//

    @Optio3MapAsReadOnly
    public ZonedDateTime lastHeartbeat;

    @Optio3MapAsReadOnly
    public DeploymentHostOnlineSessions onlineSessions;

    //--//

    @Optio3MapAsReadOnly
    public DeploymentHostDetails details;

    //--//

    @Optio3MapAsReadOnly
    public boolean delayedOperations;

    //--//

    @Optio3DontMap
    @JsonIgnore
    public CustomerService rawService;

    @Optio3DontMap
    @JsonIgnore
    public DeploymentTask[] rawTasks;

    @Optio3DontMap
    @JsonIgnore
    public DeploymentAgent[] rawAgents;

    //--//

    @Optio3MapAsReadOnly
    public ZonedDateTime lastOutput;

    @Optio3MapAsReadOnly
    public int lastOffset;

    //--//

    public List<DeploymentTask> getTasksForRole(DeploymentStatus desiredState,
                                                DeploymentRole desiredRole,
                                                String imageSHA,
                                                Boolean withContainer)
    {
        List<DeploymentTask> tasks = Lists.newArrayList();
        collectTasksForRole(tasks, desiredState, desiredRole, imageSHA, withContainer);
        return tasks;
    }

    public boolean collectTasksForRole(List<DeploymentTask> tasks,
                                       DeploymentStatus desiredState,
                                       DeploymentRole desiredRole,
                                       String imageSHA,
                                       Boolean withContainer)
    {
        boolean found = false;

        for (DeploymentTask task : rawTasks)
        {
            boolean hasContainer = task.dockerId != null;

            if (withContainer != null && withContainer != hasContainer)
            {
                continue;
            }

            if (desiredState != null && task.status != desiredState)
            {
                continue;
            }

            if (task.ensurePurpose() != desiredRole)
            {
                continue;
            }

            if (imageSHA != null && !StringUtils.equals(task.image, imageSHA))
            {
                continue;
            }

            found = true;

            if (tasks == null)
            {
                // Only interested in the presence of a task.
                break;
            }

            tasks.add(task);
        }

        return found;
    }

    //--//

    @JsonIgnore
    public boolean hasRole(DeploymentRole role)
    {
        return roles != null && roles.contains(role);
    }

    public DeploymentInstance ensureInstanceType()
    {
        if (instanceType == null)
        {
            MetadataMap hostMetadata = decodeMetadata();

            instanceType = DeploymentHostRecord.WellKnownMetadata.instanceType.get(hostMetadata);
        }

        return instanceType;
    }

    public String computeDisplayName()
    {
        if (StringUtils.isNotBlank(hostName))
        {
            return hostId + " [" + hostName + "]";
        }
        else
        {
            return hostId;
        }
    }
}
