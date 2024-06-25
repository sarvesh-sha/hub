/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import static com.optio3.util.Exceptions.getAndUnwrapException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.model.deployment.DeploymentAgent;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.client.deployer.model.ShellInput;
import com.optio3.cloud.client.deployer.model.ShellOutput;
import com.optio3.cloud.client.deployer.model.ShellToken;
import com.optio3.cloud.client.deployer.proxy.DeployerControlApi;
import com.optio3.cloud.client.deployer.proxy.DeployerShellApi;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.infra.directory.SshKey;
import com.optio3.logging.LoggerConfiguration;
import com.optio3.logging.Severity;
import com.optio3.util.BoxingUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import io.swagger.annotations.Api;

@Api(tags = { "DeploymentAgents" }) // For Swagger
@Optio3RestEndpoint(name = "DeploymentAgents") // For Optio3 Shell
@Path("/v1/deployment-agents")
public class DeploymentAgents
{
    @Inject
    private BuilderApplication m_app;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @GET
    @Path("all/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<DeploymentAgentRecord> getAll(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostRecord rec_deployer = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            return TypedRecordIdentityList.toList(rec_deployer.getAgents());
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<DeploymentAgent> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<DeploymentAgentRecord> helper = sessionHolder.createHelper(DeploymentAgentRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, DeploymentAgentRecord.getBatch(helper, ids));
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentAgent get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, DeploymentAgentRecord.class, id);
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

            RecordHelper<DeploymentAgentRecord> helper = validation.sessionHolder.createHelper(DeploymentAgentRecord.class);
            DeploymentAgentRecord               rec    = helper.getOrNull(id);
            if (rec != null)
            {
                rec.remove(validation, helper);
            }

            return validation.getResults();
        }
    }

    //--//

    @GET
    @Path("item/{id}/check-online")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public boolean checkOnline(@PathParam("id") String id) throws
                                                           Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentAgentRecord rec = sessionHolder.getEntity(DeploymentAgentRecord.class, id);
            if (rec == null)
            {
                return false;
            }

            if (rec.getStatus() != DeploymentStatus.Ready)
            {
                return false;
            }

            var ci = rec.extractConnectionInfo();
            return getAndUnwrapException(ci.checkIfOnline(m_app, 3, TimeUnit.SECONDS)) != null;
        }
    }

    @GET
    @Path("item/{id}/make-active")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public boolean makeActive(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentAgentRecord rec_agent = sessionHolder.getEntity(DeploymentAgentRecord.class, id);

            if (rec_agent.isActive())
            {
                return true;
            }

            DeploymentHostRecord rec_host = rec_agent.getDeployment();
            if (rec_host == null)
            {
                return false;
            }

            rec_host.activateAgent(rec_agent);

            sessionHolder.commit();

            return true;
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

            DeploymentAgentRecord rec = validation.sessionHolder.getEntityOrNull(DeploymentAgentRecord.class, id);
            if (rec != null)
            {
                rec.terminate(validation);
            }

            return validation.getResults();
        }
    }

    //--//

    @GET
    @Path("item/{id}/flush")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    @Optio3RequestLogLevel(Severity.Debug)
    public boolean flush(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentAgentRecord rec = sessionHolder.getEntityOrNull(DeploymentAgentRecord.class, id);
            if (rec != null)
            {
                if (rec.canSupport(DeploymentAgentFeature.FlushAndRestart))
                {
                    DeployerControlApi proxy = getControlProxy(rec);
                    if (proxy != null)
                    {
                        getAndUnwrapException(proxy.flushHeartbeat(), 20, TimeUnit.SECONDS);

                        return true;
                    }
                }
            }
        }
        catch (Throwable t)
        {
            // Assume Gateway not online.
        }

        return false;
    }

    @GET
    @Path("item/{id}/restart")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    @Optio3RequestLogLevel(Severity.Debug)
    public boolean restart(@PathParam("id") String id)
    {
        try
        {
            try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
            {
                DeploymentAgentRecord rec = sessionHolder.getEntityOrNull(DeploymentAgentRecord.class, id);
                if (rec != null)
                {
                    if (rec.canSupport(DeploymentAgentFeature.FlushAndRestart))
                    {
                        DeployerControlApi proxy = getControlProxy(rec);
                        if (proxy != null)
                        {
                            getAndUnwrapException(proxy.restart(), 20, TimeUnit.SECONDS);

                            return true;
                        }
                    }
                }
            }
        }
        catch (Throwable t)
        {
            // Assume Gateway not online.
        }

        return false;
    }

    //--//

    @GET
    @Path("item/{id}/loggers/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LoggerConfiguration> getLoggers(@PathParam("id") String id)
    {
        try
        {
            try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
            {
                DeploymentAgentRecord rec = sessionHolder.getEntityOrNull(DeploymentAgentRecord.class, id);
                if (rec != null)
                {
                    DeployerControlApi proxy = getControlProxy(rec);
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
    @Path("item/{id}/loggers/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    @Optio3RequestLogLevel(Severity.Debug)
    public LoggerConfiguration configLogger(@PathParam("id") String id,
                                            LoggerConfiguration cfg)
    {
        try
        {
            try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
            {
                DeploymentAgentRecord rec = sessionHolder.getEntityOrNull(DeploymentAgentRecord.class, id);
                if (rec != null)
                {
                    DeployerControlApi proxy = getControlProxy(rec);
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

    @GET
    @Path("item/{id}/threads")
    @Produces(MediaType.TEXT_PLAIN)
    @Optio3RequestLogLevel(Severity.Debug)
    public String dumpThreads(@PathParam("id") String id,
                              @QueryParam("includeMemInfo") Boolean includeMemInfo)
    {
        try
        {
            try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
            {
                DeploymentAgentRecord rec = sessionHolder.getEntity(DeploymentAgentRecord.class, id);

                DeployerControlApi proxy = getControlProxy(rec);
                if (proxy != null)
                {
                    List<String> lines = getAndUnwrapException(proxy.dumpThreads(BoxingUtils.get(includeMemInfo)));

                    return String.join("\n", lines);
                }
            }
        }
        catch (Throwable t)
        {
            BuilderApplication.LoggerInstance.debug("dumpThreads for %s failed with %s", id, t);
        }

        return null;
    }

    //--//

    @GET
    @Path("item/{id}/shell/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<ShellToken> getAllShells(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentAgentRecord rec = sessionHolder.getEntity(DeploymentAgentRecord.class, id);

            try
            {
                DeployerShellApi proxy = getShellProxy(rec);
                if (proxy != null)
                {
                    return getAndUnwrapException(proxy.listSessions());
                }
            }
            catch (Throwable t)
            {
                BuilderApplication.LoggerInstance.error("getAllShells failed with %s", t);
            }

            return null;
        }
    }

    @GET
    @Path("item/{id}/shell/new")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    @Optio3RequestLogLevel(Severity.Debug)
    public ShellToken openShell(@PathParam("id") String id,
                                @QueryParam("cmd") String cmd)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentAgentRecord rec = sessionHolder.getEntity(DeploymentAgentRecord.class, id);

            try
            {
                DeployerShellApi proxy = getShellProxy(rec);
                if (proxy != null)
                {
                    final String prefix = "ssh:";

                    if (cmd.startsWith(prefix))
                    {
                        DeployLogicForAgent logic = new DeployLogicForAgent(sessionHolder, rec.getDeployment());

                        SshKey key = logic.getSshKey();
                        if (key != null)
                        {
                            String server = cmd.substring(prefix.length());

                            return getAndUnwrapException(proxy.startWithSsh(server, key.user, key.getPrivateKey(), key.getPublicKey(), key.passphrase.getBytes(), 1, TimeUnit.MINUTES));
                        }
                    }
                    else
                    {
                        return getAndUnwrapException(proxy.start(cmd, 1, TimeUnit.MINUTES));
                    }
                }
            }
            catch (Throwable t)
            {
                BuilderApplication.LoggerInstance.debug("openShell failed with %s", t);
            }

            return null;
        }
    }

    @GET
    @Path("item/{id}/shell/close/{session}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    @Optio3RequestLogLevel(Severity.Debug)
    public boolean closeShell(@PathParam("id") String id,
                              @PathParam("session") String session)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentAgentRecord rec = sessionHolder.getEntity(DeploymentAgentRecord.class, id);

            try
            {
                DeployerShellApi proxy = getShellProxy(rec);
                if (proxy != null)
                {
                    ShellToken token = new ShellToken();
                    token.id = session;
                    getAndUnwrapException(proxy.stop(token), 2, TimeUnit.SECONDS);
                }

                return true;
            }
            catch (Throwable t)
            {
                BuilderApplication.LoggerInstance.debug("closeShell failed with %s", t);
            }

            return false;
        }
    }

    @POST
    @Path("item/{id}/shell/write/{session}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    @Optio3RequestLogLevel(Severity.Debug)
    public boolean writeToShell(@PathParam("id") String id,
                                @PathParam("session") String session,
                                ShellInput input)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentAgentRecord rec = sessionHolder.getEntity(DeploymentAgentRecord.class, id);
            try
            {
                DeployerShellApi proxy = getShellProxy(rec);
                if (proxy != null)
                {
                    ShellToken token = new ShellToken();
                    token.id = session;

                    getAndUnwrapException(proxy.write(token, input.text.getBytes()));
                    return true;
                }
            }
            catch (Throwable t)
            {
                BuilderApplication.LoggerInstance.debug("writeToShell failed with %s", t);
            }

            return false;
        }
    }

    @GET
    @Path("item/{id}/shell/read/{session}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    @Optio3RequestLogLevel(Severity.Debug)
    public List<ShellOutput> readFromShell(@PathParam("id") String id,
                                           @PathParam("session") String session)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentAgentRecord rec = sessionHolder.getEntity(DeploymentAgentRecord.class, id);

            MonotonousTime timeout = TimeUtils.computeTimeoutExpiration(4, TimeUnit.SECONDS);

            while (!TimeUtils.isTimeoutExpired(timeout))
            {
                try
                {
                    DeployerShellApi proxy = getShellProxy(rec);
                    if (proxy != null)
                    {
                        ShellToken token = new ShellToken();
                        token.id = session;

                        List<ShellOutput> resStdOut = Lists.newArrayList();
                        List<ShellOutput> resStdErr = Lists.newArrayList();

                        int exitCode = getAndUnwrapException(proxy.poll(token, 2048, (readStdOut) -> pushOutput(resStdOut, 1, readStdOut), (readStdErr) -> pushOutput(resStdErr, 2, readStdErr)));

                        List<ShellOutput> res = Lists.newArrayList();
                        res.addAll(resStdErr);
                        res.addAll(resStdOut);

                        // Drain the streams before checking for exit code.
                        if (!res.isEmpty())
                        {
                            return res;
                        }

                        if (exitCode >= 0)
                        {
                            return null;
                        }

                        Thread.sleep(100);
                    }
                }
                catch (Throwable t)
                {
                    BuilderApplication.LoggerInstance.debug("readFromShell failed with %s", t);
                }
            }

            return Collections.emptyList();
        }
    }

    //--//

    private DeployerControlApi getControlProxy(DeploymentAgentRecord rec) throws
                                                                          Exception
    {
        var ci = rec.extractConnectionInfo();

        return getAndUnwrapException(ci.getProxyOrNull(m_app, DeployerControlApi.class, 100));
    }

    private DeployerShellApi getShellProxy(DeploymentAgentRecord rec) throws
                                                                      Exception
    {
        var ci = rec.extractConnectionInfo();

        return getAndUnwrapException(ci.getProxyOrNull(m_app, DeployerShellApi.class, 100));
    }

    private CompletableFuture<Void> pushOutput(List<ShellOutput> outputs,
                                               int fd,
                                               byte[] output)
    {
        ShellOutput so = new ShellOutput();
        so.timestamp = TimeUtils.now();
        so.fd        = fd;
        so.payload   = new String(output);
        outputs.add(so);

        return AsyncRuntime.NullResult;
    }
}
