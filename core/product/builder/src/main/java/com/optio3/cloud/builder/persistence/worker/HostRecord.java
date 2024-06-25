/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.worker;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.HostRemoter;
import com.optio3.cloud.builder.model.worker.Host;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogHandler;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordForWorker;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.CollectionUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "HOST")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "Host", model = Host.class, metamodel = HostRecord_.class)
public class HostRecord extends HostBoundResource implements RecordForWorker<HostRecord>,
                                                             LogHandler.ILogHost<HostLogRecord>,
                                                             ModelMapperTarget<Host, HostRecord_>
{
    @Column(name = "domain_name", nullable = false)
    private String domainName;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    //--//

    /**
     * List of all the resources on this host.
     */
    @OneToMany(mappedBy = "owningHost", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<HostBoundResource> resources;

    //--//

    @Column(name = "last_output")
    private ZonedDateTime lastOutput;

    @Column(name = "last_offset", nullable = false)
    private int lastOffset;

    @Lob
    @Column(name = "log_ranges")
    private byte[] logRanges;

    //--//

    public HostRecord()
    {
    }

    //--//

    public String getDomainName()
    {
        return domainName;
    }

    public void setDomainName(String domainName)
    {
        this.domainName = domainName;
    }

    public String getIpAddress()
    {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress)
    {
        this.ipAddress = ipAddress;
    }

    //--//

    public List<HostBoundResource> getResources()
    {
        return CollectionUtils.asEmptyCollectionIfNull(resources);
    }

    public <T extends HostBoundResource> List<T> getResources(Class<T> clz)
    {
        List<T> list = Lists.newArrayList();

        for (HostBoundResource resource : getResources())
        {
            T typedResource = SessionHolder.asEntityOfClassOrNull(resource, clz);
            if (typedResource != null)
            {
                list.add(typedResource);
            }
        }

        return list;
    }

    public List<DockerTemporaryImageRecord> getTemporaryImages()
    {
        return getResources(DockerTemporaryImageRecord.class);
    }

    public List<DockerContainerRecord> getContainers()
    {
        return getResources(DockerContainerRecord.class);
    }

    public List<DockerVolumeRecord> getVolumes()
    {
        return getResources(DockerVolumeRecord.class);
    }

    public List<ManagedDirectoryRecord> getDirectories()
    {
        return getResources(ManagedDirectoryRecord.class);
    }

    public List<RepositoryCheckoutRecord> getCheckouts()
    {
        return getResources(RepositoryCheckoutRecord.class);
    }

    //--//

    public static TypedRecordIdentityList<HostRecord> list(RecordHelper<HostRecord> helper)
    {
        return QueryHelperWithCommonFields.list(helper);
    }

    public static List<HostRecord> getBatch(RecordHelper<HostRecord> helper,
                                            List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    @Override
    protected void checkConditionsForFreeingResourcesInner(ValidationResultsHolder validation)
    {
        checkConditionsForFreeingResources(getResources(), validation);
    }

    @Override
    protected void freeResourcesInner(HostRemoter remoter,
                                      ValidationResultsHolder validation) throws
                                                                          Exception
    {
        freeChildResources(remoter, validation, getContainers());
        freeChildResources(remoter, validation, getVolumes());
        freeChildResources(remoter, validation, getTemporaryImages());
        freeChildResources(remoter, validation, getCheckouts());
        freeChildResources(remoter, validation, getDirectories());
    }

    @Override
    protected List<? extends HostBoundResource> deleteRecursivelyInner(HostRemoter remoter,
                                                                       ValidationResultsHolder validation) throws
                                                                                                           Exception
    {
        deleteChildren(remoter, validation, getContainers());
        deleteChildren(remoter, validation, getVolumes());
        deleteChildren(remoter, validation, getTemporaryImages());
        deleteChildren(remoter, validation, getCheckouts());
        deleteChildren(remoter, validation, getDirectories());

        return null;
    }

    //--//

    public ZonedDateTime getLastOutput()
    {
        return lastOutput;
    }

    public int getLastOffset()
    {
        return lastOffset;
    }

    @Override
    public byte[] getLogRanges()
    {
        return logRanges;
    }

    @Override
    public void setLogRanges(byte[] logRanges,
                             ZonedDateTime lastOutput,
                             int lastOffset)
    {
        if (!Arrays.equals(this.logRanges, logRanges))
        {
            this.logRanges  = logRanges;
            this.lastOutput = lastOutput;
            this.lastOffset = lastOffset;
        }
    }

    @Override
    public void refineLogQuery(LogHandler.JoinHelper<?, HostLogRecord> jh)
    {
        jh.addWhereClauseWithEqual(jh.rootLog, HostLogRecord_.owningHost, this);
    }

    @Override
    public HostLogRecord allocateNewLogInstance()
    {
        return HostLogRecord.newInstance(this);
    }

    public static LogHandler<HostRecord, HostLogRecord> allocateLogHandler(RecordLocked<HostRecord> lock)
    {
        return new LogHandler<>(lock, HostLogRecord.class);
    }

    public static LogHandler<HostRecord, HostLogRecord> allocateLogHandler(SessionHolder sessionHolder,
                                                                           HostRecord rec)
    {
        return new LogHandler<>(sessionHolder, rec, HostLogRecord.class);
    }
}
