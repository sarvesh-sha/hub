/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForHub;
import com.optio3.cloud.builder.model.admin.HubHeapAndThreads;
import com.optio3.cloud.builder.model.common.LogLine;
import com.optio3.cloud.builder.model.customer.CheckUsagesProgress;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.customer.CustomerServiceDesiredState;
import com.optio3.cloud.builder.model.customer.RoleAndArchitectureWithImage;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularChargePerHost;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularCharges;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularChargesSummary;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentHostConfig;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.model.scheduler.BackgroundActivity;
import com.optio3.cloud.builder.orchestration.tasks.deploy.BaseHostDeployTask;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentInstance;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForDesiredState;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForHostCreation;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForHostMigration;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForHubCheckUsages;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForServiceTermination;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.builder.persistence.customer.BackupKind;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord_;
import com.optio3.cloud.builder.persistence.customer.EmbeddedDatabaseConfiguration;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.client.deployer.model.DeployerShutdownConfiguration;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogEntryFilterRequest;
import com.optio3.cloud.persistence.LogRange;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Encryption;
import com.optio3.util.TimeUtils;
import io.dropwizard.jersey.PATCH;
import io.dropwizard.jersey.jsr310.ZonedDateTimeParam;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;

@Api(tags = { "CustomerServices" }) // For Swagger
@Optio3RestEndpoint(name = "CustomerServices") // For Optio3 Shell
@Path("/v1/customer-services")
public class CustomerServices
{
    @Inject
    private BuilderConfiguration m_cfg;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    //--//

    @GET
    @Path("all/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<CustomerServiceRecord> getAll(@PathParam("customerId") String customerId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            CustomerRecord rec_customer = sessionHolder.getEntity(CustomerRecord.class, customerId);

            return TypedRecordIdentityList.toList(rec_customer.getServices());
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<CustomerService> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<CustomerServiceRecord> helper = sessionHolder.createHelper(CustomerServiceRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, CustomerServiceRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("create/{customerId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public CustomerService create(@PathParam("customerId") String customerId,
                                  CustomerService model) throws
                                                         Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<CustomerRecord> helper = sessionHolder.createHelper(CustomerRecord.class);

            CustomerRecord rec_customer = helper.get(customerId);

            for (CustomerServiceRecord rec_svc : rec_customer.getServices())
            {
                if (StringUtils.isBlank(model.name))
                {
                    throw new InvalidArgumentException("Service name required");
                }

                if (StringUtils.equals(rec_svc.getName(), model.name))
                {
                    throw new InvalidArgumentException("Similar customer service already exists");
                }
            }

            if (StringUtils.isBlank(model.url))
            {
                throw new InvalidArgumentException("Service URL required");
            }

            CustomerServiceRecord rec_svc = CustomerServiceRecord.findSimilarURL(helper.wrapFor(CustomerServiceRecord.class), model.url);
            if (rec_svc != null)
            {
                throw new InvalidArgumentException("Host name already in use");
            }

            if (model.operationalStatus == null)
            {
                model.operationalStatus = DeploymentOperationalStatus.idle;
            }

            CustomerServiceRecord rec_service = CustomerServiceRecord.newInstance(rec_customer);
            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, model, rec_service);
            rec_service.setInstanceRegion(model.instanceRegion);

            rec_service.setPurposes(Sets.newHashSet(DeploymentRole.hub, DeploymentRole.gateway));

            EmbeddedDatabaseConfiguration dbConfig = new EmbeddedDatabaseConfiguration();
            dbConfig.setMode(model.dbMode);
            switch (model.dbMode)
            {
                case H2InMemory:
                    dbConfig.setDatabaseName("hub_db");
                    dbConfig.setDatabaseUser("sa");
                    dbConfig.setDatabasePassword(m_cfg.encrypt("sa"));
                    break;

                case H2OnDisk:
                    dbConfig.setDatabaseName("hub_db");
                    dbConfig.setDatabaseUser("sa");
                    dbConfig.setDatabasePassword(m_cfg.encrypt("sa"));
                    break;

                case MariaDB:
                    dbConfig.setDatabaseName("hub_db");
                    dbConfig.setDatabaseUser("root");
                    dbConfig.setDatabasePassword(m_cfg.encrypt(Encryption.generateRandomKeyAsBase64()));
                    dbConfig.setServer("database_host:3306");

                    Set<DeploymentRole> purposes = rec_service.getPurposes();
                    purposes.add(DeploymentRole.database);
                    rec_service.setPurposes(purposes);
                    break;
            }

            rec_service.setDbConfiguration(dbConfig);

            rec_service.setMasterKey(m_cfg.encrypt(Encryption.generateRandomKeyAsBase64()));

            sessionHolder.persistEntity(rec_service);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_service);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public CustomerService get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, CustomerServiceRecord.class, id);
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    CustomerService model) throws
                                                           Exception
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            RecordHelper<CustomerServiceRecord> helper = validation.sessionHolder.createHelper(CustomerServiceRecord.class);

            RecordLocked<CustomerServiceRecord> lock_svc = helper.getWithLock(id, 2, TimeUnit.MINUTES);
            CustomerServiceRecord               rec_svc  = lock_svc.get();

            model.url = StringUtils.lowerCase(model.url);

            if (!StringUtils.equals(rec_svc.getUrl(), model.url))
            {
                DeploymentGlobalDescriptor globalDescriptor = fetchDeploymentGlobalDescriptor(validation.sessionHolder);

                if (rec_svc.hasRunningCloudHosts(globalDescriptor))
                {
                    validation.addFailure("url", "Can't change URL to a service with running Cloud instances");
                }

                CustomerServiceRecord rec_svc2 = CustomerServiceRecord.findSimilarURL(helper.wrapFor(CustomerServiceRecord.class), model.url);
                if (rec_svc2 != null)
                {
                    validation.addFailure("url", "Host name already in use by '%s'", rec_svc.getDisplayName());
                }
            }

            if (!StringUtils.equals(rec_svc.getName(), model.name))
            {
                if (rec_svc.hasBackups())
                {
                    validation.addFailure("name", "Can't change the name of a service with backups");
                }

                long count = QueryHelperWithCommonFields.count(helper, (qh) ->
                {
                    qh.addWhereClauseWithEqual(qh.root, CustomerServiceRecord_.name, model.name);
                });

                if (count != 0)
                {
                    validation.addFailure("name", "Customer service named '%s' already exists!", model.name);
                }
            }

            if (validation.canProceed())
            {
                if (rec_svc.getDisableServiceWorker() != model.disableServiceWorker)
                {
                    DeploymentTaskRecord rec_task = rec_svc.findAnyTaskForRole(validation.sessionHolder, DeploymentStatus.Ready, DeploymentRole.hub);
                    if (rec_task != null)
                    {
                        try
                        {

                            DeployLogicForHub logic = DeployLogicForHub.fromRecord(validation.sessionHolder, rec_svc);
                            logic.configureServiceWorker(model.disableServiceWorker);
                        }
                        catch (Throwable t)
                        {
                            // Ignore failures.
                            BuilderApplication.LoggerInstance.warn("Failed to configure ServiceWorker on '%s': %s", rec_svc.getDisplayName(), t);
                        }
                    }
                }

                ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec_svc);
                rec_svc.setInstanceRegion(model.instanceRegion);

                if (rec_svc.getOperationalStatus() == DeploymentOperationalStatus.retired)
                {
                    TaskForServiceTermination.scheduleTask(lock_svc, Duration.of(10, ChronoUnit.HOURS));
                }
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

            RecordHelper<CustomerServiceRecord> helper = validation.sessionHolder.createHelper(CustomerServiceRecord.class);
            CustomerServiceRecord               rec    = helper.getOrNull(id);
            if (rec != null)
            {
                rec.remove(validation, helper);
            }

            return validation.getResults();
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
            CustomerServiceRecord rec = sessionHolder.getEntity(CustomerServiceRecord.class, id);

            var logHandler = CustomerServiceRecord.allocateLogHandler(sessionHolder, rec);
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
            CustomerServiceRecord rec = sessionHolder.getEntity(CustomerServiceRecord.class, id);

            List<LogLine> lines = Lists.newArrayList();

            var logHandler = CustomerServiceRecord.allocateLogHandler(sessionHolder, rec);
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
            RecordLocked<CustomerServiceRecord> lock = sessionHolder.getEntityWithLock(CustomerServiceRecord.class, id, 30, TimeUnit.SECONDS);

            int deleteCount;

            try (var logHandler = CustomerServiceRecord.allocateLogHandler(lock))
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

    @GET
    @Path("item/{id}/charges")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentCellularChargesSummary getCharges(@PathParam("id") String id,
                                                       @QueryParam("maxTopHosts") Integer maxTopHosts)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            CustomerServiceRecord rec_svc = sessionHolder.getEntity(CustomerServiceRecord.class, id);

            DeploymentGlobalDescriptor globalDescriptor = fetchDeploymentGlobalDescriptor(sessionHolder);

            Map<TypedRecordIdentity<DeploymentHostRecord>, DeploymentCellularCharges> map = Maps.newHashMap();
            rec_svc.collectCharges(globalDescriptor, map);

            DeploymentCellularChargesSummary res = new DeploymentCellularChargesSummary();
            res.compute(map, BoxingUtils.get(maxTopHosts, 100));

            return res;
        }
    }

    @GET
    @Path("item/{id}/charges-report/{fileName}")
    @Produces("application/csv")
    @Optio3RequestLogLevel(Severity.Debug)
    public String getChargesReport(@PathParam("id") String id,
                                   @PathParam("fileName") String fileName) throws
                                                                           IOException
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            CustomerServiceRecord rec_svc = sessionHolder.getEntity(CustomerServiceRecord.class, id);

            DeploymentGlobalDescriptor globalDescriptor = fetchDeploymentGlobalDescriptor(sessionHolder);

            Map<TypedRecordIdentity<DeploymentHostRecord>, DeploymentCellularCharges> map = Maps.newHashMap();
            rec_svc.collectCharges(globalDescriptor, map);

            return DeploymentCellularChargePerHost.report(sessionHolder, map);
        }
    }

    @POST
    @Path("item/{id}/deploy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public DeploymentHost deploy(@PathParam("id") String id,
                                 DeploymentHostConfig config) throws
                                                              Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            //
            // Lock record to avoid races in assigning a unique ID to the deployment.
            //
            RecordLocked<CustomerServiceRecord> lock_svc = sessionHolder.getEntityWithLock(CustomerServiceRecord.class, id, 2, TimeUnit.MINUTES);

            BaseHostDeployTask.ActivityWithHost res = TaskForHostCreation.scheduleTask(lock_svc, config, Duration.of(20, ChronoUnit.MINUTES));

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, res.lock_host.get());
        }
    }

    @PATCH
    @Path("item/{id}/refresh-certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public BackgroundActivity refreshCertificate(@PathParam("id") String id) throws
                                                                             Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<CustomerServiceRecord> lock_svc = sessionHolder.getEntityWithLock(CustomerServiceRecord.class, id, 2, TimeUnit.MINUTES);

            CustomerServiceRecord rec_svc = lock_svc.get();
            rec_svc.lockForStateChange(sessionHolder, true);

            BackgroundActivityRecord rec_activity = rec_svc.refreshCertificate(lock_svc);
            if (rec_activity == null)
            {
                return null;
            }

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_activity);
        }
    }

    @PATCH
    @Path("item/{id}/migrate")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public BackgroundActivity migrate(@PathParam("id") String id) throws
                                                                  Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<CustomerServiceRecord> lock_svc = sessionHolder.getEntityWithLock(CustomerServiceRecord.class, id, 2, TimeUnit.MINUTES);

            CustomerServiceRecord rec_svc = lock_svc.get();
            rec_svc.lockForStateChange(sessionHolder, false);

            // Pick a really long timeout, to account for DNS propagation.
            BackgroundActivityRecord rec_task = TaskForHostMigration.scheduleTask(lock_svc, Duration.of(10, ChronoUnit.HOURS));

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_task);
        }
    }

    @GET
    @Path("item/{id}/upgrade-blocker")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean manageUpgradeBlocker(@PathParam("id") String id,
                                        @QueryParam("until") ZonedDateTimeParam untilParam) throws
                                                                                            Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            CustomerServiceRecord rec_svc = sessionHolder.getEntity(CustomerServiceRecord.class, id);

            UserRecord rec_user = m_cfg.userLogic.getUserWithAuthentication(sessionHolder, null, m_principalAccessor, null);

            rec_svc.updateUpgradeBlockers(rec_user, untilParam != null ? untilParam.get() : null);

            sessionHolder.commit();

            return true;
        }
    }

    @POST
    @Path("item/{id}/desired-state")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BackgroundActivity desiredState(@PathParam("id") String id,
                                           CustomerServiceDesiredState spec) throws
                                                                             Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<CustomerServiceRecord> lock_svc = sessionHolder.getEntityWithLock(CustomerServiceRecord.class, id, 2, TimeUnit.MINUTES);

            CustomerServiceRecord rec_svc = lock_svc.get();
            rec_svc.lockForStateChange(sessionHolder, false);

            // Pick a really long timeout, in case we are connected over Cellular networks.
            BackgroundActivityRecord rec_task = TaskForDesiredState.scheduleTask(lock_svc, spec, Duration.of(10, ChronoUnit.HOURS));

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_task);
        }
    }

    @POST
    @Path("item/{id}/add-image")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public boolean addImage(@PathParam("id") String id,
                            RoleAndArchitectureWithImage spec) throws
                                                               Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<CustomerServiceRecord> lock_svc = sessionHolder.getEntityWithLock(CustomerServiceRecord.class, id, 2, TimeUnit.MINUTES);

            CustomerServiceRecord rec_svc = lock_svc.get();
            rec_svc.lockForStateChange(sessionHolder, false);

            List<RoleAndArchitectureWithImage> roleImages = rec_svc.getRoleImages();
            if (RoleAndArchitectureWithImage.locate(roleImages, spec.role, spec.architecture) == null)
            {
                RoleAndArchitectureWithImage.add(roleImages, spec.role, spec.architecture, spec.image);
                rec_svc.setRoleImages(roleImages);

                sessionHolder.commit();

                return true;
            }

            return false;
        }
    }

    @GET
    @Path("item/{id}/backup")
    @Produces(MediaType.APPLICATION_JSON)
    public BackgroundActivity backup(@PathParam("id") String id) throws
                                                                 Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<CustomerServiceRecord> lock_svc = sessionHolder.getEntityWithLock(CustomerServiceRecord.class, id, 2, TimeUnit.MINUTES);

            CustomerServiceRecord rec_svc = lock_svc.get();
            rec_svc.lockForStateChange(sessionHolder, true);

            BackgroundActivityRecord rec_activity = rec_svc.startBackup(lock_svc, BackupKind.OnDemand, false);
            if (rec_activity == null)
            {
                return null;
            }

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_activity);
        }
    }

    @GET
    @Path("item/{id}/refresh-accounts")
    @Produces(MediaType.APPLICATION_JSON)
    public BackgroundActivity refreshAccounts(@PathParam("id") String id) throws
                                                                          Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<CustomerServiceRecord> lock_svc = sessionHolder.getEntityWithLock(CustomerServiceRecord.class, id, 2, TimeUnit.MINUTES);

            CustomerServiceRecord rec_svc = lock_svc.get();
            rec_svc.lockForStateChange(sessionHolder, true);

            BackgroundActivityRecord rec_task = rec_svc.refreshAccounts(sessionHolder);
            if (rec_task == null)
            {
                return null;
            }

            rec_svc.setCurrentActivity(rec_task);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_task);
        }
    }

    @GET
    @Path("item/{id}/refresh-secrets")
    @Produces(MediaType.APPLICATION_JSON)
    public BackgroundActivity refreshSecrets(@PathParam("id") String id) throws
                                                                         Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<CustomerServiceRecord> lock_svc = sessionHolder.getEntityWithLock(CustomerServiceRecord.class, id, 2, TimeUnit.MINUTES);

            CustomerServiceRecord rec_svc = lock_svc.get();
            rec_svc.lockForStateChange(sessionHolder, true);

            BackgroundActivityRecord rec_task = rec_svc.refreshSecrets(sessionHolder);
            if (rec_task == null)
            {
                return null;
            }

            rec_svc.setCurrentActivity(rec_task);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_task);
        }
    }

    @GET
    @Path("item/{id}/compact-time-series")
    @Produces(MediaType.APPLICATION_JSON)
    public BackgroundActivity compactTimeSeries(@PathParam("id") String id) throws
                                                                            Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<CustomerServiceRecord> lock_svc = sessionHolder.getEntityWithLock(CustomerServiceRecord.class, id, 2, TimeUnit.MINUTES);

            CustomerServiceRecord rec_svc = lock_svc.get();
            rec_svc.lockForStateChange(sessionHolder, true);

            BackgroundActivityRecord rec_task = rec_svc.compactTimeSeries(sessionHolder);
            if (rec_task == null)
            {
                return null;
            }

            rec_svc.setCurrentActivity(rec_task);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_task);
        }
    }

    @GET
    @Path("item/{id}/alert-thresholds")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public boolean setAlertThresholds(@PathParam("id") String id,
                                      @QueryParam("role") DeploymentRole role,
                                      @QueryParam("warningThreshold") Integer warningThreshold,
                                      @QueryParam("alertThreshold") Integer alertThreshold) throws
                                                                                            Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<CustomerServiceRecord> helper  = sessionHolder.createHelper(CustomerServiceRecord.class);
            CustomerServiceRecord               rec_svc = helper.get(id);

            rec_svc.setAlertThresholds(sessionHolder, role, warningThreshold, alertThreshold);

            sessionHolder.commit();

            return true;
        }
    }

    @POST
    @Path("item/{id}/battery-thresholds")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public boolean setBatteryThresholds(@PathParam("id") String id,
                                        DeployerShutdownConfiguration cfg) throws
                                                                           Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<CustomerServiceRecord> helper  = sessionHolder.createHelper(CustomerServiceRecord.class);
            CustomerServiceRecord               rec_svc = helper.get(id);

            rec_svc.setBatteryThresholds(sessionHolder, cfg);

            sessionHolder.commit();

            return true;
        }
    }

    @GET
    @Path("item/{id}/heap-status-history")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<HubHeapAndThreads> getHeapStatusHistory(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            CustomerServiceRecord rec_svc = sessionHolder.getEntity(CustomerServiceRecord.class, id);

            return rec_svc.getHeapStatusHistory();
        }
    }

    //--//

    @POST
    @Path("item/{id}/check-usages")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Optio3RequestLogLevel(Severity.Debug)
    public String checkUsages(@PathParam("id") String id,
                              com.optio3.cloud.client.hub.model.UsageFilterRequest filters) throws
                                                                                            Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<CustomerServiceRecord> lock_svc = sessionHolder.getEntityWithLock(CustomerServiceRecord.class, id, 2, TimeUnit.MINUTES);

            CustomerServiceRecord rec_svc = lock_svc.get();
            rec_svc.lockForStateChange(sessionHolder, true);

            BackgroundActivityRecord rec_task = rec_svc.checkUsages(sessionHolder, filters);
            if (rec_task == null)
            {
                return null;
            }

            rec_svc.setCurrentActivity(rec_task);

            sessionHolder.commit();

            return rec_task.getSysId();
        }
    }

    @GET
    @Path("check-usages/progress/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public CheckUsagesProgress checkUsagesProgress(@PathParam("id") String id,
                                                   @QueryParam("detailed") Boolean detailed)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.getProgress(helper, id, BoxingUtils.get(detailed), TaskForHubCheckUsages.class);
        }
    }

    //--//

    @GET
    @Path("regions/{instanceType}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<String> getAvailableRegions(@PathParam("instanceType") DeploymentInstance instanceType)
    {
        List<String> regions = CollectionUtils.transformToList(instanceType.getAvailableRegions(), Object::toString);
        regions.sort(String::compareToIgnoreCase);
        return regions;
    }

    @GET
    @Path("accounts/{instanceType}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<String> getAvailableAccounts(@PathParam("instanceType") DeploymentInstance instanceType)
    {
        return instanceType.getAvailableAccounts(m_cfg);
    }

    //--//

    private DeploymentGlobalDescriptor fetchDeploymentGlobalDescriptor(SessionHolder sessionHolder)
    {
        DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
        settings.loadDeployments = true;
        settings.loadServices    = true;
        return DeploymentGlobalDescriptor.get(sessionHolder, settings);
    }
}
