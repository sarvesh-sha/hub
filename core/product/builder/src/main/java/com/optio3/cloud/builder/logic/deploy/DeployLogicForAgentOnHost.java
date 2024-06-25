/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.logic.deploy;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.customer.CustomerVertical;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentInstance;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.StatusCheckResult;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.infra.deploy.CommonDeployer;
import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.util.Base64EncodedValue;
import com.optio3.util.TimeUtils;

public class DeployLogicForAgentOnHost extends DeployLogicForAgent
{
    private final RecordLocator<CustomerServiceRecord> m_targetService;
    private final DeploymentRole[]                     m_targetRoles;

    public DeployLogicForAgentOnHost(SessionHolder sessionHolder,
                                     DeploymentHostRecord targetHost,
                                     CustomerServiceRecord targetService,
                                     DeploymentRole[] targetRoles)
    {
        super(sessionHolder, targetHost);

        m_targetService = sessionHolder.createLocator(requireNonNull(targetService));
        m_targetRoles   = targetRoles;
    }

    //--//

    public static RecordLocked<DeploymentHostRecord> initializeNewHost(RecordLocked<CustomerServiceRecord> lock_targetService,
                                                                       String instanceAccount,
                                                                       DeploymentInstance instanceType,
                                                                       String instanceRegion,
                                                                       DeploymentRole[] targetRoles) throws
                                                                                                     Exception
    {
        CustomerServiceRecord targetService = lock_targetService.get();

        SessionHolder                      sessionHolder = lock_targetService.getSessionHolder();
        RecordHelper<DeploymentHostRecord> helper_host   = sessionHolder.createHelper(DeploymentHostRecord.class);

        String hostIdPrefix = targetService.getName();
        hostIdPrefix = hostIdPrefix.replaceAll("[^0-9a-zA-Z_]", "_");

        // Assign a unique Id Prefix to the rec_host.
        int seq = 1;
        while (true)
        {
            String hostIdUnique = hostIdPrefix + "_" + seq;

            if (DeploymentHostRecord.findByHostId(helper_host, hostIdUnique) == null)
            {
                DeploymentHostRecord rec_host = DeploymentHostRecord.buildNewHost(hostIdUnique, hostIdUnique, instanceType.deployerArch, instanceAccount, instanceType, instanceRegion);
                rec_host.setLastHeartbeat(TimeUtils.now());
                rec_host.setDnsName(targetService);

                RecordLocked<DeploymentHostRecord> targetHost = helper_host.persist(rec_host);

                for (DeploymentRole role : targetRoles)
                {
                    rec_host.bindRole(targetService, role);
                }

                rec_host.renameBasedOnRole(sessionHolder);

                return targetHost;
            }

            seq++;
        }
    }

    //--//

    public String bootHost(RecordLocator<RegistryTaggedImageRecord> loc_image,
                           Integer diskSize,
                           boolean allowSNS,
                           boolean allowEmail) throws
                                               Exception
    {
        CommonDeployer deployer = prepareDeployer();

        if (diskSize != null)
        {
            deployer.diskSize = diskSize;
        }

        sessionProvider.callWithSessionWithAutoCommit(sessionHolder ->
                                                      {
                                                          RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocator(loc_image);

                                                          // Remove the registry information.
                                                          DockerImageIdentifier imageParsed = new DockerImageIdentifier(rec_taggedImage.getTag());
                                                          imageParsed.registryHost = null;
                                                          imageParsed.registryPort = null;

                                                          deployer.imageTag = imageParsed.getFullName();

                                                          Base64EncodedValue config = rec_taggedImage.findLabelOrDefault(WellKnownDockerImageLabel.ConfigTemplate, null);
                                                          deployer.configTemplate = new String(config.getValue());
                                                      });

        return deployer.deploy(false, allowSNS, allowEmail);
    }

    public StatusCheckResult checkHostStartup() throws
                                                Exception
    {
        CommonDeployer deployer = prepareDeployer();

        StatusCheckResult res = deployer.checkForStartup();
        if (res == StatusCheckResult.Positive)
        {
            String ip = deployer.getPublicIp();
            deployer.updateDns(null, ip);

            sessionProvider.callWithSessionWithAutoCommit(sessionHolder ->
                                                          {
                                                              DeploymentHostRecord rec_host = sessionHolder.fromLocator(loc_targetHost);
                                                              rec_host.setInstanceIp(ip);
                                                          });
        }

        return res;
    }

    public String getPublicIp() throws
                                Exception
    {
        CommonDeployer deployer = prepareDeployer();

        return deployer.getPublicIp();
    }

    public List<CommonDeployer.Metrics> getMetrics(ZonedDateTime start,
                                                   ZonedDateTime end,
                                                   Duration interval) throws
                                                                      Exception
    {
        CommonDeployer deployer = prepareDeployer();

        return deployer.getMetrics(start, end, interval);
    }

    public void updateDns(String oldIp,
                          String newIp) throws
                                        Exception
    {
        CommonDeployer deployer = prepareDeployer();

        deployer.updateDns(oldIp, newIp);

        sessionProvider.callWithSessionWithAutoCommit(sessionHolder ->
                                                      {
                                                          DeploymentHostRecord rec_host = sessionHolder.fromLocator(loc_targetHost);
                                                          rec_host.setInstanceIp(newIp);
                                                      });
    }

    public Set<String> lookupDnsRecords(CommonDeployer.DnsCache cache) throws
                                                                       Exception
    {
        CommonDeployer deployer = prepareDeployer();

        return deployer.lookupDnsRecords(cache);
    }

    public void terminateHost() throws
                                Exception
    {
        CommonDeployer deployer = prepareDeployer();

        sessionProvider.callWithSessionWithAutoCommit(sessionHolder ->
                                                      {
                                                          DeploymentHostRecord rec_host = sessionHolder.fromLocator(loc_targetHost);
                                                          String               ip       = rec_host.getInstanceIp();
                                                          if (ip != null)
                                                          {
                                                              deployer.updateDns(ip, null);
                                                              rec_host.setInstanceIp(null);
                                                          }
                                                      });

        deployer.terminate(false);
    }

    public StatusCheckResult checkHostShutdown() throws
                                                 Exception
    {
        CommonDeployer deployer = prepareDeployer();

        return deployer.checkForTermination();
    }

    //--//

    private CommonDeployer prepareDeployer() throws
                                             Exception
    {
        return sessionProvider.computeInReadOnlySession(sessionHolder ->
                                                        {
                                                            DeploymentHostRecord  rec_host = sessionHolder.fromLocator(loc_targetHost);
                                                            CustomerServiceRecord rec_svc  = sessionHolder.fromLocator(m_targetService);

                                                            CommonDeployer deployer = allocateDeployer(rec_host.getInstanceAccount(),
                                                                                                       rec_host.getInstanceType(),
                                                                                                       rec_host.getInstanceRegion(),
                                                                                                       getConfiguration(),
                                                                                                       rec_svc.getCustomer(),
                                                                                                       rec_svc,
                                                                                                       rec_host);
                                                            deployer.agentFqdn = rec_host.getDnsName();

                                                            if (m_targetRoles != null)
                                                            {
                                                                for (DeploymentRole role : m_targetRoles)
                                                                {
                                                                    String name = role.name();

                                                                    if (deployer.functionId != null)
                                                                    {
                                                                        name = deployer.functionId + "," + name;
                                                                    }

                                                                    deployer.functionId = name;
                                                                }
                                                            }

                                                            CustomerVertical vertical = rec_svc.getVertical();
                                                            vertical.fixupDeployer(deployer);

                                                            vertical.extraTcpPortsToOpen()
                                                                    .forEach((k, v) -> deployer.portsTCP.add(k));

                                                            vertical.extraUdpPortsToOpen()
                                                                    .forEach((k, v) -> deployer.portsUDP.add(k));

                                                            BuilderConfiguration cfg = sessionHolder.getServiceNonNull(BuilderConfiguration.class);
                                                            if (cfg.developerSettings.developerMode)
                                                            {
                                                                // Reduce DNS TTL to 30 minutes on developer machines.
                                                                deployer.dnsTTL = 30 * 60;
                                                            }

                                                            return deployer;
                                                        });
    }
}
