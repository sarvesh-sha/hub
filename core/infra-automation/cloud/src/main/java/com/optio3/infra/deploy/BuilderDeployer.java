/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.deploy;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.Route;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.Role;
import com.google.common.collect.Lists;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.concurrency.Executors;
import com.optio3.infra.AnnotatedName;
import com.optio3.infra.AwsHelper;
import com.optio3.infra.StatusCheckResult;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.CredentialDirectory;

public class BuilderDeployer extends CommonDeployerForAWS
{
    public String roleName       = "Builder-Instance";
    public String instanceConfig = "builder";

    public String computeInstanceName        = "builder";
    public String computeInstanceDisplayName = "Builder service";

    public String vpcName        = "vpc--builder";
    public String vpcDisplayName = "Builder service";

    public String hostName   = "builder.dev";
    public String domainName = WellKnownSites.optio3DomainName();

    //--//

    public BuilderDeployer(CredentialDirectory credDir)
    {
        super(credDir, DockerImageArchitecture.X86, null, null, null, Regions.US_WEST_2);
    }

    //--//

    @Override
    public String deploy(boolean waitForStartup,
                         boolean allowSNS,
                         boolean allowEmail) throws
                                             IOException
    {
        try (AwsHelper aws = fetchAwsHelper(null))
        {
            Policy policy = new Policy();

            // Allow reading the configuration.
            aws.addAccessToInstanceConfig(policy, instanceConfig, S3Actions.GetObject);

            // Allow reading and writing the backup.
            aws.addAccessToInstanceBackup(policy, computeInstanceName, S3Actions.GetObject, S3Actions.PutObject);

            Role            role    = aws.acquireRole(roleName, "Handle-Instance-Data", policy);
            InstanceProfile profile = aws.acquireInstanceProfile(role, roleName);

            //--//

            AnnotatedName vpcAnnotatedName = new AnnotatedName();
            vpcAnnotatedName.uniqueIdentifier = vpcName;
            vpcAnnotatedName.description = vpcDisplayName;

            List<AvailabilityZone> zones = aws.listAvailabilityZones();

            Vpc vpc = aws.acquireVpc(vpcAnnotatedName, "10.0.0.0/16");

            InternetGateway ig = aws.acquireInternetGateway(vpc, vpcAnnotatedName);

            @SuppressWarnings("unused") Route route = aws.acquireRoute(vpc, vpcAnnotatedName, "0.0.0.0/0", ig);

            List<Subnet> subnets = Lists.newArrayList();
            for (int i = 0; i < zones.size(); i++)
            {
                AvailabilityZone az = zones.get(i);

                Subnet subnet = aws.acquireSubnet(vpc, vpcAnnotatedName, az.getZoneName(), String.format("10.0.%d.0/24", i + 1));
                subnets.add(subnet);
            }

            AnnotatedName securityGroupName = new AnnotatedName();
            securityGroupName.uniqueIdentifier = computeInstanceName;
            securityGroupName.description = "Default SecurityGroup for " + computeInstanceName;

            SecurityGroup sc = aws.acquireSecurityGroup(vpc, securityGroupName, vpcAnnotatedName, (scNew) ->
            {
                aws.authorizeIngress(scNew, "tcp", PORT_TCP_SSH, "0.0.0.0/0");
                aws.authorizeIngress(scNew, "tcp", PORT_TCP_HTTPS, "0.0.0.0/0");
                aws.authorizeIngress(scNew, "udp", PORT_UDP_MESSAGEBUS, "0.0.0.0/0");
            });

            Executors.safeSleep(5000);

            AwsHelper.InstanceConfig cfg = new AwsHelper.InstanceConfig(targetArch);

            cfg.sc = sc;
            cfg.subnet = subnets.get(0);
            cfg.privateIp = "10.0.1.10";

            cfg.instanceType = InstanceType.T3Xlarge.toString();
            cfg.bootDiskSize = 64;

            cfg.profile = profile;
            aws.useGenericBootScript(cfg, "builder", repoReader.user, repoReader.getEffectivePassword());

            Instance instance = aws.acquireInstance(computeInstanceDisplayName, computeInstanceName, cfg);

            boolean success = aws.pollForStartup(instance, 10, 10, (elapsed, refreshedInstance) ->
            {
                String val = refreshedInstance.getState()
                                              .getName();
                System.out.printf("Status: [%d] %s%n", elapsed.getSeconds(), val);

                return true;
            });

            if (success)
            {
                Address eip = associateDnsWithElasticIp();
                aws.associateElasticIp(instance, eip);
            }

            return instance.getInstanceId();
        }
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
