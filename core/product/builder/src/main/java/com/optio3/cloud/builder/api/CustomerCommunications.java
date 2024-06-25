/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3NoAuthenticationNeeded;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.communication.CrashReport;
import com.optio3.cloud.builder.model.communication.DeviceDetails;
import com.optio3.cloud.builder.model.communication.EmailMessage;
import com.optio3.cloud.builder.model.communication.TextMessage;
import com.optio3.cloud.builder.model.deployment.DeploymentHostDetails;
import com.optio3.cloud.builder.model.deployment.DeploymentHostProvisioningInfo;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.model.provision.ProvisionReport;
import com.optio3.cloud.builder.orchestration.tasks.bookkeeping.TaskForNotification;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentInstance;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.NotAuthenticatedException;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.TimeUtils;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;

@Api(tags = { "CustomerCommunications" }) // For Swagger
@Optio3RestEndpoint(name = "CustomerCommunications") // For Optio3 Shell
@Path("/v1/customer-communications")
public class CustomerCommunications
{
    @Inject
    private BuilderConfiguration m_cfg;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    // Temporary filter to remove duplicates...
    private static final Set<String> s_seen = Sets.newHashSet();

    @POST
    @Path("send-email")
    @Optio3NoAuthenticationNeeded
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public String sendEmail(@QueryParam("customerId") String customerId,
                            @QueryParam("customerAccessKey") String customerAccessKey,
                            EmailMessage def) throws
                                              Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            CustomerServiceRecord rec_svc = sessionHolder.getEntity(CustomerServiceRecord.class, customerId);
            if (!rec_svc.checkAccessKey(m_cfg, customerAccessKey))
            {
                BuilderApplication.LoggerInstance.info("Invalid access key, ignoring Email request from '%s'", rec_svc.getDisplayName());
                return null;
            }

            if (rec_svc.getDisableEmails() && !def.systemGenerated)
            {
                BuilderApplication.LoggerInstance.info("Ignoring non-system generated Email request from '%s'", rec_svc.getDisplayName());
                return null;
            }

            final BackgroundActivityRecord rec_activity = TaskForNotification.scheduleTask(sessionHolder, rec_svc, def, null, null);

            sessionHolder.commit();

            return rec_activity.getSysId();
        }
    }

    @POST
    @Path("send-text")
    @Optio3NoAuthenticationNeeded
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public String sendText(@QueryParam("customerId") String customerId,
                           @QueryParam("customerAccessKey") String customerAccessKey,
                           TextMessage def) throws
                                            Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            CustomerServiceRecord rec_svc = sessionHolder.getEntity(CustomerServiceRecord.class, customerId);
            if (!rec_svc.checkAccessKey(m_cfg, customerAccessKey))
            {
                BuilderApplication.LoggerInstance.info("Invalid access key, ignoring SMS request from '%s'", rec_svc.getDisplayName());
                return null;
            }

            if (rec_svc.getDisableTexts() && !def.systemGenerated)
            {
                BuilderApplication.LoggerInstance.info("Ignoring non-system generated SMS request from '%s'", rec_svc.getDisplayName());
                return null;
            }

            final BackgroundActivityRecord rec_activity = TaskForNotification.scheduleTask(sessionHolder, rec_svc, null, def, null);

            sessionHolder.commit();

            return rec_activity.getSysId();
        }
    }

    @POST
    @Path("register-device")
    @Optio3NoAuthenticationNeeded
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String registerDevice(@QueryParam("customerId") String customerId,
                                 @QueryParam("customerAccessKey") String customerAccessKey,
                                 DeviceDetails def) throws
                                                    Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<CustomerServiceRecord> helper_svc = sessionHolder.createHelper(CustomerServiceRecord.class);
            RecordLocked<CustomerServiceRecord> lock_svc   = helper_svc.getWithLock(customerId, 2, TimeUnit.MINUTES);
            CustomerServiceRecord               rec_svc    = lock_svc.get();
            if (!rec_svc.checkAccessKey(m_cfg, customerAccessKey))
            {
                throw new NotAuthenticatedException("Invalid access key");
            }

            //--//

            if (StringUtils.isBlank(def.hostId))
            {
                throw new InvalidArgumentException("No hostId");
            }

            if (StringUtils.isBlank(def.productId))
            {
                throw new InvalidArgumentException("No productId");
            }

            if (StringUtils.isBlank(def.iccid))
            {
                throw new InvalidArgumentException("No ICCID");
            }

            //--//

            DeploymentHostRecord                      rec_host;
            RecordLocked<DeploymentHostRecord>        lock_host;
            RecordHelper<DeploymentHostRecord>        helper_host = sessionHolder.createHelper(DeploymentHostRecord.class);
            TypedRecordIdentity<DeploymentHostRecord> ri_host     = DeploymentHostRecord.findByHostId(helper_host, def.hostId);
            if (ri_host == null)
            {
                rec_host = DeploymentHostRecord.buildNewHost(def.hostId, def.hostId, DockerImageArchitecture.UNKNOWN, null, def.instanceType, null);
                rec_host.setStatus(DeploymentStatus.Ready);
                rec_host.setOperationalStatus(DeploymentOperationalStatus.operational);
                rec_host.setWarningThreshold(7 * 24 * 60); // One week
                lock_host = helper_host.persist(rec_host);
            }
            else
            {
                lock_host = helper_host.getWithLock(ri_host.sysId, 2, TimeUnit.MINUTES);
                rec_host  = lock_host.get();
            }

            DeploymentInstance instanceType = rec_host.getInstanceType();
            if (instanceType != null && instanceType.autoRole != null)
            {
                if (!rec_host.hasRole(instanceType.autoRole))
                {
                    rec_host.bindRole(rec_svc, def.instanceType.autoRole);
                    rec_host.renameBasedOnRole(sessionHolder);
                }
            }

            DeploymentHostProvisioningInfo info              = rec_host.getProvisioningInfo(true);
            ProvisionReport                manufacturingInfo = info.manufacturingInfo;
            if (manufacturingInfo == null)
            {
                manufacturingInfo = new ProvisionReport();

                manufacturingInfo.hostId = def.hostId;
                manufacturingInfo.imei   = def.imei;
                manufacturingInfo.iccid  = def.iccid;

                manufacturingInfo.boardHardwareVersion = def.hardwareRevision;
                manufacturingInfo.firmwareVersion      = def.firmwareVersion;

                info.manufacturingInfo = manufacturingInfo;
                rec_host.setProvisioningInfo(info);
            }

            DeploymentHostDetails details = rec_host.getDetails();
            if (details == null || details.cellular == null || !details.cellular.sameICCID(def.iccid))
            {
                if (!rec_host.tryLinkingToCellular(m_cfg, def.iccid, def.imsi, def.productId))
                {
                    BuilderApplication.LoggerInstance.warn("Unable to link host '%s' with ICCID '%s' to any Cellular Provider!!", def.hostId, def.iccid);
                }
            }

            rec_host.setLastHeartbeat(TimeUtils.now());

            sessionHolder.commit();

            return rec_host.getSysId();
        }
    }

    @POST
    @Path("report-crash")
    @Optio3NoAuthenticationNeeded
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public String reportCrash(@QueryParam("customerId") String customerId,
                              @QueryParam("customerAccessKey") String customerAccessKey,
                              CrashReport def) throws
                                               Exception
    {
        synchronized (s_seen)
        {
            if (def.stack != null)
            {
                // Only look at top N lines for duplicate detection.
                final int     maxLinesForContext = 10;
                StringBuilder sb                 = new StringBuilder();
                String[]      lines              = StringUtils.split(def.stack, '\n');
                int           maxLines           = Math.min(lines.length, maxLinesForContext);
                for (int i = 0; i < maxLines; i++)
                {
                    sb.append(lines[i]);
                }

                if (s_seen.add(sb.toString()))
                {
                    try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
                    {
                        CustomerServiceRecord rec_svc = sessionHolder.getEntity(CustomerServiceRecord.class, customerId);
                        if (!rec_svc.checkAccessKey(m_cfg, customerAccessKey))
                        {
                            BuilderApplication.LoggerInstance.info("Invalid access key, ignoring Email request from '%s'", rec_svc.getDisplayName());
                            return null;
                        }

                        def.site = rec_svc.getUrl();
                        final BackgroundActivityRecord rec_activity = TaskForNotification.scheduleTask(sessionHolder, rec_svc, null, null, def);

                        sessionHolder.commit();

                        return rec_activity.getSysId();
                    }
                }
            }

            return null;
        }
    }
}
