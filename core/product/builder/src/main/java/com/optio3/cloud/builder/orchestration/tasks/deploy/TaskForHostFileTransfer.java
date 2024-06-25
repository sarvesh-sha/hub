/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.archive.TarBuilder;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostFileRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.directory.SshKey;
import com.optio3.util.Exceptions;
import com.optio3.util.FileSystem;
import com.optio3.util.TimeUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class TaskForHostFileTransfer extends BaseDeployTask
{
    public RecordLocator<DeploymentHostFileRecord> loc_file;
    public String                                  path;
    public boolean                                 upload;

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        DeploymentHostFileRecord file,
                                                        boolean upload) throws
                                                                        Exception
    {
        Exceptions.requireNotNull(file, InvalidArgumentException.class, "No file provided");

        DeploymentHostRecord targetHost = file.getDeployment();
        Exceptions.requireNotNull(targetHost, InvalidArgumentException.class, "No host provided");

        RecordLocked<DeploymentHostRecord> lock_targetHost = sessionHolder.optimisticallyUpgradeToLocked(targetHost, 2, TimeUnit.MINUTES);

        return BaseDeployTask.scheduleActivity(lock_targetHost, null, TaskForHostFileTransfer.class, (t) ->
        {
            t.loc_file = sessionHolder.createLocator(file);
            t.path     = file.getPath();
            t.upload   = upload;
        });
    }

    //--//

    @Override
    public void configureContext()
    {
        loggerInstance = DeploymentHostRecord.buildContextualLogger(loggerInstance, getTargetHostLocator());
    }

    @Override
    public String getTitle()
    {
        return String.format(upload ? "Upload file '%s' to host '%s'" : "Download file '%s' from host '%s'", path, getHostDisplayName());
    }

    @BackgroundActivityMethod(autoRetry = true)
    public CompletableFuture<Void> process() throws
                                             Exception
    {
        BuilderConfiguration cfg = getServiceNonNull(BuilderConfiguration.class);

        DeployLogicForAgent agentLogic = getLogicForAgent();

        SshKey key = agentLogic.getSshKey();
        if (key == null)
        {
            return markAsFailed("Unexpected architecture: %s", agentLogic.host_architecture);
        }

        class State
        {
            DeployLogicForAgent.TransferProgress<?> transferProgress;
            String                                  dockerId;
            long                                    length;
        }

        State state = withLocatorReadonlyOrNull(loc_file, (sessionHolder, rec_file) ->
        {
            State newState = new State();
            newState.transferProgress = createTransferTracker(loggerInstance, getHostId(), rec_file.getPath());

            var rec_task = rec_file.getTask();
            if (rec_task != null)
            {
                newState.dockerId = rec_task.getDockerId();
            }

            newState.length = rec_file.getLength();

            return newState;
        });

        Path file     = Path.of(path);
        Path dir      = file.getParent();
        Path fileName = file.getFileName();

        int timeoutInHours = getArchitecture().isArm32() ? 24 : 2;

        if (upload)
        {
            InputStream stream = withLocatorReadonly(loc_file, (sessionHolder, rec_file) ->
            {
                return rec_file.readAsStream(getSessionProvider());
            });

            long length = withLocatorReadonly(loc_file, (sessionHolder, rec_file) ->
            {
                return rec_file.getLength();
            });

            if (state.dockerId != null)
            {
                try (FileSystem.TmpFileHolder holder = FileSystem.createTempFile())
                {
                    try (FileOutputStream streamTar = new FileOutputStream(holder.get()))
                    {
                        try (TarBuilder builder = new TarBuilder(streamTar, true))
                        {
                            builder.addAsStream(null, fileName.toString(), stream, (int) state.length, 0664);
                        }
                    }

                    await(agentLogic.writeContainerFileSystemFromTar(state.dockerId, dir, holder.get(), true, timeoutInHours * 60, state.transferProgress));
                }
            }
            else
            {
                await(agentLogic.writeStreamToOuterFileSystem(key.user,
                                                              key.getPrivateKey(),
                                                              key.getPublicKey(),
                                                              key.passphrase.getBytes(),
                                                              stream,
                                                              length,
                                                              path,
                                                              timeoutInHours * 60,
                                                              state.transferProgress));
            }

            withLocatorOrNull(loc_file, (sessionHolder, rec_file) ->
            {
                if (rec_file != null)
                {
                    rec_file.setUploadedOn(TimeUtils.now());
                }
            });
        }
        else
        {
            withLocatorOrNull(loc_file, (sessionHolder, rec_file) ->
            {
                if (rec_file != null)
                {
                    rec_file.deleteContents(sessionHolder);
                }
            });

            try (OutputStream stream = DeploymentHostFileRecord.writeAsStream(getSessionProvider(), loc_file))
            {
                if (state.dockerId != null)
                {
                    await(agentLogic.enumerateContainerFileSystem(state.dockerId, file, false, timeoutInHours * 60, state.transferProgress, (entry) ->
                    {
                        if (StringUtils.equals(entry.name, fileName.toString()))
                        {
                            IOUtils.copyLarge(entry.getStream(), stream);
                        }

                        return true;
                    }));
                }
                else
                {
                    await(agentLogic.readStreamFromOuterFileSystem(key.user,
                                                                   key.getPrivateKey(),
                                                                   key.getPublicKey(),
                                                                   key.passphrase.getBytes(),
                                                                   stream,
                                                                   path,
                                                                   timeoutInHours * 60,
                                                                   state.transferProgress));
                }
            }

            withLocatorOrNull(loc_file, (sessionHolder, rec_file) ->
            {
                if (rec_file != null)
                {
                    rec_file.setDownloadedOn(TimeUtils.now());
                }
            });
        }

        return markAsCompleted();
    }
}
