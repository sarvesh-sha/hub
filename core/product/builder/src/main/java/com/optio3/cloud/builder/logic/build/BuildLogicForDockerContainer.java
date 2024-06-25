/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.logic.build;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotFoundException;

import com.google.common.collect.Lists;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.builder.persistence.worker.DockerContainerRecord;
import com.optio3.cloud.builder.persistence.worker.DockerTemporaryImageRecord;
import com.optio3.cloud.builder.persistence.worker.DockerVolumeRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.builder.persistence.worker.ManagedDirectoryRecord;
import com.optio3.cloud.builder.persistence.worker.MappedDockerVolumeRecord;
import com.optio3.cloud.builder.remoting.RemoteDockerApi;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogHolder;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.WellKnownEnvironmentVariable;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.infra.docker.ContainerBuilder;
import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.infra.docker.model.Image;
import com.optio3.util.Exceptions;
import com.optio3.util.IdGenerator;

public class BuildLogicForDockerContainer extends BaseBuildLogicWithJob
{
    private static final String OPTIO3_BUILDER_WORKER = "Optio3_BuilderWorker";

    public class MappingConfig
    {
        public Path path;

        public ManagedDirectoryRecord directory;
        public DockerVolumeRecord     volume;
    }

    private final SessionHolder m_sessionHolder;

    private ContainerConfiguration m_createConfig;
    private List<MappingConfig>    m_mapped = Lists.newArrayList();

    public BuildLogicForDockerContainer(BuilderConfiguration config,
                                        SessionHolder sessionHolder,
                                        HostRecord targetHost,
                                        JobRecord job)
    {
        super(config, targetHost, job);

        m_sessionHolder = sessionHolder;
    }

    public BuildLogicForDockerContainer(BuilderConfiguration config,
                                        SessionHolder sessionHolder,
                                        JobStepRecord step)
    {
        super(config, step.getOwningHost(), step.getOwningJob());

        m_sessionHolder = sessionHolder;
    }

    //--//

    public BuildLogicForDockerContainer allowAccessToDockerDaemon()
    {
        ensureMainConfig().allowAccessToDockerDaemon = true;

        return this;
    }

    public String getImage()
    {
        return ensureMainConfig().image;
    }

    public BuildLogicForDockerContainer setImage(String image)
    {
        requireNonNull(image);

        ensureMainConfig().image = image;

        return this;
    }

    public Path getWorkingDir()
    {
        return ensureMainConfig().workingDir;
    }

    public BuildLogicForDockerContainer setWorkingDir(Path workingDir)
    {
        requireNonNull(workingDir);

        ensureMainConfig().workingDir = workingDir;

        return this;
    }

    public BuildLogicForDockerContainer setCommandLine(String line)
    {
        ensureMainConfig().overrideCommandLine(line);

        return this;
    }

    public BuildLogicForDockerContainer setCommand(String... args)
    {
        ensureMainConfig().overrideCommands(args);

        return this;
    }

    public void addEnvironmentVariable(String name,
                                       String value)
    {
        ensureMainConfig().environmentVariables.put(name, value);
    }

    public <T> void addEnvironmentVariable(WellKnownEnvironmentVariable name,
                                           T value)
    {
        name.setValue(ensureMainConfig().environmentVariables, value);
    }

    //--//

    public BuildLogicForDockerContainer addBind(ManagedDirectoryRecord hostPath,
                                                Path containerPath)
    {
        return addBind(hostPath, containerPath, null);
    }

    public BuildLogicForDockerContainer addBind(ManagedDirectoryRecord hostPath,
                                                Path containerPath,
                                                String file)
    {
        requireNonNull(hostPath);
        requireNonNull(containerPath);

        MappingConfig cfg = new MappingConfig();
        cfg.path      = containerPath;
        cfg.directory = hostPath;
        m_mapped.add(cfg);

        return addBind(hostPath.getPath(), containerPath, file);
    }

    public BuildLogicForDockerContainer addBind(Path hostPath,
                                                Path guestPath)
    {
        return addBind(hostPath, guestPath, null);
    }

    public BuildLogicForDockerContainer addBind(Path hostPath,
                                                Path guestPath,
                                                String file)
    {
        requireNonNull(hostPath);
        requireNonNull(guestPath);

        if (file != null)
        {
            hostPath  = hostPath.resolve(file);
            guestPath = guestPath.resolve(file);
        }

        ensureMainConfig().addBind(hostPath, guestPath);

        return this;
    }

    public BuildLogicForDockerContainer addBind(DockerVolumeRecord volume,
                                                Path guestPath) throws
                                                                Exception
    {
        requireNonNull(volume);
        requireNonNull(guestPath);

        MappingConfig cfg = new MappingConfig();
        cfg.path   = guestPath;
        cfg.volume = volume;
        m_mapped.add(cfg);

        //--//

        ensureMainConfig().addBind(volume.getDockerId(), guestPath);

        return this;
    }

    public Path mapFromGuestToHost(Path guestPath)
    {
        requireNonNull(guestPath);

        return ensureMainConfig().mapFromGuestToHost(guestPath);
    }

    //--//

    public BuildLogicForDockerContainer addPort(int hostPort,
                                                int containerPort,
                                                boolean useUDP)
    {
        ensureMainConfig().addPort(hostPort, containerPort, useUDP);

        return this;
    }

    //--//

    /**
     * Given an image, it returns the account to use for interacting with its origin registry.
     *
     * @param imageParsed Image spec.
     * @param forPush     If true, search for an account with write permissions.
     *
     * @return Credentials for the repository
     */
    public UserInfo getCredentialForRepo(DockerImageIdentifier imageParsed,
                                         boolean forPush)
    {
        requireNonNull(imageParsed);

        return getCredentialForRepo(imageParsed.registryHost, forPush);
    }

    /**
     * Given a registry address, it returns the account to use for interacting with it.
     *
     * @param registryAddress Network address of Docker Registry.
     * @param forPush         If true, search for an account with write permissions.
     *
     * @return Credentials for the repository
     */
    public UserInfo getCredentialForRepo(String registryAddress,
                                         boolean forPush)
    {
        if (registryAddress == null)
        {
            return null;
        }

        int port = registryAddress.indexOf(':');
        if (port >= 0)
        {
            registryAddress = registryAddress.substring(0, port);
        }

        try
        {
            return getCredentialForHost(registryAddress, forPush ? RoleType.Publisher : RoleType.Subscriber);
        }
        catch (NotFoundException e)
        {
            throw Exceptions.newRuntimeException("No credentials for Docker Registry %s", registryAddress);
        }
    }

    //--//

    public Image inspectImage(String image) throws
                                            Exception
    {
        DockerImageIdentifier imageParsed = new DockerImageIdentifier(image);

        RemoteDockerApi proxy = getProxy(RemoteDockerApi.class);

        return getAndUnwrapException(proxy.inspectImage(imageParsed));
    }

    public DockerImageArchitecture inspectImageArchitecture(String image) throws
                                                                          Exception
    {
        DockerImageIdentifier imageParsed = new DockerImageIdentifier(image);

        RemoteDockerApi proxy = getProxy(RemoteDockerApi.class);

        return getAndUnwrapException(proxy.inspectImageArchitecture(imageParsed));
    }

    public void removeImage(String image,
                            boolean force) throws
                                           Exception
    {
        DockerImageIdentifier imageParsed = new DockerImageIdentifier(image);

        RemoteDockerApi proxy = getProxy(RemoteDockerApi.class);

        getAndUnwrapException(proxy.deleteImage(imageParsed, force));
    }

    public Image pullImage(RecordLocked<JobStepRecord> lock_step,
                           String image) throws
                                         Exception
    {
        DockerImageIdentifier imageParsed = new DockerImageIdentifier(image);

        UserInfo userInfo = getCredentialForRepo(imageParsed, false);

        RemoteDockerApi proxy = getProxy(RemoteDockerApi.class);

        try (var logHandler = JobStepRecord.allocateLogHandler(lock_step))
        {
            try (LogHolder log = logHandler.newLogHolder())
            {
                return getAndUnwrapException(proxy.pullImage(imageParsed, userInfo, (en) -> log.addLineAsync(en.fd, en.timestamp, null, null, null, null, en.line)));
            }
        }
    }

    public String buildImage(RecordLocked<JobStepRecord> lock_step,
                             Path sourceDirectory,
                             String dockerFile,
                             Map<String, String> buildargs,
                             String registryAddress,
                             Map<String, String> labels,
                             ZonedDateTime overrideTime) throws
                                                         Exception
    {
        UserInfo userInfo = getCredentialForRepo(registryAddress, false);

        RemoteDockerApi proxy = getProxy(RemoteDockerApi.class);

        String temporaryImageTag;

        try (var logHandler = JobStepRecord.allocateLogHandler(lock_step))
        {
            try (LogHolder log = logHandler.newLogHolder())
            {
                temporaryImageTag = getAndUnwrapException(proxy.buildImage(sourceDirectory,
                                                                           dockerFile,
                                                                           buildargs,
                                                                           registryAddress,
                                                                           userInfo,
                                                                           labels,
                                                                           overrideTime,
                                                                           (en) -> log.addLineAsync(en.fd, en.timestamp, null, null, null, null, en.line)));
            }
        }

        //
        // Track the temporary image, so it will be deleted at the end of the job.
        //
        RecordHelper<DockerTemporaryImageRecord> helperTempImage = m_sessionHolder.createHelper(DockerTemporaryImageRecord.class);
        DockerTemporaryImageRecord               rec             = DockerTemporaryImageRecord.newInstance(m_targetHost, temporaryImageTag);
        rec.setDeleteOnRelease(true);
        helperTempImage.persist(rec);
        rec.acquire(m_job);

        return temporaryImageTag;
    }

    public boolean tagImage(String source,
                            String target) throws
                                           Exception
    {
        DockerImageIdentifier sourceParsed = new DockerImageIdentifier(source);
        DockerImageIdentifier targetParsed = new DockerImageIdentifier(target);

        RemoteDockerApi proxy = getProxy(RemoteDockerApi.class);

        return getAndUnwrapException(proxy.tagImage(sourceParsed, targetParsed));
    }

    public Image pushImage(RecordLocked<JobStepRecord> lock_step,
                           String image) throws
                                         Exception
    {
        DockerImageIdentifier imageParsed = new DockerImageIdentifier(image);

        UserInfo userInfo = getCredentialForRepo(imageParsed, true);

        RemoteDockerApi proxy = getProxy(RemoteDockerApi.class);

        // Make sure the image exists.
        Image imageDetails = getAndUnwrapException(proxy.inspectImage(imageParsed));

        try (var logHandler = JobStepRecord.allocateLogHandler(lock_step))
        {
            try (LogHolder log = logHandler.newLogHolder())
            {
                String error = getAndUnwrapException(proxy.pushImage(imageParsed, userInfo, (en) -> log.addLineAsync(en.fd, en.timestamp, null, null, null, null, en.line)));
                if (error != null)
                {
                    throw Exceptions.newRuntimeException("Failed to push image %s, got error '%s'", image, error);
                }
            }
        }

        return imageDetails;
    }

    //--//

    public void createContainer(JobStepRecord step) throws
                                                    Exception
    {
        DockerContainerRecord rec = DockerContainerRecord.newInstance(m_targetHost);

        ContainerConfiguration config = ensureMainConfig();
        config.labels.put(OPTIO3_BUILDER_WORKER, "true");

        String containerName = IdGenerator.newGuid();
        rec.createContainer(m_config.hostRemoter, containerName, config);

        //
        // Since we have created a physical resource,
        // register for transaction notification to terminate the container,
        // in case its record cannot be committed to the database.
        //
        m_sessionHolder.onTransactionRollback(() ->
                                              {
                                                  try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionHolder, null, true))
                                                  {
                                                      rec.freeResources(m_config.hostRemoter, validation);
                                                  }
                                              });

        rec.setDeleteOnRelease(true);
        m_sessionHolder.persistEntity(rec);
        rec.acquire(m_job);

        for (MappingConfig cfg : m_mapped)
        {
            MappedDockerVolumeRecord rec2 = MappedDockerVolumeRecord.newInstance(rec);
            rec2.setPath(ContainerBuilder.getPathAsString(cfg.path));
            rec2.setDirectory(cfg.directory);
            rec2.setVolume(cfg.volume);
            m_sessionHolder.persistEntity(rec2);
        }

        // Get ready for the next build.
        m_createConfig = null;
        m_mapped.clear();

        step.setContainer(rec);
    }

    public void start(JobStepRecord step) throws
                                          Exception
    {
        DockerContainerRecord rec_container = step.getContainer();
        if (rec_container != null)
        {
            rec_container.startContainer(m_config.hostRemoter);
        }
    }

    public boolean refresh(RecordLocked<JobStepRecord> lock_step) throws
                                                                  Exception
    {
        boolean gotNewOutput = false;

        JobStepRecord         rec_step      = lock_step.get();
        DockerContainerRecord rec_container = rec_step.getContainer();
        if (rec_container != null)
        {
            gotNewOutput = rec_container.fetchOutput(m_config.hostRemoter, lock_step);

            rec_container.refreshExitCode(m_config.hostRemoter);

            updateExitCode(rec_step);
        }

        return gotNewOutput;
    }

    public void stop(RecordLocked<JobStepRecord> lock_step) throws
                                                            Exception
    {
        JobStepRecord         rec_step      = lock_step.get();
        DockerContainerRecord rec_container = rec_step.getContainer();
        if (rec_container != null)
        {
            rec_container.stopContainer(m_config.hostRemoter);

            updateExitCode(rec_step);

            rec_container.fetchOutput(m_config.hostRemoter, lock_step);

            try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionHolder, null, false))
            {
                rec_step.freeResources(m_config.hostRemoter, validation);
            }
        }
    }

    //--//

    public void updateExitCode(JobStepRecord step)
    {
        DockerContainerRecord rec_container = step.getContainer();
        if (rec_container != null)
        {
            Integer exitCode = rec_container.getExitCode();
            if (exitCode != null)
            {
                if (exitCode == 0)
                {
                    step.setStatus(JobStatus.COMPLETED);
                }
                else
                {
                    step.setStatus(JobStatus.FAILED);
                }
            }
        }
    }

    private ContainerConfiguration ensureMainConfig()
    {
        if (m_createConfig == null)
        {
            m_createConfig = new ContainerConfiguration();
        }

        return m_createConfig;
    }
}
