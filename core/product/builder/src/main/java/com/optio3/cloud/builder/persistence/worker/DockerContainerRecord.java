/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.worker;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.HostRemoter;
import com.optio3.cloud.builder.model.worker.DockerContainer;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.builder.remoting.RemoteDockerApi;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "DOCKER_CONTAINER")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "DockerContainer", model = DockerContainer.class, metamodel = DockerContainerRecord_.class)
public class DockerContainerRecord extends HostBoundResource implements ModelMapperTarget<DockerContainer, DockerContainerRecord_>
{
    @Column(name = "docker_id")
    private String dockerId;

    @Column(name = "started_on")
    private ZonedDateTime startedOn;

    @Column(name = "exit_code")
    private Integer exitCode;

    //--//

    /**
     * List of all the volumes mapped into this container.
     */
    @OneToMany(mappedBy = "owningContainer", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<MappedDockerVolumeRecord> mappedTo;

    //--//

    public DockerContainerRecord()
    {
    }

    public static DockerContainerRecord newInstance(HostRecord host)
    {
        requireNonNull(host);

        DockerContainerRecord res = new DockerContainerRecord();
        res.setOwningHost(host);
        return res;
    }

    //--//

    public String getDockerId()
    {
        return dockerId;
    }

    public void setDockerId(String dockerId)
    {
        this.dockerId = dockerId;
    }

    public ZonedDateTime getStartedOn()
    {
        return startedOn;
    }

    public void setStartedOn(ZonedDateTime startedOn)
    {
        this.startedOn = startedOn;
    }

    public Integer getExitCode()
    {
        return exitCode;
    }

    public List<MappedDockerVolumeRecord> getMappedTo()
    {
        return CollectionUtils.asEmptyCollectionIfNull(mappedTo);
    }

    //--//

    public static TypedRecordIdentityList<DockerContainerRecord> list(RecordHelper<DockerContainerRecord> helper,
                                                                      HostRecord host)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            if (host != null)
            {
                jh.addWhereClauseWithEqual(jh.root, HostBoundResource_.owningHost, host);
            }
        });
    }

    public static List<DockerContainerRecord> getBatch(RecordHelper<DockerContainerRecord> helper,
                                                       List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public void createContainer(HostRemoter remoter,
                                String name,
                                ContainerConfiguration config) throws
                                                               Exception
    {
        String id = getDockerId();
        if (id != null)
        {
            throw Exceptions.newRuntimeException("DockerContainerRecord %s already bound to container %s", getSysId(), id);
        }

        RemoteDockerApi proxy = remoter.createRemotableProxy(this, RemoteDockerApi.class);

        id = getAndUnwrapException(proxy.createContainer(name, config));
        setDockerId(id);
    }

    public void startContainer(HostRemoter remoter) throws
                                                    Exception
    {
        String id = getDockerId();
        if (id == null)
        {
            throw Exceptions.newRuntimeException("DockerContainerRecord %s not bound to any containers", getSysId());
        }

        RemoteDockerApi proxy = remoter.createRemotableProxy(this, RemoteDockerApi.class);

        getAndUnwrapException(proxy.startContainer(id));
    }

    public boolean fetchOutput(HostRemoter remoter,
                               RecordLocked<JobStepRecord> lock_step) throws
                                                                      Exception
    {
        String id = getDockerId();
        if (id == null)
        {
            return false;
        }

        RemoteDockerApi proxy = remoter.createRemotableProxy(this, RemoteDockerApi.class);

        try (var logHandler = JobStepRecord.allocateLogHandler(lock_step))
        {
            try (LogHolder log = logHandler.newLogHolder())
            {
                ZonedDateTime lastOutput = logHandler.getLastOutput();
                ZonedDateTime newOutput  = getAndUnwrapException(proxy.fetchOutput(id, lastOutput, (en) -> log.addLineAsync(en.fd, en.timestamp, null, null, null, null, en.line)));

                return TimeUtils.compare(lastOutput, newOutput) != 0;
            }
        }
    }

    public void stopContainer(HostRemoter remoter) throws
                                                   Exception
    {
        String id = getDockerId();
        if (id != null)
        {
            RemoteDockerApi proxy = remoter.createRemotableProxy(this, RemoteDockerApi.class);

            exitCode = getAndUnwrapException(proxy.stopContainer(id));

            setDockerId(null);
        }
    }

    public void refreshExitCode(HostRemoter remoter) throws
                                                     Exception
    {
        String id = getDockerId();
        if (id != null)
        {
            RemoteDockerApi proxy = remoter.createRemotableProxy(this, RemoteDockerApi.class);

            exitCode = getAndUnwrapException(proxy.getExitCode(id));
        }
    }

    //--//

    @Override
    protected void checkConditionsForFreeingResourcesInner(ValidationResultsHolder validation)
    {
        // Nothing to check.
    }

    @Override
    protected void freeResourcesInner(HostRemoter remoter,
                                      ValidationResultsHolder validation) throws
                                                                          Exception
    {
        stopContainer(remoter);

        for (MappedDockerVolumeRecord rec_child : Lists.newArrayList(getMappedTo()))
        {
            rec_child.deleteResources(validation.sessionHolder);
        }
    }

    @Override
    protected List<? extends HostBoundResource> deleteRecursivelyInner(HostRemoter remoter,
                                                                       ValidationResultsHolder validation)
    {
        return getOwningHost().getResources();
    }
}
