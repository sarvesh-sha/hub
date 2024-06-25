/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import static com.optio3.util.Exceptions.getAndUnwrapException;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.client.gateway.model.prober.ProberNetworkStatus;
import com.optio3.cloud.client.gateway.model.prober.ProberOperation;
import com.optio3.cloud.client.gateway.proxy.GatewayControlApi;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.asset.GatewayAsset;
import com.optio3.cloud.hub.model.asset.GatewayProberOperation;
import com.optio3.cloud.hub.model.common.LogLine;
import com.optio3.cloud.hub.orchestration.tasks.TaskForProberOperation;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.prober.GatewayProberOperationRecord;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.LogEntryFilterRequest;
import com.optio3.cloud.persistence.LogRange;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.FileSystem;
import com.optio3.util.TimeUtils;
import io.swagger.annotations.Api;

@Api(tags = { "Gateways" }) // For Swagger
@Optio3RestEndpoint(name = "Gateways") // For Optio3 Shell
@Path("/v1/gateways")
public class Gateways
{
    @Inject
    private HubApplication m_app;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    //--//

    @GET
    @Path("item/{gatewayId}/threads")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    @Optio3RequestLogLevel(Severity.Debug)
    public String dumpThreads(@PathParam("gatewayId") String gatewayId,
                              @QueryParam("includeMemInfo") Boolean includeMemInfo)
    {
        try
        {
            try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
            {
                GatewayAssetRecord rec = sessionHolder.getEntity(GatewayAssetRecord.class, gatewayId);

                GatewayControlApi proxy = rec.getProxy(m_app, GatewayControlApi.class);
                if (proxy != null)
                {
                    List<String> lines = getAndUnwrapException(proxy.dumpThreads(BoxingUtils.get(includeMemInfo)));

                    return String.join("\n", lines);
                }
            }
        }
        catch (Throwable t)
        {
            HubApplication.LoggerInstance.debug("dumpThreads for %s failed with %s", gatewayId, t);
        }

        return null;
    }

    //--//

    @GET
    @Path("item/{gatewayId}/lookup")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    @Optio3RequestLogLevel(Severity.Debug)
    public GatewayAsset lookup(@PathParam("gatewayId") String gatewayId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            TypedRecordIdentity<GatewayAssetRecord> ri_gateway  = GatewayAssetRecord.findByInstanceId(sessionHolder.createHelper(GatewayAssetRecord.class), gatewayId);
            GatewayAssetRecord                      rec_gateway = sessionHolder.fromIdentityOrNull(ri_gateway);

            return (GatewayAsset) ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_gateway);
        }
    }

    //--//

    @GET
    @Path("item/{gatewayId}/check-network-status")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public ProberNetworkStatus checkNetworkStatus(@PathParam("gatewayId") String gatewayId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            GatewayAssetRecord rec_gateway = sessionHolder.getEntity(GatewayAssetRecord.class, gatewayId);
            if (rec_gateway == null)
            {
                return null;
            }

            return rec_gateway.checkNetwork(sessionHolder);
        }
    }

    @POST
    @Path("item/{gatewayId}/start-op")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public GatewayProberOperation startOperation(@PathParam("gatewayId") String gatewayId,
                                                 ProberOperation input) throws
                                                                        Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            GatewayAssetRecord rec_gateway = sessionHolder.getEntity(GatewayAssetRecord.class, gatewayId);

            GatewayProberOperationRecord rec_op = TaskForProberOperation.scheduleOperation(sessionHolder, rec_gateway, input, Duration.of(12, ChronoUnit.HOURS));

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_op);
        }
    }

    //--//

    @POST
    @Path("item/{gatewayId}/log/filter")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LogRange> filterLog(@PathParam("gatewayId") String gatewayId,
                                    LogEntryFilterRequest filters) throws
                                                                   Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            GatewayAssetRecord rec = sessionHolder.getEntity(GatewayAssetRecord.class, gatewayId);

            try (var logHandler = GatewayAssetRecord.allocateLogHandler(sessionHolder, rec))
            {
                return logHandler.filter(filters);
            }
        }
    }

    @GET
    @Path("item/{gatewayId}/log")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LogLine> getLog(@PathParam("gatewayId") String gatewayId,
                                @QueryParam("fromOffset") Integer fromOffset,
                                @QueryParam("toOffset") Integer toOffset,
                                @QueryParam("limit") Integer limit) throws
                                                                    Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            GatewayAssetRecord rec = sessionHolder.getEntity(GatewayAssetRecord.class, gatewayId);

            List<LogLine> lines = Lists.newArrayList();

            try (var logHandler = GatewayAssetRecord.allocateLogHandler(sessionHolder, rec))
            {
                logHandler.extract(fromOffset, toOffset, limit, (item, offset) ->
                {
                    LogLine newLine = new LogLine();
                    newLine.lineNumber = offset;
                    newLine.copyFrom(item);
                    lines.add(newLine);
                });
            }

            return lines;
        }
    }

    @DELETE
    @Path("item/{gatewayId}/log")
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    @Produces(MediaType.APPLICATION_JSON)
    public int deleteLog(@PathParam("gatewayId") String gatewayId,
                         @QueryParam("olderThanXMinutes") Integer olderThanXMinutes) throws
                                                                                     Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<GatewayAssetRecord> lock = sessionHolder.getEntityWithLock(GatewayAssetRecord.class, gatewayId, 30, TimeUnit.SECONDS);

            int deleteCount;

            try (var logHandler = GatewayAssetRecord.allocateLogHandler(lock))
            {
                ZonedDateTime olderThan = olderThanXMinutes != null ? TimeUtils.now()
                                                                               .minus(olderThanXMinutes, ChronoUnit.MINUTES) : null;

                deleteCount = logHandler.delete(olderThan);
            }

            sessionHolder.commit();

            return deleteCount;
        }
    }

    @GET
    @Path("all-logs/stream/{fileName}")
    @Produces("application/zip")
    @Optio3RequestLogLevel(Severity.Debug)
    public InputStream getAllLogs(@PathParam("fileName") String fileName) throws
                                                                          Exception
    {
        FileSystem.TmpFileHolder                  tmpFile       = FileSystem.createTempFile("logs", "tar.gz");
        AtomicReference<FileSystem.TmpFileHolder> tmpFileHolder = new AtomicReference<>(tmpFile);

        try
        {
            List<RecordIdentity> gateways;

            try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
            {
                RecordHelper<GatewayAssetRecord> helper_gateway = sessionHolder.createHelper(GatewayAssetRecord.class);

                gateways = GatewayAssetRecord.filterGateways(helper_gateway, null);
            }

            try (FileOutputStream stream = new FileOutputStream(tmpFile.get()))
            {
                try (ZipOutputStream zip = new ZipOutputStream(stream))
                {
                    CollectionUtils.transformInParallel(gateways, HubApplication.GlobalRateLimiter, (ri) ->
                    {
                        ByteArrayOutputStream streamOut = new ByteArrayOutputStream();

                        try (SessionHolder subSessionHolder = m_sessionProvider.newSessionWithTransaction())
                        {
                            RecordLocked<GatewayAssetRecord> lock_gateway = subSessionHolder.getEntityWithLock(GatewayAssetRecord.class, ri.sysId, 30, TimeUnit.SECONDS);

                            try (var logHandler = GatewayAssetRecord.allocateLogHandler(lock_gateway))
                            {
                                logHandler.extract(null, null, null, (item, offset) ->
                                {
                                    String line = String.format("%s: %s\n", item.timestamp, item.line);
                                    streamOut.write(line.getBytes(StandardCharsets.UTF_8));
                                });
                            }

                            subSessionHolder.commit();

                            GatewayAssetRecord rec_gateway = lock_gateway.get();

                            synchronized (zip)
                            {
                                ZipEntry zipEntry = new ZipEntry(String.format("%s - %s.log", escape(rec_gateway.getInstanceId()), escape(rec_gateway.getName())));
                                zip.putNextEntry(zipEntry);
                                zip.write(streamOut.toByteArray());
                                zip.closeEntry();
                            }
                        }

                        return null;
                    });
                }
            }

            final InputStream resultStream = new FileInputStream(tmpFile.get());
            tmpFileHolder.set(null); // Move ownership to the lambda.
            return new InputStream()
            {
                @Override
                public void close() throws
                                    IOException
                {
                    resultStream.close();
                    tmpFile.close();
                }

                @Override
                public int read(byte[] b,
                                int off,
                                int len) throws
                                         IOException
                {
                    return resultStream.read(b, off, len);
                }

                @Override
                public int read() throws
                                  IOException
                {
                    return resultStream.read();
                }

                @Override
                public long skip(long n) throws
                                         IOException
                {
                    return resultStream.skip(n);
                }

                @Override
                public int available() throws
                                       IOException
                {
                    return resultStream.available();
                }
            };
        }
        finally
        {
            FileSystem.TmpFileHolder tmpFile2 = tmpFileHolder.get();
            if (tmpFile2 != null)
            {
                tmpFile2.close();
            }
        }
    }

    private static String escape(String v)
    {
        v = v.replace(':', '_');
        v = v.replace('/', '_');
        v = v.replace('\\', '_');
        v = v.replace('&', '_');
        return v;
    }
}
