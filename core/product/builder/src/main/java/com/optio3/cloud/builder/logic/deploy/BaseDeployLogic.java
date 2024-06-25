/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.logic.deploy;

import com.amazonaws.regions.Regions;
import com.azure.core.management.Region;
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentInstance;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.infra.AwsHelper;
import com.optio3.infra.AzureHelper;
import com.optio3.infra.deploy.AgentDeployerForAWS;
import com.optio3.infra.deploy.AgentDeployerForAzure;
import com.optio3.infra.deploy.CommonDeployer;
import com.optio3.infra.directory.SshKey;
import com.optio3.util.Exceptions;

public abstract class BaseDeployLogic
{
    public static final String MACHINE_ACCOUNT__FIRSTNAME = "site";
    public static final String MACHINE_ACCOUNT__LASTNAME  = "automation";
    public static final String MACHINE_ACCOUNT            = "site@localhost";

    //--//

    public final SessionProvider                     sessionProvider;
    public final RecordLocator<DeploymentHostRecord> loc_targetHost;
    public final String                              host_displayName;
    public final DockerImageArchitecture             host_architecture;
    public final DeploymentStatus                    host_status;

    private final BuilderApplication   m_app;
    private final BuilderConfiguration m_cfg;
    private final String               m_credentialForSSH;

    protected BaseDeployLogic(SessionHolder sessionHolder,
                              DeploymentHostRecord rec_host)
    {
        sessionProvider = sessionHolder.getSessionProvider();
        loc_targetHost  = sessionHolder.createLocator(rec_host);

        host_displayName  = rec_host.getDisplayName();
        host_architecture = rec_host.getArchitecture();
        host_status       = rec_host.getStatus();

        m_app = sessionHolder.getServiceNonNull(BuilderApplication.class);
        m_cfg = sessionHolder.getServiceNonNull(BuilderConfiguration.class);

        DeploymentInstance instanceType = rec_host.getInstanceType();
        if (instanceType.deployerClass == AgentDeployerForAWS.class)
        {
            m_credentialForSSH = AwsHelper.API_CREDENTIALS_SITE;
        }
        else if (instanceType.deployerClass == AgentDeployerForAzure.class)
        {
            m_credentialForSSH = AzureHelper.API_CREDENTIALS_SITE;
        }
        else
        {
            m_credentialForSSH = "gateway.optio3.com";
        }
    }

    //--//

    public BuilderApplication getApplication()
    {
        return m_app;
    }

    public BuilderConfiguration getConfiguration()
    {
        return m_cfg;
    }

    public SshKey getSshKey()
    {
        return m_cfg.credentials.findFirstSshKey(m_credentialForSSH, null);
    }

    //--//

    public static CommonDeployer allocateDeployer(String instanceAccount,
                                                  DeploymentInstance instanceType,
                                                  String region,
                                                  BuilderConfiguration cfg,
                                                  CustomerRecord rec_cust,
                                                  CustomerServiceRecord rec_svc,
                                                  DeploymentHostRecord rec_host)
    {
        CommonDeployer.ConfigIdentities identities = new CommonDeployer.ConfigIdentities();

        if (rec_cust != null)
        {
            identities.customer.sysId = rec_cust.getSysId();
            identities.customer.id    = rec_cust.getCloudId();
            identities.customer.name  = rec_cust.getName();
        }

        if (rec_svc != null)
        {
            identities.service.sysId = rec_svc.getSysId();
            identities.service.id    = rec_svc.getName();
            identities.service.name  = rec_svc.getName();
        }

        if (rec_host != null)
        {
            identities.host.sysId = rec_host.getSysId();
            identities.host.id    = rec_host.getHostId();
            identities.host.name  = rec_host.getHostName();
        }

        if (instanceType.deployerClass == AgentDeployerForAWS.class)
        {
            Regions regionTyped = (Regions) instanceType.parseTypedInstanceRegion(region);

            AgentDeployerForAWS deployerForAWS = new AgentDeployerForAWS(cfg.credentials, instanceType.deployerArch, null, cfg.getCloudConnectionUrl(), identities, regionTyped);
            deployerForAWS.instanceType = instanceType.deployerContext.toString();

            if (instanceAccount != null)
            {
                deployerForAWS.domainName = instanceAccount;
            }

            return deployerForAWS;
        }

        if (instanceType.deployerClass == AgentDeployerForAzure.class)
        {
            Region regionTyped = (Region) instanceType.parseTypedInstanceRegion(region);

            AgentDeployerForAzure deployerForAzure = new AgentDeployerForAzure(cfg.credentials, instanceType.deployerArch, null, cfg.getCloudConnectionUrl(), identities, regionTyped);
            deployerForAzure.instanceType = (VirtualMachineSizeTypes) instanceType.deployerContext;

            if (instanceAccount != null)
            {
                deployerForAzure.domainName = instanceAccount;
            }

            return deployerForAzure;
        }

        throw Exceptions.newIllegalArgumentException("Unknown instance type: %s", instanceType);
    }
}
