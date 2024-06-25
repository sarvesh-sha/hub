/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.worker;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.HostRemoter;
import com.optio3.cloud.builder.model.worker.ManagedDirectory;
import com.optio3.cloud.builder.remoting.RemoteFileSystemApi;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.util.CollectionUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "MANAGED_DIRECTORY")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "ManagedDirectory", model = ManagedDirectory.class, metamodel = ManagedDirectoryRecord_.class)
public class ManagedDirectoryRecord extends HostBoundResource implements ModelMapperTarget<ManagedDirectory, ManagedDirectoryRecord_>
{
    @Column(name = "path", nullable = false)
    private String path;

    //--//

    /**
     * List of all the Git repositories using this directory for Git metadata.
     */
    @OneToMany(mappedBy = "directoryForDb", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<RepositoryCheckoutRecord> checkoutsForDb;

    /**
     * List of all the Git repositories using this directory as Git working tree.
     */
    @OneToMany(mappedBy = "directoryForWork", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<RepositoryCheckoutRecord> checkoutsForWork;

    /**
     * List of all the volumes mapped in a container using this directory as the host side.
     */
    @OneToMany(mappedBy = "directory", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<MappedDockerVolumeRecord> mappedIn;

    //--//

    public ManagedDirectoryRecord()
    {
    }

    public static ManagedDirectoryRecord newInstance(HostRecord host,
                                                     String path)
    {
        requireNonNull(host);
        requireNonNull(path);

        ManagedDirectoryRecord res = new ManagedDirectoryRecord();
        res.setOwningHost(host);
        res.path = path;
        return res;
    }

    public static ManagedDirectoryRecord newInstance(HostRecord host,
                                                     Path path)
    {
        requireNonNull(host);
        requireNonNull(path);

        return newInstance(host, getPathAsString(path));
    }

    public static String getPathAsString(Path path)
    {
        return path != null ? path.toAbsolutePath()
                                  .toString() : null;
    }

    //--//

    public Path getPath()
    {
        return path != null ? Paths.get(path) : null;
    }

    public String getPathAsString()
    {
        return getPathAsString(getPath());
    }

    public List<RepositoryCheckoutRecord> getCheckoutsForDb()
    {
        return CollectionUtils.asEmptyCollectionIfNull(checkoutsForDb);
    }

    public List<RepositoryCheckoutRecord> getCheckoutsForWork()
    {
        return CollectionUtils.asEmptyCollectionIfNull(checkoutsForWork);
    }

    public List<MappedDockerVolumeRecord> getMappedIn()
    {
        return CollectionUtils.asEmptyCollectionIfNull(mappedIn);
    }

    //--//

    public static TypedRecordIdentityList<ManagedDirectoryRecord> list(RecordHelper<ManagedDirectoryRecord> helper,
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

    public static List<ManagedDirectoryRecord> getBatch(RecordHelper<ManagedDirectoryRecord> helper,
                                                        List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public void createDirectory(HostRemoter remoter) throws
                                                     Exception
    {
        Path dir = getPath();
        if (dir != null)
        {
            RemoteFileSystemApi proxy = remoter.createRemotableProxy(this, RemoteFileSystemApi.class);

            getAndUnwrapException(proxy.createDirectory(dir));
        }
    }

    public void deleteDirectory(HostRemoter remoter) throws
                                                     Exception
    {
        Path dir = getPath();
        if (dir != null)
        {
            RemoteFileSystemApi proxy = remoter.createRemotableProxy(this, RemoteFileSystemApi.class);

            getAndUnwrapException(proxy.deleteDirectory(dir));
        }
    }

    //--//

    public boolean isMappedInAnyContainer()
    {
        return !getMappedIn().isEmpty();
    }

    @Override
    protected void checkConditionsForFreeingResourcesInner(ValidationResultsHolder validation)
    {
        if (isMappedInAnyContainer())
        {
            validation.addFailure("mappedIn", "Directory %s still bound to containers", getSysId());
        }

        //
        // Delegate to checkout, in case it's in use somewhere.
        //
        for (RepositoryCheckoutRecord rec_checkout : getCheckoutsForDb())
        {
            rec_checkout.checkConditionsForFreeingResources(validation);
        }

        for (RepositoryCheckoutRecord rec_checkout : getCheckoutsForWork())
        {
            rec_checkout.checkConditionsForFreeingResources(validation);
        }
    }

    @Override
    protected void freeResourcesInner(HostRemoter remoter,
                                      ValidationResultsHolder validation) throws
                                                                          Exception
    {
        deleteDirectory(remoter);
    }

    @Override
    protected List<? extends HostBoundResource> deleteRecursivelyInner(HostRemoter remoter,
                                                                       ValidationResultsHolder validation)
    {
        return getOwningHost().getResources();
    }
}
