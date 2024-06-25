/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.customer;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.serialization.Reflection;

@Embeddable
public class RoleAndArchitecture
{
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private DeploymentRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "architecture")
    private DockerImageArchitecture architecture;

    public RoleAndArchitecture()
    {
    }

    public RoleAndArchitecture(DeploymentRole role,
                               DockerImageArchitecture architecture)
    {
        this.role         = role;
        this.architecture = architecture;
    }

    //--//

    public DeploymentRole getRole()
    {
        return role;
    }

    public void setRole(DeploymentRole role)
    {
        this.role = role;
    }

    public DockerImageArchitecture getArchitecture()
    {
        return architecture;
    }

    public void setArchitecture(DockerImageArchitecture architecture)
    {
        this.architecture = architecture;
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        RoleAndArchitecture that = Reflection.as(o, RoleAndArchitecture.class);
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
}
