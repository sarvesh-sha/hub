/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.deploy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.archive.TarBuilder;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.infra.GoDaddyHelper;
import com.optio3.infra.StatusCheckResult;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.infra.godaddy.model.DNSRecord;
import com.optio3.infra.godaddy.model.RecordType;
import com.optio3.util.ConfigVariables;
import com.optio3.util.Exceptions;
import com.optio3.util.IConfigVariable;
import io.dropwizard.logging.LoggingUtil;

public abstract class CommonDeployer
{
    public static class DnsCache
    {
        final Map<String, Multimap<String, String>> m_map = Maps.newHashMap();

        public void flush()
        {
            m_map.clear();
        }
    }

    public static class ConfigIdentity
    {
        public String sysId;
        public String id;
        public String name;
    }

    public static class ConfigIdentities
    {
        public final ConfigIdentity customer = new ConfigIdentity();
        public final ConfigIdentity service  = new ConfigIdentity();
        public final ConfigIdentity host     = new ConfigIdentity();
        public       String         instanceId;
    }

    public enum ConfigTag
    {
        // @formatter:off
        CustomerSysId("Optio3Deployment_CustomerSysId"),
        CustomerId   ("Optio3Deployment_CustomerId"   ),
        CustomerName ("Optio3Deployment_CustomerName" ),
        ServiceSysId ("Optio3Deployment_ServiceSysId" ),
        ServiceId    ("Optio3Deployment_ServiceId"    ),
        ServiceName  ("Optio3Deployment_ServiceName"  ),
        FunctionId   ("Optio3Deployment_FunctionId"   ),
        HostSysId    ("Optio3Deployment_HostSysId"    ),
        HostId       ("Optio3Deployment_HostId"       ),
        HostName     ("Optio3Deployment_HostName"     );
        // @formatter:on

        //
        private final String m_variable;

        ConfigTag(String variable)
        {
            m_variable = variable;
        }

        public String getTag()
        {
            return m_variable;
        }
    }

    public enum ConfigVariable implements IConfigVariable
    {
        DnsHints("DNS_HINTS"),
        WebSocketConnectionUrl("WS_CONNECTION_URL"),
        CustomerId("CUSTOMER_ID"),
        HostId("HOST_ID"),
        InstanceId("INSTANCE_ID"),
        ImageTag("IMAGE_TAG"),
        RepoAccount("REPO_ACCOUNT"),
        RepoPassword("REPO_PASSWORD"),
        MachineAccountName("ACCOUNT_NAME"),
        MachineAccountPassword("ACCOUNT_PASSWORD"),
        // AWS specific
        ConfigFile("CONFIG_FILE"),
        // Azure specific
        ConfigValue("CONFIG_VALUE");

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

    public static class Metrics
    {
        public final ZonedDateTime timestamp;
        public       double        creditsRemainingSurplus;
        public       double        creditsRemaining;
        public       double        creditsConsumed;
        public       double        cpuLoad;

        public Metrics(ZonedDateTime timestamp)
        {
            this.timestamp = timestamp;
        }
    }

    public static final int PORT_TCP_SSH        = 22;
    public static final int PORT_TCP_HTTPS      = 443;
    public static final int PORT_UDP_MESSAGEBUS = 20443;

    protected static final ConfigVariables.Validator<ConfigVariable> s_configValidator        = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final   ConfigVariables.Template<ConfigVariable>  s_template_YamlFromBuild = s_configValidator.newTemplate(CommonDeployer.class, null, "${", "}");

    //--//

    protected final CredentialDirectory credDir;

    protected final UserInfo repoReader;
    protected final UserInfo repoWriter;

    //--//

    public final String           builderHostName;
    public final String           connectionUrl;
    public final ConfigIdentities identities;

    //
    // Values used to track an instance back to the customer/service/function.
    //
    public String functionId;

    public String  agentFqdn;
    public String  domainName = WellKnownSites.optio3DomainName();
    public boolean useStaticIp;
    public int     dnsTTL     = 3600;

    public final DockerImageArchitecture targetArch;
    public       String                  imageTag;
    public       String                  configTemplate;

    public       Integer      diskSize;
    public final Set<Integer> portsTCP = Sets.newHashSet();
    public final Set<Integer> portsUDP = Sets.newHashSet();

    protected CommonDeployer(CredentialDirectory credDir,
                             DockerImageArchitecture arch,
                             String builderHostName,
                             String connectionUrl,
                             ConfigIdentities identities)
    {
        disableSlf4jLogger();

        this.credDir    = credDir;
        this.targetArch = arch;

        String repo = WellKnownSites.dockerRegistry();
        repoReader = credDir.findFirstAutomationUser(repo, RoleType.Subscriber);
        repoWriter = credDir.findFirstAutomationUser(repo, RoleType.Publisher);

        //--//

        portsTCP.add(PORT_TCP_SSH);
        portsTCP.add(PORT_TCP_HTTPS);
        portsUDP.add(PORT_UDP_MESSAGEBUS);

        if (builderHostName == null)
        {
            builderHostName = WellKnownSites.builderServer();
        }

        if (connectionUrl == null)
        {
            connectionUrl = "wss://" + builderHostName;
        }

        if (identities == null)
        {
            identities = new ConfigIdentities();
        }

        if (identities.instanceId == null)
        {
            identities.instanceId = "bootstrap";
        }

        this.builderHostName = builderHostName;
        this.connectionUrl   = connectionUrl;
        this.identities      = identities;
    }

    private void disableSlf4jLogger()
    {
        final Logger root = LoggingUtil.getLoggerContext()
                                       .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.detachAndStopAllAppenders();
    }

    public byte[] generateConfiguration(String root,
                                        String dnsHints) throws
                                                         IOException
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try (TarBuilder builder = new TarBuilder(stream, true))
        {
            UserInfo machineAccount = credDir.findFirstAutomationUser(builderHostName, RoleType.Machine);

            ConfigVariables<ConfigVariable> parameters = s_template_YamlFromBuild.allocate();

            parameters.setValue(ConfigVariable.DnsHints, dnsHints);
            parameters.setValue(ConfigVariable.WebSocketConnectionUrl, connectionUrl);
            parameters.setValue(ConfigVariable.HostId, identities.host.id);
            parameters.setValue(ConfigVariable.InstanceId, identities.instanceId);

            // Legacy fields, remove after all the deployers have been updated.
            parameters.setValue(ConfigVariable.MachineAccountName, machineAccount.getEffectiveEmailAddress());
            parameters.setValue(ConfigVariable.MachineAccountPassword, machineAccount.getEffectivePassword());

            String yamlFile = parameters.convert(configTemplate);

            builder.addAsString(root, "deployer-prod.yml", yamlFile, 0440);
        }

        return stream.toByteArray();
    }

    //--//

    public abstract String deploy(boolean waitForStartup,
                                  boolean allowSNS,
                                  boolean allowEmail) throws
                                                      Exception;

    public abstract StatusCheckResult checkForStartup();

    public abstract String getPublicIp();

    public abstract List<Metrics> getMetrics(ZonedDateTime start,
                                             ZonedDateTime end,
                                             Duration interval);

    public abstract void updateDns(String oldIp,
                                   String newIp);

    public abstract void terminate(boolean waitForShutdown);

    public abstract StatusCheckResult checkForTermination();

    public abstract void cleanupService();

    public abstract void cleanupCustomerInRegion();

    public abstract void cleanupCustomer();

    //--//

    protected void validateHostname()
    {
        if (agentFqdn == null)
        {
            throw new RuntimeException("No Fully Qualified Domain Name for Agent");
        }

        if (!agentFqdn.endsWith("." + domainName))
        {
            throw Exceptions.newIllegalArgumentException("Domain mismatch between '%s' and '%s'", agentFqdn, domainName);
        }
    }

    private String getHostname()
    {
        validateHostname();

        return agentFqdn.substring(0, agentFqdn.length() - 1 - domainName.length());
    }

    protected String getDns()
    {
        try
        {
            InetAddress publicIp = InetAddress.getByName(getHostname());

            return publicIp.getHostAddress();
        }
        catch (UnknownHostException e)
        {
            return null;
        }
    }

    protected void refreshDns(String oldIp,
                              String newIp)
    {
        try (GoDaddyHelper godaddy = fetchGoDaddyHelper())
        {
            godaddy.refreshRecord(domainName, getHostname(), oldIp, newIp, dnsTTL);
        }
    }

    public Set<String> lookupDnsRecords(DnsCache cache)
    {
        Multimap<String, String> map = cache.m_map.get(domainName);
        if (map == null)
        {
            map = HashMultimap.create();
            cache.m_map.put(domainName, map);

            try (GoDaddyHelper godaddy = fetchGoDaddyHelper())
            {
                for (DNSRecord rec : godaddy.listDomain(domainName, RecordType.A))
                {
                    map.put(rec.name, rec.data);
                }
            }
        }

        return Sets.newHashSet(map.get(getHostname()));
    }

    protected GoDaddyHelper fetchGoDaddyHelper()
    {
        return GoDaddyHelper.buildCachedWithDirectoryLookup(credDir, domainName);
    }
}
