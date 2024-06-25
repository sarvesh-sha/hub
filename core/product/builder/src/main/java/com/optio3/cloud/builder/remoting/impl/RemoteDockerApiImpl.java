/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.remoting.impl;

import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.builder.remoting.RemoteDockerApi;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.infra.directory.UserInfo;
import com.optio3.infra.docker.ContainerBuilder;
import com.optio3.infra.docker.DockerHelper;
import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.infra.docker.model.BuildInfo;
import com.optio3.infra.docker.model.ContainerInspection;
import com.optio3.infra.docker.model.Image;
import com.optio3.infra.docker.model.Volume;
import com.optio3.infra.docker.model.VolumesCreateBody;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.AsyncFunctionWithException;

@Optio3RemotableEndpoint(itf = RemoteDockerApi.class)
public final class RemoteDockerApiImpl implements RemoteDockerApi
{
    @Override
    public CompletableFuture<String> createVolume(String name,
                                                  Map<String, String> labels,
                                                  String driver,
                                                  Map<String, String> driverOpts) throws
                                                                                  Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(2, null, (helper) ->
        {
            VolumesCreateBody config = new VolumesCreateBody();
            config.name = name;

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
    public CompletableFuture<Void> deleteVolume(String volumeId,
                                                boolean force) throws
                                                               Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            try
            {
                helper.deleteVolume(volumeId, force);
            }
            catch (ClientErrorException e)
            {
                switch (DockerHelper.extractResponseStatus(e))
                {
                    case NO_CONTENT:
                    case NOT_FOUND:
                        return null;

                    case CONFLICT:
                    default:
                        throw e;
                }
            }

            return null;
        });
    }

    @Override
    public CompletableFuture<String> getVolumeMountPoint(String volumeName) throws
                                                                            Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            try
            {
                Volume state = helper.inspectVolume(volumeName);

                return state.mountpoint;
            }
            catch (ClientErrorException e)
            {
                Response response = e.getResponse();
                switch (Response.Status.fromStatusCode(response.getStatus()))
                {
                    case NO_CONTENT:
                    case NOT_FOUND:
                        return null;

                    case CONFLICT:
                    default:
                        throw e;
                }
            }
        });
    }
    //--//

    @Override
    public CompletableFuture<String> createContainer(String containerName,
                                                     ContainerConfiguration config) throws
                                                                                    Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(2, null, (helper) ->
        {
            ContainerBuilder builder = new ContainerBuilder();
            builder.loadFrom(config);

            return helper.createContainer(containerName, builder);
        });
    }

    @Override
    public CompletableFuture<Void> startContainer(String containerId) throws
                                                                      Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            helper.startContainer(containerId, Duration.ofSeconds(5));

            return null;
        });
    }

    @Override
    public CompletableFuture<ZonedDateTime> fetchOutput(String containerId,
                                                        ZonedDateTime lastOutput,
                                                        AsyncFunctionWithException<DockerHelper.LogEntry, Void> progressCallback) throws
                                                                                                                                  Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            LogConverter  log           = new LogConverter(progressCallback);
            ZonedDateTime newLastOutput = lastOutput;

            List<DockerHelper.LogEntry> list = helper.getLogs(containerId, true, true, newLastOutput, 500);
            for (DockerHelper.LogEntry dockerLog : list)
            {
                log.addLine(dockerLog.fd, dockerLog.timestamp, dockerLog.line);

                newLastOutput = dockerLog.timestamp;
            }

            return newLastOutput;
        });
    }

    @Override
    public CompletableFuture<Integer> stopContainer(String containerId) throws
                                                                        Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            helper.stopContainer(containerId, Duration.ofSeconds(5));

            ContainerInspection inspect = helper.inspectContainerNoThrow(containerId);
            if (inspect == null)
            {
                return -1;
            }

            helper.deleteContainer(containerId, true, true);

            return inspect.state.exitCode;
        });
    }

    @Override
    public CompletableFuture<Integer> getExitCode(String containerId) throws
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
    public CompletableFuture<Void> deleteImage(DockerImageIdentifier image,
                                               boolean force) throws
                                                              Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            helper.removeImage(image, force);

            return null;
        });
    }

    @Override
    public CompletableFuture<Image> pullImage(DockerImageIdentifier image,
                                              UserInfo credentials,
                                              AsyncFunctionWithException<DockerHelper.LogEntry, Void> progressCallback) throws
                                                                                                                        Exception
    {
        return DockerHelper.callWithHelper(credentials, (helper) ->
        {
            LogConverter converter = new LogConverter(progressCallback);

            Image newImage = helper.pullImage(image, (bi) ->
            {
                converter.addBuildInfo(bi);
            });

            return newImage;
        });
    }

    @Override
    public CompletableFuture<String> buildImage(Path sourceDirectory,
                                                String dockerFile,
                                                Map<String, String> buildargs,
                                                String registryAddress,
                                                UserInfo registryUser,
                                                Map<String, String> labels,
                                                ZonedDateTime overrideTime,
                                                AsyncFunctionWithException<DockerHelper.LogEntry, Void> progressCallback) throws
                                                                                                                          Exception
    {
        return DockerHelper.callWithHelper(registryUser, (helper) ->
        {
            LogConverter converter = new LogConverter(progressCallback);

            String imageTag = helper.buildImage(sourceDirectory, dockerFile, buildargs, registryAddress, labels, overrideTime, (bi) ->
            {
                converter.addBuildInfo(bi);
            });

            return imageTag;
        });
    }

    @Override
    public CompletableFuture<Image> inspectImage(DockerImageIdentifier image) throws
                                                                              Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            return helper.inspectImage(image);
        });
    }

    @Override
    public CompletableFuture<DockerImageArchitecture> inspectImageArchitecture(DockerImageIdentifier image) throws
                                                                                                            Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            return helper.inspectImageArchitecture(image);
        });
    }

    @Override
    public CompletableFuture<Boolean> tagImage(DockerImageIdentifier source,
                                               DockerImageIdentifier target) throws
                                                                             Exception
    {
        return DockerHelper.callWithHelperAndAutoRetry(3, null, (helper) ->
        {
            helper.tagImage(source, target);

            return true;
        });
    }

    @Override
    public CompletableFuture<String> pushImage(DockerImageIdentifier imageParsed,
                                               UserInfo credentials,
                                               AsyncFunctionWithException<DockerHelper.LogEntry, Void> progressCallback) throws
                                                                                                                         Exception
    {
        return DockerHelper.callWithHelper(credentials, (helper) ->
        {
            LogConverter converter = new LogConverter(progressCallback);

            return helper.pushImage(imageParsed, (bi) ->
            {
                converter.addBuildInfo(bi);
            });
        });
    }

    //--//

    private class LogConverter
    {
        private final AsyncFunctionWithException<DockerHelper.LogEntry, Void> m_progressCallback;

        public LogConverter(AsyncFunctionWithException<DockerHelper.LogEntry, Void> progressCallback)
        {
            m_progressCallback = progressCallback;
        }

        void addBuildInfo(BuildInfo bi) throws
                                        Exception
        {
            if (bi.stream != null)
            {
                addLine(bi.stream);
            }
            else if (bi.error != null)
            {
                addLine(String.format("ERROR: %s", bi.error));
            }
            else if (bi.status != null)
            {
                addLine(String.format("STATUS: %s %s", bi.status, bi.progress));
            }
            else if (bi.errorDetail != null)
            {
                addLine(String.format("ERR: %s", bi.errorDetail.message));
            }
        }

        void addLine(String line) throws
                                  Exception
        {
            addLine(1, TimeUtils.now(), line);
        }

        void addLine(int fd,
                     ZonedDateTime timestamp,
                     String line) throws
                                  Exception
        {
            if (m_progressCallback != null)
            {
                DockerHelper.LogEntry en = new DockerHelper.LogEntry();
                en.fd = fd;
                en.timestamp = timestamp;
                en.line = line;
                m_progressCallback.apply(en)
                                  .get();
            }
        }
    }
}
