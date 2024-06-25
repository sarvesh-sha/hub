/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer.logic;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.NotFoundException;

import com.google.common.collect.Lists;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.batch.DockerBatch;
import com.optio3.cloud.client.deployer.model.batch.DockerBatchForContainerLaunch;
import com.optio3.cloud.client.deployer.model.batch.DockerBatchForContainerTerminate;
import com.optio3.cloud.client.deployer.model.batch.DockerBatchForVolumeCreate;
import com.optio3.cloud.client.deployer.model.batch.DockerBatchForVolumeDelete;
import com.optio3.cloud.deployer.DeployerApplication;
import com.optio3.infra.docker.ContainerBuilder;
import com.optio3.infra.docker.DockerHelper;
import com.optio3.infra.docker.model.ContainerInspection;
import com.optio3.infra.docker.model.MountPoint;
import com.optio3.infra.docker.model.Volume;
import com.optio3.infra.docker.model.VolumesCreateBody;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

public class BatchSessionWorker
{
    private final DeployerApplication m_app;
    private final List<DockerBatch>   m_list;
    private final Callable<Boolean>   m_shutdownCheck;
    private final Consumer<String>    m_addLine;

    public BatchSessionWorker(DeployerApplication app,
                              List<DockerBatch> list,
                              Callable<Boolean> shutdownCheck,
                              Consumer<String> addLine)
    {
        m_app           = app;
        m_list          = list;
        m_shutdownCheck = shutdownCheck;
        m_addLine       = addLine;
    }

    CompletableFuture<List<DockerBatch.BaseResult>> execute() throws
                                                              Exception
    {
        List<DockerBatch.BaseResult> results = Lists.newArrayList();

        Boolean res = null;

        for (DockerBatch batch : m_list)
        {
            await(sleep(10, TimeUnit.MILLISECONDS));

            res = handle(res, results, batch, DockerBatchForVolumeCreate.class, DockerBatchForVolumeCreate.Result.class, 3, this::handleVolumeCreate, (request, result, t) ->
            {
                reportProgress("Failed to create volume '%s', due to %s", request.volumeName, result.failure);
            });

            res = handle(res, results, batch, DockerBatchForVolumeDelete.class, DockerBatchForVolumeDelete.Result.class, 3, this::handleVolumeDelete, (request, result, t) ->
            {
                reportProgress("Failed to delete volume '%s', due to %s", request.volumeName, result.failure);
            });

            res = handle(res, results, batch, DockerBatchForContainerLaunch.class, DockerBatchForContainerLaunch.Result.class, 3, this::handleContainerStart, (request, result, t) ->
            {
                reportProgress("Failed to start container with image '%s', due to %s", request.config.image, result.failure);
            });

            res = handle(res, results, batch, DockerBatchForContainerTerminate.class, DockerBatchForContainerTerminate.Result.class, 3, this::handleContainerStop, (request, result, t) ->
            {
                reportProgress("Failed to stop container '%s', due to %s", request.dockerId, result.failure);
            });
        }

        m_app.flushHeartbeat(false);

        return wrapAsync(results);
    }

    //--//

    private void reportProgress(String fmt,
                                Object... args)
    {
        m_addLine.accept(String.format(fmt, args));
    }

    //--//

    @FunctionalInterface
    interface BatchProcessor<Req extends DockerBatch, Res extends DockerBatch.BaseResult>
    {
        void run(DockerHelper dockerHelper,
                 Req request,
                 Res result,
                 int pass) throws
                           Exception;
    }

    @FunctionalInterface
    interface BatchProcessorFailure<Req extends DockerBatch, Res extends DockerBatch.BaseResult>
    {
        void run(Req request,
                 Res result,
                 Throwable t);
    }

    private <Req extends DockerBatch, Res extends DockerBatch.BaseResult> Boolean handle(Boolean res,
                                                                                         List<DockerBatch.BaseResult> results,
                                                                                         DockerBatch batch,
                                                                                         Class<Req> requestClass,
                                                                                         Class<Res> resultClass,
                                                                                         int maxRetries,
                                                                                         BatchProcessor<Req, Res> callback,
                                                                                         BatchProcessorFailure<Req, Res> failureCallback)
    {
        Req batchTyped = Reflection.as(batch, requestClass);
        if (batchTyped == null)
        {
            return res;
        }

        Res result = Reflection.newInstance(resultClass);
        results.add(result);

        if (res != null && !res)
        {
            result.cancelled = true;
            return res;
        }

        int pass = 0;

        reportProgress("Processing %s", batch);
        while (true)
        {
            try (DockerHelper helper = new DockerHelper(null))
            {
                callback.run(helper, batchTyped, result, pass);

                return result.failure == null;
            }
            catch (Throwable e)
            {
                reportProgress("Failed at pass %d: %s", pass, e.getMessage());

                if (pass++ < maxRetries)
                {
                    continue;
                }

                result.failure = e.getMessage();

                if (failureCallback != null)
                {
                    failureCallback.run(batchTyped, result, e);
                }

                return false;
            }
        }
    }

    //--//

    private void handleVolumeCreate(DockerHelper dockerHelper,
                                    DockerBatchForVolumeCreate request,
                                    DockerBatchForVolumeCreate.Result result,
                                    int pass)
    {
        VolumesCreateBody config = new VolumesCreateBody();
        config.name = request.volumeName;

        if (request.labels != null && !request.labels.isEmpty())
        {
            config.labels = request.labels;
        }

        config.driver = request.driver;

        if (request.driverOpts != null && !request.driverOpts.isEmpty())
        {
            config.driverOpts = request.driverOpts;
        }

        Volume volume = dockerHelper.createVolume(config);
        result.id = volume.name;
    }

    private void handleVolumeDelete(DockerHelper dockerHelper,
                                    DockerBatchForVolumeDelete request,
                                    DockerBatchForVolumeDelete.Result result,
                                    int pass)
    {
        if (!StringUtils.isEmpty(request.volumeName))
        {
            dockerHelper.deleteVolume(request.volumeName, request.force);
        }
    }

    private void handleContainerStart(DockerHelper dockerHelper,
                                      DockerBatchForContainerLaunch request,
                                      DockerBatchForContainerLaunch.Result result,
                                      int pass) throws
                                                Exception
    {
        ContainerConfiguration config = request.config;

        if (config.networkMode != null)
        {
            switch (config.networkMode)
            {
                case "host":
                case "none":
                case "bridge":
                    break;

                default:
                    dockerHelper.createNetworkIfMissing(config.networkMode, "bridge");
                    break;
            }
        }

        ContainerBuilder builder = new ContainerBuilder();
        builder.loadFrom(config);

        result.dockerId = dockerHelper.createContainer(request.name, builder);

        if (request.configurationFiles != null)
        {
            for (DockerBatchForContainerLaunch.FileSystemInit configurationFile : request.configurationFiles)
            {
                if (!dockerHelper.writeArchiveToContainer(result.dockerId, configurationFile.containerPath, new ByteArrayInputStream(configurationFile.input), configurationFile.decompress))
                {
                    result.failure = String.format("Can't write configuration file '%s'", configurationFile.containerPath);
                    return;
                }
            }
        }

        dockerHelper.startContainer(result.dockerId, Duration.ofSeconds(5));
    }

    private void handleContainerStop(DockerHelper dockerHelper,
                                     DockerBatchForContainerTerminate request,
                                     DockerBatchForContainerTerminate.Result result,
                                     int pass)
    {
        try
        {
            if (StringUtils.isEmpty(request.dockerId))
            {
                return;
            }

            if (StringUtils.equals(DockerHelper.getSelfDockerId(), request.dockerId))
            {
                result.failure = "Can't terminate self!";
                return;
            }

            ContainerInspection container = dockerHelper.inspectContainer(request.dockerId);

            dockerHelper.deleteContainer(request.dockerId, true, true);

            for (MountPoint mount : container.mounts)
            {
                if (StringUtils.equals(mount.type, "volume"))
                {
                    dockerHelper.deleteVolume(mount.name, false);
                }
            }
        }
        catch (NotFoundException e)
        {
            // Not there, we are good!
        }
    }
}
