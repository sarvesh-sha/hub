/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs.output;

import java.util.Collection;
import java.util.Objects;

import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.serialization.Reflection;

public class ReleaseStatusReport
{
    public DeploymentRole role;

    public DockerImageArchitecture architecture;

    public TypedRecordIdentity<RegistryTaggedImageRecord> image;

    //--//

    @Override
    public boolean equals(Object o)
    {
        ReleaseStatusReport that = Reflection.as(o, ReleaseStatusReport.class);
        if (that != null)
        {
            return role == that.role && architecture == that.architecture;
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(role, architecture);
    }

    //--//

    public static TypedRecordIdentity<RegistryTaggedImageRecord> findCompatible(Collection<ReleaseStatusReport> lst,
                                                                                DeploymentRole role,
                                                                                DockerImageArchitecture architecture)
    {
        for (ReleaseStatusReport item : lst)
        {
            if (item.role == role && item.architecture == architecture)
            {
                return item.image;
            }
        }

        return null;
    }
}
