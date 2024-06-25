/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.worker;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static java.util.Objects.requireNonNull;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.HostRemoter;
import com.optio3.cloud.builder.model.worker.DockerTemporaryImage;
import com.optio3.cloud.builder.remoting.RemoteDockerApi;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.infra.docker.DockerImageIdentifier;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "DOCKER_TEMPORARY_IMAGE")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "DockerTemporaryImage", model = DockerTemporaryImage.class, metamodel = DockerTemporaryImageRecord_.class)
public class DockerTemporaryImageRecord extends HostBoundResource implements ModelMapperTarget<DockerTemporaryImage, DockerTemporaryImageRecord_>
{
    @Column(name = "image_tag")
    private String imageTag;

    //--//

    public DockerTemporaryImageRecord()
    {
    }

    public static DockerTemporaryImageRecord newInstance(HostRecord host,
                                                         String imageTag)
    {
        requireNonNull(host);

        DockerTemporaryImageRecord res = new DockerTemporaryImageRecord();
        res.setOwningHost(host);
        res.setImageTag(imageTag);
        return res;
    }

    //--//

    public String getImageTag()
    {
        return imageTag;
    }

    public void setImageTag(String imageTag)
    {
        this.imageTag = imageTag;
    }

    //--//

    public static TypedRecordIdentityList<DockerTemporaryImageRecord> list(RecordHelper<DockerTemporaryImageRecord> helper,
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

    public static List<DockerTemporaryImageRecord> getBatch(RecordHelper<DockerTemporaryImageRecord> helper,
                                                            List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public void deleteImage(HostRemoter remoter) throws
                                                 Exception
    {
        String imageTag = getImageTag();
        if (imageTag != null)
        {
            RemoteDockerApi proxy = remoter.createRemotableProxy(this, RemoteDockerApi.class);

            DockerImageIdentifier imageParsed = new DockerImageIdentifier(imageTag);
            getAndUnwrapException(proxy.deleteImage(imageParsed, false));

            setImageTag(null);
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
        deleteImage(remoter);
    }

    @Override
    protected List<? extends HostBoundResource> deleteRecursivelyInner(HostRemoter remoter,
                                                                       ValidationResultsHolder validation)
    {
        return getOwningHost().getResources();
    }
}
