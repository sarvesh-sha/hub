/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.deploy;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.model.Datapoint;
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
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.concurrency.Executors;
import com.optio3.infra.AnnotatedName;
import com.optio3.infra.AwsHelper;
import com.optio3.infra.StatusCheckResult;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.util.ConfigVariables;
import com.optio3.util.FileSystem;
import com.optio3.util.TimeUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class AgentDeployerForAWS extends CommonDeployerForAWS
{
    private static final ConfigVariables.Template<ConfigVariable> s_template_deployerBootScript_v1 = s_configValidator.newTemplate(AgentDeployerForAWS.class,
                                                                                                                                   "aws/deployer-boot-script_v1.txt",
                                                                                                                                   "$[",
                                                                                                                                   "]");

    private static final ConfigVariables.Template<ConfigVariable> s_template_deployerBootScript_v2 = s_configValidator.newTemplate(AgentDeployerForAWS.class,
                                                                                                                                   "aws/deployer-boot-script_v2.txt",
                                                                                                                                   "$[",
                                                                                                                                   "]");

    //--//

    public String instanceType = InstanceType.T2Micro.toString();

    //--//

    static class ComputedState
    {
        final String sanitizedCustomerId;

        final String computeInstanceName;
        final String computeInstanceDisplayName;

        final String configFile;

        ComputedState(ConfigIdentities identities)
        {
            sanitizedCustomerId = AwsHelper.sanitizeId(identities.customer.id);

            computeInstanceName = identities.host.id;

            if (StringUtils.isNotEmpty(identities.customer.id))
            {
                final String ignorePrefix = "Customer__";

                computeInstanceDisplayName = identities.customer.id.replace(ignorePrefix, "") + " # " + identities.host.id;
            }
            else
            {
                computeInstanceDisplayName = "Managed Instance for " + identities.host.id;
            }

            configFile = "config-" + identities.host.id + ".tgz";
        }
    }

    private final Supplier<ComputedState> m_state = Suppliers.memoize(() -> new ComputedState(identities));

    //--//

    public AgentDeployerForAWS(CredentialDirectory credDir,
                               DockerImageArchitecture arch,
                               String builderHostName,
                               String connectionUrl,
                               ConfigIdentities identities,
                               Regions region)

    {
        super(credDir, arch, builderHostName, connectionUrl, identities, region);
    }

    //--//

    @Override
    public String deploy(boolean waitForStartup,
                         boolean allowSNS,
                         boolean allowEmail) throws
                                             Exception
    {
        var state = m_state.get();

        validateHostname();

        //--//

        generateConfigurationOnS3();

        //--//

        AwsHelper.InstanceConfig instanceCfg = new AwsHelper.InstanceConfig(targetArch);

        {
            ConfigVariables<ConfigVariable> parameters = s_template_deployerBootScript_v2.allocate();

            parameters.setValue(ConfigVariable.CustomerId, state.sanitizedCustomerId);
            parameters.setValue(ConfigVariable.HostId, identities.host.id);
            parameters.setValue(ConfigVariable.ConfigFile, state.configFile);
            parameters.setValue(ConfigVariable.RepoAccount, repoReader.user);
            parameters.setValue(ConfigVariable.RepoPassword, repoReader.getEffectivePassword());
            parameters.setValue(ConfigVariable.ImageTag, imageTag);

            instanceCfg.userData = parameters.convert();
        }

        //--//

        try (AwsHelper aws = fetchAwsHelper(null))
        {
            Policy policy = new Policy();

            // Allow reading and deleting the configuration.
            aws.addAccessToInstanceConfig(policy, state.sanitizedCustomerId, S3Actions.GetObject, S3Actions.DeleteObject);

            // Allow reading and writing the backups.
            aws.addAccessToInstanceBackup(policy, state.sanitizedCustomerId, S3Actions.GetObject, S3Actions.PutObject);

            if (allowSNS)
            {
                aws.addAccessToSNS(policy);
            }

            if (allowEmail)
            {
                aws.addAccessToEmail(policy);
            }

            Role            role    = aws.acquireRole(state.sanitizedCustomerId, "Handle-Instance-Data", policy);
            InstanceProfile profile = aws.acquireInstanceProfile(role, state.sanitizedCustomerId);

            //--//

            AnnotatedName vpcAnnotatedName = new AnnotatedName();
            vpcAnnotatedName.uniqueIdentifier = state.sanitizedCustomerId;
            vpcAnnotatedName.description      = "Network for " + identities.customer.id;

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
            securityGroupName.uniqueIdentifier = identities.host.id;
            securityGroupName.description      = "Default SecurityGroup for " + identities.host.id;

            SecurityGroup sc = aws.acquireSecurityGroup(vpc, securityGroupName, vpcAnnotatedName, (scNew) ->
            {
                for (Integer port : portsTCP)
                {
                    aws.authorizeIngress(scNew, "tcp", port, "0.0.0.0/0");
                }

                for (Integer port : portsUDP)
                {
                    aws.authorizeIngress(scNew, "udp", port, "0.0.0.0/0");
                }
            });

            instanceCfg.sc     = sc;
            instanceCfg.subnet = subnets.get(0);

            instanceCfg.instanceType = instanceType;

            if (diskSize != null)
            {
                instanceCfg.bootDiskSize = diskSize;
            }

            instanceCfg.profile = profile;

            instanceCfg.addTag(ConfigTag.HostId.getTag(), identities.host.id);
            instanceCfg.addTagIfValuePresent(ConfigTag.CustomerId.getTag(), identities.customer.id);
            instanceCfg.addTagIfValuePresent(ConfigTag.ServiceId.getTag(), identities.service.id);
            instanceCfg.addTagIfValuePresent(ConfigTag.FunctionId.getTag(), functionId);

            //--//

            Instance instance;
            int      retries = 0;

            while (true)
            {
                Executors.safeSleep(1000);

                try
                {
                    instance = aws.acquireInstance(state.computeInstanceDisplayName, state.computeInstanceName, instanceCfg);
                    break;
                }
                catch (Throwable t)
                {
                    if (retries++ < 30)
                    {
                        // Retry, policy changes take a while to propagate.
                        continue;
                    }

                    throw t;
                }
            }

            if (waitForStartup)
            {
                boolean success = aws.pollForStartup(instance, 10, 10, (elapsed, refreshedInstance) ->
                {
                    String val = refreshedInstance.getState()
                                                  .getName();
                    System.out.printf("Status: [%d] %s%n", elapsed.getSeconds(), val);

                    return true;
                });

                if (success)
                {
                    if (useStaticIp)
                    {
                        Address eip = associateDnsWithElasticIp();
                        aws.associateElasticIp(instance, eip);
                    }
                }
            }

            return instance.getInstanceId();
        }
    }

    @Override
    public String getPublicIp()
    {
        var state = m_state.get();

        try (AwsHelper aws = fetchAwsHelper(null))
        {
            Instance instance = aws.findInstanceByPurpose(state.computeInstanceName);
            if (instance != null)
            {
                return instance.getPublicIpAddress();
            }

            return null;
        }
    }

    @Override
    public List<Metrics> getMetrics(ZonedDateTime start,
                                    ZonedDateTime end,
                                    Duration interval)
    {
        var state = m_state.get();

        try (AwsHelper aws = fetchAwsHelper(null))
        {
            Instance instance = aws.findInstanceByPurpose(state.computeInstanceName);
            if (instance != null)
            {
                class Accessor
                {
                    final TreeMap<ZonedDateTime, Metrics> results = new TreeMap<>();

                    void accessMetric(String metricName,
                                      BiConsumer<Datapoint, Metrics> callback)
                    {
                        for (Datapoint datapoint : aws.getMetric("AWS/EC2", metricName, instance, start, end, (int) interval.toSeconds(), "Average"))
                        {
                            ZonedDateTime timestamp = TimeUtils.fromInstantToUtcTime(datapoint.getTimestamp()
                                                                                              .toInstant());
                            Metrics metric = results.computeIfAbsent(timestamp, Metrics::new);

                            callback.accept(datapoint, metric);
                        }
                    }
                }

                final var accessor = new Accessor();
                accessor.accessMetric("CPUCreditUsage", (dp, metric) -> metric.creditsConsumed = dp.getAverage());
                accessor.accessMetric("CPUCreditBalance", (dp, metric) -> metric.creditsRemaining = dp.getAverage());
                accessor.accessMetric("CPUSurplusCreditBalance", (dp, metric) -> metric.creditsRemainingSurplus = dp.getAverage());
                accessor.accessMetric("CPUUtilization", (dp, metric) -> metric.cpuLoad = dp.getAverage());

                return Lists.newArrayList(accessor.results.values());
            }

            return Collections.emptyList();
        }
    }

    @Override
    public void updateDns(String oldIp,
                          String newIp)
    {
        refreshDns(oldIp, newIp);
    }

    @Override
    public void terminate(boolean waitForShutdown)
    {
        var state = m_state.get();

        try (AwsHelper aws = fetchAwsHelper(null))
        {
            Instance instance = aws.findInstanceByPurpose(state.computeInstanceName);
            if (instance != null)
            {
                aws.releaseInstance(instance);

                if (waitForShutdown)
                {
                    aws.pollForTermination(instance, 10, 10, (elapsed, refreshedInstance) ->
                    {
                        String val = refreshedInstance.getState()
                                                      .getName();
                        System.out.printf("Status: [%d] %s%n", elapsed.getSeconds(), val);

                        return true;
                    });
                }
            }
        }
    }

    @Override
    public void cleanupService()
    {
        // Nothing is tracked per-service on AWS.
    }

    @Override
    public void cleanupCustomerInRegion()
    {
        var state = m_state.get();

        try (AwsHelper aws = fetchAwsHelper(null))
        {
            AnnotatedName vpcAnnotatedName = new AnnotatedName();
            vpcAnnotatedName.uniqueIdentifier = state.sanitizedCustomerId;

            Vpc vpc = aws.findVpc(vpcAnnotatedName);
            if (vpc != null)
            {
                aws.releaseVpc(vpc);
            }

            InstanceProfile profile = aws.findInstanceProfile(state.sanitizedCustomerId);
            if (profile != null)
            {
                aws.releaseInstanceProfile(profile);
            }

            Role role = aws.findRole(state.sanitizedCustomerId);
            if (role != null)
            {
                aws.releaseRole(role);
            }
        }
    }

    @Override
    public void cleanupCustomer()
    {
        // All the resources are tracked per-region.
    }

    //--//

    @Override
    public StatusCheckResult checkForStartup()
    {
        var state = m_state.get();

        validateHostname();

        try (AwsHelper aws = fetchAwsHelper(null))
        {
            Instance instance = aws.findInstanceByPurpose(state.computeInstanceName);
            if (instance == null)
            {
                // No instance on AWS. Assume something went wrong.
                return StatusCheckResult.Negative;
            }

            return aws.checkForStartup(instance, null);
        }
    }

    public StatusCheckResult checkForTermination()
    {
        var state = m_state.get();

        try (AwsHelper aws = fetchAwsHelper(null))
        {
            StatusCheckResult res;

            Instance instance = aws.findInstanceByPurpose(state.computeInstanceName);
            if (instance == null)
            {
                res = StatusCheckResult.Positive; // No result from the query, assume it's gone.
            }
            else
            {
                res = aws.checkForTermination(instance, null);
            }

            if (res == StatusCheckResult.Positive)
            {
                AnnotatedName vpcAnnotatedName = new AnnotatedName();
                vpcAnnotatedName.uniqueIdentifier = state.sanitizedCustomerId;

                Vpc vpc = aws.findVpc(vpcAnnotatedName);
                if (vpc != null)
                {
                    AnnotatedName securityGroupName = new AnnotatedName();
                    securityGroupName.uniqueIdentifier = identities.host.id;

                    SecurityGroup sg = aws.findSecurityGroup(vpc, securityGroupName);
                    if (sg != null)
                    {
                        aws.releaseSecurityGroup(sg);
                    }
                }
            }

            return res;
        }
    }

    public void generateConfigurationOnS3() throws
                                            IOException,
                                            InterruptedException
    {
        var state = m_state.get();

        try (FileSystem.TmpFileHolder holder = FileSystem.createTempFile())
        {
            FileUtils.writeByteArrayToFile(holder.get(), generateConfiguration("", ""));

            try (AwsHelper aws = fetchAwsHelper(Regions.US_WEST_2)) // S3 Bucket is in this region.
            {
                aws.saveFileToS3(aws.formatConfigPath(state.sanitizedCustomerId, state.configFile), holder.get());
            }
        }
    }

    public List<Instance> listActiveDeployments()
    {
        try (AwsHelper aws = fetchAwsHelper(null))
        {
            return aws.filterInstances(true, AwsHelper.filterForTagValue(ConfigTag.CustomerSysId.getTag(), identities.customer.id));
        }
    }
}
