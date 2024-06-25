/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import java.util.Collections;
import java.util.List;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;
import com.azure.core.management.Region;
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes;
import com.google.common.collect.Lists;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.model.IEnumDescription;
import com.optio3.infra.deploy.AgentDeployerForAWS;
import com.optio3.infra.deploy.AgentDeployerForAzure;
import com.optio3.infra.deploy.CommonDeployer;
import org.apache.commons.lang3.StringUtils;

public enum DeploymentInstance implements IEnumDescription
{
    // @formatter:off
    AWS_T2_Micro  ("AWS t2.micro (1 vCPU, 1GB)"         , true , true , true , null                  , 1,   1024, 0.0116, DockerImageArchitecture.X86    , AgentDeployerForAWS  .class, InstanceType.T2Micro                 , Regions.US_WEST_2),
    AWS_T2_Small  ("AWS t2.small (1 vCPU, 2GB)"         , true , true , true , null                  , 1,   2048, 0.023 , DockerImageArchitecture.X86    , AgentDeployerForAWS  .class, InstanceType.T2Small                 , Regions.US_WEST_2),
    AWS_T2_Medium ("AWS t2.medium (2 vCPU, 4GB)"        , true , true , true , null                  , 2,   4096, 0.0464, DockerImageArchitecture.X86    , AgentDeployerForAWS  .class, InstanceType.T2Medium                , Regions.US_WEST_2),
    AWS_T3_Small  ("AWS t3.small (2 vCPU, 2GB)"         , true , true , true , null                  , 2,   2048, 0.0208, DockerImageArchitecture.X86    , AgentDeployerForAWS  .class, InstanceType.T3Small                 , Regions.US_WEST_2),
    AWS_T3_Medium ("AWS t3.medium (2 vCPU, 4GB)"        , true , true , true , null                  , 2,   4096, 0.0416, DockerImageArchitecture.X86    , AgentDeployerForAWS  .class, InstanceType.T3Medium                , Regions.US_WEST_2),
    AWS_T3_Large  ("AWS t3.large (2 vCPU, 8GB)"         , true , true , true , null                  , 2,   8192, 0.0832, DockerImageArchitecture.X86    , AgentDeployerForAWS  .class, InstanceType.T3Large                 , Regions.US_WEST_2),
    AWS_T3_XLarge ("AWS t3.xlarge (4 vCPU, 16GB)"       , true , true , true , null                  , 4,  16384, 0.1664, DockerImageArchitecture.X86    , AgentDeployerForAWS  .class, InstanceType.T3Xlarge                , Regions.US_WEST_2),

    AWS_T4G_Small ("AWS t4g.small (2 vCPU, 2GB, ARM64)" , true , true , true , null                  , 2,   2048, 0.0168, DockerImageArchitecture.ARM64v8, AgentDeployerForAWS  .class, "t4g.small"                          , Regions.US_WEST_2),
    AWS_T4G_Medium("AWS t4g.medium (2 vCPU, 4GB, ARM64)", true , true , true , null                  , 2,   4096, 0.0336, DockerImageArchitecture.ARM64v8, AgentDeployerForAWS  .class, "t4g.medium"                         , Regions.US_WEST_2),
    AWS_T4G_Large ("AWS t4g.large (2 vCPU, 8GB, ARM64)" , true , true , true , null                  , 2,   8192, 0.0672, DockerImageArchitecture.ARM64v8, AgentDeployerForAWS  .class, "t4g.large"                          , Regions.US_WEST_2),
    AWS_T4G_XLarge("AWS t4g.large (4 vCPU, 16GB, ARM64)", true , true , true , null                  , 2,  16384, 0.1344, DockerImageArchitecture.ARM64v8, AgentDeployerForAWS  .class, "t4g.large"                          , Regions.US_WEST_2),

    AZURE_B1S     ("Azure B1S (1 vCPU, 1GB)"            , true , true , true , null                  , 1,   1024, 0.0124, DockerImageArchitecture.X86    , AgentDeployerForAzure.class, VirtualMachineSizeTypes.STANDARD_B1S , Region.US_WEST   ),
    AZURE_B1MS    ("Azure B1MS (1 vCPU, 2GB)"           , true , true , true , null                  , 1,   2048, 0.0248, DockerImageArchitecture.X86    , AgentDeployerForAzure.class, VirtualMachineSizeTypes.STANDARD_B1MS, Region.US_WEST   ),
    AZURE_B2S     ("Azure B2S (2 vCPU, 4GB)"            , true , true , true , null                  , 2,   4096, 0.0496, DockerImageArchitecture.X86    , AgentDeployerForAzure.class, VirtualMachineSizeTypes.STANDARD_B2S , Region.US_WEST   ),
    AZURE_B2MS    ("Azure B2MS (2 vCPU, 8GB)"           , true , true , true , null                  , 2,   8192, 0.0992, DockerImageArchitecture.X86    , AgentDeployerForAzure.class, VirtualMachineSizeTypes.STANDARD_B2MS, Region.US_WEST   ),
    AZURE_B4MS    ("Azure B4MS (4 vCPU, 16GB)"          , true , true , true , null                  , 4,  16384, 0.166 , DockerImageArchitecture.X86    , AgentDeployerForAzure.class, VirtualMachineSizeTypes.STANDARD_B4MS, Region.US_WEST   ),

    Edge          ("Edge Gateway"                       , true , false, true , null                  , 0,      0, 0.0   , null                           , null                       , null                                 , null             ),
    AZURE_EDGE    ("Azure Edge Gateway"                 , true , false, false, DeploymentRole.gateway, 0,      0, 0.0   , null                           , null                       , null                                 , null             ),
    DigitalMatter ("Digital Matter"                     , false, false, false, DeploymentRole.tracker, 0,      0, 0.0   , null                           , null                       , null                                 , null             );
    // @formatter:on

    private final String                          m_displayName;
    public final  boolean                         isDeployable;
    public final  boolean                         canTerminate;
    public final  boolean                         hasAgent;
    public final  DeploymentRole                  autoRole;
    public final  int                             vCPU;
    public final  int                             memory;
    public final  double                          costPerHour;
    public final  DockerImageArchitecture         deployerArch;
    public final  Class<? extends CommonDeployer> deployerClass;
    public final  Object                          deployerContext;
    public final  String                          deployerDefaultRegion;

    DeploymentInstance(String displayName,
                       boolean isDeployable,
                       boolean canTerminate,
                       boolean hasAgent,
                       DeploymentRole autoRole,
                       int vCPU,
                       int memory,
                       double costPerHour,
                       DockerImageArchitecture arch,
                       Class<? extends CommonDeployer> clz,
                       Object ctx,
                       Object region)
    {
        m_displayName              = displayName;
        this.isDeployable          = isDeployable;
        this.canTerminate          = canTerminate;
        this.hasAgent              = hasAgent;
        this.autoRole              = autoRole;
        this.vCPU                  = vCPU;
        this.memory                = memory;
        this.costPerHour           = costPerHour;
        this.deployerArch          = arch;
        this.deployerClass         = clz;
        this.deployerContext       = ctx;
        this.deployerDefaultRegion = region != null ? region.toString() : null;
    }

    @Override
    public String getDisplayName()
    {
        return m_displayName;
    }

    @Override
    public String getDescription()
    {
        return String.format("$%.2f dollars per month", costPerHour * 24 * 365 / 12);
    }

    public List<Object> getAvailableRegions()
    {
        List<Object> lst = Lists.newArrayList();

        if (deployerClass == AgentDeployerForAWS.class)
        {
            for (Regions value : Regions.values())
            {
                lst.add(value.name());
            }
        }

        if (deployerClass == AgentDeployerForAzure.class)
        {
            for (Region value : Region.values())
            {
                lst.add(value.name());
            }
        }

        return lst;
    }

    public List<String> getAvailableAccounts(BuilderConfiguration cfg)
    {
        if (deployerClass == AgentDeployerForAWS.class)
        {
            AgentDeployerForAWS deployerForAWS = new AgentDeployerForAWS(cfg.credentials, deployerArch, null, null, null, null);

            return deployerForAWS.getAvailableAccounts();
        }

        if (deployerClass == AgentDeployerForAzure.class)
        {
            AgentDeployerForAzure deployerForAzure = new AgentDeployerForAzure(cfg.credentials, deployerArch, null, null, null, null);

            return deployerForAzure.getAvailableAccounts();
        }

        return Collections.emptyList();
    }

    public Object parseTypedInstanceRegion(String instanceRegion)
    {
        if (deployerClass == AgentDeployerForAWS.class)
        {
            for (Regions value : Regions.values())
            {
                if (StringUtils.equals(instanceRegion, value.name()))
                {
                    return value;
                }
            }
        }

        if (deployerClass == AgentDeployerForAzure.class)
        {
            for (Region value : Region.values())
            {
                if (StringUtils.equals(instanceRegion, value.name()))
                {
                    return value;
                }
            }
        }

        return null;
    }
}
