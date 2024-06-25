/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.builder.model.jobs.output.RegistryTaggedImageUsage;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;

public class JobUsage
{
    public static class Role<T extends RecordWithCommonFields>
    {
        public DeploymentRole              role;
        public Set<TypedRecordIdentity<T>> entries = Sets.newHashSet();

        @Override
        public boolean equals(Object o)
        {
            Role<?> that = Reflection.as(o, Role.class);
            if (that == null)
            {
                return false;
            }

            return role == that.role;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(role);
        }

        public static <T extends RecordWithCommonFields> Role<T> addRole(DeploymentRole role,
                                                                         List<Role<T>> lst)
        {
            for (Role<T> en : lst)
            {
                if (en.role == role)
                {
                    return en;
                }
            }

            Role<T> enNew = new Role<>();
            enNew.role = role;
            lst.add(enNew);
            return enNew;
        }
    }

    public String name;
    public String desc;
    public String descShort;

    public ZonedDateTime createdOn;

    //--//

    public List<RegistryTaggedImageUsage> imagesInUse = Lists.newArrayList();

    public void addImage(RegistryTaggedImageUsage usage)
    {
        CollectionUtils.addIfMissingAndNotNull(imagesInUse, usage);
    }

    //--//

    public List<Role<DeploymentHostRecord>> hostsInRole = Lists.newArrayList();

    public void addHostInRole(DeploymentRole role,
                              String hostSysId)
    {
        Role<DeploymentHostRecord> val = Role.addRole(role, hostsInRole);
        val.entries.add(TypedRecordIdentity.newTypedInstance(DeploymentHostRecord.class, hostSysId));
    }

    //--//

    public List<Role<CustomerServiceRecord>> servicesInRole = Lists.newArrayList();

    public void addServiceInRole(DeploymentRole role,
                                 String svcSysId)
    {
        Role<CustomerServiceRecord> val = Role.addRole(role, servicesInRole);
        val.entries.add(TypedRecordIdentity.newTypedInstance(CustomerServiceRecord.class, svcSysId));
    }
}
