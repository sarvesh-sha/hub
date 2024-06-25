/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.logic.deploy;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.ws.rs.NotFoundException;

import com.google.common.base.Stopwatch;
import com.optio3.archive.TarArchiveEntry;
import com.optio3.archive.TarWalker;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.builder.model.deployment.DeploymentAgentDetails;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.client.deployer.model.BatchToken;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.ContainerStatus;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.client.deployer.model.ImagePullToken;
import com.optio3.cloud.client.deployer.model.ImageStatus;
import com.optio3.cloud.client.deployer.model.LogEntry;
import com.optio3.cloud.client.deployer.model.ShellOutput;
import com.optio3.cloud.client.deployer.model.VolumeStatus;
import com.optio3.cloud.client.deployer.model.batch.DockerBatch;
import com.optio3.cloud.client.deployer.proxy.DeployerControlApi;
import com.optio3.cloud.client.deployer.proxy.DeployerDockerApi;
import com.optio3.cloud.messagebus.channel.RpcConnectionInfo;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.logging.Logger;
import com.optio3.util.Exceptions;
import com.optio3.util.FileSystem;
import com.optio3.util.IdGenerator;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.ConsumerWithException;
import com.optio3.util.function.FunctionWithException;
import com.optio3.util.function.PredicateWithException;
import org.apache.commons.lang3.StringUtils;

public class DeployLogicForAgent extends BaseDeployLogic
{
    static class AdaptiveChunk
    {
        private final Stopwatch m_st        = Stopwatch.createUnstarted();
        private       int       m_chunkSize = 32 * 1024;

        public int size()
        {
            return m_chunkSize;
        }

        public void start()
        {
            m_st.reset();
            m_st.start();
        }

        public void stop()
        {
            long chunkTime = m_st.elapsed(TimeUnit.MILLISECONDS);

            if (chunkTime > 2000)
            {
                m_chunkSize = Math.max(4096, (int) (m_chunkSize * 0.8));
            }
            else if (chunkTime < 1000)
            {
                //
                // The MessageBus maximum frame is around 1MB.
                // Content is Base64-encoded and then compressed, let's assume it doubles in size.
                // Limit maximum encoded chunk to half the frame limit.
                //
                m_chunkSize = Math.min(256 * 1024, (int) (m_chunkSize * 1.2));
            }
        }
    }

    public static final Logger LoggerInstance = new Logger(DeployLogicForAgent.class);

    public final RecordLocator<DeploymentAgentRecord> loc_activeAgent;
    public final RpcConnectionInfo                    agent_connection;
    public final DeploymentAgentDetails               agent_details;
    public final boolean                              agent_onlineInLastHour;

    public DeployLogicForAgent(SessionHolder sessionHolder,
                               DeploymentHostRecord rec_host)
    {
        super(sessionHolder, rec_host);

        DeploymentAgentRecord rec_agent = rec_host.findActiveAgent();
        loc_activeAgent = sessionHolder.createLocator(rec_agent);

        if (rec_agent != null)
        {
            agent_connection       = rec_agent.extractConnectionInfo();
            agent_details          = rec_agent.getDetails();
            agent_onlineInLastHour = TimeUtils.wasUpdatedRecently(rec_agent.getLastHeartbeat(), 1, TimeUnit.HOURS);
        }
        else
        {
            agent_connection                 = new RpcConnectionInfo();
            agent_connection.hostDisplayName = rec_host.getDisplayName();
            agent_details                    = new DeploymentAgentDetails();
            agent_onlineInLastHour           = false;
        }
    }

    protected void refreshConnectionInfo() throws
                                           Exception
    {
        sessionProvider.callWithReadOnlySession(sessionHolder ->
                                                {
                                                    DeploymentHostRecord  rec_host  = sessionHolder.fromLocator(loc_targetHost);
                                                    DeploymentAgentRecord rec_agent = rec_host.getActiveAgent();
                                                    agent_connection.rpcId = rec_agent.getRpcId();
                                                });
    }

    public boolean hasEnoughFreeDisk(long needed)
    {
        return agent_details.diskFree >= needed;
    }

    public boolean canSupport(DeploymentAgentFeature... features)
    {
        return agent_details.canSupport(features);
    }

    public <T> CompletableFuture<T> getProxyOrNull(Class<T> clz,
                                                   int timeoutInSeconds) throws
                                                                         Exception
    {
        return agent_connection.getProxyOrNull(getApplication(), clz, timeoutInSeconds);
    }

    public <T> CompletableFuture<T> getProxy(Class<T> clz,
                                             int timeoutInSeconds) throws
                                                                   Exception
    {
        return agent_connection.getProxy(getApplication(), clz, timeoutInSeconds);
    }

    //--//

    public static DeploymentAgentRecord initializeNewAgent(RecordLocked<DeploymentHostRecord> targetHost)
    {
        DeploymentHostRecord  rec_host  = targetHost.get();
        DeploymentAgentRecord rec_agent = DeploymentAgentRecord.newInstance(rec_host);

        // Assign a unique Id Prefix to the rec_agent.
        int seq = 1;

        for (DeploymentAgentRecord rec_agentRecord : rec_host.getAgents())
        {
            String id = rec_agentRecord.getInstanceId();

            if (id != null && id.startsWith("v"))
            {
                try
                {
                    int idSeq = Integer.parseInt(id.substring(1));

                    seq = Math.max(seq, idSeq + 1);
                }
                catch (NumberFormatException e)
                {
                    // Not in the format "v<digits>"
                }
            }
        }

        String instanceIdUnique = "v" + seq;
        rec_agent.setInstanceId(instanceIdUnique);

        SessionHolder sessionHolder = targetHost.getSessionHolder();
        sessionHolder.persistEntity(rec_agent);

        return rec_agent;
    }

    //--//

    public CompletableFuture<ZonedDateTime> setSystemTime(ZonedDateTime time)
    {
        ZonedDateTime res = null;

        try
        {
            DeployerControlApi proxy = await(getProxyOrNull(DeployerControlApi.class, 10));
            if (proxy != null)
            {
                res = await(proxy.setSystemTime(time));
            }
        }
        catch (TimeoutException te)
        {
            // Expected...
        }
        catch (Throwable t)
        {
            LoggerInstance.warn("Failed to set system time on Agent %s on Host '%s': %s", agent_connection.instanceId, agent_connection.hostDisplayName, t);
        }

        return wrapAsync(res);
    }

    //--//

    public CompletableFuture<List<ImageStatus>> listImages(boolean all) throws
                                                                        Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 1 * 60));
        return proxy.listImages(all);
    }

    public CompletableFuture<ImagePullToken> startPullImage(String image) throws
                                                                          Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 30));

        DockerImageIdentifier parsedImage = new DockerImageIdentifier(image);

        if (parsedImage.tag == null)
        {
            parsedImage.tag = "latest";
        }

        return proxy.startPullImage(parsedImage.getFullName(), null, null);
    }

    public CompletableFuture<Boolean> removeImage(String image,
                                                  boolean force)
    {
        boolean res;

        try
        {
            DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 30));

            res = await(proxy.removeImage(image, force));
        }
        catch (Throwable t)
        {
            res = false;
        }

        return wrapAsync(res);
    }

    public CompletableFuture<Boolean> tagImage(String sourceImage,
                                               String targetImage)
    {
        boolean res = false;

        try
        {
            DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 30));

            res = await(proxy.tagImage(sourceImage, targetImage));
        }
        catch (TimeoutException t1)
        {
            // Expected...
        }
        catch (Throwable t)
        {
            LoggerInstance.error("tagImage on %s: failed to tag '%s' as '%s', due to %s", agent_connection.hostDisplayName, sourceImage, targetImage, t);
        }

        return wrapAsync(res);
    }

    //--//

    public CompletableFuture<List<VolumeStatus>> listVolumes()
    {
        List<VolumeStatus> res;

        try
        {
            DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

            res = await(proxy.listVolumes(null));
        }
        catch (Throwable t)
        {
            res = null;
        }

        return wrapAsync(res);
    }

    public CompletableFuture<VolumeStatus> inspectVolume(String volumeName) throws
                                                                            Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        return proxy.inspectVolume(volumeName);
    }

    public CompletableFuture<String> createVolume(String volumeName,
                                                  Map<String, String> labels) throws
                                                                              Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        return proxy.createVolume(volumeName, labels, null, null);
    }

    public CompletableFuture<Void> deleteVolume(String volumeName,
                                                boolean force) throws
                                                               Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        try
        {
            await(proxy.deleteVolume(volumeName, force));
        }
        catch (NotFoundException ex)
        {
            // It had already been deleted, it's fine.
        }
        catch (Throwable t)
        {
            if (force)
            {
                throw t;
            }
        }

        return wrapAsync(null);
    }

    public CompletableFuture<Boolean> restoreFileSystem(String containerId,
                                                        Path targetPath,
                                                        int timeoutInMinutes,
                                                        ConsumerWithException<File> generatorCallback,
                                                        TransferProgress<?> transferProgress) throws
                                                                                              Exception
    {
        boolean success;

        try (FileSystem.TmpFileHolder holder = FileSystem.createTempFile())
        {
            generatorCallback.accept(holder.get());

            success = await(writeContainerFileSystemFromTar(containerId, targetPath, holder.get(), true, timeoutInMinutes, transferProgress));
        }

        return wrapAsync(success);
    }

    //--//

    public CompletableFuture<List<ContainerStatus>> listContainers() throws
                                                                     Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        return proxy.listContainers(null);
    }

    public CompletableFuture<String> createContainer(String name,
                                                     ContainerConfiguration config) throws
                                                                                    Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        return proxy.createContainer(name, config);
    }

    public CompletableFuture<Void> startContainer(String containerId) throws
                                                                      Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        return proxy.startContainer(containerId);
    }

    public CompletableFuture<Boolean> signalContainer(String containerId,
                                                      String signal) throws
                                                                     Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        return proxy.signalContainer(containerId, signal);
    }

    public CompletableFuture<Integer> stopContainer(String containerId) throws
                                                                        Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        return proxy.stopContainer(containerId);
    }

    public CompletableFuture<ContainerStatus> inspectContainer(String containerId) throws
                                                                                   Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        return proxy.inspectContainer(containerId);
    }

    public CompletableFuture<ContainerStatus> removeContainer(String containerId) throws
                                                                                  Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        return proxy.removeContainer(containerId);
    }

    //--//

    public CompletableFuture<BatchToken> prepareBatch(List<DockerBatch> list) throws
                                                                              Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        DockerBatch.Request req = new DockerBatch.Request();
        req.items = list;
        return proxy.prepareBatch(req);
    }

    public CompletableFuture<DockerBatch.Report> startBatch(BatchToken token) throws
                                                                              Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        return proxy.startBatch(token);
    }

    public CompletableFuture<DockerBatch.Report> checkBatch(BatchToken token,
                                                            int offset,
                                                            Consumer<ShellOutput> callback) throws
                                                                                            Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        return proxy.checkBatchProgress(token, offset, (line) ->
        {
            callback.accept(line);

            return AsyncRuntime.NullResult;
        });
    }

    public CompletableFuture<Void> closeBatch(BatchToken token) throws
                                                                Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        return proxy.closeBatch(token);
    }

    //--//

    public static abstract class MonitorExecution
    {
        public abstract ZonedDateTime getLastOutput();

        public abstract void setLastOutput(ZonedDateTime lastOutput);

        public abstract boolean processLine(LogEntry en);

        public abstract void processExitCode(int exitCode);
    }

    public CompletableFuture<Void> monitorExecution(String containerId,
                                                    int limit,
                                                    MonitorExecution state) throws
                                                                            Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, 2 * 60));

        await(proxy.fetchOutput(containerId, limit, 64 * 1024, false, state.getLastOutput(), (lst) ->
        {
            for (LogEntry en : lst)
            {
                state.setLastOutput(en.timestamp);

                if (state.processLine(en))
                {
                    break;
                }
            }

            return AsyncRuntime.NullResult;
        }));

        Integer exitCode = await(proxy.getContainerExitCode(containerId));
        if (exitCode != null)
        {
            state.processExitCode(exitCode);
        }

        return wrapAsync(null);
    }

    //--//

    public CompletableFuture<Boolean> enumerateContainerFileSystem(String containerId,
                                                                   Path containerPath,
                                                                   boolean isDirectory,
                                                                   int timeoutInMinutes,
                                                                   TransferProgress<?> transferProgress,
                                                                   PredicateWithException<TarArchiveEntry> callback) throws
                                                                                                                     Exception
    {
        return readContainerFileSystemToTar(containerId, containerPath, isDirectory, true, timeoutInMinutes, transferProgress, (tmpFile) ->
        {
            return TarWalker.walk(tmpFile, true, callback);
        });
    }

    public CompletableFuture<Boolean> readContainerFileSystemToTar(String containerId,
                                                                   Path path,
                                                                   boolean isDirectory,
                                                                   boolean compress,
                                                                   int timeoutInMinutes,
                                                                   TransferProgress<?> transferProgress,
                                                                   FunctionWithException<File, Boolean> callback) throws
                                                                                                                  Exception
    {
        boolean res;

        String fileOnAgent = await(exportContainerFileSystemToTar(containerId, path, isDirectory, compress, timeoutInMinutes));
        if (fileOnAgent == null)
        {
            res = false;
        }
        else
        {
            try
            {
                res = await(copyFileFromAgent(fileOnAgent, timeoutInMinutes, transferProgress, callback));
            }
            finally
            {
                await(deleteFileOnAgent(fileOnAgent));
            }
        }

        return wrapAsync(res);
    }

    public CompletableFuture<Boolean> writeContainerFileSystemFromTar(String containerId,
                                                                      Path containerPath,
                                                                      File inputArchive,
                                                                      boolean decompress,
                                                                      int timeoutInMinutes,
                                                                      TransferProgress<?> transferProgress) throws
                                                                                                            Exception
    {
        String tmpFileOnAgent = IdGenerator.newGuid();

        try
        {
            await(copyFileToAgent(inputArchive, tmpFileOnAgent, timeoutInMinutes, transferProgress));

            boolean res = await(importContainerFileSystemFromTar(containerId, containerPath, tmpFileOnAgent, decompress, timeoutInMinutes));
            return wrapAsync(res);
        }
        finally
        {
            await(deleteFileOnAgent(tmpFileOnAgent));
        }
    }

    public CompletableFuture<String> exportContainerFileSystemToTar(String containerId,
                                                                    Path path,
                                                                    boolean isDirectory,
                                                                    boolean compress,
                                                                    int timeoutInMinutes) throws
                                                                                          Exception
    {
        String pathText = path.toString();

        if (isDirectory && !pathText.endsWith("/"))
        {
            // We need to append a trailing slash to the path, so the Agent can understand we want to pack a directory instead of just a file.
            pathText += "/";
        }

        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, timeoutInMinutes * 60));

        return proxy.readContainerFileSystemToTar(containerId, pathText, compress);
    }

    public CompletableFuture<Boolean> importContainerFileSystemFromTar(String containerId,
                                                                       Path containerPath,
                                                                       String fileOnAgent,
                                                                       boolean decompress,
                                                                       int timeoutInMinutes) throws
                                                                                             Exception
    {
        DeployerDockerApi proxy = await(getProxy(DeployerDockerApi.class, timeoutInMinutes * 60));

        return proxy.writeContainerFileSystemFromTar(containerId, containerPath.toString(), fileOnAgent, decompress);
    }

    //--//

    public CompletableFuture<Boolean> writeStreamToOuterFileSystem(String user,
                                                                   byte[] privateKey,
                                                                   byte[] publicKey,
                                                                   byte[] passphrase,
                                                                   InputStream stream,
                                                                   long length,
                                                                   String targetFile,
                                                                   int timeoutInMinutes,
                                                                   TransferProgress<?> transferProgress) throws
                                                                                                         Exception
    {
        String tmpFileOnAgent = IdGenerator.newGuid();

        try
        {
            await(copyStreamToAgent(stream, tmpFileOnAgent, length, timeoutInMinutes, transferProgress));

            DeployerControlApi proxy = await(getProxy(DeployerControlApi.class, timeoutInMinutes * 60));

            int exitCode = await(proxy.copyFileToHost(user, privateKey, publicKey, passphrase, tmpFileOnAgent, targetFile));
            return wrapAsync(exitCode == 0);
        }
        finally
        {
            await(deleteFileOnAgent(tmpFileOnAgent));
        }
    }

    public CompletableFuture<Boolean> readStreamFromOuterFileSystem(String user,
                                                                    byte[] privateKey,
                                                                    byte[] publicKey,
                                                                    byte[] passphrase,
                                                                    OutputStream stream,
                                                                    String sourceFile,
                                                                    int timeoutInMinutes,
                                                                    TransferProgress<?> transferProgress) throws
                                                                                                          Exception
    {
        String tmpFileOnAgent = IdGenerator.newGuid();

        try
        {
            DeployerControlApi proxy = await(getProxy(DeployerControlApi.class, timeoutInMinutes * 60));

            int exitCode = await(proxy.copyFileFromHost(user, privateKey, publicKey, passphrase, tmpFileOnAgent, sourceFile));
            if (exitCode != 0)
            {
                return wrapAsync(false);
            }

            long totalSize = await(copyStreamFromAgent(tmpFileOnAgent, stream, timeoutInMinutes, transferProgress));
            return wrapAsync(totalSize > 0);
        }
        finally
        {
            await(deleteFileOnAgent(tmpFileOnAgent));
        }
    }

    //--//

    public static abstract class TransferProgress<T>
    {
        public T context;

        public long currentPos;
        public long totalSize;

        public abstract void notifyBegin();

        public abstract void notifyUpdate();

        public abstract void notifyEnd(boolean success);

        public abstract boolean wasCancelled();
    }

    public CompletableFuture<Long> fileSizeOnAgent(String fileOnAgent) throws
                                                                       Exception
    {
        long res;

        if (StringUtils.isEmpty(fileOnAgent))
        {
            res = -1;
        }
        else
        {
            DeployerControlApi proxy = await(getProxy(DeployerControlApi.class, 2 * 60));
            res = await(proxy.getFileSize(fileOnAgent));
        }

        return wrapAsync(res);
    }

    public CompletableFuture<Void> deleteFileOnAgent(String fileOnAgent) throws
                                                                         Exception
    {
        if (StringUtils.isNotEmpty(fileOnAgent))
        {
            DeployerControlApi proxy = await(getProxy(DeployerControlApi.class, 2 * 60));
            await(proxy.deleteFile(fileOnAgent));
        }

        return wrapAsync(null);
    }

    public CompletableFuture<Boolean> copyFileFromAgent(String fileOnAgent,
                                                        int timeoutInMinutes,
                                                        TransferProgress<?> transferProgress,
                                                        FunctionWithException<File, Boolean> callback) throws
                                                                                                       Exception
    {
        try (FileSystem.TmpFileHolder holder = FileSystem.createTempFile())
        {
            File localDestination = holder.get();

            try (FileOutputStream stream = new FileOutputStream(localDestination))
            {
                long totalSize = await(copyStreamFromAgent(fileOnAgent, stream, timeoutInMinutes, transferProgress));
                if (totalSize <= 0)
                {
                    return AsyncRuntime.False;
                }
            }

            return wrapAsync(callback.apply(localDestination));
        }
    }

    public CompletableFuture<Void> copyFileToAgent(File localSource,
                                                   String fileOnAgent,
                                                   int timeoutInMinutes,
                                                   TransferProgress<?> transferProgress) throws
                                                                                         Exception
    {
        try (FileInputStream stream = new FileInputStream(localSource))
        {
            await(copyStreamToAgent(stream, fileOnAgent, localSource.length(), timeoutInMinutes, transferProgress));
        }

        return wrapAsync(null);
    }

    public CompletableFuture<Void> copyStreamToAgent(InputStream inputStream,
                                                     String fileOnAgent,
                                                     long length,
                                                     int timeoutInMinutes,
                                                     TransferProgress<?> transferProgress) throws
                                                                                           Exception
    {
        DeployerControlApi proxy = await(getProxy(DeployerControlApi.class, timeoutInMinutes * 60));

        try
        {
            if (transferProgress != null)
            {
                transferProgress.totalSize = length;
                transferProgress.notifyBegin();
            }

            if (canSupport(DeploymentAgentFeature.CopyFileChunk))
            {
                MonotonousTime timeout     = TimeUtils.computeTimeoutExpiration(timeoutInMinutes, TimeUnit.MINUTES);
                AdaptiveChunk  chunkHelper = new AdaptiveChunk();
                byte[]         buf         = null;
                long           offset      = 0;

                proxy = null;

                while (true)
                {
                    int chunkSize = chunkHelper.size();
                    if (buf == null || buf.length < chunkSize)
                    {
                        buf = new byte[chunkSize];
                    }

                    int read = inputStream.read(buf, 0, chunkSize);
                    if (read <= 0)
                    {
                        break;
                    }

                    byte[] bufOut = read == buf.length ? buf : Arrays.copyOf(buf, read);

                    while (true)
                    {
                        if (transferProgress != null)
                        {
                            if (transferProgress.wasCancelled())
                            {
                                throw Exceptions.newRuntimeException("Transfer cancelled");
                            }
                        }

                        try
                        {
                            if (proxy == null)
                            {
                                refreshConnectionInfo();

                                proxy = await(getProxy(DeployerControlApi.class, 5 * 60));
                            }

                            chunkHelper.start();

                            await(proxy.writeFileChunk(fileOnAgent, offset, bufOut));

                            chunkHelper.stop();

                            offset += bufOut.length;

                            if (transferProgress != null)
                            {
                                transferProgress.currentPos = offset;
                                transferProgress.notifyUpdate();
                            }

                            break;
                        }
                        catch (TimeoutException e)
                        {
                            //
                            // Chunk failed, retry if not past the overall timeout.
                            //
                            if (TimeUtils.isTimeoutExpired(timeout))
                            {
                                throw e;
                            }

                            proxy = null;
                        }
                    }
                }

                await(proxy.closeFile(fileOnAgent));
            }
            else
            {
                await(proxy.writeFile(fileOnAgent, (len) ->
                {
                    byte[] buf  = new byte[len];
                    int    read = inputStream.read(buf);

                    if (read > 0)
                    {
                        if (transferProgress != null)
                        {
                            transferProgress.currentPos += read;
                            transferProgress.notifyUpdate();
                        }
                    }

                    if (transferProgress != null)
                    {
                        if (transferProgress.wasCancelled())
                        {
                            read = -1;
                        }
                    }

                    return CompletableFuture.completedFuture(read < 0 ? null : Arrays.copyOf(buf, read));
                }));
            }

            if (transferProgress != null)
            {
                transferProgress.notifyEnd(true);
            }
        }
        catch (Throwable t)
        {
            LoggerInstance.error("copyStreamToAgent on %s: failed to copy '%s', due to %s", agent_connection.hostDisplayName, fileOnAgent, t);

            if (transferProgress != null)
            {
                transferProgress.notifyEnd(false);
            }

            throw t;
        }

        return wrapAsync(null);
    }

    public CompletableFuture<Long> copyStreamFromAgent(String fileOnAgent,
                                                       OutputStream outputStream,
                                                       int timeoutInMinutes,
                                                       TransferProgress<?> transferProgress) throws
                                                                                             Exception
    {
        DeployerControlApi proxy = await(getProxy(DeployerControlApi.class, timeoutInMinutes * 60));

        try
        {
            long totalSize = await(proxy.getFileSize(fileOnAgent));
            if (totalSize <= 0)
            {
                return wrapAsync(-1L);
            }

            if (transferProgress != null)
            {
                transferProgress.totalSize = totalSize;
                transferProgress.notifyBegin();
            }

            long offset = 0;

            if (canSupport(DeploymentAgentFeature.CopyFileChunk))
            {
                MonotonousTime timeout     = TimeUtils.computeTimeoutExpiration(timeoutInMinutes, TimeUnit.MINUTES);
                AdaptiveChunk  chunkHelper = new AdaptiveChunk();

                //
                // Force the recreation of the proxy, to get a much shorter timeout.
                //
                proxy = null;

                while (true)
                {
                    if (transferProgress != null)
                    {
                        if (transferProgress.wasCancelled())
                        {
                            throw Exceptions.newRuntimeException("Transfer cancelled");
                        }
                    }

                    try
                    {
                        if (proxy == null)
                        {
                            refreshConnectionInfo();

                            proxy = await(getProxy(DeployerControlApi.class, 5 * 60));
                        }

                        while (offset < totalSize)
                        {
                            long remaining = totalSize - offset;
                            int  chunkSize = (int) Math.min(chunkHelper.size(), remaining);

                            chunkHelper.start();

                            byte[] buf = await(proxy.readFileChunk(fileOnAgent, offset, chunkSize));
                            if (buf == null || buf.length == 0)
                            {
                                break;
                            }

                            chunkHelper.stop();

                            outputStream.write(buf);
                            offset += buf.length;

                            if (transferProgress != null)
                            {
                                transferProgress.currentPos = offset;
                                transferProgress.notifyUpdate();

                                if (transferProgress.wasCancelled())
                                {
                                    throw Exceptions.newRuntimeException("Transfer cancelled");
                                }
                            }
                        }

                        break;
                    }
                    catch (TimeoutException e)
                    {
                        //
                        // Chunk failed, retry if not past the overall timeout.
                        //
                        if (TimeUtils.isTimeoutExpired(timeout))
                        {
                            throw e;
                        }

                        proxy = null;
                    }
                }

                await(proxy.closeFile(fileOnAgent));
            }
            else
            {
                AtomicLong actualSize = new AtomicLong();

                await(proxy.readFile(fileOnAgent, (buf) ->
                {
                    outputStream.write(buf);
                    actualSize.addAndGet(buf.length);

                    if (transferProgress != null)
                    {
                        transferProgress.currentPos += buf.length;
                        transferProgress.notifyUpdate();

                        if (transferProgress.wasCancelled())
                        {
                            return AsyncRuntime.False;
                        }
                    }

                    return AsyncRuntime.True;
                }));

                offset = actualSize.get();
            }

            if (offset != totalSize)
            {
                throw Exceptions.newRuntimeException("Size mismatch: expecting %,d bytes, got %,d bytes", totalSize, offset);
            }

            if (transferProgress != null)
            {
                transferProgress.notifyEnd(true);
            }

            return wrapAsync(offset);
        }
        catch (Throwable t)
        {
            LoggerInstance.error("copyStreamFromAgent on %s: failed to copy '%s', due to %s", agent_connection.hostDisplayName, fileOnAgent, t);

            if (transferProgress != null)
            {
                transferProgress.notifyEnd(false);
            }

            throw t;
        }
    }
}
