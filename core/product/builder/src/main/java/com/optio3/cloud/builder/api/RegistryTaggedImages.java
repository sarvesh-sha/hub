/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3NoAuthenticationNeeded;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.jobs.output.RegistryImageReleaseStatus;
import com.optio3.cloud.builder.model.jobs.output.RegistryTaggedImage;
import com.optio3.cloud.builder.model.jobs.output.RegistryTaggedImageUsage;
import com.optio3.cloud.builder.model.jobs.output.ReleaseStatusReport;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedImagePull;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;

@Api(tags = { "RegistryTaggedImages" }) // For Swagger
@Optio3RestEndpoint(name = "RegistryTaggedImages") // For Optio3 Shell
@Path("/v1/registry-tagged-images")
public class RegistryTaggedImages
{
    @Inject
    private BuilderApplication m_app;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    //--//

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<RegistryTaggedImage> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<RegistryTaggedImageRecord> helper = sessionHolder.createHelper(RegistryTaggedImageRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, RegistryTaggedImageRecord.getBatch(helper, ids));
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public RegistryTaggedImage get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, RegistryTaggedImageRecord.class, id);
    }

    @DELETE
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults remove(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            RecordHelper<RegistryTaggedImageRecord> helper = validation.sessionHolder.createHelper(RegistryTaggedImageRecord.class);
            RegistryTaggedImageRecord               rec    = helper.getOrNull(id);
            if (rec != null)
            {
                rec.remove(validation, helper);
            }

            return validation.getResults();
        }
    }

    //--//

    @GET
    @Path("item/{id}/usage")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public RegistryTaggedImageUsage getUsage(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RegistryTaggedImageRecord rec = sessionHolder.getEntity(RegistryTaggedImageRecord.class, id);

            DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
            settings.loadImages      = true;
            settings.loadDeployments = true;
            settings.loadServices    = true;
            settings.loadBackups     = true;
            settings.linkImages      = true;

            DeploymentGlobalDescriptor globalDescriptor = DeploymentGlobalDescriptor.get(sessionHolder, settings);

            return rec.getUsage(globalDescriptor);
        }
    }

    //--//

    @POST
    @Path("item/{id}/mark/{status}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    public RegistryTaggedImage mark(@PathParam("id") String id,
                                    @PathParam("status") RegistryImageReleaseStatus status) throws
                                                                                            Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<RegistryTaggedImageRecord> helper = sessionHolder.createHelper(RegistryTaggedImageRecord.class);
            RegistryTaggedImageRecord               rec    = helper.get(id);

            rec.mark(sessionHolder, helper, status);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("report/{status}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ReleaseStatusReport> report(@PathParam("status") RegistryImageReleaseStatus status)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            Set<ReleaseStatusReport> results = RegistryTaggedImageRecord.reportReleaseStatus(sessionHolder, status);

            return Lists.newArrayList(results);
        }
    }

    @GET
    @Path("lookup-tag")
    @Produces(MediaType.TEXT_PLAIN)
    @Optio3NoAuthenticationNeeded
    public String lookupTagForRole(@QueryParam("status") RegistryImageReleaseStatus status,
                                   @QueryParam("arch") DockerImageArchitecture arch,
                                   @QueryParam("role") DeploymentRole role)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            for (ReleaseStatusReport report : RegistryTaggedImageRecord.reportReleaseStatus(sessionHolder, status))
            {
                if (report.architecture == arch && report.role == role)
                {
                    RegistryTaggedImageRecord rec = sessionHolder.fromIdentity(report.image);
                    return rec.getTag();
                }
            }
        }

        return "";
    }

    //--//

    @GET
    @Path("item/{id}/distribute")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    public int distribute(@PathParam("id") String id,
                          @QueryParam("status") DeploymentOperationalStatus status,
                          @QueryParam("hostId") String hostId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<DeploymentHostRecord>      helper_host  = sessionHolder.createHelper(DeploymentHostRecord.class);
            RecordHelper<RegistryTaggedImageRecord> helper_image = sessionHolder.createHelper(RegistryTaggedImageRecord.class);

            RegistryTaggedImageRecord rec_taggedImage = helper_image.get(id);
            DockerImageArchitecture   architecture    = rec_taggedImage.getArchitecture();
            int                       count           = 0;

            DeploymentRole role = rec_taggedImage.getTargetService();
            for (RecordIdentity ri : DeploymentHostRecord.list(helper_host, null))
            {
                if (hostId != null && !StringUtils.equals(hostId, ri.sysId))
                {
                    continue;
                }

                DeploymentHostRecord rec_host = helper_host.get(ri.sysId);

                if (status != null && rec_host.getOperationalStatus() != status)
                {
                    continue;
                }

                if (!DockerImageArchitecture.areCompatible(rec_host.getArchitecture(), architecture))
                {
                    continue;
                }

                if (role != null)
                {
                    switch (role)
                    {
                        case deployer:
                        case waypoint:
                        case provisioner:
                            break; // All hosts can get this image.

                        case gateway:
                        case hub:
                        case database:
                            if (!rec_host.hasRole(role))
                            {
                                // Only send image to hosts compatible with the role.
                                continue;
                            }

                            break;
                    }
                }

                try (SessionHolder subSessionHolder = sessionHolder.spawnNewSessionWithTransaction())
                {
                    RecordLocked<DeploymentHostRecord> lock_host        = subSessionHolder.getEntityWithLock(DeploymentHostRecord.class, ri.sysId, 30, TimeUnit.SECONDS);
                    RegistryTaggedImageRecord          rec_taggedImage2 = subSessionHolder.getEntity(RegistryTaggedImageRecord.class, rec_taggedImage.getSysId());

                    if (DelayedImagePull.queue(lock_host, rec_taggedImage2, null))
                    {
                        count++;
                    }

                    subSessionHolder.commit();
                }
                catch (Throwable t)
                {
                    BuilderApplication.LoggerInstance.error("Failed to start download of '%s' on host '%s', due to %s", rec_taggedImage.getTag(), rec_host.getDisplayName(), t);
                }
            }

            return count;
        }
    }
}
