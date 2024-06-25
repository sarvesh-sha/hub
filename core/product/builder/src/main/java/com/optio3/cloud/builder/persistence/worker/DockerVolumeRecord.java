/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.worker;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.HostRemoter;
import com.optio3.cloud.builder.model.worker.DockerVolume;
import com.optio3.cloud.builder.remoting.RemoteDockerApi;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "DOCKER_VOLUME")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "DockerVolume", model = DockerVolume.class, metamodel = DockerVolumeRecord_.class)
public class DockerVolumeRecord extends HostBoundResource implements ModelMapperTarget<DockerVolume, DockerVolumeRecord_>
{
    @Column(name = "dockerId", nullable = false)
    private String dockerId;

    //--//

    /**
     * List of all the mappings of this volume into containers.
     */
    @OneToMany(mappedBy = "volume", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<MappedDockerVolumeRecord> mappedAs;

    //--//

    public DockerVolumeRecord()
    {
    }

    public static DockerVolumeRecord newInstance(HostRecord host)
    {
        requireNonNull(host);

        DockerVolumeRecord res = new DockerVolumeRecord();
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

    public List<MappedDockerVolumeRecord> getMappedAs()
    {
        return CollectionUtils.asEmptyCollectionIfNull(mappedAs);
    }

    //--//

    public static TypedRecordIdentityList<DockerVolumeRecord> list(RecordHelper<DockerVolumeRecord> helper,
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

    public static List<DockerVolumeRecord> getBatch(RecordHelper<DockerVolumeRecord> helper,
                                                    List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public void createVolume(HostRemoter remoter,
                             SessionHolder sessionHolder,
                             String name,
                             Map<String, String> labels) throws
                                                         Exception
    {
        requireNonNull(remoter);
        requireNonNull(sessionHolder);
        requireNonNull(name);

        String id = getDockerId();
        if (id != null)
        {
            throw Exceptions.newRuntimeException("DockerVolumeRecord %s already bound to volume %s", getSysId(), id);
        }

        RemoteDockerApi proxy = remoter.createRemotableProxy(this, RemoteDockerApi.class);

        id = getAndUnwrapException(proxy.createVolume(name, labels, null, null));

        setDockerId(id);
        sessionHolder.flush();
    }

    //--//

    @Override
    protected void checkConditionsForFreeingResourcesInner(ValidationResultsHolder validation)
    {
        if (!getMappedAs().isEmpty())
        {
            validation.addFailure("mappedAs", "Volume %s still bound to containers", getSysId());
        }
    }

    @Override
    protected void freeResourcesInner(HostRemoter remoter,
                                      ValidationResultsHolder validation) throws
                                                                          Exception
    {
        String id = getDockerId();
        if (id != null)
        {
            RemoteDockerApi proxy = remoter.createRemotableProxy(this, RemoteDockerApi.class);

            proxy.deleteVolume(id, false)
                 .join();

            setDockerId(null);
        }
    }

    @Override
    protected List<? extends HostBoundResource> deleteRecursivelyInner(HostRemoter remoter,
                                                                       ValidationResultsHolder validation)
    {
        return getOwningHost().getResources();
    }
}
