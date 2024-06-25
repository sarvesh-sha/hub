/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.lang.management.ThreadInfo;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3NoAuthenticationNeeded;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForHub;
import com.optio3.cloud.builder.model.admin.CustomerUpgradeLevel;
import com.optio3.cloud.builder.model.admin.ServiceUpgradeLevel;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.FixupProcessingRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.MessageBusDatagramSession;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.concurrency.Executors;
import com.optio3.logging.LoggerConfiguration;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.Severity;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.StackTraceAnalyzer;
import io.swagger.annotations.Api;

@Api(tags = { "AdminTasks" }) // For Swagger
@Optio3RestEndpoint(name = "AdminTasks") // For Optio3 Shell
@Path("/v1/admin-tasks")
@Optio3RequestLogLevel(Severity.Debug)
public class AdminTasks
{
    @Inject
    private BuilderApplication m_app;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

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

    //--//

    @GET
    @Path("threads")
    @Produces(MediaType.TEXT_PLAIN)
    public String dumpThreads()
    {
        Map<StackTraceAnalyzer, List<ThreadInfo>> uniqueStackTraces = StackTraceAnalyzer.allThreadInfos();
        List<String>                              lines             = StackTraceAnalyzer.printThreadInfos(true, uniqueStackTraces);

        return String.join("\n", lines);
    }

    @GET
    @Path("request-stats")
    @Produces(MediaType.TEXT_PLAIN)
    public String dumpRequestStatistics()
    {
        List<String> lines = m_app.dumpRequestStatistics();

        return String.join("\n", lines);
    }

    @GET
    @Path("message-bus-stats")
    @Produces(MediaType.TEXT_PLAIN)
    public String dumpMessageBusStatistics()
    {
        List<String> lines = m_app.dumpMessageBusStatistics();

        return String.join("\n", lines);
    }

    @GET
    @Path("rpc-stats")
    @Produces(MediaType.TEXT_PLAIN)
    public String dumpRpcStatistics()
    {
        List<String> lines = m_app.getRpcStatistics();

        return String.join("\n", lines);
    }

    @GET
    @Path("datagram-sessions")
    @Produces(MediaType.APPLICATION_JSON)
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
    public List<String> checkUpgradeLevel()
    {
        return FixupProcessingRecord.listExecutedHandlers(m_app);
    }

    @GET
    @Path("upgrade/list/services")
    @Produces(MediaType.APPLICATION_JSON)
    public List<CustomerUpgradeLevel> checkUpgradeLevelForServices()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<CustomerRecord>        helper_cust = sessionHolder.createHelper(CustomerRecord.class);
            RecordHelper<CustomerServiceRecord> helper_svc  = sessionHolder.createHelper(CustomerServiceRecord.class);

            List<CustomerUpgradeLevel> res = Lists.newArrayList();

            for (CustomerRecord rec_cust : helper_cust.listAll())
            {
                // Only go through the services with a Hub.
                List<CustomerServiceRecord> lst_svc = Lists.newArrayList(rec_cust.getServices());
                lst_svc.removeIf((rec_svc) -> rec_svc.findAnyTaskForRole(sessionHolder, DeploymentStatus.Ready, DeploymentRole.hub) == null);

                if (!lst_svc.isEmpty())
                {
                    TypedRecordIdentityList<CustomerServiceRecord> id_lst  = TypedRecordIdentityList.toList(lst_svc);
                    List<RecordLocator<CustomerServiceRecord>>     loc_lst = RecordLocator.createList(helper_svc, id_lst);

                    CustomerUpgradeLevel upg_cust = new CustomerUpgradeLevel();
                    upg_cust.customer = TypedRecordIdentity.newTypedInstance(rec_cust);
                    upg_cust.services = CollectionUtils.transformInParallel(loc_lst, Executors.allocateSemaphore(2), (loc_svc) ->
                    {
                        ServiceUpgradeLevel upg_svc = new ServiceUpgradeLevel();
                        upg_svc.service = TypedRecordIdentity.newTypedInstance(loc_svc);

                        try (SessionHolder sessionHolder2 = m_sessionProvider.newReadOnlySession())
                        {
                            CustomerServiceRecord rec_svc = sessionHolder2.fromLocator(loc_svc);

                            try
                            {
                                DeployLogicForHub logic = DeployLogicForHub.fromRecord(sessionHolder2, rec_svc);

                                logic.login(false);

                                com.optio3.cloud.client.hub.api.AdminTasksApi adminProxy = logic.createHubProxy(com.optio3.cloud.client.hub.api.AdminTasksApi.class);
                                upg_svc.fixupProcessors = adminProxy.checkUpgradeLevel();
                            }
                            catch (Throwable t)
                            {
                                // If this fails, it's because the site has not been upgraded to have the correct Admin API.
                            }
                        }

                        return upg_svc;
                    });

                    res.add(upg_cust);
                }
            }

            return res;
        }
    }

    //--//

    @GET
    @Path("logger/flush")
    @Produces(MediaType.APPLICATION_JSON)
    public String flushLogger(@QueryParam("toConsole") Boolean toConsole) throws
                                                                          Exception
    {
        m_app.flushLogEntries(BoxingUtils.get(toConsole, false));

        return "<done>";
    }

    @GET
    @Path("logger/pending")
    @Produces(MediaType.APPLICATION_JSON)
    public int pendingLogEntries() throws
                                   Exception
    {
        return m_app.getPendingLogEntries();
    }

    //--//

    @GET
    @Path("loggers/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<LoggerConfiguration> getLoggers()
    {
        return LoggerFactory.getLoggersConfiguration();
    }

    @POST
    @Path("loggers/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    public LoggerConfiguration configLogger(LoggerConfiguration cfg)
    {
        return LoggerFactory.setLoggerConfiguration(cfg);
    }
}
