/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.amazonaws.regions.Regions;
import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3NoAuthenticationNeeded;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.deployment.DeploymentHostProvisioningInfo;
import com.optio3.cloud.builder.model.deployment.DeploymentHostProvisioningNotes;
import com.optio3.cloud.builder.model.deployment.DeploymentHostProvisioningStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.model.provision.ProvisionFirmware;
import com.optio3.cloud.builder.model.provision.ProvisionReport;
import com.optio3.cloud.builder.orchestration.tasks.recurring.RecurringAgentCheck;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.infra.AwsHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.cellular.CellularProvider;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.TimeUtils;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;

@Api(tags = { "DeploymentHostProvisioning" }) // For Swagger
@Optio3RestEndpoint(name = "DeploymentHostProvisioning") // For Optio3 Shell
@Path("/v1/deployment-host-provisioning")
public class DeploymentHostProvisioning
{
    enum ConfigVariable implements IConfigVariable
    {
        HostSysId("HOST_SYSID"),
        HostId("HOST_ID");

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

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator  = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_newUnit = s_configValidator.newTemplate(RecurringAgentCheck.class, "emails/provisioning/new_unit.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_newNote = s_configValidator.newTemplate(RecurringAgentCheck.class, "emails/provisioning/new_notes.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_ready   = s_configValidator.newTemplate(RecurringAgentCheck.class, "emails/provisioning/ready.txt", "${", "}");

    @Inject
    private BuilderApplication m_app;

    @Inject
    private BuilderConfiguration m_cfg;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    //--//

    @GET
    @Path("item/{id}/check-status")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @Optio3NoAuthenticationNeeded
    public DeploymentHostProvisioningStatus checkStatus(@PathParam("id") String id) throws
                                                                                    Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostProvisioningStatus status = new DeploymentHostProvisioningStatus();

            DeploymentHostRecord rec_host = getHost(sessionHolder, id, false, false);
            if (rec_host != null)
            {
                status.alreadyAssociatedWithCustomer = rec_host.getCustomerService() != null;
                status.recentlyOnline                = TimeUtils.wasUpdatedRecently(rec_host.getLastHeartbeat(), 30, TimeUnit.MINUTES);

                DeploymentHostProvisioningInfo info = rec_host.getProvisioningInfo(false);
                if (info != null)
                {
                    for (DeploymentHostProvisioningNotes note : info.notes)
                    {
                        status.alreadyShipped |= note.readyForShipping;
                        status.alreadyMarkedForProduction |= note.readyForProduction;
                    }
                }
            }

            return status;
        }
    }

    @POST
    @Path("item/{id}/add-notes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @Optio3NoAuthenticationNeeded
    public boolean addNotes(@PathParam("id") String id,
                            DeploymentHostProvisioningNotes note)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostRecord rec_host = getHost(sessionHolder, id, true, false);
            if (rec_host != null)
            {
                DeploymentHostProvisioningInfo info = rec_host.getProvisioningInfo(true);

                if (CollectionUtils.findFirst(info.notes, (noteOld) -> noteOld.equals(note)) == null)
                {
                    String subject;

                    ConfigVariables.Template<ConfigVariable> template;

                    if (note.readyForProduction && CollectionUtils.findFirst(info.notes, (noteOld) -> noteOld.readyForProduction) == null)
                    {
                        subject  = "Unit Ready For Production";
                        template = s_template_ready;
                    }
                    else if (note.readyForShipping && CollectionUtils.findFirst(info.notes, (noteOld) -> noteOld.readyForShipping) == null)
                    {
                        subject  = "Unit Ready For Shipping";
                        template = s_template_newNote;
                    }
                    else
                    {
                        subject  = "New Provisioning Notes";
                        template = s_template_newNote;
                    }

                    info.addNote(note);

                    rec_host.setProvisioningInfo(info);

                    ConfigVariables<ConfigVariable> parameters = prepareEmailBody(template, rec_host);

                    m_app.sendEmailNotification(BuilderApplication.EmailFlavor.Provisioning, rec_host.prepareEmailSubject(subject), parameters);

                    sessionHolder.commit();

                    return true;
                }
            }
        }

        return false;
    }

    @POST
    @Path("checkin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public boolean checkin(ProvisionReport report)
    {
        if (report == null || StringUtils.isEmpty(report.hostId))
        {
            return false;
        }

        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            report.imei = DeploymentHostProvisioningInfo.fixupNA(report.imei);

            DeploymentHostRecord rec_host = ensureHost(sessionHolder, report.hostId, StringUtils.isNotBlank(report.imei), CollectionUtils.isNotEmpty(report.tests));

            if (rec_host.getArchitecture() == DockerImageArchitecture.UNKNOWN)
            {
                rec_host.setArchitecture(report.architecture);
            }

            DeploymentHostProvisioningInfo info = rec_host.getProvisioningInfo(true);

            BuilderApplication.LoggerInstance.info("Provisioning Checkin");
            BuilderApplication.LoggerInstance.info("%s", ObjectMappers.prettyPrintAsJson(report));

            rec_host.putMetadata(DeploymentHostRecord.WellKnownMetadata.provisioningCheckin, TimeUtils.now());

            if (info.updateManufacturingInfo(report))
            {
                rec_host.setProvisioningInfo(info);

                ConfigVariables<DeploymentHostProvisioning.ConfigVariable> parameters = prepareEmailBody(s_template_newUnit, rec_host);

                m_app.sendEmailNotification(BuilderApplication.EmailFlavor.Provisioning, rec_host.prepareEmailSubject("Unit Registered From Production"), parameters);
            }

            // Link host to cellular network, if possible.
            if (CellularProvider.isValidICCID(report.iccid))
            {
                if (!rec_host.tryLinkingToCellular(m_cfg, report.iccid, report.imsi, null))
                {
                    BuilderApplication.LoggerInstance.warn("Unable to link host '%s' with ICCID '%s' to any Cellular Provider!!", report.hostId, report.iccid);
                }
            }

            sessionHolder.commit();

            return true;
        }
    }

    @GET
    @Path("recentCheckins/{manufacturingLocation}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProvisionReport> recentCheckins(@PathParam("manufacturingLocation") String manufacturingLocation)
    {
        List<ProvisionReport> res = Lists.newArrayList();

        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostRecord.streamAllRaw(sessionHolder, null, (host) ->
            {
                MetadataMap metadata = host.decodeMetadata();

                DeploymentHostProvisioningInfo info        = DeploymentHostRecord.WellKnownMetadata.provisioningInfo.get(metadata);
                ZonedDateTime                  lastCheckin = DeploymentHostRecord.WellKnownMetadata.provisioningCheckin.get(metadata);
                if (info != null && TimeUtils.wasUpdatedRecently(lastCheckin, 7, TimeUnit.DAYS))
                {
                    ProvisionReport manufacturingInfo = info.manufacturingInfo;
                    if (manufacturingInfo != null && StringUtils.equals(manufacturingInfo.manufacturingLocation, manufacturingLocation))
                    {
                        manufacturingInfo.timestamp = lastCheckin;
                        res.add(manufacturingInfo);
                    }
                }
            });
        }

        // Sort from most recent to oldest.
        res.sort((a, b) -> TimeUtils.compare(b.timestamp, a.timestamp));

        return res;
    }

    @GET
    @Path("firmware/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<ProvisionFirmware> listFirmwares(@QueryParam("prefix") String prefix)
    {
        List<ProvisionFirmware> firmwares = Lists.newArrayList();

        if (prefix == null)
        {
            prefix = "firmware_";
        }

        try (AwsHelper aws = AwsHelper.buildCachedWithDirectoryLookup(m_cfg.credentials, WellKnownSites.optio3DomainName(), Regions.US_WEST_2))
        {
            String            pathOnS3 = aws.formatStatePath("builder", null);
            AwsHelper.S3Entry root     = aws.listFilesOnS3(pathOnS3);
            for (AwsHelper.S3Entry file : root.getFiles())
            {
                String name = file.getKey()
                                  .substring(pathOnS3.length());

                if (name.startsWith(prefix))
                {
                    ProvisionFirmware firmware = new ProvisionFirmware();
                    firmware.size      = file.getSize();
                    firmware.name      = name;
                    firmware.timestamp = file.getLastModified();
                    firmwares.add(firmware);
                }
            }

            return firmwares;
        }
    }

    @GET
    @Path("firmware/stream/{fileName}")
    @Produces("application/gzip")
    @Optio3NoAuthenticationNeeded
    public InputStream streamFirmware(@PathParam("fileName") String fileName)
    {
        try (AwsHelper aws = AwsHelper.buildCachedWithDirectoryLookup(m_cfg.credentials, WellKnownSites.optio3DomainName(), Regions.US_WEST_2))
        {
            String fileOnS3 = aws.formatStatePath("builder", AwsHelper.sanitizePath(fileName));

            return aws.loadStreamFromS3(fileOnS3);
        }
    }

    //--//

    static DeploymentHostRecord ensureHost(SessionHolder sessionHolder,
                                           String id,
                                           boolean hasCellular,
                                           boolean hasTests)
    {
        DeploymentHostRecord rec_host = getHost(sessionHolder, id, hasCellular, hasTests);
        if (rec_host == null)
        {
            String uniqueName = DeploymentHostRecord.findUniqueName(sessionHolder, id, null, hasCellular);

            rec_host = DeploymentHostRecord.buildNewHost(id, uniqueName, DockerImageArchitecture.UNKNOWN, null, null, null);
            rec_host.setStatus(DeploymentStatus.Ready);
            rec_host.setOperationalStatus(hasTests ? DeploymentOperationalStatus.factoryFloor : DeploymentOperationalStatus.provisioned);
            rec_host.setWarningThreshold(900);

            sessionHolder.persistEntity(rec_host);
        }

        return rec_host;
    }

    static DeploymentHostRecord getHost(SessionHolder sessionHolder,
                                        String id,
                                        boolean hasCellular,
                                        boolean hasTests)
    {
        RecordHelper<DeploymentHostRecord> helper_host = sessionHolder.createHelper(DeploymentHostRecord.class);

        RecordLocked<DeploymentHostRecord> lock_host = helper_host.getWithLockOrNull(id, 2, TimeUnit.MINUTES);
        if (lock_host == null)
        {
            TypedRecordIdentity<DeploymentHostRecord> ri_host = DeploymentHostRecord.findByHostId(helper_host, id);
            if (ri_host != null)
            {
                lock_host = helper_host.getWithLockOrNull(ri_host.sysId, 2, TimeUnit.MINUTES);
            }
        }

        return lock_host != null ? lock_host.get() : null;
    }

    private ConfigVariables<ConfigVariable> prepareEmailBody(ConfigVariables.Template<ConfigVariable> template,
                                                             DeploymentHostRecord rec_host)
    {
        ConfigVariables<ConfigVariable> parameters = template.allocate();
        String                          name       = rec_host.getDisplayName();

        parameters.setValue(ConfigVariable.HostSysId, rec_host.getSysId());
        parameters.setValue(ConfigVariable.HostId, name);

        return parameters;
    }
}
