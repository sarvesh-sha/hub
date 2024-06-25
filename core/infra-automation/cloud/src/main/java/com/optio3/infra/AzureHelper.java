/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.rest.PagedFlux;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.Context;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.cdn.CdnManager;
import com.azure.resourcemanager.cdn.fluent.CdnManagementClient;
import com.azure.resourcemanager.cdn.fluent.EndpointsClient;
import com.azure.resourcemanager.cdn.fluent.models.EndpointInner;
import com.azure.resourcemanager.cdn.models.CdnEndpoint;
import com.azure.resourcemanager.cdn.models.CdnProfile;
import com.azure.resourcemanager.cdn.models.CdnProfiles;
import com.azure.resourcemanager.cdn.models.DeliveryRule;
import com.azure.resourcemanager.cdn.models.DeliveryRuleResponseHeaderAction;
import com.azure.resourcemanager.cdn.models.EndpointPropertiesUpdateParametersDeliveryPolicy;
import com.azure.resourcemanager.cdn.models.EndpointUpdateParameters;
import com.azure.resourcemanager.cdn.models.HeaderAction;
import com.azure.resourcemanager.cdn.models.HeaderActionParameters;
import com.azure.resourcemanager.cdn.models.SkuName;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineImage;
import com.azure.resourcemanager.monitor.models.MetricDefinition;
import com.azure.resourcemanager.monitor.models.MetricDefinitions;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.NetworkInterface;
import com.azure.resourcemanager.network.models.NetworkSecurityGroup;
import com.azure.resourcemanager.network.models.PublicIpAddress;
import com.azure.resourcemanager.network.models.SecurityRuleProtocol;
import com.azure.resourcemanager.resources.fluentcore.arm.collection.SupportsListingByResourceGroup;
import com.azure.resourcemanager.resources.fluentcore.arm.models.Resource;
import com.azure.resourcemanager.resources.fluentcore.utils.ResourceNamer;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.resources.models.ResourceGroups;
import com.azure.resourcemanager.storage.models.Endpoints;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.resourcemanager.storage.models.StorageAccountKey;
import com.azure.resourcemanager.storage.models.StorageAccountSkuType;
import com.azure.resourcemanager.storage.models.StorageAccounts;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.PublicAccessType;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.infra.deploy.CommonDeployer;
import com.optio3.infra.directory.ApiInfo;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.infra.directory.SshKey;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.IdGenerator;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class AzureHelper implements AutoCloseable
{
    public static final  String API_CREDENTIALS_SITE                 = "azure.com";
    private static final String c_keypair__INFRASTRUCTURE_MANAGEMENT = "azure-user";
    private static final String c_globalResourceGroup                = "global";
    private static final String c_defaultStorageAccount              = "optio3";

    private static final String c_role     = "Optio3_ROLE";
    private static final String c_role_CDN = "CDN";

    private static final LinkedList<AzureHelper> s_cache = new LinkedList<>();

    //--//

    private final String               m_accountDomain;
    private final AzureEnvironment     m_azureEnvironment;
    private final Region               m_azureRegion;
    private final AzureResourceManager m_azure;
    private final MonotonousTime       m_expiration;

    public final String subscriptionId;
    public final String servicePrincipal;
    public final SshKey sshKey;

    //--//

    private ResourceGroup  m_globalResourceGroup;
    private StorageAccount m_defaultStorageAccount;

    private CdnHelper m_cdnHelper;

    private AzureHelper(String accountDomain,
                        String subscriptionId,
                        String servicePrincipal,
                        SshKey sshKey,
                        Region azureRegion,
                        TokenCredential credential,
                        AzureProfile profile)
    {
        this.subscriptionId   = subscriptionId;
        this.servicePrincipal = servicePrincipal;
        this.sshKey           = sshKey;

        m_accountDomain    = accountDomain;
        m_azureEnvironment = profile.getEnvironment();
        m_azureRegion      = azureRegion;
        m_azure            = AzureResourceManager.authenticate(credential, profile)
                                                 .withSubscription(subscriptionId);

        m_expiration = TimeUtils.computeTimeoutExpiration(10, TimeUnit.MINUTES);
    }

    @Override
    public void close()
    {
        if (!TimeUtils.isTimeoutExpired(m_expiration))
        {
            synchronized (s_cache)
            {
                s_cache.add(this);
            }
        }
    }

    public static AzureHelper buildCachedWithDirectoryLookup(CredentialDirectory credDir,
                                                             String accountDomain,
                                                             AzureEnvironment environment,
                                                             Region region)
    {
        synchronized (s_cache)
        {
            for (Iterator<AzureHelper> it = s_cache.iterator(); it.hasNext(); )
            {
                var helper = it.next();

                if (Objects.equals(helper.getAccountDomain(), accountDomain) && Objects.equals(helper.getEnvironment(), environment) && Objects.equals(helper.getRegion(), region))
                {
                    it.remove();

                    if (!TimeUtils.isTimeoutExpired(helper.m_expiration))
                    {
                        return helper;
                    }
                }
            }

            return AzureHelper.buildWithDirectoryLookup(credDir, accountDomain, environment, region);
        }
    }

    public static AzureHelper buildWithDirectoryLookup(CredentialDirectory credDir,
                                                       String accountDomain,
                                                       AzureEnvironment environment,
                                                       Region region)
    {
        ApiInfo apiInfo = credDir.findFirstApiCredential(API_CREDENTIALS_SITE, accountDomain);
        SshKey  sshKey  = credDir.findFirstSshKey(API_CREDENTIALS_SITE, c_keypair__INFRASTRUCTURE_MANAGEMENT);

        TokenCredential credential = new ClientSecretCredentialBuilder().clientId(apiInfo.clientId)
                                                                        .clientSecret(apiInfo.secretKey)
                                                                        .tenantId(apiInfo.tenantId)
                                                                        .build();

        AzureProfile profile = new AzureProfile(apiInfo.tenantId, apiInfo.subscriptionId, environment);

        return new AzureHelper(accountDomain, apiInfo.subscriptionId, apiInfo.servicePrincipal, sshKey, region, credential, profile);
    }

    public static List<String> getAvailableAccounts(CredentialDirectory credDir)
    {
        List<String> res = Lists.newArrayList();

        for (ApiInfo apiInfo : CollectionUtils.asEmptyCollectionIfNull(credDir.apiCredentials.get(API_CREDENTIALS_SITE)))
        {
            res.add(apiInfo.key);
        }

        return res;
    }

    //--//

    public String getAccountDomain()
    {
        return m_accountDomain;
    }

    public AzureEnvironment getEnvironment()
    {
        return m_azureEnvironment;
    }

    public Region getRegion()
    {
        return m_azureRegion;
    }

    //--//

    public static String generateName(String prefix)
    {
        return sanitizeId(prefix + "-" + UUID.randomUUID(), 70);
    }

    public static String sanitizeId(String id)
    {
        return sanitizeId(id, 63); // Max length for ARN ids.
    }

    public static String sanitizeId(String id,
                                    int maxLength)
    {
        if (id == null)
        {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (char c : id.toCharArray())
        {
            switch (c)
            {
                case ',':
                    c = '_';
                    break;

                case '<':
                case '>':
                case '%':
                case '&':
                case '\\':
                case '?':
                case '/':
                    continue;

                case '-':
                    break;

                default:
                    if (!Character.isLetterOrDigit(c))
                    {
                        continue;
                    }
            }

            sb.append(Character.toLowerCase(c));
        }

        String res = sb.toString();
        if (res.length() > maxLength)
        {
            res = res.substring(0, maxLength);
        }

        return res;
    }

    //--//

    public static NetworkSecurityGroup.DefinitionStages.WithCreate allowTCP(NetworkSecurityGroup.DefinitionStages.WithCreate nsg,
                                                                            int priority,
                                                                            int port)
    {
        return nsg.defineRule("ALLOW-TCP-" + port)
                  .allowInbound()
                  .fromAnyAddress()
                  .fromAnyPort()
                  .toAnyAddress()
                  .toPort(port)
                  .withProtocol(SecurityRuleProtocol.TCP)
                  .withPriority(priority)
                  .attach();
    }

    public static NetworkSecurityGroup.DefinitionStages.WithCreate allowUDP(NetworkSecurityGroup.DefinitionStages.WithCreate nsg,
                                                                            int priority,
                                                                            int port)
    {
        return nsg.defineRule("ALLOW-UDP-" + port)
                  .allowInbound()
                  .fromAnyAddress()
                  .fromAnyPort()
                  .toAnyAddress()
                  .toPort(port)
                  .withProtocol(SecurityRuleProtocol.UDP)
                  .withPriority(priority)
                  .attach();
    }

    //--//

    public ResourceGroup getGlobalResourceGroup()
    {
        if (m_globalResourceGroup == null)
        {
            for (ResourceGroup resourceGroup : m_azure.resourceGroups()
                                                      .list())
            {
                if (StringUtils.equals(resourceGroup.name(), c_globalResourceGroup))
                {
                    m_globalResourceGroup = resourceGroup;
                    break;
                }
            }
        }

        return m_globalResourceGroup;
    }

    public ResourceGroup ensureGlobalResourceGroup()
    {
        ResourceGroup resourceGroup = getGlobalResourceGroup();
        if (resourceGroup == null)
        {
            resourceGroup = m_azure.resourceGroups()
                                   .define(c_globalResourceGroup)
                                   .withRegion(m_azureRegion)
                                   .create();

            m_globalResourceGroup = resourceGroup;
        }

        return resourceGroup;
    }

    //--//

    public ResourceGroup getCustomerResourceGroup(String customerId)
    {
        ResourceGroups resourceGroups = m_azure.resourceGroups();
        return getFirst(resourceGroups.listByTagAsync(CommonDeployer.ConfigTag.CustomerSysId.getTag(), customerId), 30);
    }

    public boolean deleteCustomerResourceGroup(String customerId)
    {
        ResourceGroup rg = getCustomerResourceGroup(customerId);
        if (rg == null)
        {
            return false;
        }

        m_azure.resourceGroups()
               .deleteByName(rg.name());
        return true;
    }

    public ResourceGroup ensureCustomerResourceGroup(String customerId,
                                                     String customerName)
    {
        ResourceGroup resourceGroup = getCustomerResourceGroup(customerId);
        if (resourceGroup == null)
        {
            resourceGroup = m_azure.resourceGroups()
                                   .define(generateName("customer"))
                                   .withRegion(m_azureRegion)
                                   .withTag(CommonDeployer.ConfigTag.CustomerSysId.getTag(), customerId)
                                   .withTag(CommonDeployer.ConfigTag.CustomerName.getTag(), customerName)
                                   .create();
        }

        return resourceGroup;
    }

    //--//

    public StorageAccounts storageAccounts()
    {
        return m_azure.storageAccounts();
    }

    public BlobServiceClient createBlobClient(StorageAccount storageAccount)
    {
        Endpoints endpoint = storageAccount.endPoints()
                                           .primary();

        String            blob = endpoint.blob();
        StorageAccountKey key  = CollectionUtils.firstElement(storageAccount.getKeys());

        StringBuilder sb = new StringBuilder();
        appendConnectionString(sb, "DefaultEndpointsProtocol", "https");
        appendConnectionString(sb, "BlobEndpoint", blob);
        appendConnectionString(sb, "AccountName", storageAccount.name());
        appendConnectionString(sb, "AccountKey", key.value());

        return new BlobServiceClientBuilder().connectionString(sb.toString())
                                             .buildClient();
    }

    private static void appendConnectionString(StringBuilder sb,
                                               String key,
                                               String value)
    {
        if (sb.length() > 0)
        {
            sb.append(";");
        }

        sb.append(key);
        sb.append("=");
        sb.append(value);
    }

    public StorageAccount getDefaultStorageAccount()
    {
        if (m_defaultStorageAccount == null)
        {
            for (StorageAccount storageAccount : storageAccounts().list())
            {
                if (StringUtils.equals(storageAccount.name(), c_defaultStorageAccount))
                {
                    m_defaultStorageAccount = storageAccount;
                    break;
                }
            }
        }

        return m_defaultStorageAccount;
    }

    public StorageAccount ensureDefaultStorageAccount()
    {
        StorageAccount storageAccount = getDefaultStorageAccount();
        if (storageAccount == null)
        {
            storageAccount = storageAccounts().define(c_defaultStorageAccount)
                                              .withRegion(m_azureRegion)
                                              .withExistingResourceGroup(ensureGlobalResourceGroup())
                                              .withSku(StorageAccountSkuType.STANDARD_LRS)
                                              .withGeneralPurposeAccountKindV2()
                                              .withAccessFromAllNetworks()
                                              .withAccessFromAzureServices()
                                              .withSystemAssignedManagedServiceIdentity()
                                              .create();

            m_defaultStorageAccount = storageAccount;
        }

        return storageAccount;
    }

    public StorageAccount getCustomerStorageAccount(String customerId)
    {
        ResourceGroup resourceGroup = getCustomerResourceGroup(customerId);
        if (resourceGroup != null)
        {
            return getFirst(storageAccounts().listByResourceGroupAsync(resourceGroup.name()), 10);
        }

        return null;
    }

    public StorageAccount ensureCustomerStorageAccount(String customerId,
                                                       String customerName)
    {
        StorageAccount storageAccount = getCustomerStorageAccount(customerId);
        if (storageAccount == null)
        {
            ResourceNamer rn = new ResourceNamer(c_defaultStorageAccount);

            storageAccount = storageAccounts().define(rn.getRandomName("", 24))
                                              .withRegion(m_azureRegion)
                                              .withExistingResourceGroup(ensureCustomerResourceGroup(customerId, customerName))
                                              .withSku(StorageAccountSkuType.STANDARD_LRS)
                                              .withGeneralPurposeAccountKindV2()
                                              .withAccessFromAllNetworks()
                                              .withAccessFromAzureServices()
                                              .withSystemAssignedManagedServiceIdentity()
                                              .withTag(CommonDeployer.ConfigTag.CustomerSysId.getTag(), customerId)
                                              .withTag(CommonDeployer.ConfigTag.CustomerName.getTag(), customerName)
                                              .create();
        }

        return storageAccount;
    }

    public MetricDefinitions metricDefinitions()
    {
        return m_azure.metricDefinitions();
    }

    //--//

    public class CdnHelper
    {
        private ResourceGroup  m_resourceGroup;
        private StorageAccount m_storageAccount;
        private CdnProfile     m_cdnProfile;
        private CdnEndpoint    m_cdnEndpoint;

        public void deleteAllResources()
        {
            ResourceGroup resourceGroup = getResourceGroup(false);
            if (resourceGroup != null)
            {
                m_azure.resourceGroups()
                       .deleteByName(resourceGroup.name());
            }

            m_resourceGroup  = null;
            m_storageAccount = null;
            m_cdnProfile     = null;
            m_cdnEndpoint    = null;
        }

        public void uploadContents(String containerName,
                                   Map<String, File> files)
        {
            ensureCdnEndpoint();

            BlobContainerClient blobContainer = getContainer(containerName, true);

            for (Map.Entry<String, File> entry : files.entrySet())
            {
                String name = entry.getKey();
                File   file = entry.getValue();

                BlobClient blob = blobContainer.getBlobClient(name);

                BlobHttpHeaders headers = new BlobHttpHeaders();

                int pos = name.lastIndexOf('.');
                if (pos > 0)
                {
                    switch (name.substring(pos))
                    {
                        case ".htm":
                        case ".html":
                            headers.setContentType("text/html");
                            break;

                        case ".js":
                            headers.setContentType("application/javascript");
                            break;

                        case ".css":
                            headers.setContentType("text/css");
                            break;

                        case ".woff2":
                            headers.setContentType("font/woff2");
                            break;

                        case ".png":
                            headers.setContentType("image/png");
                            break;

                        case ".svg":
                            headers.setContentType("image/svg+xml");
                            break;

                        case ".xml":
                            headers.setContentType("application/xml");
                            break;
                    }
                }

                var options = new BlobUploadFromFileOptions(file.getAbsolutePath()).setHeaders(headers);

                blob.uploadFromFileWithResponse(options, null, null);
            }
        }

        public void deleteContents(String containerName)
        {
            BlobContainerClient blobContainer = getContainer(containerName, false);
            if (blobContainer != null)
            {
                blobContainer.delete();
            }
        }

        //--//

        public Map<String, File> prepareListForUpload(Path root,
                                                      String prefix)
        {
            List<File> files = Lists.newArrayList();
            collectFiles(files, root.toFile());

            Map<String, File> contents = Maps.newHashMap();

            for (File file : files)
            {
                Path path = root.relativize(file.toPath());

                contents.put(prefix + "/" + path.toString(), file);
            }

            return contents;
        }

        private void collectFiles(List<File> results,
                                  File dir)
        {
            for (File file : dir.listFiles())
            {
                if (file.isFile())
                {
                    results.add(file);
                }
                else if (file.isDirectory())
                {
                    collectFiles(results, file);
                }
            }
        }

        //--//

        private ResourceGroup getResourceGroup(boolean createIfMissing)
        {
            if (m_resourceGroup == null)
            {
                for (ResourceGroup resourceGroup : m_azure.resourceGroups()
                                                          .list())
                {
                    if (hasMatchingTag(resourceGroup, c_role, c_role_CDN))
                    {
                        m_resourceGroup = resourceGroup;
                        break;
                    }
                }

                if (m_resourceGroup == null && createIfMissing)
                {
                    m_resourceGroup = m_azure.resourceGroups()
                                             .define(c_role_CDN + "-" + IdGenerator.newGuid())
                                             .withRegion(m_azureRegion)
                                             .withTag(c_role, c_role_CDN)
                                             .create();
                }
            }

            return m_resourceGroup;
        }

        private void ensureStorageAccount()
        {
            if (m_storageAccount == null)
            {
                ResourceGroup resourceGroup = getResourceGroup(true);

                for (StorageAccount storageAccount : storageAccounts().listByResourceGroup(resourceGroup.name()))
                {
                    if (hasMatchingTag(storageAccount, c_role, c_role_CDN))
                    {
                        m_storageAccount = storageAccount;
                        break;
                    }
                }

                if (m_storageAccount == null)
                {
                    ResourceNamer rn   = new ResourceNamer(c_defaultStorageAccount);
                    String        name = rn.getRandomName("", 24);

                    m_storageAccount = storageAccounts().define(name)
                                                        .withRegion(m_azureRegion)
                                                        .withExistingResourceGroup(resourceGroup)
                                                        .withSku(StorageAccountSkuType.STANDARD_LRS)
                                                        .withGeneralPurposeAccountKindV2()
                                                        .withAccessFromAllNetworks()
                                                        .withAccessFromAzureServices()
                                                        .withSystemAssignedManagedServiceIdentity()
                                                        .withTag(c_role, c_role_CDN)
                                                        .create();
                }
            }
        }

        private void ensureCdnProfile()
        {
            if (m_cdnProfile == null)
            {
                ResourceGroup resourceGroup = getResourceGroup(true);

                CdnProfiles cdnProfiles = m_azure.cdnProfiles();

                m_cdnProfile = getFirst(cdnProfiles.listByResourceGroupAsync(resourceGroup.name()), 10);
                if (m_cdnProfile == null)
                {
                    m_cdnProfile = cdnProfiles.define("optio3-cdn")
                                              .withRegion("global")
                                              .withExistingResourceGroup(resourceGroup)
                                              .withSku(SkuName.STANDARD_MICROSOFT)
                                              .create();
                }
            }
        }

        private void ensureCdnEndpoint()
        {
            if (m_cdnEndpoint == null)
            {
                ensureCdnProfile();

                Map<String, CdnEndpoint> endpoints = m_cdnProfile.endpoints();

                m_cdnEndpoint = CollectionUtils.firstElement(endpoints.values());
                if (m_cdnEndpoint == null)
                {
                    ensureStorageAccount();

                    Endpoints endpoint = m_storageAccount.endPoints()
                                                         .primary();

                    URL url;

                    try
                    {
                        url = new URL(endpoint.blob());
                    }
                    catch (MalformedURLException e)
                    {
                        throw new RuntimeException(e);
                    }

                    CdnProfiles cdnProfiles = m_azure.cdnProfiles();
                    String      name;
                    int         counter     = 0;

                    while (true)
                    {
                        name = String.format("optio3-cdn-%d", counter++);

                        if (cdnProfiles.checkEndpointNameAvailability(name)
                                       .nameAvailable())
                        {
                            break;
                        }
                    }

                    m_cdnProfile = m_cdnProfile.update()
                                               .defineNewEndpoint(name)
                                               .withOrigin(url.getHost())
                                               .withHostHeader(url.getHost())
                                               .withCompressionEnabled(true)
                                               .withContentTypeToCompress("application/javascript")
                                               .withContentTypeToCompress("text/css")
                                               .attach()
                                               .apply();

                    m_cdnEndpoint = CollectionUtils.firstElement(endpoints.values());
                }
            }
        }

        public void initializeRules()
        {
            ensureCdnEndpoint();

            CdnManager          manager         = m_cdnProfile.manager();
            CdnManagementClient serviceClient   = manager.serviceClient();
            EndpointsClient     endpointsClient = serviceClient.getEndpoints();

            HeaderActionParameters actionParameters = new HeaderActionParameters().withHeaderAction(HeaderAction.OVERWRITE)
                                                                                  .withHeaderName("Access-Control-Allow-Origin")
                                                                                  .withValue("*");
            DeliveryRuleResponseHeaderAction action = new DeliveryRuleResponseHeaderAction().withParameters(actionParameters);

            DeliveryRule expectedRule = new DeliveryRule().withName("Global")
                                                          .withConditions(Lists.newArrayList())
                                                          .withActions(Lists.newArrayList(action));

            EndpointInner ep = endpointsClient.get(m_resourceGroup.name(), m_cdnProfile.name(), m_cdnEndpoint.name());

            EndpointPropertiesUpdateParametersDeliveryPolicy deliveryPolicy = ep.deliveryPolicy();
            if (deliveryPolicy == null)
            {
                deliveryPolicy = new EndpointPropertiesUpdateParametersDeliveryPolicy();
            }

            for (DeliveryRule rule : CollectionUtils.asEmptyCollectionIfNull(deliveryPolicy.rules()))
            {
                if (StringUtils.equals(ObjectMappers.prettyPrintAsJson(rule), ObjectMappers.prettyPrintAsJson(expectedRule)))
                {
                    return;
                }
            }

            deliveryPolicy.withRules(Lists.newArrayList(expectedRule));

            try
            {
                endpointsClient.update(m_resourceGroup.name(), m_cdnProfile.name(), m_cdnEndpoint.name(), new EndpointUpdateParameters().withDeliveryPolicy(deliveryPolicy));
            }
            catch (Throwable t)
            {
                throw Exceptions.newRuntimeException("Failed to create CDN rule: %s", ObjectMappers.prettyPrintAsJson(expectedRule));
            }
        }

        public String getURL(String name)
        {
            ensureCdnEndpoint();

            return String.format("https://%s/%s", m_cdnEndpoint.hostname(), name);
        }

        public String normalizeContainerName(String name)
        {
            return name.replace('_', '-');
        }

        public BlobContainerClient getContainer(String name,
                                                boolean createIfMissing)
        {
            ensureStorageAccount();

            BlobServiceClient blobClient = createBlobClient(m_storageAccount);

            BlobContainerClient blobContainer = blobClient.getBlobContainerClient(name);
            if (!blobContainer.exists())
            {
                if (!createIfMissing)
                {
                    return null;
                }

                blobContainer.createWithResponse(null, PublicAccessType.CONTAINER, null, Context.NONE)
                             .getValue();
            }

            return blobContainer;
        }
    }

    public CdnHelper getCdnHelper()
    {
        if (m_cdnHelper == null)
        {
            m_cdnHelper = new CdnHelper();
        }

        return m_cdnHelper;
    }

    //--//

    private <T extends Resource> T findResourceForCustomer(SupportsListingByResourceGroup<T> src,
                                                           ResourceGroup resourceGroup,
                                                           String customerId)
    {
        for (T res : src.listByResourceGroup(resourceGroup.name()))
        {
            if (hasMatchingTag(res, CommonDeployer.ConfigTag.CustomerSysId.getTag(), customerId))
            {
                return res;
            }
        }

        return null;
    }

    private <T extends Resource> T findResourceForService(SupportsListingByResourceGroup<T> src,
                                                          ResourceGroup resourceGroup,
                                                          String serviceId)
    {
        return CollectionUtils.firstElement(findResourcesForService(src, resourceGroup, serviceId));
    }

    private <T extends Resource> List<T> findResourcesForService(SupportsListingByResourceGroup<T> src,
                                                                 ResourceGroup resourceGroup,
                                                                 String serviceId)
    {
        List<T> resources = Lists.newArrayList();

        for (T res : src.listByResourceGroup(resourceGroup.name()))
        {
            if (hasMatchingTag(res, CommonDeployer.ConfigTag.ServiceSysId.getTag(), serviceId))
            {
                resources.add(res);
            }
        }

        return resources;
    }

    private <T extends Resource> T findResourceForHost(SupportsListingByResourceGroup<T> src,
                                                       ResourceGroup resourceGroup,
                                                       String hostId)
    {
        for (T res : src.listByResourceGroup(resourceGroup.name()))
        {
            if (hasMatchingTag(res, CommonDeployer.ConfigTag.HostSysId.getTag(), hostId))
            {
                return res;
            }
        }

        return null;
    }

    //--//

    public NetworkSecurityGroup getNetworkSecurityGroup(ResourceGroup resourceGroup,
                                                        String serviceId)
    {
        return findResourceForService(m_azure.networkSecurityGroups(), resourceGroup, serviceId);
    }

    public NetworkSecurityGroup.DefinitionStages.WithCreate buildNetworkSecurityGroup(ResourceGroup resourceGroup,
                                                                                      String serviceId,
                                                                                      String serviceName)
    {
        Map<String, String> tags = Maps.newHashMap(resourceGroup.tags());
        updateTag(tags, CommonDeployer.ConfigTag.ServiceSysId, serviceId);
        updateTag(tags, CommonDeployer.ConfigTag.ServiceName, serviceName);

        return m_azure.networkSecurityGroups()
                      .define("nsg-" + serviceId)
                      .withRegion(m_azureRegion)
                      .withExistingResourceGroup(resourceGroup)
                      .withTags(tags);
    }

    //--//

    public Network getVirtualNetwork(ResourceGroup resourceGroup,
                                     String serviceId)
    {
        return findResourceForService(m_azure.networks(), resourceGroup, serviceId);
    }

    public Network.DefinitionStages.WithCreate buildVirtualNetwork(ResourceGroup resourceGroup,
                                                                   String serviceId,
                                                                   String serviceName)
    {
        Map<String, String> tags = Maps.newHashMap(resourceGroup.tags());
        updateTag(tags, CommonDeployer.ConfigTag.ServiceSysId, serviceId);
        updateTag(tags, CommonDeployer.ConfigTag.ServiceName, serviceName);

        return m_azure.networks()
                      .define(generateName("vpc"))
                      .withRegion(m_azureRegion)
                      .withExistingResourceGroup(resourceGroup)
                      .withTags(tags);
    }

    //--//

    public List<PublicIpAddress> listPublicIPAddresses(ResourceGroup resourceGroup,
                                                       String serviceId)
    {
        return findResourcesForService(m_azure.publicIpAddresses(), resourceGroup, serviceId);
    }

    public PublicIpAddress.DefinitionStages.WithCreate buildPublicIpAddress(Network network,
                                                                            String hostId,
                                                                            String hostName)
    {
        ResourceGroup resourceGroup = m_azure.resourceGroups()
                                             .getByName(network.resourceGroupName());

        Map<String, String> tags = Maps.newHashMap(network.tags());
        updateTag(tags, CommonDeployer.ConfigTag.HostSysId, hostId);
        updateTag(tags, CommonDeployer.ConfigTag.HostName, hostName);

        return m_azure.publicIpAddresses()
                      .define(generateName("pip"))
                      .withRegion(m_azureRegion)
                      .withExistingResourceGroup(resourceGroup)
                      .withTags(tags);
    }

    //--//

    public NetworkInterface getNetworkInterface(ResourceGroup resourceGroup,
                                                String hostId)
    {
        return findResourceForHost(m_azure.networkInterfaces(), resourceGroup, hostId);
    }

    public NetworkInterface.DefinitionStages.WithCreate buildNetworkInterface(Network network,
                                                                              String hostId,
                                                                              String hostName,
                                                                              String subnet)
    {
        ResourceGroup resourceGroup = m_azure.resourceGroups()
                                             .getByName(network.resourceGroupName());

        Map<String, String> tags = Maps.newHashMap(network.tags());
        updateTag(tags, CommonDeployer.ConfigTag.HostSysId, hostId);
        updateTag(tags, CommonDeployer.ConfigTag.HostName, hostName);

        return m_azure.networkInterfaces()
                      .define(generateName("itf"))
                      .withRegion(m_azureRegion)
                      .withExistingResourceGroup(resourceGroup)
                      .withExistingPrimaryNetwork(network)
                      .withSubnet(subnet)
                      .withPrimaryPrivateIPAddressDynamic()
                      .withTags(tags);
    }

    //--//

    public Iterable<VirtualMachineImage> getVirtualMachineImages()
    {
        return m_azure.virtualMachineImages()
                      .listByRegion(m_azureRegion);
    }

    //--//

    public VirtualMachine getVirtualMachine(ResourceGroup resourceGroup,
                                            String hostId)
    {
        return findResourceForHost(m_azure.virtualMachines(), resourceGroup, hostId);
    }

    public VirtualMachine.DefinitionStages.WithOS buildVirtualMachine(NetworkInterface itf)
    {
        ResourceGroup resourceGroup = m_azure.resourceGroups()
                                             .getByName(itf.resourceGroupName());

        return m_azure.virtualMachines()
                      .define(generateName("vm"))
                      .withRegion(m_azureRegion)
                      .withExistingResourceGroup(resourceGroup)
                      .withExistingPrimaryNetworkInterface(itf);
    }

    //--//

    private static <T> T getFirst(PagedFlux<T> pagedFlux,
                                  int maxWaitInSeconds)
    {
        return pagedFlux.blockFirst(Duration.of(maxWaitInSeconds, ChronoUnit.SECONDS));
    }

    //--//

    private static boolean hasMatchingTag(Resource resource,
                                          String key,
                                          String value)
    {
        if (resource != null)
        {
            Map<String, String> tags = resource.tags();
            if (tags != null)
            {
                return StringUtils.equals(tags.get(key), value);
            }
        }

        return false;
    }

    public static void updateTag(Map<String, String> tags,
                                 CommonDeployer.ConfigTag tag,
                                 String value)
    {
        if (value != null)
        {
            tags.put(tag.getTag(), value);
        }
    }
}
