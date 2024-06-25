/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs.output;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.builder.model.customer.Customer;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.customer.CustomerServiceBackup;
import com.optio3.cloud.builder.model.deployment.DeploymentHostStatusDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentTask;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

public class RegistryTaggedImageUsage
{
    public String  tag;
    public boolean isRC;
    public boolean isRTM;

    public RegistryImage image;

    public List<String> services = Lists.newArrayList();
    public List<String> backups  = Lists.newArrayList();
    public List<String> tasks    = Lists.newArrayList();

    public Map<String, Customer>                       lookupCustomer = Maps.newHashMap();
    public Map<String, CustomerService>                lookupService  = Maps.newHashMap();
    public Map<String, CustomerServiceBackup>          lookupBackup   = Maps.newHashMap();
    public Map<String, DeploymentTask>                 lookupTask     = Maps.newHashMap();
    public Map<String, DeploymentHostStatusDescriptor> lookupHost     = Maps.newHashMap();

    //--//

    @Override
    public boolean equals(Object o)
    {
        RegistryTaggedImageUsage that = Reflection.as(o, RegistryTaggedImageUsage.class);
        if (that == null)
        {
            return false;
        }

        return StringUtils.equals(tag, that.tag);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(tag);
    }

    //--//

    public boolean getSafeToDelete()
    {
        if (isInUse(true))
        {
            return false;
        }

        if (hasEntries(this.tasks))
        {
            for (DeploymentHostStatusDescriptor host : lookupHost.values())
            {
                switch (host.operationalStatus)
                {
                    case lostConnectivity:
                    case storageCorruption:
                    case RMA_warranty:
                    case RMA_nowarranty:
                    case retired:
                        // Images referenced by these hosts are not really in use.
                        break;

                    default:
                        return false;
                }
            }
        }

        return true;
    }

    public boolean isInUse(boolean ignoreTasks)
    {
        if (this.isRC || this.isRTM || hasEntries(this.services) || hasEntries(this.backups))
        {
            return true;
        }

        return !ignoreTasks && hasEntries(this.tasks);
    }

    private static <T> boolean hasEntries(List<T> lst)
    {
        return lst != null && !lst.isEmpty();
    }
}
