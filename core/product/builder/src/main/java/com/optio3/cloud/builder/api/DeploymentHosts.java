/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import static com.optio3.util.Exceptions.getAndUnwrapException;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

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
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForHub;
import com.optio3.cloud.builder.model.common.LogLine;
import com.optio3.cloud.builder.model.customer.RoleAndArchitectureWithImage;
import com.optio3.cloud.builder.model.deployment.BootOptions;
import com.optio3.cloud.builder.model.deployment.DeploymentAgentUpgrade;
import com.optio3.cloud.builder.model.deployment.DeploymentAgentUpgradeDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularCharges;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularChargesSummary;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularCommunications;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularSession;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularSessions;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentHostDetails;
import com.optio3.cloud.builder.model.deployment.DeploymentHostFilterRequest;
import com.optio3.cloud.builder.model.deployment.DeploymentHostImage;
import com.optio3.cloud.builder.model.deployment.DeploymentHostOffline;
import com.optio3.cloud.builder.model.deployment.DeploymentHostProvisioningInfo;
import com.optio3.cloud.builder.model.deployment.DeploymentHostProvisioningNotes;
import com.optio3.cloud.builder.model.deployment.DeploymentHostServiceDetails;
import com.optio3.cloud.builder.model.deployment.DeploymentHostStatusDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentTaskConfiguration;
import com.optio3.cloud.builder.orchestration.tasks.bookkeeping.TaskForAlertThresholds;
import com.optio3.cloud.builder.orchestration.tasks.bookkeeping.TaskForServiceBatteryThresholds;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForGatewayCreation;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForTestCreation;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperations;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostFileRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedAgentBatteryConfiguration;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedAgentCreation;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedBootOptionsPull;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedBootOptionsPush;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedImagePruning;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedTaskTermination;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedWaypointUpdate;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.DeployerShutdownConfiguration;
import com.optio3.cloud.client.deployer.model.ImageStatus;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogEntryFilterRequest;
import com.optio3.cloud.persistence.LogRange;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.infra.docker.ContainerBuilder;
import com.optio3.infra.docker.model.ExtendedConfig;
import com.optio3.infra.provision.imaging.LegacyLabelerHelper;
import com.optio3.infra.waypoint.BootConfig;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;

@Api(tags = { "DeploymentHosts" }) // For Swagger
@Optio3RestEndpoint(name = "DeploymentHosts") // For Optio3 Shell
@Path("/v1/deployment-hosts")
public class DeploymentHosts
{
    @Inject
    private BuilderApplication m_app;

    @Inject
    private BuilderConfiguration m_cfg;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    //--//

    @POST
    @Path("inventory/{fileName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/octet-stream")
    public InputStream reportInventory(@PathParam("fileName") String fileName,
                                       DeploymentHostFilterRequest filters) throws
                                                                            IOException
    {
        return DeploymentHostRecord.generateUnitsReport(m_app, filters);
    }

    //--//

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<DeploymentHost> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<DeploymentHostRecord> helper = sessionHolder.createHelper(DeploymentHostRecord.class);

            List<DeploymentHost> list = ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, DeploymentHostRecord.getBatch(helper, ids));
            for (int i = 0; i < ids.size(); i++)
            {
                if (list.get(i) == null)
                {
                    list.set(i, get(ids.get(i)));
                }
            }
            return list;
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DeploymentHost get(@PathParam("id") String id)
    {
        if (id != null)
        {
            try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
            {
                DeploymentHostRecord rec = sessionHolder.getEntityOrNull(DeploymentHostRecord.class, id);
                if (rec != null)
                {
                    return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
                }

                RecordHelper<DeploymentHostRecord> helper_host = sessionHolder.createHelper(DeploymentHostRecord.class);

                TypedRecordIdentity<DeploymentHostRecord> ri_host = DeploymentHostRecord.findByHostId(helper_host, id);
                if (ri_host != null && ri_host.sysId != null)
                {
                    return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, DeploymentHostRecord.class, ri_host.sysId);
                }
            }
        }

        return null;
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    DeploymentHost model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            RecordHelper<DeploymentHostRecord> helper = validation.sessionHolder.createHelper(DeploymentHostRecord.class);
            DeploymentHostRecord               rec    = helper.get(id);

            if (validation.canProceed())
            {
                ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec);

                rec.cleanupState(validation, helper);
            }

            return validation.getResults();
        }
    }

    @DELETE
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults remove(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun) throws
                                                                          Exception
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            RecordHelper<DeploymentHostRecord> helper = validation.sessionHolder.createHelper(DeploymentHostRecord.class);
            DeploymentHostRecord               rec    = helper.getOrNull(id);
            if (rec != null)
            {
                rec.remove(validation, helper);
            }

            return validation.getResults();
        }
    }

    @GET
    @Path("item/{id}/remote")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentHostServiceDetails getRemote(@PathParam("id") String id,
                                                  @QueryParam("force") Boolean force)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostServiceDetails details = null;

            DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, id);
            if (rec_host.hasRole(DeploymentRole.gateway))
            {
                try
                {
                    DeployLogicForHub logic = DeployLogicForHub.fromRecord(sessionHolder, rec_host.getCustomerService());
                    details = logic.getGatewayDetails(rec_host, BoxingUtils.get(force, false));

                    sessionHolder.commit();
                }
                catch (Throwable t)
                {
                    // If this fails, it's because the site has not been upgraded to have the correct Gateway API.
                }
            }

            return details;
        }
    }

    //--//

    @POST
    @Path("item/{id}/log/filter")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LogRange> filterLog(@PathParam("id") String id,
                                    LogEntryFilterRequest filters) throws
                                                                   Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostRecord rec = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            var logHandler = DeploymentHostRecord.allocateLogHandler(sessionHolder, rec);
            {
                return logHandler.filter(filters);
            }
        }
    }

    @GET
    @Path("item/{id}/log")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LogLine> getLog(@PathParam("id") String id,
                                @QueryParam("fromOffset") Integer fromOffset,
                                @QueryParam("toOffset") Integer toOffset,
                                @QueryParam("limit") Integer limit) throws
                                                                    Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostRecord rec = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            List<LogLine> lines = Lists.newArrayList();

            var logHandler = DeploymentHostRecord.allocateLogHandler(sessionHolder, rec);
            logHandler.extract(fromOffset, toOffset, limit, (item, offset) ->
            {
                LogLine newLine = new LogLine();
                newLine.lineNumber = offset;
                newLine.copyFrom(item);
                lines.add(newLine);
            });

            return lines;
        }
    }

    @DELETE
    @Path("item/{id}/log")
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    @Produces(MediaType.APPLICATION_JSON)
    public int deleteLog(@PathParam("id") String id,
                         @QueryParam("olderThanXMinutes") Integer olderThanXMinutes) throws
                                                                                     Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<DeploymentHostRecord> lock = sessionHolder.getEntityWithLock(DeploymentHostRecord.class, id, 30, TimeUnit.SECONDS);

            int deleteCount;

            try (var logHandler = DeploymentHostRecord.allocateLogHandler(lock))
            {
                ZonedDateTime olderThan = olderThanXMinutes != null ? TimeUtils.now()
                                                                               .minus(olderThanXMinutes, ChronoUnit.MINUTES) : null;

                deleteCount = logHandler.delete(olderThan);
            }

            sessionHolder.commit();

            return deleteCount;
        }
    }

    //--//

    @POST
    @Path("describe-filtered")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<DeploymentHostStatusDescriptor> describeFiltered(DeploymentHostFilterRequest filters)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            return DeploymentHostRecord.describeFiltered(sessionHolder.createHelper(DeploymentHostRecord.class), filters);
        }
    }

    @POST
    @Path("upgrade-agents")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public List<DeploymentAgentUpgradeDescriptor> upgradeAgents(DeploymentAgentUpgrade upgrade) throws
                                                                                                Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            List<DeploymentAgentUpgradeDescriptor> res = DeploymentHostRecord.upgradeAgents(sessionHolder, upgrade);

            sessionHolder.commit();

            return res;
        }
    }

    @GET
    @Path("item/{id}/charges")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentCellularChargesSummary getCharges(@PathParam("id") String id,
                                                       @QueryParam("refresh") Boolean refresh)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            if (BoxingUtils.get(refresh, false))
            {
                rec_host.refreshCharges(m_cfg, TimeUtils.nowUtc(), 0);

                sessionHolder.commit();
            }

            Map<TypedRecordIdentity<DeploymentHostRecord>, DeploymentCellularCharges> map = Maps.newHashMap();
            DeploymentCellularCharges                                                 res = rec_host.getCharges();
            if (res != null)
            {
                map.put(TypedRecordIdentity.newTypedInstance(rec_host), res);
            }

            DeploymentCellularChargesSummary resSummary = new DeploymentCellularChargesSummary();
            resSummary.compute(map, 1);

            return resSummary;
        }
    }

    @GET
    @Path("item/{id}/data-connection-status")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentCellularStatus getDataConnectionStatus(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            return rec_host.getCellularConnectionStatus(m_cfg);
        }
    }

    @GET
    @Path("item/{id}/data-sessions")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<DeploymentCellularSession> getDataSessions(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            DeploymentCellularSessions res = rec_host.getCellularSessions(m_cfg);
            return res != null ? res.sessions : null;
        }
    }

    @GET
    @Path("item/{id}/data-exchanges")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentCellularCommunications getDataExchanges(@PathParam("id") String id,
                                                             @QueryParam("days") Integer days)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            final ConcurrentMap<String, String> ipLookup = Maps.newConcurrentMap();

            Function<String, String> resolver = (ip) ->
            {
                String name = ipLookup.get(ip);
                if (name == null)
                {
                    try
                    {
                        InetAddress addr = InetAddress.getByName(ip);

                        name = addr.getCanonicalHostName();
                    }
                    catch (Throwable e)
                    {
                        name = ip;
                    }

                    ipLookup.put(ip, name);
                }

                return name;
            };

            Consumer<String> resolveWellKnown = (host) ->
            {
                try
                {
                    InetAddress addr = InetAddress.getByName(host);

                    ipLookup.put(addr.getHostAddress(), host);
                }
                catch (Throwable t)
                {
                    // Ignore lookup failures.
                }
            };

            //
            // Add some well-known entries.
            //

            resolveWellKnown.accept("builder.dev.optio3.io");
            ipLookup.put("8.8.8.8", "DNS - 8.8.8.8");
            ipLookup.put("8.8.4.4", "DNS - 8.8.4.4");
            ipLookup.put("1.1.1.1", "DNS - 1.1.1.1");

            CustomerServiceRecord.streamAllRaw(sessionHolder, null, (raw) ->
            {
                try
                {
                    URL url = new URL(raw.url);

                    resolveWellKnown.accept(url.getHost());
                }
                catch (Throwable t)
                {
                    // Ignore lookup failures.
                }
            });

            return rec_host.getCellularCommunications(m_cfg, resolver, BoxingUtils.get(days, 30));
        }
    }

    @GET
    @Path("item/{id}/agents")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<DeploymentAgentRecord> getAgents(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            return TypedRecordIdentityList.toList(rec_host.getAgents());
        }
    }

    @GET
    @Path("item/{id}/tasks")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<DeploymentTaskRecord> getTasks(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            return TypedRecordIdentityList.toList(rec_host.getTasks());
        }
    }

    @GET
    @Path("item/{id}/files")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<DeploymentHostFileRecord> getFiles(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            return TypedRecordIdentityList.toList(rec_host.getFiles());
        }
    }

    //--//

    @GET
    @Path("item/{id}/delayed-ops")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DelayedOperations getDelayedOps(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            DelayedOperations res = rec_host.getDelayedOperations(sessionHolder, false);
            if (res != null)
            {
                for (DelayedOperation op : res.ops)
                {
                    op.description = op.getSummary(sessionHolder);
                }
            }

            return res;
        }
    }

    @POST
    @Path("item/{id}/delayed-ops")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public DelayedOperations removeDelayedOp(@PathParam("id") String id,
                                             DelayedOperation op)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            DelayedOperations res = rec_host.removeDelayedOperation(sessionHolder, op);

            sessionHolder.commit();

            return res;
        }
    }

    @GET
    @Path("item/{id}/terminate")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults terminate(@PathParam("id") String id,
                                       @QueryParam("dryRun") Boolean dryRun) throws
                                                                             Exception
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            DeploymentHostRecord rec = validation.sessionHolder.getEntityOrNull(DeploymentHostRecord.class, id);
            if (rec != null)
            {
                rec.terminate(validation);
            }

            return validation.getResults();
        }
    }

    @GET
    @Path("item/{id}/notify-me")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean notifyMe(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            CookiePrincipal principal = CookiePrincipalAccessor.get(m_principalAccessor);
            String          name      = principal.getName();

            rec_host.modifyMetadata(metadata ->
                                    {
                                        DeploymentHostRecord.UsersNotification notify = DeploymentHostRecord.WellKnownMetadata.notifyWhenOnline.get(metadata);
                                        if (notify == null)
                                        {
                                            notify = new DeploymentHostRecord.UsersNotification();
                                        }

                                        CollectionUtils.addIfMissingAndNotNull(notify.users, name);

                                        DeploymentHostRecord.WellKnownMetadata.notifyWhenOnline.put(metadata, notify);
                                    });

            sessionHolder.commit();

            return true;
        }
    }

    @GET
    @Path("item/{id}/log-rpc")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public boolean logRpc(@PathParam("id") String id,
                          @QueryParam("state") Boolean state)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            boolean previous = rec_host.getMetadata(DeploymentHostRecord.WellKnownMetadata.logRPC);

            if (state != null)
            {
                CookiePrincipal principal = CookiePrincipalAccessor.get(m_principalAccessor);
                if (principal.isInRole(WellKnownRole.Administrator))
                {
                    rec_host.putMetadata(DeploymentHostRecord.WellKnownMetadata.logRPC, state);

                    sessionHolder.commit();
                }
            }

            return previous;
        }
    }

    @GET
    @Path("item/{id}/bind/{serviceId}/{role}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public boolean bindToService(@PathParam("id") String id,
                                 @PathParam("serviceId") String serviceId,
                                 @PathParam("role") DeploymentRole role) throws
                                                                         Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<CustomerServiceRecord> lock_svc  = sessionHolder.getEntityWithLock(CustomerServiceRecord.class, serviceId, 2, TimeUnit.MINUTES);
            RecordLocked<DeploymentHostRecord>  lock_host = sessionHolder.getEntityWithLock(DeploymentHostRecord.class, id, 2, TimeUnit.MINUTES);

            DeploymentHostRecord  rec_host = lock_host.get();
            CustomerServiceRecord rec_svc  = lock_svc.get();

            rec_host.bindRole(rec_svc, role);
            rec_host.renameBasedOnRole(sessionHolder);
            rec_host.setOperationalStatus(DeploymentOperationalStatus.operational);

            //
            // If the service has no image associated with the role, find a running task matching the role and try to get its image's tag.
            //
            List<RoleAndArchitectureWithImage> roleImages = rec_svc.getRoleImages();
            if (RoleAndArchitectureWithImage.locate(roleImages, role, rec_host.getArchitecture()) == null)
            {
                for (DeploymentTaskRecord rec_task : rec_host.findTasksForPurpose(DeploymentStatus.Ready, role, null, true))
                {
                    RegistryTaggedImageRecord rec_taggedImage = rec_task.findTaggedImage(sessionHolder.createHelper(RegistryImageRecord.class), null);
                    if (rec_taggedImage != null)
                    {
                        RoleAndArchitectureWithImage.add(roleImages, role, rec_host.getArchitecture(), TypedRecordIdentity.newTypedInstance(rec_taggedImage));
                        rec_svc.setRoleImages(roleImages);
                        break;
                    }
                }
            }

            TaskForAlertThresholds.scheduleTask(sessionHolder, rec_svc, rec_host);

            if (rec_host.getBatteryThresholds() == null)
            {
                TaskForServiceBatteryThresholds.scheduleTask(sessionHolder, rec_svc, rec_host);
            }

            sessionHolder.commit();

            return true;
        }
    }

    @GET
    @Path("item/{id}/unbind/{role}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public boolean unbindFromService(@PathParam("id") String id,
                                     @PathParam("role") DeploymentRole role) throws
                                                                             Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<DeploymentHostRecord> helper = sessionHolder.createHelper(DeploymentHostRecord.class);

            DeploymentHostRecord rec_host = helper.get(id);

            boolean res = rec_host.unbindRole(role);
            if (res)
            {
                if (rec_host.getInstanceType().hasAgent)
                {
                    for (DeploymentTaskRecord task : rec_host.getTasks())
                    {
                        if (task.getRole() == role)
                        {
                            DelayedTaskTermination.queue(sessionHolder, task, true);
                        }
                    }
                }
                else
                {
                    rec_host.setStatus(DeploymentStatus.Terminated);
                    rec_host.setOperationalStatus(DeploymentOperationalStatus.retired);

                    sessionHolder.commitAndBeginNewTransaction();

                    try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, false, false))
                    {
                        rec_host.remove(validation, helper);
                    }
                }
            }

            sessionHolder.commit();

            return res;
        }
    }

    //--//

    @GET
    @Path("item/{id}/prepare-for-customer/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public boolean prepareForCustomer(@PathParam("id") String id,
                                      @PathParam("customerId") String customerId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostRecord rec_host     = sessionHolder.getEntity(DeploymentHostRecord.class, id);
            CustomerRecord       rec_customer = sessionHolder.getEntity(CustomerRecord.class, customerId);

            rec_host.prepareHostForCustomer(sessionHolder, rec_customer);

            sessionHolder.commit();

            return true;
        }
    }

    @GET
    @Path("item/{id}/prepare-for-service/{serviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public boolean prepareForService(@PathParam("id") String id,
                                     @PathParam("serviceId") String serviceId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostRecord  rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, id);
            CustomerServiceRecord rec_svc  = sessionHolder.getEntity(CustomerServiceRecord.class, serviceId);

            rec_host.prepareHostForService(sessionHolder, rec_svc, false);

            sessionHolder.commit();

            return true;
        }
    }

    @GET
    @Path("item/{id}/prepare-for-offline-deployment")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public List<DeploymentHostOffline> prepareForOfflineDeployment(@PathParam("id") String id) throws
                                                                                               Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            List<DeploymentHostOffline> res = Lists.newArrayList();

            DeploymentHostRecord rec_host = sessionHolder.getEntityOrNull(DeploymentHostRecord.class, id);
            if (rec_host != null && rec_host.getInstanceType().isDeployable && !rec_host.getInstanceType().hasAgent)
            {
                for (DeploymentRole role : rec_host.getRoles())
                {
                    DeploymentHostOffline dep = new DeploymentHostOffline();
                    dep.role = role;

                    UserInfo user = m_cfg.getCredentialForHost(WellKnownSites.dockerRegistry(), true, RoleType.Subscriber);

                    CustomerServiceRecord rec_svc = rec_host.getCustomerService();

                    RoleAndArchitectureWithImage item = RoleAndArchitectureWithImage.locate(rec_svc.getRoleImages(), role, rec_host.getArchitecture());
                    if (item != null)
                    {
                        RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromIdentityOrNull(item.image);
                        if (rec_taggedImage != null)
                        {
                            dep.repoImage    = rec_taggedImage.getTag();
                            dep.repoAddress  = WellKnownSites.dockerRegistryAddress(false);
                            dep.repoUser     = user.user;
                            dep.repoPassword = user.getEffectivePassword();

                            ContainerConfiguration cfg = TaskForGatewayCreation.prepareContainerConfigForOfflineDeployment(m_cfg, rec_svc, rec_host, rec_taggedImage);
                            cfg.allowAccessToDockerDaemon = false;
                            cfg.privileged                = false;

                            ContainerBuilder builder = new ContainerBuilder();
                            builder.loadFrom(cfg);

                            // Needed for Azure.
                            builder.useNetwork("host");

                            ExtendedConfig dockerCfg = builder.build();
                            dockerCfg.stopSignal               = null;
                            dockerCfg.attachStdin              = null;
                            dockerCfg.attachStdout             = null;
                            dockerCfg.attachStderr             = null;
                            dockerCfg.image                    = null; // No need to specify the image, it will be added by Azure Edge.
                            dockerCfg.hostConfig.restartPolicy = null; // Not needed, it will be added by Azure Edge.

                            dep.containerConfig = ObjectMappers.prettyPrintAsJson(ObjectMappers.SkipDefaults, dockerCfg);

                            res.add(dep);
                        }
                    }
                }
            }

            return res;
        }
    }

    //--//

    @GET
    @Path("item/{id}/images")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<DeploymentHostImage> listImages(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntityOrNull(DeploymentHostRecord.class, id);
            if (rec_host != null)
            {
                return rec_host.getImages();
            }

            return null;
        }
    }

    @GET
    @Path("item/{id}/image-prune/{days}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public boolean pruneImages(@PathParam("id") String id,
                               @PathParam("days") int days) throws
                                                            Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.getEntityWithLockOrNull(DeploymentHostRecord.class, id, 2, TimeUnit.MINUTES);
            if (lock_host != null)
            {
                DelayedImagePruning.queue(lock_host, days);

                sessionHolder.commit();
                return true;
            }

            return false;
        }
    }

    @GET
    @Path("item/{id}/refresh-images")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public boolean refreshImages(@PathParam("id") String id) throws
                                                             Exception
    {
        DeployLogicForAgent logic = m_sessionProvider.computeInReadOnlySession(sessionHolder ->
                                                                               {
                                                                                   DeploymentHostRecord rec_host = sessionHolder.getEntityOrNull(DeploymentHostRecord.class, id);
                                                                                   return rec_host != null ? new DeployLogicForAgent(sessionHolder, rec_host) : null;
                                                                               });

        if (logic != null)
        {
            try
            {
                List<ImageStatus> images = getAndUnwrapException(logic.listImages(false));

                m_sessionProvider.callWithSessionWithAutoCommit(sessionHolder ->
                                                                {
                                                                    RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.getEntityWithLockOrNull(DeploymentHostRecord.class,
                                                                                                                                                         id,
                                                                                                                                                         2,
                                                                                                                                                         TimeUnit.MINUTES);
                                                                    if (lock_host != null)
                                                                    {
                                                                        DeploymentHostRecord rec_host = lock_host.get();
                                                                        rec_host.setImages(images);
                                                                    }
                                                                });

                return true;
            }
            catch (Throwable t)
            {
                // Shallow exceptions.
            }
        }

        return false;
    }

    //--//

    @GET
    @Path("item/{id}/alert-thresholds")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public String setAlertThresholds(@PathParam("id") String id,
                                     @QueryParam("role") DeploymentRole role,
                                     @QueryParam("warningThreshold") Integer warningThreshold,
                                     @QueryParam("alertThreshold") Integer alertThreshold)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntityOrNull(DeploymentHostRecord.class, id);
            if (rec_host == null)
            {
                return String.format("Unknown host '%s'", id);
            }

            if (role == null || rec_host.hasRole(role))
            {
                try
                {
                    CustomerServiceRecord rec_svc = rec_host.getCustomerService();

                    DeployLogicForHub logic = DeployLogicForHub.fromRecord(sessionHolder, rec_svc);
                    logic.login(false);

                    String failure = logic.changeThresholds(rec_host, warningThreshold, alertThreshold);
                    if (failure != null)
                    {
                        return failure;
                    }
                }
                catch (Throwable t)
                {
                    // If this fails, it's because the site has not been upgraded to have the correct Gateway API.
                    return t.getMessage();
                }
            }

            sessionHolder.commit();

            return null;
        }
    }

    @POST
    @Path("item/{id}/battery-thresholds")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public String setBatteryThresholds(@PathParam("id") String id,
                                       DeployerShutdownConfiguration cfg) throws
                                                                          Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.getEntityWithLockOrNull(DeploymentHostRecord.class, id, 2, TimeUnit.MINUTES);
            if (lock_host == null)
            {
                return String.format("Unknown host '%s'", id);
            }

            DeploymentHostRecord rec_host = lock_host.get();
            rec_host.setBatteryThresholds(sessionHolder, cfg);
            DelayedAgentBatteryConfiguration.queue(lock_host);

            sessionHolder.commit();

            return null;
        }
    }

    //--//

    @GET
    @Path("item/{id}/boot-options")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public BootOptions getBootOptions(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntityOrNull(DeploymentHostRecord.class, id);
            if (rec_host != null)
            {
                return rec_host.getBootOptions();
            }
        }

        return null;
    }

    @POST
    @Path("item/{id}/boot-options")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public BootOptions setBootOption(@PathParam("id") String id,
                                     BootConfig.OptionAndValue ov) throws
                                                                   Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.getEntityWithLockOrNull(DeploymentHostRecord.class, id, 2, TimeUnit.MINUTES);
            if (lock_host != null)
            {
                DelayedBootOptionsPush.queue(lock_host, ov);

                sessionHolder.commit();

                DeploymentHostRecord rec_host = lock_host.get();
                return rec_host.getBootOptions();
            }
        }

        return null;
    }

    @GET
    @Path("item/{id}/boot-options/fetch")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public BootOptions fetchBootOptions(@PathParam("id") String id) throws
                                                                    Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.getEntityWithLockOrNull(DeploymentHostRecord.class, id, 2, TimeUnit.MINUTES);
            if (lock_host != null)
            {
                DelayedBootOptionsPull.queue(lock_host);

                sessionHolder.commit();

                DeploymentHostRecord rec_host = lock_host.get();
                return rec_host.getBootOptions();
            }
        }

        return null;
    }

    //--//

    @GET
    @Path("item/{id}/provisioning")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentHostProvisioningInfo getProvisioningInfo(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<DeploymentHostRecord> helper = sessionHolder.createHelper(DeploymentHostRecord.class);

            DeploymentHostRecord rec_host = helper.getOrNull(id);
            if (rec_host != null)
            {
                DeploymentHostProvisioningInfo info = rec_host.getProvisioningInfo(false);
                if (info != null && info.manufacturingInfo != null)
                {
                    TypedRecordIdentity<DeploymentHostRecord> id_host2 = DeploymentHostRecord.findByHostId(helper, info.manufacturingInfo.manufacturingLocation);
                    if (id_host2 != null)
                    {
                        info.manufacturingInfo.manufacturingLocation = id_host2.sysId;
                    }
                }

                return info;
            }
        }

        return null;
    }

    @DELETE
    @Path("item/{id}/provisioning/{sysId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public DeploymentHostProvisioningInfo removeProvisioningInfo(@PathParam("id") String id,
                                                                 @PathParam("sysId") String sysId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntityOrNull(DeploymentHostRecord.class, id);
            if (rec_host != null)
            {
                DeploymentHostProvisioningInfo info = rec_host.getProvisioningInfo(false);
                if (info != null)
                {
                    info.notes.removeIf((note) -> StringUtils.equals(note.sysId, sysId));

                    rec_host.setProvisioningInfo(info);
                }

                sessionHolder.commit();

                return rec_host.getProvisioningInfo(false);
            }
        }

        return null;
    }

    @POST
    @Path("item/{id}/add-notes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public boolean addNotes(@PathParam("id") String id,
                            DeploymentHostProvisioningNotes note)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntityOrNull(DeploymentHostRecord.class, id);
            if (rec_host != null)
            {
                DeploymentHostProvisioningInfo info = rec_host.getProvisioningInfo(true);
                info.addNote(note);
                rec_host.setProvisioningInfo(info);

                sessionHolder.commit();

                return true;
            }
        }

        return false;
    }

    @GET
    @Path("item/{id}/label")
    @Produces("image/jpeg")
    @Optio3RequestLogLevel(Severity.Debug)
    public byte[] getProvisioningLabel(@PathParam("id") String id) throws
                                                                   Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<DeploymentHostRecord> helper = sessionHolder.createHelper(DeploymentHostRecord.class);

            DeploymentHostRecord rec_host = helper.getOrNull(id);
            if (rec_host != null)
            {
                String hostId = rec_host.getHostId();
                hostId = StringUtils.removeStart(hostId, "EdgeV1-");

                DeploymentHostDetails details    = rec_host.getDetails();
                String                modemIMSI  = details != null && details.cellular != null ? details.cellular.modemIMSI : "N/A";
                String                modemIMEI  = details != null && details.cellular != null ? details.cellular.modemIMEI : "N/A";
                String                modemICCID = details != null && details.cellular != null ? details.cellular.getModemICCID() : "N/A";

                LegacyLabelerHelper labelerHelper = new LegacyLabelerHelper(hostId, modemIMSI, modemIMEI, modemICCID);
                return labelerHelper.getImage();
            }
        }

        return null;
    }

    //--//

    @GET
    @Path("item/{id}/start-agent/{imageId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public boolean startAgent(@PathParam("id") String id,
                              @PathParam("imageId") String imageId) throws
                                                                    Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<DeploymentHostRecord> lock_host       = sessionHolder.getEntityWithLock(DeploymentHostRecord.class, id, 2, TimeUnit.MINUTES);
            RegistryTaggedImageRecord          rec_taggedImage = sessionHolder.getEntity(RegistryTaggedImageRecord.class, imageId);

            boolean started = DelayedAgentCreation.queue(lock_host, rec_taggedImage, false);

            sessionHolder.commit();

            return started;
        }
    }

    //--//

    @POST
    @Path("item/{id}/new-task/{imageId}")
    @Produces(MediaType.APPLICATION_JSON)
    public DeploymentHost startTask(@PathParam("id") String id,
                                    @PathParam("imageId") String imageId,
                                    DeploymentTaskConfiguration cfg) throws
                                                                     Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<DeploymentHostRecord> lock_host       = sessionHolder.getEntityWithLock(DeploymentHostRecord.class, id, 2, TimeUnit.MINUTES);
            RegistryTaggedImageRecord          rec_taggedImage = sessionHolder.getEntity(RegistryTaggedImageRecord.class, imageId);

            DeploymentHostRecord rec_host = lock_host.get();
            DeploymentRole       role     = rec_host.findRoleCompatibleWithImage(rec_taggedImage);
            if (role != null)
            {
                throw Exceptions.newGenericException(InvalidArgumentException.class, "Can't start task with image '%s' because it's associated with role '%s'", imageId, role);
            }

            if (cfg == null)
            {
                throw Exceptions.newGenericException(InvalidArgumentException.class, "No configuration for test");
            }

            TaskForTestCreation.scheduleTask(lock_host, rec_taggedImage, cfg);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_host);
        }
    }

    @GET
    @Path("item/{id}/update-waypoint/{imageId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public boolean updateWaypoint(@PathParam("id") String id,
                                  @PathParam("imageId") String imageId) throws
                                                                        Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<DeploymentHostRecord> lock_host       = sessionHolder.getEntityWithLockOrNull(DeploymentHostRecord.class, id, 2, TimeUnit.MINUTES);
            RegistryTaggedImageRecord          rec_taggedImage = sessionHolder.getEntity(RegistryTaggedImageRecord.class, imageId);

            boolean started = DelayedWaypointUpdate.queue(lock_host, rec_taggedImage);

            sessionHolder.commit();

            return started;
        }
    }
}
