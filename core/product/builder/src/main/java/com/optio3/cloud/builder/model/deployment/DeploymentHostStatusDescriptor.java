/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.builder.model.customer.Customer;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.customer.CustomerVertical;
import com.optio3.cloud.builder.model.jobs.output.RegistryImage;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentInstance;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperations;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class DeploymentHostStatusDescriptor
{
    public       TypedRecordIdentity<DeploymentHostRecord> ri;
    public       String                                    customerSysId;
    public       String                                    customerName;
    public       String                                    serviceSysId;
    public       String                                    serviceName;
    public       CustomerVertical                          serviceVertical;
    public       String                                    hostId;
    public       String                                    hostName;
    public       String                                    remoteName;
    public       List<DeploymentRole>                      roles;
    public       String                                    rolesSummary;
    public final Map<String, DeploymentTask>               tasks  = Maps.newHashMap();
    public final Map<String, DeploymentAgent>              agents = Maps.newHashMap();
    public final Map<String, RegistryImage>                images = Maps.newHashMap();
    public       DeploymentHostDetails                     hostDetails;
    public       DeploymentHostProvisioningInfo            provisioningInfo;

    public ZonedDateTime createdOn;
    public ZonedDateTime lastHeartbeat;
    public ZonedDateTime agentBuildTime;
    public float         batteryVoltage = Float.NaN;
    public float         cpuTemperature = Float.NaN;
    public long          diskTotal;
    public long          diskFree;

    public       DeploymentInstance                      instanceType;
    public       DockerImageArchitecture                 architecture;
    public       DeploymentStatus                        status;
    public       DeploymentOperationalStatus             operationalStatus;
    public       DeploymentOperationalResponsiveness     responsiveness;
    public final List<DelayedOperation>                  delayedOps = Lists.newArrayList();
    public       Set<DeploymentHostStatusDescriptorFlag> flags      = Collections.emptySet();

    public String preparedForCustomer;
    public String preparedForService;

    //--//

    public DeploymentHostStatusDescriptor()
    {
        // Just for Swagger.
    }

    public DeploymentHostStatusDescriptor(DeploymentHost rawHost,
                                          boolean includeFullDetails)
    {
        roles = Lists.newArrayList();
        roles.addAll(rawHost.roles);

        final CustomerService service = rawHost.rawService;
        if (service != null)
        {
            serviceName = service.name;

            final Customer customer = service.rawCustomer;
            if (customer != null)
            {
                customerSysId = customer.sysId;
                customerName  = customer.name;
            }

            serviceSysId    = service.sysId;
            serviceVertical = service.vertical;
        }

        roles.sort((a, b) -> a.name()
                              .compareToIgnoreCase(b.name()));

        rolesSummary = roles.isEmpty() ? "<none>" : StringUtils.join(CollectionUtils.transformToList(roles, Enum::name), ", ");

        ri       = TypedRecordIdentity.newTypedInstance(DeploymentHostRecord.class, rawHost.sysId);
        hostId   = rawHost.hostId;
        hostName = rawHost.hostName;

        createdOn     = rawHost.createdOn;
        lastHeartbeat = rawHost.lastHeartbeat;

        architecture      = rawHost.architecture;
        status            = rawHost.status;
        operationalStatus = rawHost.operationalStatus;
        responsiveness    = DeploymentHostRecord.computeOperationalResponsiveness(rawHost.lastHeartbeat, rawHost.warningThreshold);

        for (DeploymentTask task : rawHost.rawTasks)
        {
            if (task.dockerId != null)
            {
                if (task.status == DeploymentStatus.Stopped)
                {
                    addFlag(DeploymentHostStatusDescriptorFlag.StoppedTasks);
                }

                DeploymentRole role = task.ensurePurpose();
                if (role == DeploymentRole.waypoint || role == DeploymentRole.provisioner)
                {
                    addFlag(DeploymentHostStatusDescriptorFlag.Waypoints);
                }

                if (includeFullDetails)
                {
                    tasks.put(task.sysId, task);

                    RegistryImage image = task.rawImage;
                    if (image != null)
                    {
                        images.put(image.sysId, image);
                    }
                }
            }
        }

        for (DeploymentAgent agent : rawHost.rawAgents)
        {
            if (includeFullDetails)
            {
                agents.put(agent.sysId, agent);
            }

            if (agent.active && agent.details != null)
            {
                batteryVoltage = agent.details.batteryVoltage;
                cpuTemperature = agent.details.cpuTemperature;
                diskTotal      = agent.details.diskTotal;
                diskFree       = agent.details.diskFree;
            }

            if (agent.status == DeploymentStatus.Terminated)
            {
                addFlag(DeploymentHostStatusDescriptorFlag.TerminatedAgents);
            }

            if (agent.status == DeploymentStatus.Ready)
            {
                DeploymentOperationalResponsiveness responsiveness = DeploymentHostRecord.computeOperationalResponsiveness(agent.lastHeartbeat, rawHost.warningThreshold);
                final boolean                       isResponsive   = responsiveness == DeploymentOperationalResponsiveness.Responsive;

                if (isResponsive && agent.isDefaultAgent())
                {
                    addFlag(DeploymentHostStatusDescriptorFlag.DefaultAgent);
                }

                RegistryImage image = agent.findImage();
                if (image != null)
                {
                    if (agent.active)
                    {
                        agentBuildTime = image.buildTime;
                    }
                    else if (isResponsive)
                    {
                        addFlag(DeploymentHostStatusDescriptorFlag.NonActiveAgents);
                    }

                    if (image.isRelease)
                    {
                        addFlag(DeploymentHostStatusDescriptorFlag.AgentsRunningRelease);

                        if (!agent.active && isResponsive)
                        {
                            addFlag(DeploymentHostStatusDescriptorFlag.NonActiveAgentsRunningRelease);
                        }
                    }

                    if (image.isReleaseCandidate)
                    {
                        addFlag(DeploymentHostStatusDescriptorFlag.AgentsRunningReleaseCandidate);

                        if (!agent.active && isResponsive)
                        {
                            addFlag(DeploymentHostStatusDescriptorFlag.NonActiveAgentsRunningReleaseCandidate);
                        }
                    }
                }
            }
        }

        MetadataMap hostMetadata = rawHost.decodeMetadata();

        instanceType = rawHost.ensureInstanceType();

        DelayedOperations state = DeploymentHostRecord.WellKnownMetadata.delayedOperations.get(hostMetadata);
        if (state != null)
        {
            delayedOps.addAll(state.ops);
        }

        provisioningInfo = DeploymentHostProvisioningInfo.sanitize(DeploymentHostRecord.WellKnownMetadata.provisioningInfo.get(hostMetadata), false);

        hostDetails = DeploymentHostRecord.WellKnownMetadata.cellularDetails.get(hostMetadata);

        DeploymentHostServiceDetails details = DeploymentHostRecord.WellKnownMetadata.remoteDetails.get(hostMetadata);
        remoteName = details != null ? details.name : null;
    }

    public void lookupPreparedFor(DeploymentGlobalDescriptor globalDescriptor,
                                  DeploymentHost rawHost)
    {
        MetadataMap hostMetadata = rawHost.decodeMetadata();

        String preparedForCustomer = DeploymentHostRecord.WellKnownMetadata.preparedForCustomer.get(hostMetadata);
        if (preparedForCustomer != null)
        {
            final Customer customer = globalDescriptor.customers.get(preparedForCustomer);
            if (customer != null)
            {
                this.preparedForCustomer = customer.name;
            }
        }

        String preparedForService = DeploymentHostRecord.WellKnownMetadata.preparedForService.get(hostMetadata);
        if (preparedForService != null)
        {
            final CustomerService service = globalDescriptor.services.get(preparedForCustomer);
            if (service != null)
            {
                this.preparedForService = service.name;
            }
        }
    }

    public void addFlag(DeploymentHostStatusDescriptorFlag flag)
    {
        if (flags.isEmpty())
        {
            flags = Sets.newHashSet();
        }

        flags.add(flag);
    }

    public boolean hasFlag(DeploymentHostStatusDescriptorFlag flag)
    {
        return flags.contains(flag);
    }

    public boolean hasAnyRoles(DeploymentRole... roles)
    {
        for (var role : roles)
        {
            if (this.roles.contains(role))
            {
                return true;
            }
        }

        return false;
    }

    //--//

    public boolean matchFilter(String filter)
    {
        boolean match = false;

        match |= StringUtils.containsIgnoreCase(hostId, filter);
        match |= StringUtils.containsIgnoreCase(hostName, filter);
        match |= StringUtils.containsIgnoreCase(remoteName, filter);
        match |= StringUtils.containsIgnoreCase(serviceName, filter);
        match |= StringUtils.containsIgnoreCase(customerName, filter);
        match |= StringUtils.containsIgnoreCase(rolesSummary, filter);
        match |= StringUtils.containsIgnoreCase(instanceType.getDisplayName(), filter);
        match |= StringUtils.containsIgnoreCase(instanceType.name(), filter);

        if (provisioningInfo != null && provisioningInfo.notes != null)
        {
            for (DeploymentHostProvisioningNotes note : provisioningInfo.notes)
            {
                match |= StringUtils.containsIgnoreCase(note.customerInfo, filter);
                match |= StringUtils.containsIgnoreCase(note.text, filter);
            }
        }

        return match;
    }

    @Override
    public String toString()
    {
        return "DeploymentHostStatusDescriptor{" + "hostId='" + hostId + '\'' + ", hostName='" + hostName + '\'' + ", remoteName='" + remoteName + '\'' + ", serviceName='" + serviceName + '\'' + ", customerName='" + customerName + '\'' + '}';
    }
}
