/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.customer;

import java.util.List;

import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.model.TypedRecordIdentity;
import org.apache.commons.lang3.StringUtils;

public class RoleAndArchitectureWithImage
{
    public DeploymentRole                                 role;
    public DockerImageArchitecture                        architecture;
    public TypedRecordIdentity<RegistryTaggedImageRecord> image;

    //--//

    public static <T extends RoleAndArchitectureWithImage> T locate(List<T> lst,
                                                                    DeploymentRole role,
                                                                    DockerImageArchitecture architecture)
    {
        for (T item : lst)
        {
            if (item.role == role && item.architecture == architecture)
            {
                return item;
            }
        }

        return null;
    }

    public static void add(List<RoleAndArchitectureWithImage> lst,
                           DeploymentRole role,
                           DockerImageArchitecture architecture,
                           TypedRecordIdentity<RegistryTaggedImageRecord> image)
    {
        RoleAndArchitectureWithImage roleImage = new RoleAndArchitectureWithImage();
        roleImage.role         = role;
        roleImage.architecture = architecture;
        roleImage.image        = image;

        lst.add(roleImage);

        lst.sort((a, b) -> StringUtils.compareIgnoreCase(a.role.name(), b.role.name()));
    }
}
