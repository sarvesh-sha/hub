/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.deploy;

import java.util.List;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.infra.AzureHelper;
import com.optio3.infra.directory.CredentialDirectory;

public abstract class CommonDeployerForAzure extends CommonDeployer
{
    private final Region m_region;

    protected CommonDeployerForAzure(CredentialDirectory credDir,
                                     DockerImageArchitecture arch,
                                     String builderHostName,
                                     String connectionUrl,
                                     ConfigIdentities identities,
                                     Region region)
    {
        super(credDir, arch, builderHostName, connectionUrl, identities);

        m_region = region;
    }

    protected AzureHelper fetchAzureHelper()
    {
        return AzureHelper.buildCachedWithDirectoryLookup(credDir, domainName, AzureEnvironment.AZURE, m_region);
    }

    public List<String> getAvailableAccounts()
    {
        return AzureHelper.getAvailableAccounts(credDir);
    }
}
