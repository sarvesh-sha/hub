/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer.remoting.impl;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.client.deployer.model.BatchToken;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.ContainerStatus;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.client.deployer.model.ImagePullToken;
import com.optio3.cloud.client.deployer.model.ImageStatus;
import com.optio3.cloud.client.deployer.model.LogEntry;
import com.optio3.cloud.client.deployer.model.MountPointStatus;
import com.optio3.cloud.client.deployer.model.PullProgress;
import com.optio3.cloud.client.deployer.model.PullProgressStatus;
import com.optio3.cloud.client.deployer.model.ShellOutput;
import com.optio3.cloud.client.deployer.model.VolumeStatus;
import com.optio3.cloud.client.deployer.model.batch.DockerBatch;
import com.optio3.cloud.client.deployer.proxy.DeployerDockerApi;
import com.optio3.cloud.deployer.DeployerApplication;
import com.optio3.cloud.deployer.logic.BatchSession;
import com.optio3.cloud.deployer.logic.ImagePullSession;
import com.optio3.concurrency.Executors;
import com.optio3.infra.docker.ContainerBuilder;
import com.optio3.infra.docker.DockerHelper;
import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.infra.docker.model.ContainerInspection;
import com.optio3.infra.docker.model.ContainerSummary;
import com.optio3.infra.docker.model.ImageSummary;
import com.optio3.infra.docker.model.MountPoint;
import com.optio3.infra.docker.model.Volume;
import com.optio3.infra.docker.model.VolumesCreateBody;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.lang.RunnableWithException;
import com.optio3.text.AnsiParser;
import com.optio3.util.BoxingUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.FileSystem;
import com.optio3.util.IdGenerator;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.AsyncFunctionWithException;
import com.optio3.util.function.FunctionWithException;
import org.apache.commons.lang3.StringUtils;

@Optio3RemotableEndpoint(itf = DeployerDockerApi.class)
public class DeployerDockerApiImpl extends CommonDeployerApiImpl implements DeployerDockerApi
{
    @Override
    public CompletableFuture<List<ImageStatus>> listImages(boolean all) throws
                                                                        Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            List<ImageStatus> res = Lists.newArrayList();

            for (ImageSummary img : helper.listImages(all, null, false))
            {
                ImageStatus is = new ImageStatus();

                is.id       = img.id;
                is.parentId = img.parentId;

                is.size    = img.size;
                is.created = TimeUtils.fromSecondsToLocalTime(img.created);

                is.repoTags = img.repoTags;

                is.labels = img.labels;

                res.add(is);
            }

            return res;
        });
    }

    @Override
    public CompletableFuture<ImagePullToken> startPullImage(String image,
                                                            String registryAccount,
                                                            String registryPassword) throws
                                                                                     Exception
    {
        DeployerApplication app           = getApplication();
        ImagePullToken      existingToken = app.findImagePullSession(image);
        if (existingToken != null)
        {
            return wrapAsync(existingToken);
        }

        ImagePullSession session = new ImagePullSession(app, image);
        ImagePullToken   token   = app.registerImagePullSession(session);

        session.start();

        return wrapAsync(token);
    }

    @Override
    public CompletableFuture<Integer> checkPullImageProgress(ImagePullToken token,
                                                             FunctionWithException<String, CompletableFuture<Void>> output) throws
                                                                                                                            Exception
    {
        // Old API, no longer supported.
        return wrapAsync(-1);
    }

    @Override
    public CompletableFuture<Integer> checkPullImageProgressEx(ImagePullToken token,
                                                               int offset,
                                                               FunctionWithException<ShellOutput, CompletableFuture<Void>> output) throws
                                                                                                                                   Exception
    {
        ImagePullSession session = getApplication().getImagePullSession(token);
        if (session == null)
        {
            return wrapAsync(-2);
        }

        if (output != null)
        {
            List<ShellOutput> lst = session.getOutput();
            while (offset < lst.size())
            {
                await(output.apply(lst.get(offset++)));
            }
        }

        if (!session.isDone())
        {
            return wrapAsync(0);
        }

        if (session.getImageSha() != null)
        {
            return wrapAsync(1);
        }

        // Failed to fetch image.
        return wrapAsync(-1);
    }

    @Override
    public CompletableFuture<PullProgress> checkPullImageProgressEx2(ImagePullToken token,
                                                                     int offset,
                                                                     int count)
    {
        PullProgress res = new PullProgress();

        ImagePullSession session = getApplication().getImagePullSession(token);
        if (session == null)
        {
            res.status = PullProgressStatus.UnknownToken;
        }
        else
        {
            List<ShellOutput> lst = session.getOutput();

            res.linesOfLog = lst.size();

            while (offset < res.linesOfLog && count-- > 0)
            {
                res.log.add(lst.get(offset++));
            }

            if (!session.isDone())
            {
                res.status = PullProgressStatus.Processing;
            }
            else if (session.getImageSha() != null)
            {
                res.status = PullProgressStatus.Done;
            }
            else
            {
                // Failed to fetch image.
                res.status = PullProgressStatus.Failed;
            }
        }

        return wrapAsync(res);
    }

    @Override
    public CompletableFuture<Void> closePullImage(ImagePullToken token)
    {
        ImagePullSession session = getApplication().getImagePullSession(token);
        if (session != null)
        {
            session.stop();

            getApplication().unregisterImagePullSession(token);
        }

        return wrapAsync(null);
    }

    @Override
    public CompletableFuture<Boolean> removeImage(String image,
                                                  boolean force) throws
                                                                 Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            try
            {
                helper.removeImage(new DockerImageIdentifier(image), force);
                return true;
            }
            catch (Exception e2)
            {
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> tagImage(String sourceImage,
                                               String targetImage) throws
                                                                   Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            try
            {
                helper.tagImage(new DockerImageIdentifier(sourceImage), new DockerImageIdentifier(targetImage));
                return true;
            }
            catch (Exception e2)
            {
                return false;
            }
        });
    }

    //--//

    @Override
    public CompletableFuture<List<VolumeStatus>> listVolumes(Map<String, String> filterByLabels) throws
                                                                                                 Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            List<VolumeStatus> res = Lists.newArrayList();

            for (Volume vol : helper.listVolumes(filterByLabels))
                res.add(copyDetails(vol));

            return res;
        });
    }

    @Override
    public CompletableFuture<VolumeStatus> inspectVolume(String volumeName) throws
                                                                            Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            Volume vol = helper.inspectVolume(volumeName);
            return copyDetails(vol);
        });
    }

    @Override
    public CompletableFuture<String> createVolume(String volumeName,
                                                  Map<String, String> labels,
                                                  String driver,
                                                  Map<String, String> driverOpts) throws
                                                                                  Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            VolumesCreateBody config = new VolumesCreateBody();
            config.name = volumeName;

            if (labels != null && !labels.isEmpty())
            {
                config.labels = labels;
            }

            config.driver = driver;

            if (driverOpts != null && !driverOpts.isEmpty())
            {
                config.driverOpts = driverOpts;
            }

            Volume res = helper.createVolume(config);
            return res.name;
        });
    }

    @Override
    public CompletableFuture<Void> deleteVolume(String volumeName,
                                                boolean force) throws
                                                               Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            if (!StringUtils.isEmpty(volumeName))
            {
                DockerHelper.callWithFailureFiltering(() -> helper.deleteVolume(volumeName, force), Response.Status.CONFLICT, Response.Status.NOT_FOUND);
            }

            return null;
        });
    }

    @Override
    public CompletableFuture<String> backupVolume(String volumeName,
                                                  String relativePath,
                                                  int timeout,
                                                  TimeUnit unit) throws
                                                                 Exception
    {
        return DockerHelper.callWithHelper(null, (helper) -> backupVolumeInner(helper, volumeName, relativePath, timeout, unit));
    }

    private String backupVolumeInner(DockerHelper helper,
                                     String volumeName,
                                     String relativePath,
                                     int timeout,
                                     TimeUnit unit) throws
                                                    Exception
    {
        final String c_backupSrc = "/optio3-backup";
        final String c_backupDst = "/optio3-output";

        String outputFile = IdGenerator.newGuid() + ".tgz";
        String outputPath = normalizeRoot();

        ContainerBuilder builder = new ContainerBuilder();
        builder.setImage(getMinimalImageForExecInner());
        builder.addBind(volumeName, Paths.get(c_backupSrc));
        builder.addBind(Paths.get(outputPath), Paths.get(c_backupDst));

        //
        // tar -C <backupSrc>/<relativePath> -c -z -f <backupDst>/<outputFile>
        //
        {
            StringBuilder sb = new StringBuilder();
            sb.append("tar -C " + c_backupSrc);
            if (!StringUtils.isEmpty(relativePath))
            {
                sb.append("/");
                sb.append(relativePath);
            }

            sb.append(" -c -z -f " + c_backupDst + "/");
            sb.append(outputFile);
            sb.append(" .");
            builder.setCommandLine(sb.toString());
        }

        int exitCode = execWithTimeout(helper, builder, timeout, unit);
        if (exitCode < 0)
        {
            throw Exceptions.newRuntimeException("Backup of volume %s failed with timeout", volumeName);
        }

        if (exitCode > 0)
        {
            throw Exceptions.newRuntimeException("Backup of volume %s failed with error code %d", volumeName, exitCode);
        }

        return outputFile;
    }

    @Override
    public CompletableFuture<Void> restoreVolume(String volumeName,
                                                 String relativePath,
                                                 String inputFile,
                                                 int timeout,
                                                 TimeUnit unit) throws
                                                                Exception
    {
        return DockerHelper.callWithHelper(null, (helper) -> restoreVolumeInner(helper, volumeName, relativePath, inputFile, timeout, unit));
    }

    private Void restoreVolumeInner(DockerHelper helper,
                                    String volumeName,
                                    String relativePath,
                                    String inputFile,
                                    int timeout,
                                    TimeUnit unit) throws
                                                   Exception
    {
        final String c_backupSrc = "/optio3-input.tgz";
        final String c_backupDst = "/optio3-restore";

        MonitoredFile inputFile2 = normalizePath(inputFile);

        ContainerBuilder builder = new ContainerBuilder();
        builder.setImage(getMinimalImageForExecInner());
        builder.addBind(Paths.get(inputFile2.path), Paths.get(c_backupSrc));
        builder.addBind(volumeName, Paths.get(c_backupDst));

        //
        // tar -C <backupDst>/<relativePath> -x -z -f <backupSrc>
        //
        {
            StringBuilder sb = new StringBuilder();
            sb.append("tar -C " + c_backupDst);
            if (!StringUtils.isEmpty(relativePath))
            {
                sb.append("/");
                sb.append(relativePath);
            }

            sb.append(" -x -z -f " + c_backupSrc);

            builder.setCommandLine(sb.toString());
        }

        int exitCode = execWithTimeout(helper, builder, timeout, unit);
        if (exitCode < 0)
        {
            throw Exceptions.newRuntimeException("Restore of volume %s failed with timeout", volumeName);
        }

        if (exitCode > 0)
        {
            throw Exceptions.newRuntimeException("Restore of volume %s failed with error code %d", volumeName, exitCode);
        }

        return null;
    }

    public static VolumeStatus copyDetails(Volume vol)
    {
        VolumeStatus vs = new VolumeStatus();
        vs.name = vol.name;

        if (vol.labels != null)
        {
            vs.labels.putAll(vol.labels);
        }

        vs.driver     = vol.driver;
        vs.mountpoint = vol.mountpoint;
        return vs;
    }

    //--//

    @Override
    public CompletableFuture<List<ContainerStatus>> listContainers(Map<String, String> filterByLabels) throws
                                                                                                       Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            List<ContainerStatus> res = Lists.newArrayList();
            for (ContainerSummary summary : helper.listContainers(true, filterByLabels))
            {
                ContainerInspection container = helper.inspectContainerNoThrow(summary.id);
                if (container != null)
                {
                    res.add(copyDetails(container));
                }
            }

            return res;
        });
    }

    @Override
    public CompletableFuture<ContainerStatus> inspectContainer(String containerId) throws
                                                                                   Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            ContainerInspection container = helper.inspectContainer(containerId);
            return copyDetails(container);
        });
    }

    public static ContainerStatus copyDetails(ContainerInspection ci)
    {
        ContainerStatus cs = new ContainerStatus();

        cs.id           = ci.id;
        cs.running      = DockerHelper.isRunning(ci);
        cs.restartCount = BoxingUtils.get(ci.restartCount, 0);

        cs.image = ci.image;

        cs.name = ci.name;
        cs.labels.putAll(ci.config.labels);

        // Not sure why, but container's name always starts with a slash.
        if (cs.name.startsWith("/"))
        {
            cs.name = cs.name.substring(1);
        }

        for (MountPoint mp : ci.mounts)
            cs.mountPoints.add(copyDetails(mp));

        cs.mountPoints.sort(Comparator.comparing((mountpoint) -> mountpoint.destination));

        return cs;
    }

    public static MountPointStatus copyDetails(MountPoint mp)
    {
        MountPointStatus mps = new MountPointStatus();

        mps.type      = mp.type;
        mps.name      = mp.name;
        mps.readWrite = mp.RW == Boolean.TRUE;

        mps.source      = mp.source;
        mps.destination = mp.destination;
        mps.driver      = mp.driver;
        mps.mode        = mp.mode;

        mps.propagation = mp.propagation;

        return mps;
    }

    //--//

    @Override
    public CompletableFuture<String> createContainer(String name,
                                                     ContainerConfiguration config) throws
                                                                                    Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            if (config.networkMode != null)
            {
                switch (config.networkMode)
                {
                    case "host":
                    case "none":
                    case "bridge":
                        break;

                    default:
                        helper.createNetworkIfMissing(config.networkMode, "bridge");
                        break;
                }
            }

            ContainerBuilder builder = new ContainerBuilder();
            builder.loadFrom(config);

            return getWithHeartbeatRefresh(() -> helper.createContainer(name, builder));
        });
    }

    @Override
    public CompletableFuture<Void> startContainer(String containerId) throws
                                                                      Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            callWithHeartbeatRefresh(() -> helper.startContainer(containerId, Duration.ofSeconds(5)));
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> fetchOutput(String containerId,
                                               int limit,
                                               int batchSize,
                                               boolean removeEscapeSequences,
                                               ZonedDateTime lastOutput,
                                               AsyncFunctionWithException<List<LogEntry>, Void> progressCallback) throws
                                                                                                                  Exception
    {
        final int c_maxBatchSize = 128 * 1024;

        batchSize = Math.min(batchSize, c_maxBatchSize);

        try (DockerHelper helper = new DockerHelper(null))
        {
            List<LogEntry> res         = Lists.newArrayList();
            int            batchLength = 0;

            List<DockerHelper.LogEntry> list = helper.getLogs(containerId, true, true, lastOutput, limit);
            for (DockerHelper.LogEntry dockerLog : list)
            {
                String line = dockerLog.line;

                if (removeEscapeSequences)
                {
                    line = AnsiParser.removeEscapeSequences(line);
                }

                line = AnsiParser.removeBackspaces(line);

                if (line != null)
                {
                    LogEntry en = new LogEntry();
                    en.fd        = dockerLog.fd;
                    en.line      = line;
                    en.timestamp = dockerLog.timestamp;

                    res.add(en);

                    batchLength += line.length() + 100; // Approximate per-line overhead.

                    if (batchLength > batchSize)
                    {
                        await(progressCallback.apply(res));

                        res.clear();
                        batchLength = 0;
                    }
                }
            }

            if (!res.isEmpty())
            {
                await(progressCallback.apply(res));
            }
        }

        return wrapAsync(null);
    }

    @Override
    public CompletableFuture<Boolean> signalContainer(String containerId,
                                                      String signal) throws
                                                                     Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            return helper.signalContainer(containerId, signal) == null;
        });
    }

    @Override
    public CompletableFuture<Integer> stopContainer(String containerId) throws
                                                                        Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            ContainerInspection container = helper.inspectContainerNoThrow(containerId);
            if (container == null)
            {
                return -1;
            }

            if (StringUtils.equals(DockerHelper.getSelfDockerId(), container.id))
            {
                throw new IllegalArgumentException("Can't stop self!");
            }

            try
            {
                callWithHeartbeatRefresh(() -> helper.stopContainer(containerId, Duration.ofSeconds(5)));
            }
            catch (Exception e)
            {
                // Ignore failures, we want to proceed to killing anyways.
            }

            container = helper.inspectContainer(containerId);
            return container.state.exitCode;
        });
    }

    @Override
    public CompletableFuture<Integer> getContainerExitCode(String containerId) throws
                                                                               Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            ContainerInspection inspect = helper.inspectContainerNoThrow(containerId);
            if (inspect == null)
            {
                return -1;
            }

            if (DockerHelper.isRunning(inspect) || DockerHelper.isPaused(inspect))
            {
                // Still running, no exit code available.
                return null;
            }

            return inspect.state.exitCode;
        });
    }

    @Override
    public CompletableFuture<ContainerStatus> removeContainer(String containerId) throws
                                                                                  Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            if (StringUtils.isEmpty(containerId))
            {
                return null;
            }

            ContainerInspection container = helper.inspectContainerNoThrow(containerId);
            if (container == null)
            {
                return null;
            }

            if (StringUtils.equals(DockerHelper.getSelfDockerId(), container.id))
            {
                throw new IllegalArgumentException("Can't terminate self!");
            }

            callWithHeartbeatRefresh(() -> helper.deleteContainer(containerId, true, true));

            return copyDetails(container);
        });
    }

    //--//

    @Override
    public CompletableFuture<String> readContainerFileSystemToTar(String containerId,
                                                                  String containerPath,
                                                                  boolean compress) throws
                                                                                    Exception
    {
        return DockerHelper.callWithHelper(null, (helper) ->
        {
            String        outputFileName = IdGenerator.newGuid() + (compress ? ".tgz" : ".tar");
            MonitoredFile outputPath     = normalizePath(outputFileName);

            try (FileSystem.TmpFileHolder outputFile = FileSystem.autoDelete(outputPath.path))
            {
                if (helper.readArchiveFromContainer(containerId, containerPath, outputFile.get(), compress))
                {
                    outputFile.disableAutoDelete();
                    return outputFileName;
                }

                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> writeContainerFileSystemFromTar(String containerId,
                                                                      String containerPath,
                                                                      String inputFile,
                                                                      boolean decompress) throws
                                                                                          Exception
    {
        return DockerHelper.callWithHelper(null, (helper) ->
        {
            helper.ensureDirectory(containerId, containerPath);

            MonitoredFile inputPath = normalizePath(inputFile);
            return helper.writeArchiveToContainer(containerId, containerPath, inputPath.target, decompress);
        });
    }

    //--//

    @Override
    public CompletableFuture<BatchToken> prepareBatch(DockerBatch.Request request) throws
                                                                                   Exception
    {
        DeployerApplication app = getApplication();

        BatchSession session = new BatchSession(app, request.items);
        BatchToken   token   = app.registerBatchSession(session);

        return wrapAsync(token);
    }

    @Override
    public CompletableFuture<DockerBatch.Report> startBatch(BatchToken token) throws
                                                                              Exception
    {
        BatchSession session = getApplication().getBatchSession(token);
        if (session == null)
        {
            return wrapAsync(null);
        }

        session.start();

        DockerBatch.Report report = new DockerBatch.Report();
        return wrapAsync(report);
    }

    @Override
    public CompletableFuture<DockerBatch.Report> checkBatchProgress(BatchToken token,
                                                                    int offset,
                                                                    FunctionWithException<ShellOutput, CompletableFuture<Void>> output) throws
                                                                                                                                        Exception
    {
        BatchSession session = getApplication().getBatchSession(token);
        if (session == null)
        {
            return wrapAsync(null);
        }

        if (output != null)
        {
            List<ShellOutput> lst = session.getOutput();
            while (offset < lst.size())
            {
                await(output.apply(lst.get(offset++)));
            }
        }

        CompletableFuture<List<DockerBatch.BaseResult>> done = session.getDone();
        if (done.isCompletedExceptionally())
        {
            return wrapAsync(null);
        }

        DockerBatch.Report report = new DockerBatch.Report();

        if (done.isDone())
        {
            report.results = await(done);
        }

        return wrapAsync(report);
    }

    @Override
    public CompletableFuture<Void> closeBatch(BatchToken token)
    {
        BatchSession session = getApplication().getBatchSession(token);
        if (session != null)
        {
            session.stop();

            getApplication().unregisterBatchSession(token);
        }

        return wrapAsync(null);
    }

    //--//

    private String getMinimalImageForExecInner()
    {
        DockerImageArchitecture arch = FirmwareHelper.architecture();
        if (arch != null)
        {
            if (arch.isArm32())
            {
                return "armhf/debian:jessie-slim";
            }

            return "busybox";
        }

        return null;
    }

    private static int execWithTimeout(DockerHelper helper,
                                       ContainerBuilder builder,
                                       int timeout,
                                       TimeUnit unit)
    {
        String containerId = helper.createContainer(null, builder);
        try
        {
            helper.startContainer(containerId, null);

            MonotonousTime maxExecutionTime = TimeUtils.computeTimeoutExpiration(timeout, unit);

            while (true)
            {
                ContainerInspection inspect = helper.inspectContainer(containerId);
                if (DockerHelper.isRunning(inspect) || DockerHelper.isPaused(inspect))
                {
                    // Still running, no exit code available.
                    Executors.safeSleep(250);

                    if (TimeUtils.isTimeoutExpired(maxExecutionTime))
                    {
                        return -1;
                    }
                }
                else
                {
                    return inspect.state.exitCode;
                }
            }
        }
        finally
        {
            helper.deleteContainer(containerId, true, true);
        }
    }

    private <T> T getWithHeartbeatRefresh(Callable<T> callback) throws
                                                                Exception
    {
        try
        {
            return callback.call();
        }
        finally
        {
            getApplication().flushHeartbeat(false);
        }
    }

    private void callWithHeartbeatRefresh(RunnableWithException callback) throws
                                                                          Exception
    {
        try
        {
            callback.run();
        }
        finally
        {
            getApplication().flushHeartbeat(false);
        }
    }
}
