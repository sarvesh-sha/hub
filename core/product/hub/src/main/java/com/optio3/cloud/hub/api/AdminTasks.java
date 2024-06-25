/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import static com.optio3.util.Exceptions.getAndUnwrapException;

import java.io.File;
import java.lang.management.ThreadInfo;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3NoAuthenticationNeeded;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.client.builder.model.CrashReport;
import com.optio3.cloud.client.gateway.proxy.GatewayControlApi;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.logic.spooler.ResultStagingSpooler;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.config.UsageFilterRequest;
import com.optio3.cloud.hub.model.config.UsageFilterResponse;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.orchestration.tasks.TaskForBackup;
import com.optio3.cloud.hub.orchestration.tasks.TaskForNotification;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.FixupProcessingRecord;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementSampleRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceRecord;
import com.optio3.cloud.hub.persistence.config.UserPreferenceRecord;
import com.optio3.cloud.hub.persistence.dashboard.DashboardDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.normalization.NormalizationRecord;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowRecord;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.MessageBusDatagramSession;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.cloud.search.HibernateSearch;
import com.optio3.concurrency.Executors;
import com.optio3.concurrency.LongRunningThreadPool;
import com.optio3.concurrency.SyncWaitMultiple;
import com.optio3.logging.LoggerConfiguration;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.Severity;
import com.optio3.util.BoxingUtils;
import com.optio3.util.StackTraceAnalyzer;
import com.optio3.util.TimeUtils;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;

@Api(tags = { "AdminTasks" }) // For Swagger
@Optio3RestEndpoint(name = "AdminTasks") // For Optio3 Shell
@Path("/v1/admin-tasks")
@Optio3RequestLogLevel(Severity.Debug)
public class AdminTasks
{
    @Inject
    private HubApplication m_app;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    // Temporary filter to remove duplicates...
    private static final Set<String> s_seen = Sets.newHashSet();

    //--//

    @GET
    @Path("app-version")
    @Produces(MediaType.TEXT_PLAIN)
    @Optio3NoAuthenticationNeeded
    @Optio3RequestLogLevel(Severity.Debug)
    public String getAppVersion()
    {
        return m_app.getAppVersion();
    }

    @GET
    @Path("instance-config")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3NoAuthenticationNeeded
    @Optio3RequestLogLevel(Severity.Debug)
    public InstanceConfiguration getInstanceConfiguration()
    {
        return m_app.getService(InstanceConfiguration.class);
    }

    @POST
    @Path("report-crash")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @Optio3RequestLogLevel(Severity.Debug)
    public String reportCrash(@FormParam("page") String page,
                              @FormParam("stack") String stack) throws
                                                                Exception
    {
        synchronized (s_seen)
        {
            if (stack != null)
            {
                // Only look at top N lines for duplicate detection.
                final int     maxLinesForContext = 10;
                StringBuilder sb                 = new StringBuilder();
                String[]      lines              = StringUtils.split(stack, '\n');
                int           maxLines           = Math.min(lines.length, maxLinesForContext);
                for (int i = 0; i < maxLines; i++)
                {
                    sb.append(lines[i]);
                }

                if (s_seen.add(sb.toString()))
                {
                    CookiePrincipal principal = CookiePrincipalAccessor.get(m_principalAccessor);

                    CrashReport crash = new CrashReport();
                    crash.timestamp = TimeUtils.now();
                    crash.user      = principal.isAuthenticated() ? principal.getName() : "<anonymous>";
                    crash.page      = page;
                    crash.stack     = stack;

                    HubApplication.LoggerInstance.warn("Crash by %s at %s\n%s", crash.user, page, stack);

                    try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
                    {
                        TaskForNotification.scheduleTask(sessionHolder, null, null, null, crash);

                        sessionHolder.commit();
                    }
                }
            }

            return "<Done>";
        }
    }

    //--//

    @GET
    @Path("threads")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public String dumpThreads()
    {
        Map<StackTraceAnalyzer, List<ThreadInfo>> uniqueStackTraces = StackTraceAnalyzer.allThreadInfos();
        List<String>                              lines             = StackTraceAnalyzer.printThreadInfos(true, uniqueStackTraces);

        return String.join("\n", lines);
    }

    @GET
    @Path("request-stats")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public String dumpRequestStatistics()
    {
        List<String> lines = m_app.dumpRequestStatistics();

        return String.join("\n", lines);
    }

    @GET
    @Path("message-bus-stats")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public String dumpMessageBusStatistics()
    {
        List<String> lines = m_app.dumpMessageBusStatistics();

        return String.join("\n", lines);
    }

    @GET
    @Path("rpc-stats")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public String dumpRpcStatistics()
    {
        List<String> lines = m_app.getRpcStatistics();

        return String.join("\n", lines);
    }

    @GET
    @Path("datagram-sessions")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public List<MessageBusDatagramSession> getDatagramSessions()
    {
        MessageBusBroker broker = m_app.getServiceNonNull(MessageBusBroker.class);

        return broker.reportSessions();
    }

    //--//

    @GET
    @Path("db-connections")
    @Produces(MediaType.TEXT_PLAIN)
    public String dumpDbConnections()
    {
        List<String> lines = Lists.newArrayList();

        Optio3DataSourceFactory ds = m_app.getDataSourceFactory(null);
        ds.dumpOpenConnections(lines::add);

        return String.join("\n", lines);
    }

    //--//

    @GET
    @Path("upgrade/list")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public List<String> checkUpgradeLevel()
    {
        return FixupProcessingRecord.listExecutedHandlers(m_app);
    }

    //--//

    @GET
    @Path("loggers/list")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public List<LoggerConfiguration> getLoggers()
    {
        return LoggerFactory.getLoggersConfiguration();
    }

    @POST
    @Path("loggers/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public LoggerConfiguration configLogger(LoggerConfiguration cfg)
    {
        return LoggerFactory.setLoggerConfiguration(cfg);
    }

    @GET
    @Path("loggers/list-gateway/{gatewaySysId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public List<LoggerConfiguration> getLoggersForGateway(@PathParam("gatewaySysId") String gatewaySysId)
    {
        try
        {
            try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
            {
                GatewayAssetRecord rec = sessionHolder.getEntityOrNull(GatewayAssetRecord.class, gatewaySysId);
                if (rec != null)
                {
                    var ci = rec.extractConnectionInfo();

                    GatewayControlApi proxy = getAndUnwrapException(ci.getProxy(m_app, GatewayControlApi.class, 100));
                    if (proxy != null)
                    {
                        return getAndUnwrapException(proxy.getLoggers(), 20, TimeUnit.SECONDS);
                    }
                }
            }
        }
        catch (Throwable t)
        {
            // Assume Gateway not online.
        }

        return null;
    }

    @POST
    @Path("loggers/config-gateway/{gatewaySysId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public LoggerConfiguration configLoggerForGateway(@PathParam("gatewaySysId") String gatewaySysId,
                                                      LoggerConfiguration cfg)
    {
        try
        {
            try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
            {
                GatewayAssetRecord rec = sessionHolder.getEntityOrNull(GatewayAssetRecord.class, gatewaySysId);
                if (rec != null)
                {
                    var ci = rec.extractConnectionInfo();

                    GatewayControlApi proxy = getAndUnwrapException(ci.getProxy(m_app, GatewayControlApi.class, 100));
                    if (proxy != null)
                    {
                        return getAndUnwrapException(proxy.configLogger(cfg), 20, TimeUnit.SECONDS);
                    }
                }
            }
        }
        catch (Throwable t)
        {
            // Assume Gateway not online.
        }

        return null;
    }

    //--//

    @GET
    @Path("db/list-variables")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public List<String> listDatabaseVariables()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            List<String> names = Lists.newArrayList(sessionHolder.listVariables());
            names.sort(String::compareTo);
            return names;
        }
    }

    @GET
    @Path("db/get-variable")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public String getDatabaseVariable(@QueryParam("name") String name)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            return sessionHolder.getVariable(name);
        }
    }

    @GET
    @Path("db/set-variable/string")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public boolean setDatabaseVariableString(@QueryParam("name") String name,
                                             @QueryParam("value") String value)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            return sessionHolder.setVariable(name, value);
        }
    }

    @GET
    @Path("db/set-variable/integer")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public boolean setDatabaseVariableInteger(@QueryParam("name") String name,
                                              @QueryParam("value") long value)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            return sessionHolder.setVariable(name, value);
        }
    }

    @GET
    @Path("db/set-variable/decimal")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public boolean setDatabaseVariableDecimal(@QueryParam("name") String name,
                                              @QueryParam("value") double value)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            return sessionHolder.setVariable(name, value);
        }
    }

    //--//

    @GET
    @Path("logger/flush")
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    @Produces(MediaType.APPLICATION_JSON)
    public String flushLogger(@QueryParam("toConsole") Boolean toConsole) throws
                                                                          Exception
    {
        m_app.flushLogEntries(BoxingUtils.get(toConsole, false));

        return "<done>";
    }

    @GET
    @Path("logger/pending")
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    @Produces(MediaType.APPLICATION_JSON)
    public int pendingLogEntries() throws
                                   Exception
    {
        return m_app.getPendingLogEntries();
    }

    //--//

    @GET
    @Path("spooler/flush")
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    @Produces(MediaType.TEXT_PLAIN)
    public String flushSpooler()
    {
        ResultStagingSpooler spooler = m_app.getServiceNonNull(ResultStagingSpooler.class);
        spooler.queueFlush();

        return "<done>";
    }

    @GET
    @Path("query-plan-cache/flush")
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    @Produces(MediaType.TEXT_PLAIN)
    public String flushQueryPlanCache()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            sessionHolder.flushQueryPlanCache();
        }

        return "<done>";
    }

    @GET
    @Path("shutdown")
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    @Produces(MediaType.TEXT_PLAIN)
    public String shutdown()
    {
        Executors.scheduleOnDefaultPool(() ->
                                        {
                                            Runtime.getRuntime()
                                                   .exit(0);
                                        }, 1, TimeUnit.SECONDS);

        return "Bye";
    }

    @GET
    @Path("compact-time-series")
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    @Produces(MediaType.TEXT_PLAIN)
    public String compactTimeSeries(@QueryParam("force") Boolean force)
    {
        LongRunningThreadPool defaultThreadPool = Executors.getDefaultLongRunningThreadPool();

        defaultThreadPool.queue(() ->
                                {
                                    try (var searchGate = m_app.closeGate(HibernateSearch.Gate.class))
                                    {
                                        try (var tagsGate = m_app.closeGate(TagsEngine.Gate.class))
                                        {
                                            try (var spoolerGate = m_app.closeGate(ResultStagingSpooler.Gate.class))
                                            {
                                                DeviceElementRecord.compactAllTimeSeries(m_sessionProvider, BoxingUtils.get(force, false));
                                            }
                                        }
                                    }

                                    Optio3DataSourceFactory dataFactory = m_app.getDataSourceFactory(null);

                                    dataFactory.queueDefragmentation(DeviceElementSampleRecord.class);
                                });

        return "Started";
    }

    @GET
    @Path("drop-network-stats")
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public String dropNetworkStats(@QueryParam("compact") Boolean compact)
    {
        LongRunningThreadPool defaultThreadPool = Executors.getDefaultLongRunningThreadPool();

        defaultThreadPool.queue(() ->
                                {
                                    try (var searchGate = m_app.closeGate(HibernateSearch.Gate.class))
                                    {
                                        try (var tagsGate = m_app.closeGate(TagsEngine.Gate.class))
                                        {
                                            try (var spoolerGate = m_app.closeGate(ResultStagingSpooler.Gate.class))
                                            {
                                                try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
                                                {
                                                    final RecordHelper<AssetRecord>         helper_asset   = sessionHolder.createHelper(AssetRecord.class);
                                                    final RecordHelper<NetworkAssetRecord>  helper_network = sessionHolder.createHelper(NetworkAssetRecord.class);
                                                    final RecordHelper<DeviceElementRecord> helper_element = sessionHolder.createHelper(DeviceElementRecord.class);

                                                    AtomicInteger count = new AtomicInteger();

                                                    AssetRecord.enumerate(helper_network, true, -1, null, (rec_network) ->
                                                    {
                                                        HubApplication.LoggerInstance.info("NetworkStatistics: Processing %s...", rec_network.getName());

                                                        for (DeviceElementRecord rec_element : rec_network.getChildren(helper_element))
                                                        {
                                                            try (ValidationResultsHolder validation = new ValidationResultsHolder(sessionHolder, null, true))
                                                            {
                                                                rec_element.remove(validation, helper_asset);
                                                            }

                                                            if (count.incrementAndGet() % 500 == 0)
                                                            {
                                                                sessionHolder.commitAndBeginNewTransaction();
                                                                HubApplication.LoggerInstance.info("NetworkStatistics: Purged %d elements...", count.get());
                                                            }
                                                        }
                                                        return StreamHelperNextAction.Continue_Evict;
                                                    });

                                                    sessionHolder.commit();

                                                    HubApplication.LoggerInstance.info("NetworkStatistics: Purged %d elements in total", count.get());
                                                }
                                            }
                                        }
                                    }

                                    if (BoxingUtils.get(compact))
                                    {
                                        Optio3DataSourceFactory dataFactory = m_app.getDataSourceFactory(null);

                                        dataFactory.queueDefragmentation(DeviceElementSampleRecord.class);
                                    }
                                });

        return "Started";
    }

    //--//

    @GET
    @Path("backup/start")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public TypedRecordIdentity<BackgroundActivityRecord> startBackup() throws
                                                                       Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, TaskForBackup::scheduleTask);

        return RecordIdentity.newTypedInstance(loc_task);
    }

    @GET
    @Path("backup/item/{id}")
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public String getBackup(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            BackgroundActivityRecord rec = sessionHolder.getEntityOrNull(BackgroundActivityRecord.class, id);
            if (rec != null)
            {
                switch (rec.getStatus())
                {
                    case COMPLETED:
                        TaskForBackup task = (TaskForBackup) rec.getHandler(sessionHolder);
                        return task.file;

                    case FAILED:
                        return "<FAILED>";
                }
            }

            return null;
        }
    }

    @DELETE
    @Path("backup/item/{id}")
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public String deleteBackup(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            BackgroundActivityRecord rec = sessionHolder.getEntityOrNull(BackgroundActivityRecord.class, id);
            if (rec != null)
            {
                TaskForBackup task = (TaskForBackup) rec.getHandler(sessionHolder);
                if (task.file != null)
                {
                    File file = new File(task.file);
                    if (file.exists())
                    {
                        file.delete();
                    }
                }
            }

            return null;
        }
    }

    //--//

    @POST
    @Path("check-usages")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public UsageFilterResponse checkUsages(UsageFilterRequest filters)
    {
        UsageFilterResponse res = new UsageFilterResponse();

        SyncWaitMultiple waiter = new SyncWaitMultiple(HubApplication.GlobalRateLimiter);

        waiter.queue(() ->
                     {
                         res.userPreferenceHits = UserPreferenceRecord.checkUsages(m_sessionProvider, filters, res.userPreferenceItems);
                     });

        waiter.queue(() ->
                     {
                         res.systemPreferenceHits = SystemPreferenceRecord.checkUsages(m_sessionProvider, filters, res.systemPreferenceItems);
                     });

        waiter.queue(() ->
                     {
                         res.dashboardHits = DashboardDefinitionVersionRecord.checkUsages(m_sessionProvider, filters, res.dashboardItems);
                     });

        waiter.queue(() ->
                     {
                         res.alertDefinitionVersionHits = AlertDefinitionVersionRecord.checkUsages(m_sessionProvider, filters, res.alertDefinitionVersionItems);
                     });

        waiter.queue(() ->
                     {
                         res.metricsDefinitionVersionHits = MetricsDefinitionVersionRecord.checkUsages(m_sessionProvider, filters, res.metricsDefinitionVersionItems);
                     });

        waiter.queue(() ->
                     {
                         res.normalizationVersionHits = NormalizationRecord.checkUsages(m_sessionProvider, filters, res.normalizationVersionItems);
                     });

        waiter.queue(() ->
                     {
                         res.reportDefinitionVersionHits = ReportDefinitionVersionRecord.checkUsages(m_sessionProvider, filters, res.reportDefinitionVersionItems);
                     });

        waiter.queue(() ->
                     {
                         res.workflowHits = WorkflowRecord.checkUsages(m_sessionProvider, filters, res.workflowItems);
                     });

        waiter.drain(null);

        return res;
    }
}
