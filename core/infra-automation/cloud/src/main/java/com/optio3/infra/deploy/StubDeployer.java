/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.deploy;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.infra.StatusCheckResult;
import com.optio3.infra.directory.CredentialDirectory;

public class StubDeployer extends CommonDeployer
{
    public StubDeployer(CredentialDirectory credDir,
                        String builderHostName,
                        String connectionUrl,
                        ConfigIdentities identities)
    {
        super(credDir, DockerImageArchitecture.UNKNOWN, builderHostName, connectionUrl, identities);
    }

    @Override
    public String deploy(boolean waitForStartup,
                         boolean allowSNS,
                         boolean allowEmail)
    {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    @Override
    public StatusCheckResult checkForStartup()
    {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String getPublicIp()
    {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<Metrics> getMetrics(ZonedDateTime start,
                                    ZonedDateTime end,
                                    Duration interval)
    {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void updateDns(String oldIp,
                          String newIp)
    {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void terminate(boolean waitForShutdown)
    {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    @Override
    public StatusCheckResult checkForTermination()
    {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void cleanupService()
    {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void cleanupCustomerInRegion()
    {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void cleanupCustomer()
    {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }
}
