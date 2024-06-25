/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.deploy;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import com.azure.core.management.Region;
import com.azure.resourcemanager.authorization.models.BuiltInRole;
import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.compute.models.CachingTypes;
import com.azure.resourcemanager.compute.models.Disks;
import com.azure.resourcemanager.compute.models.PowerState;
import com.azure.resourcemanager.compute.models.StorageAccountTypes;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes;
import com.azure.resourcemanager.compute.models.VirtualMachines;
import com.azure.resourcemanager.monitor.models.Metric;
import com.azure.resourcemanager.monitor.models.MetricCollection;
import com.azure.resourcemanager.monitor.models.MetricDefinition;
import com.azure.resourcemanager.monitor.models.MetricDefinitions;
import com.azure.resourcemanager.monitor.models.MetricValue;
import com.azure.resourcemanager.monitor.models.ResultType;
import com.azure.resourcemanager.monitor.models.TimeSeriesElement;
import com.azure.resourcemanager.network.NetworkManager;
import com.azure.resourcemanager.network.models.IpAllocationMethod;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.NetworkInterface;
import com.azure.resourcemanager.network.models.NetworkSecurityGroup;
import com.azure.resourcemanager.network.models.PublicIPSkuType;
import com.azure.resourcemanager.network.models.PublicIpAddress;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.infra.AzureHelper;
import com.optio3.infra.StatusCheckResult;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.logging.Logger;
import com.optio3.util.Base64EncodedValue;
import com.optio3.util.BoxingUtils;
import com.optio3.util.ConfigVariables;

public class AgentDeployerForAzure extends CommonDeployerForAzure
{
    public static final Logger LoggerInstance = new Logger(AgentDeployerForAzure.class);

    private static final ConfigVariables.Template<ConfigVariable> s_template_deployerBootScript_v1 = s_configValidator.newTemplate(AgentDeployerForAzure.class,
                                                                                                                                   "azure/deployer-boot-script_v1.txt",
                                                                                                                                   "$[",
                                                                                                                                   "]");

    //--//

    public VirtualMachineSizeTypes instanceType = VirtualMachineSizeTypes.STANDARD_B1S;

    //--//

    public AgentDeployerForAzure(CredentialDirectory credDir,
                                 DockerImageArchitecture arch,
                                 String builderHostName,
                                 String connectionUrl,
                                 ConfigIdentities identities,
                                 Region region)

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
        validateHostname();

        try (AzureHelper azure = fetchAzureHelper())
        {
            ResourceGroup resourceGroup = azure.ensureCustomerResourceGroup(identities.customer.sysId, identities.customer.name);

            //--//

            Network network = azure.getVirtualNetwork(resourceGroup, identities.service.sysId);
            if (network == null)
            {
                LoggerInstance.info("Creating new Virtual Network for '%s'", identities.service.name);

                network = azure.buildVirtualNetwork(resourceGroup, identities.service.sysId, identities.service.name)
                               .withAddressSpace("10.0.0.0/24")
                               .withSubnet("default", "10.0.0.0/24")
                               .create();
            }

            //--//

            NetworkSecurityGroup networkSecurityGroup = azure.getNetworkSecurityGroup(resourceGroup, identities.service.sysId);
            if (networkSecurityGroup == null)
            {
                LoggerInstance.info("Creating new Network Security Group for '%s'", identities.service.name);

                NetworkSecurityGroup.DefinitionStages.WithCreate nsg      = azure.buildNetworkSecurityGroup(resourceGroup, identities.service.sysId, identities.service.name);
                int                                              priority = 100;

                for (Integer port : portsTCP)
                {
                    nsg = AzureHelper.allowTCP(nsg, priority++, port);
                }

                for (Integer port : portsUDP)
                {
                    nsg = AzureHelper.allowUDP(nsg, priority++, port);
                }

                networkSecurityGroup = nsg.create();
            }

            //--//

            NetworkInterface itf = azure.getNetworkInterface(resourceGroup, identities.host.sysId);
            if (itf == null)
            {
                NetworkInterface.DefinitionStages.WithCreate itfBase = azure.buildNetworkInterface(network, identities.host.sysId, identities.host.name, "default");

                if (useStaticIp)
                {
                    for (PublicIpAddress publicIPAddress : azure.listPublicIPAddresses(resourceGroup, identities.service.sysId))
                    {
                        if (publicIPAddress.ipAllocationMethod() == IpAllocationMethod.STATIC)
                        {
                            LoggerInstance.info("Reusing Static IPAddress for '%s': %s", identities.service.name, publicIPAddress.ipAddress());

                            itf = itfBase.withExistingPrimaryPublicIPAddress(publicIPAddress)
                                         .withExistingNetworkSecurityGroup(networkSecurityGroup)
                                         .create();

                            break;
                        }
                    }

                    if (itf == null)
                    {
                        LoggerInstance.info("Creating new Static IPAddress for '%s'", identities.service.name);

                        PublicIpAddress.DefinitionStages.WithCreate ip = azure.buildPublicIpAddress(network, null, null)
                                                                              .withStaticIP()
                                                                              .withIdleTimeoutInMinutes(30)
                                                                              .withoutLeafDomainLabel()
                                                                              .withSku(PublicIPSkuType.BASIC);

                        itf = itfBase.withNewPrimaryPublicIPAddress(ip)
                                     .withExistingNetworkSecurityGroup(networkSecurityGroup)
                                     .create();
                    }
                }
                else
                {
                    LoggerInstance.info("Creating new Dynamic IPAddress for '%s'", identities.service.name);

                    PublicIpAddress.DefinitionStages.WithCreate ip = azure.buildPublicIpAddress(network, identities.host.sysId, identities.host.name)
                                                                          .withDynamicIP()
                                                                          .withIdleTimeoutInMinutes(30)
                                                                          .withoutLeafDomainLabel()
                                                                          .withSku(PublicIPSkuType.BASIC);

                    itf = itfBase.withNewPrimaryPublicIPAddress(ip)
                                 .withExistingNetworkSecurityGroup(networkSecurityGroup)
                                 .create();
                }
            }

            //--//

            VirtualMachine virtualMachine = azure.getVirtualMachine(resourceGroup, identities.host.sysId);
            if (virtualMachine == null)
            {
                ConfigVariables<ConfigVariable> parameters = s_template_deployerBootScript_v1.allocate();

                parameters.setValue(ConfigVariable.CustomerId, identities.customer.sysId);
                parameters.setValue(ConfigVariable.HostId, identities.host.sysId);
                parameters.setValue(ConfigVariable.ConfigValue, new Base64EncodedValue(generateConfiguration("", "")).toString());
                parameters.setValue(ConfigVariable.RepoAccount, repoReader.user);
                parameters.setValue(ConfigVariable.RepoPassword, repoReader.getEffectivePassword());
                parameters.setValue(ConfigVariable.ImageTag, imageTag);

                String scriptValue  = parameters.convert();
                String scriptBase64 = new Base64EncodedValue(scriptValue.getBytes()).toString();

                Map<String, String> tags = Maps.newHashMap(itf.tags());
                AzureHelper.updateTag(tags, ConfigTag.FunctionId, functionId);

                final int minimumDiskSize = 30; // 30GB is the minimum OS size on Azure...

                LoggerInstance.info("Creating new Virtual Machine for '%s'", identities.service.name);

                CompletableFuture<VirtualMachine> future = azure.buildVirtualMachine(itf)
                                                                .withLatestLinuxImage("OpenLogic", "CentOS", "7.5")
                                                                .withRootUsername(azure.sshKey.user)
                                                                .withSsh(new String(azure.sshKey.getPublicKey()))
                                                                .withOSDiskStorageAccountType(StorageAccountTypes.PREMIUM_LRS)
                                                                .withOSDiskCaching(CachingTypes.READ_WRITE)
                                                                .withSize(instanceType)
                                                                .withSystemAssignedManagedServiceIdentity()
                                                                .withSystemAssignedIdentityBasedAccessToCurrentResourceGroup(BuiltInRole.CONTRIBUTOR)
                                                                .defineNewExtension("config-os")
                                                                .withPublisher("Microsoft.Azure.Extensions")
                                                                .withType("CustomScript")
                                                                .withVersion("2.0")
                                                                .withPublicSetting("script", scriptBase64)
                                                                .attach()
                                                                .withTags(tags)
                                                                .withOSDiskSizeInGB(Math.max(minimumDiskSize, BoxingUtils.get(diskSize, minimumDiskSize)))
                                                                .createAsync()
                                                                .toFuture();

                Stopwatch st = Stopwatch.createStarted();

                while (true)
                {
                    try
                    {
                        future.get(30, TimeUnit.SECONDS);
                    }
                    catch (TimeoutException e)
                    {
                        LoggerInstance.info("Waiting for Virtual Machine for '%s': %s", identities.service.name, st.elapsed(TimeUnit.SECONDS));
                    }
                    catch (Throwable t)
                    {
                        LoggerInstance.error("Failed to create Virtual Machine for '%s', due to %s", identities.service.name, t);
                    }

                    if (future.isDone())
                    {
                        virtualMachine = future.get();
                        LoggerInstance.info("Started Virtual Machine for '%s' at '%s': %s",
                                            identities.service.name,
                                            virtualMachine.getPrimaryPublicIPAddress()
                                                          .ipAddress(),
                                            st.elapsed(TimeUnit.SECONDS));
                        break;
                    }
                }
            }

            //--//

            return virtualMachine.id();
        }
    }

    @Override
    public String getPublicIp()
    {
        try (AzureHelper azure = fetchAzureHelper())
        {
            ResourceGroup resourceGroup = azure.getCustomerResourceGroup(identities.customer.sysId);
            if (resourceGroup != null)
            {
                VirtualMachine virtualMachine = azure.getVirtualMachine(resourceGroup, identities.host.sysId);

                PublicIpAddress addr = virtualMachine.getPrimaryPublicIPAddress();
                if (addr != null)
                {
                    return addr.ipAddress();
                }
            }

            return null;
        }
    }

    @Override
    public List<Metrics> getMetrics(ZonedDateTime start,
                                    ZonedDateTime end,
                                    Duration interval)
    {
        try (AzureHelper azure = fetchAzureHelper())
        {
            ResourceGroup resourceGroup = azure.getCustomerResourceGroup(identities.customer.sysId);
            if (resourceGroup != null)
            {
                VirtualMachine virtualMachine = azure.getVirtualMachine(resourceGroup, identities.host.sysId);
                if (virtualMachine != null)
                {
                    class Accessor
                    {
                        final TreeMap<ZonedDateTime, Metrics> results = new TreeMap<>();

                        void accessMetric(MetricDefinition metricDefinition,
                                          BiConsumer<Metrics, Double> callback)
                        {
                            MetricCollection res = metricDefinition.defineQuery()
                                                                   .startingFrom(start.toOffsetDateTime())
                                                                   .endsBefore(end.toOffsetDateTime())
                                                                   .withAggregation("Average")
                                                                   .withInterval(interval)
                                                                   .withResultType(ResultType.DATA)
                                                                   .execute();

                            for (Metric metric : res.metrics())
                            {
                                for (TimeSeriesElement timeseries : metric.timeseries())
                                {
                                    for (MetricValue datum : timeseries.data())
                                    {
                                        Double value = datum.average();
                                        if (value != null)
                                        {
                                            Metrics metrics = results.computeIfAbsent(datum.timestamp()
                                                                                           .toZonedDateTime(), Metrics::new);

                                            callback.accept(metrics, value);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    final var accessor = new Accessor();

                    MetricDefinitions defs = azure.metricDefinitions();
                    for (MetricDefinition metricDefinition : defs.listByResource(virtualMachine.id()))
                    {
                        switch (metricDefinition.name()
                                                .value())
                        {
                            case "CPU Credits Remaining":
                                accessor.accessMetric(metricDefinition, (metrics, value) -> metrics.creditsRemaining = value);
                                break;

                            case "CPU Credits Consumed":
                                accessor.accessMetric(metricDefinition, (metrics, value) -> metrics.creditsConsumed = value);
                                break;

                            case "Percentage CPU":
                                accessor.accessMetric(metricDefinition, (metrics, value) -> metrics.cpuLoad = value);
                                break;
                        }
                    }

                    return Lists.newArrayList(accessor.results.values());
                }
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
        try (AzureHelper azure = fetchAzureHelper())
        {
            ResourceGroup resourceGroup = azure.getCustomerResourceGroup(identities.customer.sysId);
            if (resourceGroup != null)
            {
                VirtualMachine virtualMachine = azure.getVirtualMachine(resourceGroup, identities.host.sysId);
                if (virtualMachine != null)
                {
                    final ComputeManager vmManager = virtualMachine.manager();
                    Disks                disks     = vmManager.disks();
                    VirtualMachines      vms       = vmManager.virtualMachines();

                    String diskId = virtualMachine.osDiskId();
                    String vmId   = virtualMachine.id();

                    vms.deleteById(vmId);
                    disks.deleteById(diskId);
                }

                NetworkInterface itf = azure.getNetworkInterface(resourceGroup, identities.host.sysId);
                if (itf != null)
                {
                    final NetworkManager netManager = itf.manager();

                    PublicIpAddress ip = itf.primaryIPConfiguration()
                                            .getPublicIpAddress();
                    String itfId = itf.id();

                    netManager.networkInterfaces()
                              .deleteById(itfId);

                    if (ip.ipAllocationMethod() == IpAllocationMethod.DYNAMIC)
                    {
                        netManager.publicIpAddresses()
                                  .deleteById(ip.id());
                    }
                }
            }
        }
    }

    @Override
    public void cleanupService()
    {
        try (AzureHelper azure = fetchAzureHelper())
        {
            ResourceGroup resourceGroup = azure.getCustomerResourceGroup(identities.customer.sysId);
            if (resourceGroup != null)
            {
                Network network = azure.getVirtualNetwork(resourceGroup, identities.service.sysId);
                if (network != null)
                {
                    NetworkManager manager = network.manager();

                    manager.networks()
                           .deleteById(network.id());

                    NetworkSecurityGroup networkSecurityGroup = azure.getNetworkSecurityGroup(resourceGroup, identities.service.sysId);
                    if (networkSecurityGroup != null)
                    {
                        manager.networkSecurityGroups()
                               .deleteById(networkSecurityGroup.id());
                    }
                }
            }
        }
    }

    @Override
    public void cleanupCustomerInRegion()
    {
        // All the resources are globally tracked.
    }

    @Override
    public void cleanupCustomer()
    {
        try (AzureHelper azure = fetchAzureHelper())
        {
            azure.deleteCustomerResourceGroup(identities.customer.sysId);
        }
    }

    //--//

    @Override
    public StatusCheckResult checkForStartup()
    {
        validateHostname();

        return StatusCheckResult.Positive;
    }

    public StatusCheckResult checkForTermination()
    {
        try (AzureHelper azure = fetchAzureHelper())
        {
            ResourceGroup resourceGroup = azure.getCustomerResourceGroup(identities.customer.sysId);
            if (resourceGroup == null)
            {
                return StatusCheckResult.Positive; // No result from the query, assume it's gone.
            }

            VirtualMachine virtualMachine = azure.getVirtualMachine(resourceGroup, identities.host.sysId);
            if (virtualMachine == null)
            {
                return StatusCheckResult.Positive; // No result from the query, assume it's gone.
            }

            if (virtualMachine.powerState() == PowerState.RUNNING)
            {
                return StatusCheckResult.Negative;
            }

            return StatusCheckResult.Pending;
        }
    }
}
