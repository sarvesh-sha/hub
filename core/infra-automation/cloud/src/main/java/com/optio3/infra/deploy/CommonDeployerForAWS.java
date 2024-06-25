/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.deploy;

import java.util.List;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Address;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.infra.AwsHelper;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.util.BoxingUtils;

public abstract class CommonDeployerForAWS extends CommonDeployer
{
    private final Regions m_region;

    protected CommonDeployerForAWS(CredentialDirectory credDir,
                                   DockerImageArchitecture arch,
                                   String builderHostName,
                                   String connectionUrl,
                                   ConfigIdentities identities,
                                   Regions region)
    {
        super(credDir, arch, builderHostName, connectionUrl, identities);

        m_region = region;
    }

    public List<String> getAvailableAccounts()
    {
        return AwsHelper.getAvailableAccounts(credDir);
    }

    protected AwsHelper fetchAwsHelper(Regions overrideRegion)
    {
        return AwsHelper.buildCachedWithDirectoryLookup(credDir, domainName, BoxingUtils.get(overrideRegion, m_region));
    }

    protected Address associateDnsWithElasticIp()
    {
        Address eip;

        String existingIpAddress = getDns();
        if (existingIpAddress != null)
        {
            try (AwsHelper aws = fetchAwsHelper(null))
            {
                eip = aws.selectElasticAddressFromIp(existingIpAddress);
            }
        }
        else
        {
            eip = null;
        }

        if (eip == null)
        {
            try (AwsHelper aws = fetchAwsHelper(null))
            {
                eip = aws.findUnusedOrAllocateNewElasticAddress();
            }

            refreshDns(null, eip.getPublicIp());
        }

        return eip;
    }

    protected void releaseDnsWithElasticIp()
    {
        String existingIpAddress = getDns();
        if (existingIpAddress != null)
        {
            try (AwsHelper aws = fetchAwsHelper(null))
            {
                Address eip = aws.selectElasticAddressFromIp(existingIpAddress);
                if (eip != null)
                {
                    aws.releaseElasticAddress(eip);
                }
            }
        }
    }
}
