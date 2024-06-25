/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.azure;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.resourcemanager.authorization.models.BuiltInRole;
import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.compute.models.CachingTypes;
import com.azure.resourcemanager.compute.models.Disks;
import com.azure.resourcemanager.compute.models.StorageAccountTypes;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineImage;
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes;
import com.azure.resourcemanager.compute.models.VirtualMachines;
import com.azure.resourcemanager.network.NetworkManager;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.NetworkInterface;
import com.azure.resourcemanager.network.models.NetworkSecurityGroup;
import com.azure.resourcemanager.network.models.PublicIPSkuType;
import com.azure.resourcemanager.network.models.PublicIpAddress;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.storage.models.Endpoints;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobItem;
import com.optio3.infra.AzureHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.deploy.AgentDeployerForAWS;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.test.infra.Optio3InfraTest;
import com.optio3.util.Base64EncodedValue;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class AzureTest extends Optio3InfraTest
{
    AzureHelper azure;

    @Before
    public void setup() throws
                        Exception
    {
        ensureCredentials(true, true);

        azure = AzureHelper.buildWithDirectoryLookup(credDir, WellKnownSites.optio3DomainName(), AzureEnvironment.AZURE, Region.US_WEST);
    }

    //--//

    @Ignore("Manually enable to test, since it requires access to AZURE")
    @Test
    public void listImages()
    {
        for (VirtualMachineImage virtualMachineImage : azure.getVirtualMachineImages())
        {
            System.out.printf("%s: %s # %s\n", virtualMachineImage.id(), virtualMachineImage.publisherName(), virtualMachineImage.version());
        }
    }

    //--//

    public enum ConfigVariable implements IConfigVariable
    {
        WebSocketConnectionUrl("WS_CONNECTION_URL"),
        CustomerId("CUSTOMER_ID"),
        HostId("HOST_ID"),
        InstanceId("INSTANCE_ID"),
        ImageTag("IMAGE_TAG"),
        ConfigValue("CONFIG_VALUE"),
        RepoAccount("REPO_ACCOUNT"),
        RepoPassword("REPO_PASSWORD"),
        MachineAccountName("ACCOUNT_NAME"),
        MachineAccountPassword("ACCOUNT_PASSWORD");

        private final String m_variable;

        ConfigVariable(String variable)
        {
            m_variable = variable;
        }

        public String getVariable()
        {
            return m_variable;
        }
    }

    @Ignore("Manually enable to test, since it requires access to AZURE")
    @Test
    public void startInstance()
    {
        final String customerId   = "test";
        final String customerName = "Test Customer";

        final String serviceId   = "testSvc1";
        final String serviceName = "Test Service";

        final String hostId   = "testVm1";
        final String hostName = "Test Host 2";

        ConfigVariables.Validator<ConfigVariable> configValidator                = new ConfigVariables.Validator<>(ConfigVariable.values());
        ConfigVariables.Template<ConfigVariable>  template_deployerBootScript_v1 = configValidator.newTemplate(AgentDeployerForAWS.class, "azure/deployer-boot-script_v1.txt", "$[", "]");

        String   repo       = WellKnownSites.dockerRegistry();
        UserInfo repoReader = credDir.findFirstAutomationUser(repo, RoleType.Subscriber);

        ConfigVariables<ConfigVariable> parameters = template_deployerBootScript_v1.allocate();

        parameters.setValue(ConfigVariable.CustomerId, customerId);
        parameters.setValue(ConfigVariable.HostId, "hostId");
        parameters.setValue(ConfigVariable.ConfigValue, "foo");
        parameters.setValue(ConfigVariable.RepoAccount, repoReader.user);
        parameters.setValue(ConfigVariable.RepoPassword, repoReader.getEffectivePassword());
        parameters.setValue(ConfigVariable.ImageTag, "optio3-deployer:bootstrap");

        String scriptValue  = parameters.convert();
        String scriptBase64 = new Base64EncodedValue(scriptValue.getBytes()).toString();

        //--//

        ResourceGroup resourceGroup = azure.ensureCustomerResourceGroup(customerId, customerName);

        System.out.printf("ResourceGroup: %s\n", resourceGroup.id());

        //--//

        Network network = azure.getVirtualNetwork(resourceGroup, serviceId);
        if (network == null)
        {
            network = azure.buildVirtualNetwork(resourceGroup, serviceId, serviceName)
                           .withAddressSpace("10.0.0.0/24")
                           .withSubnet("default", "10.0.0.0/24")
                           .create();
        }

        System.out.printf("Network: %s\n", network.id());

        //--//

        NetworkSecurityGroup networkSecurityGroup = azure.getNetworkSecurityGroup(resourceGroup, serviceId);
        if (networkSecurityGroup == null)
        {
            NetworkSecurityGroup.DefinitionStages.WithCreate nsg1 = azure.buildNetworkSecurityGroup(resourceGroup, serviceId, serviceName);
            NetworkSecurityGroup.DefinitionStages.WithCreate nsg2 = AzureHelper.allowTCP(nsg1, 100, 22);
            NetworkSecurityGroup.DefinitionStages.WithCreate nsg3 = AzureHelper.allowUDP(nsg2, 101, 433);
            networkSecurityGroup = nsg3.create();
        }

        System.out.printf("NetworkSecurityGroup: %s\n", networkSecurityGroup.id());

        //--//

        NetworkInterface itf = azure.getNetworkInterface(resourceGroup, hostId);
        if (itf == null)
        {
            PublicIpAddress.DefinitionStages.WithCreate ip = azure.buildPublicIpAddress(network, hostId, hostName)
                                                                  .withDynamicIP()
                                                                  .withoutLeafDomainLabel()
                                                                  .withSku(PublicIPSkuType.BASIC);

            itf = azure.buildNetworkInterface(network, hostId, hostName, "default")
                       .withNewPrimaryPublicIPAddress(ip)
                       .withExistingNetworkSecurityGroup(networkSecurityGroup)
                       .create();
        }

        System.out.printf("NetworkInterface: %s\n", itf.id());

        //--//

        VirtualMachine virtualMachine = azure.getVirtualMachine(resourceGroup, hostId);
        if (virtualMachine == null)
        {
            virtualMachine = azure.buildVirtualMachine(itf)
                                  .withLatestLinuxImage("OpenLogic", "CentOS", "7.5")
                                  .withRootUsername(azure.sshKey.user)
                                  .withSsh(new String(azure.sshKey.getPublicKey()))
                                  .withOSDiskStorageAccountType(StorageAccountTypes.PREMIUM_LRS)
                                  .withOSDiskCaching(CachingTypes.READ_WRITE)
                                  .withSize(VirtualMachineSizeTypes.STANDARD_B1S)
                                  .withSystemAssignedManagedServiceIdentity()
                                  .withSystemAssignedIdentityBasedAccessToCurrentResourceGroup(BuiltInRole.CONTRIBUTOR)
                                  .defineNewExtension("config-os")
                                  .withPublisher("Microsoft.Azure.Extensions")
                                  .withType("CustomScript")
                                  .withVersion("2.0")
                                  .withPublicSetting("script", scriptBase64)
                                  .attach()
                                  .withTags(itf.tags())
                                  .create();
        }

        System.out.printf("VirtualMachine: %s\n", virtualMachine.id());

        System.out.println(virtualMachine.getPrimaryPublicIPAddress()
                                         .ipAddress());

        final ComputeManager vmManager  = virtualMachine.manager();
        final NetworkManager netManager = itf.manager();
        Disks                disks      = vmManager.disks();
        VirtualMachines      vms        = vmManager.virtualMachines();

        String diskId = virtualMachine.osDiskId();
        String vmId   = virtualMachine.id();
        String ipId = itf.primaryIPConfiguration()
                         .publicIpAddressId();
        String itfId = itf.id();

        vms.deleteById(vmId);
        disks.deleteById(diskId);

        netManager.networkInterfaces()
                  .deleteById(itfId);

        netManager.publicIpAddresses()
                  .deleteById(ipId);
    }

    @Ignore("Manually enable to test, since it requires access to AZURE")
    @Test
    public void testStorage()
    {
        StorageAccount storageAccount = azure.ensureDefaultStorageAccount();

        Endpoints endpoint = storageAccount.endPoints()
                                           .primary();

        System.out.printf("blob: %s\n", endpoint.blob());
        System.out.printf("file: %s\n", endpoint.file());

        BlobServiceClient client = azure.createBlobClient(storageAccount);

        for (BlobContainerItem containerItem : client.listBlobContainers())
        {
            System.out.printf("Container: %s\n", containerItem.getName());

            BlobContainerClient clientContainer = client.getBlobContainerClient(containerItem.getName());
            for (BlobItem listBlob : clientContainer.listBlobs())
            {
                System.out.printf("  Blob: %s\n", listBlob.getName());
            }

            for (BlobItem test2 : clientContainer.listBlobsByHierarchy("test2/"))
            {
                System.out.printf("  Blob2: %s\n", test2.getName());
            }
        }
    }

    @Ignore("Manually enable to test, since it requires access to AZURE")
    @Test
    public void testCachingSite()
    {
        AzureHelper.CdnHelper cdnHelper = azure.getCdnHelper();

        cdnHelper.initializeRules();

        Path projectRoot = Path.of("../..")
                               .toAbsolutePath()
                               .normalize();

        String root = "product/hub/src/main/resources/assets";
        String base = "website";

        Path              dist     = projectRoot.resolve(Paths.get(root, base, "dist"));
        Map<String, File> contents = cdnHelper.prepareListForUpload(dist, base);

        cdnHelper.uploadContents("site", contents);

//        cdnHelper.deleteContents("site");
    }
}
