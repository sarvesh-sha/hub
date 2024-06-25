/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Principal.Services;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.actions.SNSActions;
import com.amazonaws.auth.policy.actions.SecurityTokenServiceActions;
import com.amazonaws.auth.policy.actions.SimpleEmailServiceActions;
import com.amazonaws.auth.policy.resources.S3BucketResource;
import com.amazonaws.auth.policy.resources.S3ObjectResource;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AttachInternetGatewayRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.CreateInternetGatewayResult;
import com.amazonaws.services.ec2.model.CreateRouteRequest;
import com.amazonaws.services.ec2.model.CreateRouteTableRequest;
import com.amazonaws.services.ec2.model.CreateRouteTableResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateSubnetRequest;
import com.amazonaws.services.ec2.model.CreateSubnetResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVpcRequest;
import com.amazonaws.services.ec2.model.CreateVpcResult;
import com.amazonaws.services.ec2.model.DeleteInternetGatewayRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSubnetRequest;
import com.amazonaws.services.ec2.model.DeleteVpcRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysResult;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.DetachInternetGatewayRequest;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.IpRange;
import com.amazonaws.services.ec2.model.ModifySubnetAttributeRequest;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.Route;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.AddRoleToInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateInstanceProfileResult;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleResult;
import com.amazonaws.services.identitymanagement.model.DeleteRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileResult;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyResult;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleResult;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListRolePoliciesRequest;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.PutRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.RemoveRoleFromInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.GetIdentityDkimAttributesRequest;
import com.amazonaws.services.simpleemail.model.GetIdentityDkimAttributesResult;
import com.amazonaws.services.simpleemail.model.GetIdentityVerificationAttributesRequest;
import com.amazonaws.services.simpleemail.model.GetIdentityVerificationAttributesResult;
import com.amazonaws.services.simpleemail.model.IdentityDkimAttributes;
import com.amazonaws.services.simpleemail.model.IdentityVerificationAttributes;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.amazonaws.services.simpleemail.model.VerifyDomainDkimRequest;
import com.amazonaws.services.simpleemail.model.VerifyDomainDkimResult;
import com.amazonaws.services.simpleemail.model.VerifyDomainIdentityRequest;
import com.amazonaws.services.simpleemail.model.VerifyDomainIdentityResult;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.util.Base64;
import com.amazonaws.util.EC2MetadataUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.concurrency.Executors;
import com.optio3.infra.directory.ApiInfo;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class AwsHelper implements AutoCloseable
{
    public static final  String API_CREDENTIALS_SITE               = "amazon.com";
    private static final String KEYPAIR__INFRASTRUCTURE_MANAGEMENT = "infrastructureManagement";

    private static final String TAG__PURPOSE   = "Purpose";
    private static final String S3_BUCKET_NAME = "optio3";

    public static final int STATE_CODE_PENDING       = 0;
    public static final int STATE_CODE_RUNNING       = 16;
    public static final int STATE_CODE_SHUTTING_DOWN = 32;
    public static final int STATE_CODE_TERMINATED    = 48;
    public static final int STATE_CODE_STOPPING      = 64;
    public static final int STATE_CODE_STOPPED       = 80;

    private static final LinkedList<AwsHelper> s_cache = new LinkedList<>();

    //--//

    public static class MetricIdentity
    {
        public final String          namespace;
        public final String          metricName;
        public final List<Dimension> dimensions = Lists.newArrayList();

        public MetricIdentity(String namespace,
                              String metricName,
                              List<Dimension> dimensions)
        {
            this.namespace  = namespace;
            this.metricName = metricName;
            this.dimensions.addAll(dimensions);
        }

        public String findByName(String name)
        {
            for (Dimension dimension : dimensions)
            {
                if (StringUtils.equals(dimension.getName(), name))
                {
                    return dimension.getValue();
                }
            }

            return null;
        }
    }

    public static class MetricName
    {
        public final String               namespace;
        public final String               metricName;
        public final List<MetricIdentity> instances = Lists.newArrayList();

        public MetricName(String namespace,
                          String metricName)
        {
            this.namespace  = namespace;
            this.metricName = metricName;
        }

        public void add(List<Dimension> dimensions)
        {
            instances.add(new MetricIdentity(namespace, metricName, dimensions));
        }
    }

    public static class MetricNamespace
    {
        public final String                  namespace;
        public final Map<String, MetricName> metrics = Maps.newHashMap();

        public MetricNamespace(String namespace)
        {
            this.namespace = namespace;
        }

        public MetricName get(String name)
        {
            return metrics.computeIfAbsent(name, nameNew -> new MetricName(namespace, nameNew));
        }
    }

    //--//

    public class S3Entry
    {
        private final String          m_bucket;
        private final String          m_key;
        private final S3ObjectSummary m_fileDetails;
        private       List<S3Entry>   m_children;

        S3Entry(String bucket,
                String key)
        {
            m_bucket      = bucket;
            m_key         = key;
            m_fileDetails = null;
        }

        S3Entry(S3ObjectSummary info)
        {
            m_bucket      = info.getBucketName();
            m_key         = info.getKey();
            m_fileDetails = info;
        }

        public String getKey()
        {
            return m_key;
        }

        public boolean isFile()
        {
            return m_fileDetails != null;
        }

        public long getSize()
        {
            return isFile() ? m_fileDetails.getSize() : -1;
        }

        public ZonedDateTime getLastModified()
        {
            return isFile() ? TimeUtils.fromInstantToLocalTime(m_fileDetails.getLastModified()
                                                                            .toInstant()) : null;
        }

        public List<S3Entry> getEntries()
        {
            if (m_children == null)
            {
                if (isFile())
                {
                    m_children = Collections.emptyList();
                }
                else
                {
                    ListObjectsV2Request req = new ListObjectsV2Request();
                    req.setBucketName(m_bucket);
                    req.setPrefix(m_key);
                    req.setDelimiter("/");

                    ListObjectsV2Result res = m_s3.listObjectsV2(req);

                    List<S3Entry> children = Lists.newArrayList();

                    for (String prefix : res.getCommonPrefixes())
                    {
                        children.add(new S3Entry(m_bucket, prefix));
                    }

                    for (S3ObjectSummary objSummary : res.getObjectSummaries())
                    {
                        if (StringUtils.equals(objSummary.getKey(), getKey()))
                        {
                            // Sometimes AWS returns a file with the same name as the folder. Ignore it.
                            continue;
                        }

                        children.add(new S3Entry(objSummary));
                    }

                    m_children = Collections.unmodifiableList(children);
                }
            }

            return m_children;
        }

        public List<S3Entry> getFiles()
        {
            return CollectionUtils.transformToListNoNulls(getEntries(), (en) -> en.isFile() ? en : null);
        }

        public List<S3Entry> getDirectories()
        {
            return CollectionUtils.transformToListNoNulls(getEntries(), (en) -> en.isFile() ? null : en);
        }
    }

    //--//

    private final MonotonousTime           m_expiration;
    private final Regions                  m_targetRegion;
    private final String                   m_targetDomain;
    private final AmazonEC2                m_ec2;
    private final AmazonIdentityManagement m_iam;

    private final AmazonS3 m_s3;

    private final AmazonSimpleEmailService m_ses;

    private final AmazonSNSClient m_sns;

    private final AmazonCloudWatchClient m_cw;

    //--//

    private List<AvailabilityZone> m_zones;

    //--//

    private static Regions s_defaultRegion;

    private static Regions resolveRegion(Regions region)
    {
        if (region == null)
        {
            if (s_defaultRegion == null)
            {
                try
                {
                    s_defaultRegion = Regions.fromName(EC2MetadataUtils.getEC2InstanceRegion());
                }
                catch (Throwable t)
                {
                    // Assume West 2...
                    s_defaultRegion = Regions.US_WEST_2;
                }
            }

            region = s_defaultRegion;
        }

        return region;
    }

    private AwsHelper(Regions region,
                      String domain,
                      AWSCredentialsProvider provider)
    {
        m_targetRegion = region;
        m_targetDomain = domain;

        m_ec2 = AmazonEC2ClientBuilder.standard()
                                      .withCredentials(provider)
                                      .withRegion(region)
                                      .build();

        m_iam = AmazonIdentityManagementClientBuilder.standard()
                                                     .withCredentials(provider)
                                                     .withRegion(region)
                                                     .build();

        m_s3 = AmazonS3ClientBuilder.standard()
                                    .withCredentials(provider)
                                    .withRegion(region)
                                    .build();

        m_ses = AmazonSimpleEmailServiceClientBuilder.standard()
                                                     .withCredentials(provider)
                                                     .withRegion(region)
                                                     .build();

        m_sns = (AmazonSNSClient) AmazonSNSClientBuilder.standard()
                                                        .withCredentials(provider)
                                                        .withRegion(region)
                                                        .build();

        m_cw = (AmazonCloudWatchClient) AmazonCloudWatchClientBuilder.standard()
                                                                     .withCredentials(provider)
                                                                     .withRegion(region)
                                                                     .build();

        m_expiration = TimeUtils.computeTimeoutExpiration(10, TimeUnit.MINUTES);
    }

    @Override
    public void close()
    {
        if (!TimeUtils.isTimeoutExpired(m_expiration) && m_targetDomain != null)
        {
            synchronized (s_cache)
            {
                s_cache.add(this);
            }
        }
    }

    public static AwsHelper buildWithProviderFromInstanceMetadata()
    {
        Regions region = resolveRegion(null);

        return new AwsHelper(region, null, null);
    }

    public static AwsHelper buildWithExplicitCredentials(Regions region,
                                                         ApiInfo ai)
    {
        AWSStaticCredentialsProvider provider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(ai.accessKey, ai.secretKey));
        return new AwsHelper(resolveRegion(region), ai.key, provider);
    }

    public static AwsHelper buildCachedWithDirectoryLookup(CredentialDirectory credDir,
                                                           String accountDomain,
                                                           Regions region)
    {
        region = resolveRegion(region);

        synchronized (s_cache)
        {
            for (Iterator<AwsHelper> it = s_cache.iterator(); it.hasNext(); )
            {
                var helper = it.next();

                if (Objects.equals(helper.getAccountDomain(), accountDomain) && Objects.equals(helper.getRegion(), region))
                {
                    it.remove();

                    if (!TimeUtils.isTimeoutExpired(helper.m_expiration))
                    {
                        return helper;
                    }
                }
            }

            return buildWithDirectoryLookup(credDir, accountDomain, region);
        }
    }

    public static AwsHelper buildWithDirectoryLookup(CredentialDirectory credDir,
                                                     String accountDomain,
                                                     Regions region)
    {
        region = resolveRegion(region);

        ApiInfo apiInfo = credDir.findFirstApiCredential(API_CREDENTIALS_SITE, accountDomain);
        return buildWithExplicitCredentials(region, apiInfo);
    }

    public static AwsHelper buildWithProfileProvider(Regions region)
    {
        region = resolveRegion(region);

        return new AwsHelper(region, null, new ProfileCredentialsProvider());
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

    public Regions getRegion()
    {
        return m_targetRegion;
    }

    public String getAccountDomain()
    {
        return m_targetDomain;
    }

    public AmazonEC2 getEc2()
    {
        return m_ec2;
    }

    public AmazonIdentityManagement getIam()
    {
        return m_iam;
    }

    //--//

    //--//

    public List<AvailabilityZone> listAvailabilityZones()
    {
        if (m_zones == null)
        {
            m_zones = m_ec2.describeAvailabilityZones()
                           .getAvailabilityZones();
        }

        return m_zones;
    }

    public List<Address> listElasticAddresses()
    {
        DescribeAddressesRequest request = new DescribeAddressesRequest();

        DescribeAddressesResult result = m_ec2.describeAddresses(request);

        return result.getAddresses();
    }

    public Address findActiveElasticAddress(String instanceId)
    {
        for (Address addr : listElasticAddresses())
        {
            if (instanceId.equals(addr.getInstanceId()))
            {
                return addr;
            }
        }

        return null;
    }

    public Address findUnusedOrAllocateNewElasticAddress()
    {
        for (Address addr : listElasticAddresses())
        {
            if (addr.getInstanceId() == null)
            {
                return addr;
            }
        }

        AllocateAddressResult result = m_ec2.allocateAddress();
        return selectElasticAddressFromIp(result.getPublicIp());
    }

    public Address selectElasticAddressFromIp(String publicIp)
    {
        DescribeAddressesRequest request = new DescribeAddressesRequest();
        request.withFilters(new Filter("public-ip").withValues(publicIp));

        DescribeAddressesResult result = m_ec2.describeAddresses(request);

        for (Address addr : result.getAddresses())
        {
            return addr;
        }

        return null;
    }

    public void releaseElasticAddress(Address addr)
    {
        ReleaseAddressRequest request = new ReleaseAddressRequest();
        request.withAllocationId(addr.getAllocationId());
        m_ec2.releaseAddress(request);
    }

    public void associateElasticIp(Instance instance,
                                   Address eip)
    {
        AssociateAddressRequest req = new AssociateAddressRequest();
        req.withInstanceId(instance.getInstanceId());
        req.withAllocationId(eip.getAllocationId());
        m_ec2.associateAddress(req);
    }

    //--//

    enum BootVariable implements IConfigVariable
    {
        InstanceType("INSTANCE_TYPE"),
        RepoAccount("REPO_ACCOUNT"),
        RepoPassword("REPO_PASSWORD");

        private final String m_variable;

        BootVariable(String variable)
        {
            m_variable = variable;
        }

        public String getVariable()
        {
            return m_variable;
        }
    }

    private static final ConfigVariables.Validator<BootVariable> s_configValidator            = new ConfigVariables.Validator<>(BootVariable.values());
    private static final ConfigVariables.Template<BootVariable>  s_template_genericBootScript = s_configValidator.newTemplate(AwsHelper.class, "aws/generic-boot-script.txt", "$[", "]");

    //--//

    public void useGenericBootScript(InstanceConfig cfg,
                                     String instanceType,
                                     String repoAccount,
                                     String repoPasswd) throws
                                                        IOException
    {
        ConfigVariables<BootVariable> parameters = s_template_genericBootScript.allocate();

        parameters.setValue(BootVariable.InstanceType, instanceType);
        parameters.setValue(BootVariable.RepoAccount, repoAccount);
        parameters.setValue(BootVariable.RepoPassword, repoPasswd);

        cfg.userData = parameters.convert();
    }

    //--//

    public void addAccessToSNS(Policy policy)
    {
        Statement stmt = newStatement(policy, Effect.Allow, new Resource("*"));
        stmt.withActions(SNSActions.Publish);
    }

    public void addAccessToEmail(Policy policy)
    {
        Statement stmt = newStatement(policy, Effect.Allow, new Resource("*"));
        stmt.withActions(SimpleEmailServiceActions.SendEmail, SimpleEmailServiceActions.SendRawEmail);
    }

    public void addAccessToInstanceConfig(Policy policy,
                                          String configNamespace,
                                          S3Actions... actions)
    {
        allowListingAllObjectsAndBuckets(policy);

        Statement stmt = newConfigAccess(policy, configNamespace);
        stmt.withActions(actions);
    }

    public void addAccessToInstanceBackup(Policy policy,
                                          String backupNamespace,
                                          S3Actions... actions)
    {
        allowListingAllObjectsAndBuckets(policy);

        Statement stmt = newBackupAccess(policy, backupNamespace);
        stmt.withActions(actions);
    }

    public String formatConfigPath(String configNamespace,
                                   String configFile)
    {
        return formatGenericPath(configNamespace, configFile, "config");
    }

    public String formatBackupPath(String backupNamespace,
                                   String backupFile)
    {
        return formatGenericPath(backupNamespace, backupFile, "backup");
    }

    public String formatStatePath(String stateNamespace,
                                  String stateFile)
    {
        return formatGenericPath(stateNamespace, stateFile, "state");
    }

    private static String formatGenericPath(String nameSpace,
                                            String file,
                                            String purpose)
    {
        return String.format("instance/%s/%s/%s", nameSpace, purpose, file != null ? file : "");
    }

    public static String sanitizePath(String file)
    {
        Path path = new File(file).toPath();

        return path.normalize()
                   .getFileName()
                   .toString();
    }

    private void allowListingAllObjectsAndBuckets(Policy policy)
    {
        Statement stmt = ensureStatementForResource(policy, Effect.Allow, new S3BucketResource("*"));
        stmt.withActions(S3Actions.ListBuckets, S3Actions.ListObjects);
    }

    private Statement newConfigAccess(Policy policy,
                                      String configNamespace)
    {
        return ensureStatementForResource(policy, Effect.Allow, new S3ObjectResource(S3_BUCKET_NAME, formatConfigPath(configNamespace, "*")));
    }

    private Statement newBackupAccess(Policy policy,
                                      String backupNamespace)
    {
        return ensureStatementForResource(policy, Effect.Allow, new S3ObjectResource(S3_BUCKET_NAME, formatBackupPath(backupNamespace, "*")));
    }

    private static Statement ensureStatementForResource(Policy policy,
                                                        Effect effect,
                                                        Resource resource)
    {
        Statement stmt = findStatementForResource(policy, effect, resource);
        if (stmt == null)
        {
            stmt = newStatement(policy, effect, resource);
        }

        return stmt;
    }

    private static Statement findStatementForResource(Policy policy,
                                                      Effect effect,
                                                      Resource resource)
    {
        for (Statement stmt : policy.getStatements())
        {
            for (Resource res : stmt.getResources())
            {
                if (StringUtils.equals(res.getId(), resource.getId()))
                {
                    if (effect == null || stmt.getEffect() == effect)
                    {
                        return stmt;
                    }
                }
            }
        }

        return null;
    }

    private static Statement newStatement(Policy policy,
                                          Effect effect,
                                          Resource resource)
    {
        Statement stmt = new Statement(effect);

        if (resource != null)
        {
            stmt.withResources(resource);
        }

        policy.getStatements()
              .add(stmt);

        return stmt;
    }

    private static Policy defaultAssumePolicy()
    {
        Policy policy = new Policy();

        Statement stmt = newStatement(policy, Effect.Allow, null);
        stmt.setPrincipals(new Principal(Services.AmazonEC2));
        stmt.withActions(SecurityTokenServiceActions.AssumeRole);

        return policy;
    }

    //--//

    public Role acquireRole(String roleName,
                            String policyName,
                            Policy policy)
    {
        Role role = findRole(roleName);
        if (role == null)
        {
            CreateRoleRequest req = new CreateRoleRequest();
            req.setRoleName(roleName);
            req.setAssumeRolePolicyDocument(defaultAssumePolicy().toJson());
            CreateRoleResult res = m_iam.createRole(req);

            role = res.getRole();
        }

        updatePolicy(role, policyName, policy);

        return role;
    }

    private void updatePolicy(Role role,
                              String policyName,
                              Policy policy)
    {
        String newPolicyDocument = policy.toJson();

        try
        {
            GetRolePolicyRequest req = new GetRolePolicyRequest();
            req.withRoleName(role.getRoleName());
            req.withPolicyName(policyName);
            GetRolePolicyResult res = m_iam.getRolePolicy(req);

            String policyOld = sanitizeUrlEncodedText(res.getPolicyDocument());
            if (newPolicyDocument.equals(policyOld))
            {
                return;
            }
        }
        catch (NoSuchEntityException e)
        {
        }

        PutRolePolicyRequest req = new PutRolePolicyRequest();
        req.withRoleName(role.getRoleName());
        req.withPolicyName(policyName);
        req.withPolicyDocument(newPolicyDocument);

        m_iam.putRolePolicy(req);
    }

    public void releaseRole(Role role)
    {
        String                  roleName    = role.getRoleName();
        ListRolePoliciesRequest reqPolicies = new ListRolePoliciesRequest().withRoleName(roleName);
        for (String policyName : m_iam.listRolePolicies(reqPolicies)
                                      .getPolicyNames())
        {
            DeleteRolePolicyRequest reqDelPolicy = new DeleteRolePolicyRequest();
            reqDelPolicy.withRoleName(roleName);
            reqDelPolicy.withPolicyName(policyName);
            m_iam.deleteRolePolicy(reqDelPolicy);
        }

        DeleteRoleRequest reqDelRole = new DeleteRoleRequest().withRoleName(roleName);
        m_iam.deleteRole(reqDelRole);
    }

    public Role findRole(String roleName)
    {
        try
        {
            GetRoleRequest req = new GetRoleRequest();
            req.withRoleName(roleName);
            GetRoleResult res = m_iam.getRole(req);

            return res.getRole();
        }
        catch (NoSuchEntityException e)
        {
            return null;
        }
    }

    //--//

    public InstanceProfile acquireInstanceProfile(Role role,
                                                  String profileName)
    {
        InstanceProfile profile = findInstanceProfile(profileName);
        if (profile == null)
        {
            CreateInstanceProfileRequest req = new CreateInstanceProfileRequest();
            req.setInstanceProfileName(profileName);
            CreateInstanceProfileResult res = m_iam.createInstanceProfile(req);

            profile = res.getInstanceProfile();
        }

        for (Role role2 : profile.getRoles())
        {
            if (StringUtils.equals(role2.getRoleName(), role.getRoleName()))
            {
                return profile;
            }
        }

        {
            AddRoleToInstanceProfileRequest req = new AddRoleToInstanceProfileRequest();
            req.setInstanceProfileName(profileName);
            req.setRoleName(role.getRoleName());

            m_iam.addRoleToInstanceProfile(req);

            profile.getRoles()
                   .add(role);
        }

        return profile;
    }

    public void releaseInstanceProfile(InstanceProfile profile)
    {
        for (Role role : profile.getRoles())
        {
            RemoveRoleFromInstanceProfileRequest req = new RemoveRoleFromInstanceProfileRequest();
            req.setInstanceProfileName(profile.getInstanceProfileName());
            req.setRoleName(role.getRoleName());
            m_iam.removeRoleFromInstanceProfile(req);
        }
    }

    public InstanceProfile findInstanceProfile(String profileName)
    {
        try
        {
            GetInstanceProfileRequest req = new GetInstanceProfileRequest();
            req.withInstanceProfileName(profileName);
            GetInstanceProfileResult res = m_iam.getInstanceProfile(req);

            return res.getInstanceProfile();
        }
        catch (NoSuchEntityException e)
        {
            return null;
        }
    }

    //--//

    public boolean pollForStartup(Instance instance,
                                  int pollIntervalInSeconds,
                                  int timeoutInMinutes,
                                  BiPredicate<Duration, Instance> callback)
    {
        return pollForInstanceStatusInner(instance, STATE_CODE_RUNNING, pollIntervalInSeconds, timeoutInMinutes, callback);
    }

    public boolean pollForTermination(Instance instance,
                                      int pollIntervalInSeconds,
                                      int timeoutInMinutes,
                                      BiPredicate<Duration, Instance> callback)
    {
        return pollForInstanceStatusInner(instance, STATE_CODE_TERMINATED, pollIntervalInSeconds, timeoutInMinutes, callback);
    }

    private boolean pollForInstanceStatusInner(Instance instance,
                                               int expectedCode,
                                               int pollIntervalInSeconds,
                                               int timeoutInMinutes,
                                               BiPredicate<Duration, Instance> callback)
    {
        Instant start  = Instant.now();
        Instant expire = start.plus(timeoutInMinutes, ChronoUnit.MINUTES);

        while (true)
        {
            Instant now = Instant.now();
            if (now.compareTo(expire) > 0)
            {
                // Timeout expired.
                return false;
            }

            StatusCheckResult res = checkForInstanceStatusInner(instance, expectedCode, (refreshedInstance) ->
            {
                if (callback == null)
                {
                    return true;
                }

                return callback.test(Duration.between(start, now), refreshedInstance);
            });
            if (res != StatusCheckResult.Pending)
            {
                return res == StatusCheckResult.Positive;
            }

            Executors.safeSleep(pollIntervalInSeconds * 1000);
        }
    }

    //--//

    public StatusCheckResult checkForStartup(Instance instance,
                                             Predicate<Instance> callback)
    {
        return checkForInstanceStatusInner(instance, STATE_CODE_RUNNING, callback);
    }

    public StatusCheckResult checkForTermination(Instance instance,
                                                 Predicate<Instance> callback)
    {
        return checkForInstanceStatusInner(instance, STATE_CODE_TERMINATED, callback);
    }

    private StatusCheckResult checkForInstanceStatusInner(Instance instance,
                                                          int expectedCode,
                                                          Predicate<Instance> callback)
    {
        Instance refreshedInstance = refreshInstance(instance);
        if (refreshedInstance == null && expectedCode == STATE_CODE_TERMINATED)
        {
            // No instances, assume it's gone.
            return StatusCheckResult.Positive;
        }

        if (refreshedInstance != null && callback != null && !callback.test(refreshedInstance))
        {
            return StatusCheckResult.Negative;
        }

        int code = refreshedInstance.getState()
                                    .getCode() & 0xFF;
        if (code == expectedCode)
        {
            return StatusCheckResult.Positive;
        }

        return StatusCheckResult.Pending;
    }

    //--//

    public static class InstanceConfig
    {
        public String instanceType = InstanceType.T2Micro.toString();

        public String image;

        public SecurityGroup sc;
        public Subnet        subnet;
        public String        privateIp;

        public int bootDiskSize    = 8; // In Gigabytes.
        public int provisionedIOPS = 0; // If you want provisioned IOPS, set to non-zero.

        public InstanceProfile profile;
        public String          userData;

        public List<Tag> tags = Lists.newArrayList();

        //--//

        public InstanceConfig(DockerImageArchitecture arch)
        {
            if (arch == DockerImageArchitecture.ARM64v8)
            {
                image = "ami-0f48d15c9efb5f63d"; // Amazon Linux 2 AMI (HVM) - Kernel 5.10, SSD Volume Type
            }
            else
            {
                //image = "ami-01725fc47587f08fc"; // amzn2-ami-minimal-hvm-2.0.20210813.1-x86_64-ebs
                image = "ami-0ca285d4c2cda3300"; // Amazon Linux 2 AMI (HVM) - Kernel 5.10, SSD Volume Type
            }
        }

        public void addTag(String name,
                           String value)
        {
            requireNonNull(name);
            requireNonNull(value);

            tags.add(new Tag(name, value));
        }

        public void addTagIfValuePresent(String name,
                                         String value)
        {
            if (StringUtils.isEmpty(value))
            {
                return;
            }

            addTag(name, value);
        }
    }

    public List<Instance> listInstances(boolean running)
    {
        DescribeInstancesRequest request = new DescribeInstancesRequest();

        if (running)
        {
            request.withFilters(new Filter("instance-state-name").withValues("running"));
        }

        DescribeInstancesResult result = m_ec2.describeInstances(request);

        List<Instance> instances = Lists.newArrayList();

        for (Reservation res : result.getReservations())
        {
            instances.addAll(res.getInstances());
        }

        return instances;
    }

    public Instance acquireInstance(String name,
                                    String purpose,
                                    InstanceConfig cfg)
    {
        RunInstancesRequest req = new RunInstancesRequest();

        if (cfg.profile != null)
        {
            IamInstanceProfileSpecification spec = new IamInstanceProfileSpecification().withArn(cfg.profile.getArn());
            req.withIamInstanceProfile(spec);
        }

        req.withSecurityGroupIds(cfg.sc.getGroupId());
        req.withSubnetId(cfg.subnet.getSubnetId());
        req.withPrivateIpAddress(cfg.privateIp);

        req.withKeyName(KEYPAIR__INFRASTRUCTURE_MANAGEMENT);

        req.withImageId(cfg.image);
        req.withInstanceType(cfg.instanceType);
        req.withMinCount(1);
        req.withMaxCount(1);

        List<Tag> tags = Lists.newArrayList(newNameTag(name), newPurposeTag(purpose));
        tags.addAll(cfg.tags);
        TagSpecification tagSpecifications = new TagSpecification();
        tagSpecifications.withResourceType("instance");
        tagSpecifications.withTags(tags);
        req.withTagSpecifications(tagSpecifications);

        {
            EbsBlockDevice ebs = new EbsBlockDevice().withVolumeSize(cfg.bootDiskSize)
                                                     .withDeleteOnTermination(true);

            if (cfg.provisionedIOPS > 0)
            {
                ebs.setVolumeType("io1");
                ebs.setIops(cfg.provisionedIOPS);
            }
            else
            {
                ebs.setVolumeType("gp2");
            }

            BlockDeviceMapping blockMapping = new BlockDeviceMapping();
            blockMapping.withDeviceName("/dev/xvda");
            blockMapping.withEbs(ebs);

            req.withBlockDeviceMappings(blockMapping);
//            req.withEbsOptimized(true);
        }

        if (cfg.userData != null)
        {
            setUserData(req, cfg.userData);
        }

        Reservation res = m_ec2.runInstances(req)
                               .getReservation();

        return CollectionUtils.firstElement(res.getInstances());
    }

    public void releaseInstance(Instance instance)
    {
        TerminateInstancesRequest req = new TerminateInstancesRequest().withInstanceIds(instance.getInstanceId());
        m_ec2.terminateInstances(req);
    }

    public Instance findInstanceByPurpose(String purpose)
    {
        return CollectionUtils.firstElement(filterInstances(true, filterForPurpose(purpose)));
    }

    public Instance findInstanceById(String instanceId)
    {
        DescribeInstancesRequest req = new DescribeInstancesRequest().withInstanceIds(instanceId);
        DescribeInstancesResult  res = m_ec2.describeInstances(req);

        for (Reservation reservation : res.getReservations())
        {
            for (Instance instance : reservation.getInstances())
            {
                if (StringUtils.equals(instance.getInstanceId(), instanceId))
                {
                    return instance;
                }
            }
        }

        return null;
    }

    public List<Instance> filterInstances(boolean skipTerminated,
                                          Filter... filters)
    {
        DescribeInstancesRequest req = new DescribeInstancesRequest().withFilters(filters);
        DescribeInstancesResult  res = m_ec2.describeInstances(req);

        List<Instance> result = Lists.newArrayList();

        for (Reservation reservation : res.getReservations())
        {
            for (Instance instance : reservation.getInstances())
            {
                if (skipTerminated && StringUtils.equals(instance.getState()
                                                                 .getName(), "terminated"))
                {
                    continue;
                }

                result.add(instance);
            }
        }

        return result;
    }

    public Instance refreshInstance(Instance pattern)
    {
        return findInstanceById(pattern.getInstanceId());
    }

    private void setUserData(RunInstancesRequest req,
                             String content)
    {
        req.withUserData(Base64.encodeAsString(content.getBytes()));
    }

    //--//

    public Vpc acquireVpc(AnnotatedName name,
                          String cidr)
    {
        Vpc vpc = findVpc(name);
        if (vpc == null)
        {
            {
                CreateVpcRequest req = new CreateVpcRequest(cidr);
                CreateVpcResult  res = m_ec2.createVpc(req);

                vpc = res.getVpc();
            }

            tagResource(vpc, newNameTag(name.description), newPurposeTag(name.uniqueIdentifier));
        }

        return vpc;
    }

    public void releaseVpc(Vpc vpc)
    {
        InternetGateway ig = findInternetGateway(vpc, null);
        if (ig != null)
        {
            DetachInternetGatewayRequest req = new DetachInternetGatewayRequest().withVpcId(vpc.getVpcId())
                                                                                 .withInternetGatewayId(ig.getInternetGatewayId());
            m_ec2.detachInternetGateway(req);

            releaseInternetGateway(ig);
        }

        for (Subnet subnet : findSubnets(vpc, null))
            releaseSubnet(subnet);

        SecurityGroup sg = findSecurityGroup(vpc, null);
        if (sg != null)
        {
            releaseSecurityGroup(sg);
        }

        DeleteVpcRequest req = new DeleteVpcRequest().withVpcId(vpc.getVpcId());
        m_ec2.deleteVpc(req);
    }

    public Vpc findVpc(AnnotatedName name)
    {
        DescribeVpcsRequest req = new DescribeVpcsRequest().withFilters(filterForPurpose(name.uniqueIdentifier));
        DescribeVpcsResult  res = m_ec2.describeVpcs(req);

        return CollectionUtils.firstElement(res.getVpcs());
    }

    //--//

    public SecurityGroup acquireSecurityGroup(Vpc vpc,
                                              AnnotatedName groupName,
                                              AnnotatedName valueForTag,
                                              Consumer<SecurityGroup> configureCallback)
    {
        SecurityGroup sc = findSecurityGroup(vpc, groupName);
        if (sc == null)
        {
            CreateSecurityGroupRequest req = new CreateSecurityGroupRequest();
            req.setVpcId(vpc.getVpcId());
            req.setGroupName(groupName.uniqueIdentifier);
            req.setDescription(groupName.description);
            CreateSecurityGroupResult res = m_ec2.createSecurityGroup(req);

            sc = new SecurityGroup().withGroupId(res.getGroupId());

            tagResource(sc, newNameTag(valueForTag.description), newPurposeTag(valueForTag.uniqueIdentifier));
        }

        if (configureCallback != null)
        {
            configureCallback.accept(sc);
        }

        return sc;
    }

    public void authorizeIngress(SecurityGroup sc,
                                 String ipProtocol,
                                 int port,
                                 String cidrIp)
    {
        authorizeIngress(sc, ipProtocol, port, port, cidrIp);
    }

    public void authorizeIngress(SecurityGroup sc,
                                 String ipProtocol,
                                 int fromPort,
                                 int toPort,
                                 String cidrIp)
    {
        for (IpPermission ipPerm : sc.getIpPermissions())
        {
            if (!StringUtils.equals(ipPerm.getIpProtocol(), ipProtocol))
            {
                continue;
            }

            if (ipPerm.getFromPort() != fromPort)
            {
                continue;
            }

            if (ipPerm.getToPort() != toPort)
            {
                continue;
            }

            if (findIpRange(ipPerm, cidrIp) == null)
            {
                continue;
            }

            // The permission already exist.
            return;
        }

        AuthorizeSecurityGroupIngressRequest req = new AuthorizeSecurityGroupIngressRequest();
        req.setGroupId(sc.getGroupId());
        req.setCidrIp(cidrIp);
        req.setFromPort(fromPort);
        req.setToPort(toPort);
        req.setIpProtocol(ipProtocol);
        m_ec2.authorizeSecurityGroupIngress(req);
    }

    public void revokeIngress(SecurityGroup sc,
                              String ipProtocol,
                              int port,
                              String cidrIp)
    {
        revokeIngress(sc, ipProtocol, port, port, cidrIp);
    }

    public void revokeIngress(SecurityGroup sc,
                              String ipProtocol,
                              int fromPort,
                              int toPort,
                              String cidrIp)
    {
        RevokeSecurityGroupIngressRequest req = new RevokeSecurityGroupIngressRequest();
        req.setGroupId(sc.getGroupId());
        req.setCidrIp(cidrIp);
        req.setFromPort(fromPort);
        req.setToPort(toPort);
        req.setIpProtocol(ipProtocol);
        m_ec2.revokeSecurityGroupIngress(req);
    }

    private static IpRange findIpRange(IpPermission ipPerm,
                                       String cidrIp)
    {
        for (IpRange ipRange : ipPerm.getIpv4Ranges())
        {
            if (StringUtils.equals(ipRange.getCidrIp(), cidrIp))
            {
                return ipRange;
            }
        }

        return null;
    }

    public void releaseSecurityGroup(SecurityGroup sg)
    {
        DeleteSecurityGroupRequest req = new DeleteSecurityGroupRequest().withGroupId(sg.getGroupId());
        m_ec2.deleteSecurityGroup(req);
    }

    public SecurityGroup findSecurityGroup(Vpc vpc,
                                           AnnotatedName groupName)
    {
        DescribeSecurityGroupsRequest req = new DescribeSecurityGroupsRequest();

        if (groupName != null)
        {
            req.getFilters()
               .add(new Filter("group-name").withValues(groupName.uniqueIdentifier));
        }

        req.getFilters()
           .add(filterForVpc(vpc));

        DescribeSecurityGroupsResult res = m_ec2.describeSecurityGroups(req);

        for (SecurityGroup sc : res.getSecurityGroups())
        {
            if (StringUtils.equals(sc.getGroupName(), "default"))
            {
                continue;
            }

            return sc;
        }

        return null;
    }

    private Filter filterForPurpose(String purpose)
    {
        return filterForTagValue(TAG__PURPOSE, purpose);
    }

    private Tag newPurposeTag(String purpose)
    {
        return new Tag(TAG__PURPOSE, purpose);
    }

    private Tag newNameTag(String name)
    {
        return new Tag("Name", name);
    }

    public static Filter filterForTagValue(String tagName,
                                           String tagValue)
    {
        return new Filter("tag:" + tagName).withValues(tagValue);
    }

    //--//

    public InternetGateway acquireInternetGateway(Vpc vpc,
                                                  AnnotatedName name)
    {
        InternetGateway ig = findInternetGateway(vpc, name);
        if (ig == null)
        {
            {
                CreateInternetGatewayResult res = m_ec2.createInternetGateway();

                ig = res.getInternetGateway();
            }

            {
                AttachInternetGatewayRequest req = new AttachInternetGatewayRequest().withVpcId(vpc.getVpcId())
                                                                                     .withInternetGatewayId(ig.getInternetGatewayId());
                m_ec2.attachInternetGateway(req);
            }

            tagResource(ig, newNameTag(name.description), newPurposeTag(name.uniqueIdentifier));
        }

        return ig;
    }

    public void releaseInternetGateway(InternetGateway ig)
    {
        DeleteInternetGatewayRequest req = new DeleteInternetGatewayRequest().withInternetGatewayId(ig.getInternetGatewayId());
        m_ec2.deleteInternetGateway(req);
    }

    public InternetGateway findInternetGateway(Vpc vpc,
                                               AnnotatedName name)
    {
        DescribeInternetGatewaysRequest req = new DescribeInternetGatewaysRequest();

        if (name != null && name.uniqueIdentifier != null)
        {
            req.getFilters()
               .add(filterForPurpose(name.uniqueIdentifier));
        }

        req.getFilters()
           .add(new Filter("attachment.vpc-id").withValues(vpc.getVpcId()));

        DescribeInternetGatewaysResult res = m_ec2.describeInternetGateways(req);

        return CollectionUtils.firstElement(res.getInternetGateways());
    }

    //--//

    public Route acquireRoute(Vpc vpc,
                              AnnotatedName name,
                              String cidr,
                              InternetGateway target)
    {
        RouteTable rt = findRouteTable(vpc);
        if (rt == null)
        {
            CreateRouteTableRequest req = new CreateRouteTableRequest().withVpcId(vpc.getVpcId());
            CreateRouteTableResult  res = m_ec2.createRouteTable(req);

            rt = res.getRouteTable();

            tagResource(rt, newNameTag(name.description), newPurposeTag(name.uniqueIdentifier));
        }

        for (Route route : rt.getRoutes())
        {
            if (route.getDestinationCidrBlock()
                     .equals(cidr) && route.getGatewayId()
                                           .equals(target.getInternetGatewayId()))
            {
                return route;
            }
        }

        {
            CreateRouteRequest req = new CreateRouteRequest();
            req.setDestinationCidrBlock(cidr);
            req.setGatewayId(target.getInternetGatewayId());
            req.setRouteTableId(rt.getRouteTableId());

            m_ec2.createRoute(req);
        }

        return null;
    }

    public RouteTable findRouteTable(Vpc vpc)
    {
        DescribeRouteTablesRequest req = new DescribeRouteTablesRequest().withFilters(filterForVpc(vpc));
        DescribeRouteTablesResult  res = m_ec2.describeRouteTables(req);

        return CollectionUtils.firstElement(res.getRouteTables());
    }

    //--//

    public Subnet acquireSubnet(Vpc vpc,
                                AnnotatedName name,
                                String zoneId,
                                String cidr)
    {
        Subnet subnet = findSubnet(vpc, name, zoneId);
        if (subnet == null)
        {
            {
                CreateSubnetRequest req = new CreateSubnetRequest(vpc.getVpcId(), cidr);
                req.withAvailabilityZone(zoneId);
                CreateSubnetResult res = m_ec2.createSubnet(req);

                subnet = res.getSubnet();
            }

            tagResource(subnet, newNameTag(name.description), newPurposeTag(name.uniqueIdentifier));

            {
                ModifySubnetAttributeRequest req = new ModifySubnetAttributeRequest();
                req.withSubnetId(subnet.getSubnetId());
                req.setMapPublicIpOnLaunch(true);
                m_ec2.modifySubnetAttribute(req);
            }
        }

        return subnet;
    }

    public void releaseSubnet(Subnet subnet)
    {
        DeleteSubnetRequest req = new DeleteSubnetRequest().withSubnetId(subnet.getSubnetId());
        m_ec2.deleteSubnet(req);
    }

    public Subnet findSubnet(Vpc vpc,
                             AnnotatedName name,
                             String zoneId)
    {
        for (Subnet subnet : findSubnets(vpc, name))
        {
            if (subnet.getAvailabilityZone()
                      .equals(zoneId))
            {
                return subnet;
            }
        }

        return null;
    }

    public List<Subnet> findSubnets(Vpc vpc,
                                    AnnotatedName name)
    {
        DescribeSubnetsRequest req = new DescribeSubnetsRequest();

        if (name != null && name.uniqueIdentifier != null)
        {
            req.withFilters(filterForPurpose(name.uniqueIdentifier), filterForVpc(vpc));
        }
        else
        {
            req.withFilters(filterForVpc(vpc));
        }

        DescribeSubnetsResult res = m_ec2.describeSubnets(req);

        return res.getSubnets();
    }

    private Filter filterForVpc(Vpc vpc)
    {
        return new Filter("vpc-id").withValues(vpc.getVpcId());
    }

    //--//

    public IdentityVerificationAttributes checkDomainVerification(String domain)
    {
        GetIdentityVerificationAttributesRequest req = new GetIdentityVerificationAttributesRequest();
        req.withIdentities(domain);

        GetIdentityVerificationAttributesResult res = m_ses.getIdentityVerificationAttributes(req);
        return res.getVerificationAttributes()
                  .get(domain);
    }

    public String getDomainVerificationToken(String domain)
    {
        VerifyDomainIdentityRequest req = new VerifyDomainIdentityRequest();
        req.withDomain(domain);

        VerifyDomainIdentityResult res = m_ses.verifyDomainIdentity(req);
        return res.getVerificationToken();
    }

    //--//

    public IdentityDkimAttributes checkDkimVerification(String domain)
    {
        GetIdentityDkimAttributesRequest req = new GetIdentityDkimAttributesRequest();
        req.withIdentities(domain);

        GetIdentityDkimAttributesResult res = m_ses.getIdentityDkimAttributes(req);
        return res.getDkimAttributes()
                  .get(domain);
    }

    public List<String> getDkimTokens(String domain)
    {
        VerifyDomainDkimRequest req = new VerifyDomainDkimRequest();
        req.withDomain(domain);

        VerifyDomainDkimResult res = m_ses.verifyDomainDkim(req);
        return res.getDkimTokens();
    }

    //--//

    public void tagResource(Vpc vpc,
                            Tag... tags)
    {
        tagResource(vpc.getVpcId(), tags);
    }

    public void tagResource(SecurityGroup sc,
                            Tag... tags)
    {
        tagResource(sc.getGroupId(), tags);
    }

    public void tagResource(RouteTable rt,
                            Tag... tags)
    {
        tagResource(rt.getRouteTableId(), tags);
    }

    public void tagResource(Subnet subnet,
                            Tag... tags)
    {
        tagResource(subnet.getSubnetId(), tags);
    }

    public void tagResource(InternetGateway ig,
                            Tag... tags)
    {
        tagResource(ig.getInternetGatewayId(), tags);
    }

    private void tagResource(String resource,
                             Tag... tags)
    {
        int attempts = 0;

        while (true)
        {
            try
            {
                CreateTagsRequest req = new CreateTagsRequest();
                req.withResources(resource);
                req.withTags(tags);

                m_ec2.createTags(req);
                return;
            }
            catch (Throwable t)
            {
                if (++attempts >= 10)
                {
                    throw t;
                }

                Executors.safeSleep(attempts * 500);
            }
        }
    }

    //--//

    public S3Entry listFilesOnS3(String prefix) throws
                                                AmazonServiceException
    {
        S3Entry res = new S3Entry(S3_BUCKET_NAME, prefix);
        res.getEntries();
        return res;
    }

    public void saveFileToS3(String key,
                             File file) throws
                                        AmazonClientException,
                                        InterruptedException
    {
        TransferManager tx = TransferManagerBuilder.standard()
                                                   .withS3Client(m_s3)
                                                   .build();
        try
        {
            Upload upload = tx.upload(S3_BUCKET_NAME, key, file);
            upload.waitForCompletion();
        }
        finally
        {
            tx.shutdownNow(false);
        }
    }

    public void loadFileFromS3(String key,
                               File file) throws
                                          AmazonClientException,
                                          InterruptedException
    {
        TransferManager tx = TransferManagerBuilder.standard()
                                                   .withS3Client(m_s3)
                                                   .build();
        try
        {
            Download download = tx.download(S3_BUCKET_NAME, key, file);
            download.waitForCompletion();
        }
        finally
        {
            tx.shutdownNow(false);
        }
    }

    public void deleteFileFromS3(String key) throws
                                             AmazonServiceException
    {
        m_s3.deleteObject(S3_BUCKET_NAME, key);
    }

    public void saveStreamToS3(String key,
                               InputStream input)
    {
        m_s3.putObject(S3_BUCKET_NAME, key, input, null);
    }

    public S3ObjectInputStream loadStreamFromS3(String key)
    {
        S3Object obj = m_s3.getObject(S3_BUCKET_NAME, key);
        return obj.getObjectContent();
    }

    //--//

    public String sendTextEmail(String from,
                                String subject,
                                String body,
                                String... to)
    {
        SendEmailRequest req = new SendEmailRequest();
        req.withSource(from);
        req.withDestination(new Destination().withToAddresses(to));

        Message message = new Message();
        message.withSubject(new Content(subject));
        message.withBody(new Body().withText(new Content(body)));
        req.withMessage(message);

        SendEmailResult res = m_ses.sendEmail(req);
        return res.getMessageId();
    }

    public String sendHtmlEmail(String from,
                                String subject,
                                String body,
                                String... to)
    {
        SendEmailRequest req = new SendEmailRequest();
        req.withSource(from);
        req.withDestination(new Destination().withToAddresses(to));

        Message message = new Message();
        message.withSubject(new Content(subject));
        message.withBody(new Body().withHtml(new Content(body)));
        req.withMessage(message);

        SendEmailResult res = m_ses.sendEmail(req);
        return res.getMessageId();
    }

    //--//

    public String sendTextMessage(String senderId,
                                  String message,
                                  String phoneNumber)
    {
        PublishRequest request = new PublishRequest().withMessage(message)
                                                     .withPhoneNumber(phoneNumber);

        request.addMessageAttributesEntry("AWS.SNS.SMS.SenderID", newStringAttribute(BoxingUtils.get(senderId, "Optio3")));
        request.addMessageAttributesEntry("AWS.SNS.SMS.SMSType", newStringAttribute("Transactional"));
        request.addMessageAttributesEntry("AWS.SNS.SMS.MaxPrice", newNumberAttribute("0.10"));

        PublishResult res = m_sns.publish(request);
        return res.getMessageId();
    }

    public static MessageAttributeValue newStringAttribute(String value)
    {
        return new MessageAttributeValue().withDataType("String")
                                          .withStringValue(value);
    }

    public static MessageAttributeValue newNumberAttribute(String value)
    {
        return new MessageAttributeValue().withDataType("Number")
                                          .withStringValue(value);
    }

    //--//

    public Map<String, MetricNamespace> listMetrics()
    {
        Map<String, MetricNamespace> metrics = Maps.newHashMap();

        ListMetricsRequest req = new ListMetricsRequest();
        while (true)
        {
            ListMetricsResult result = m_cw.listMetrics(req);
            for (Metric metric : result.getMetrics())
            {
                MetricNamespace metricNamespace = metrics.computeIfAbsent(metric.getNamespace(), MetricNamespace::new);
                MetricName      metricName      = metricNamespace.get(metric.getMetricName());

                metricName.add(metric.getDimensions());
            }

            String nextToken = result.getNextToken();
            if (nextToken == null)
            {
                break;
            }

            req.setNextToken(nextToken);
        }

        return metrics;
    }

    public List<MetricIdentity> listMetric(String namespace,
                                           String metricName)
    {
        ListMetricsRequest req = new ListMetricsRequest();
        req.setNamespace(namespace);
        req.setMetricName(metricName);

        MetricName m = new MetricName(namespace, metricName);

        while (true)
        {
            ListMetricsResult result = m_cw.listMetrics(req);
            for (Metric metric : result.getMetrics())
            {
                m.add(metric.getDimensions());
            }

            String nextToken = result.getNextToken();
            if (nextToken == null)
            {
                break;
            }

            req.setNextToken(nextToken);
        }

        return m.instances;
    }

    public List<Datapoint> getMetric(MetricIdentity metricIdentity,
                                     ZonedDateTime start,
                                     ZonedDateTime end,
                                     int period,
                                     String statistics)
    {
        GetMetricStatisticsRequest req = new GetMetricStatisticsRequest();
        req.setNamespace(metricIdentity.namespace);
        req.setMetricName(metricIdentity.metricName);
        req.setDimensions(metricIdentity.dimensions);

        req.setStartTime(Date.from(start.toInstant()));
        req.setEndTime(Date.from(end.toInstant()));
        req.setPeriod(period);
        req.setStatistics(Lists.newArrayList(statistics));

        GetMetricStatisticsResult res = m_cw.getMetricStatistics(req);
        return res.getDatapoints();
    }

    public List<Datapoint> getMetric(String namespace,
                                     String metricName,
                                     Instance instance,
                                     ZonedDateTime start,
                                     ZonedDateTime end,
                                     int period,
                                     String statistics)
    {
        GetMetricStatisticsRequest req = new GetMetricStatisticsRequest();
        req.setNamespace(namespace);
        req.setMetricName(metricName);
        req.withDimensions(new Dimension().withName("InstanceId")
                                          .withValue(instance.getInstanceId()));
        req.setStartTime(Date.from(start.toInstant()));
        req.setEndTime(Date.from(end.toInstant()));
        req.setPeriod(period);
        req.setStatistics(Lists.newArrayList(statistics));

        GetMetricStatisticsResult res = m_cw.getMetricStatistics(req);
        return res.getDatapoints();
    }

    //--//

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
            // It must contain only alphanumeric characters and/or the following: +=,.@_- (Service: AmazonIdentityManagement; Status Code: 400; Error Code: ValidationError; Request ID: 1361faf3-bb52-11e7-a88d-dd05476bf9e2)
            switch (c)
            {
                case ',':
                    c = '_';
                    break;

                case '+':
                case '=':
                case '.':
                case '@':
                case '_':
                case '-':
                    break;

                default:
                    if (!Character.isLetterOrDigit(c))
                    {
                        continue;
                    }
            }

            sb.append(c);
        }

        String res = sb.toString();
        if (res.length() > maxLength)
        {
            res = res.substring(0, maxLength);
        }

        return res;
    }

    private static String sanitizeUrlEncodedText(String txt)
    {
        if (txt != null && txt.startsWith("%"))
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < txt.length(); i++)
            {
                char c = txt.charAt(i);
                if (c == '%' && i + 2 < txt.length())
                {
                    final int u = Character.digit(txt.charAt(i + 1), 16);
                    final int l = Character.digit(txt.charAt(i + 2), 16);

                    sb.append((char) ((u << 4) + l));
                    i += 2;
                }
                else
                {
                    sb.append(c);
                }
            }

            return sb.toString();
        }

        return txt;
    }
}
