/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.worker;

import static java.util.Objects.requireNonNull;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.HostRemoter;
import com.optio3.cloud.builder.model.worker.RepositoryCheckout;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "REPOSITORY_CHECKOUT")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "RepositoryCheckout", model = RepositoryCheckout.class, metamodel = RepositoryCheckoutRecord_.class)
public class RepositoryCheckoutRecord extends HostBoundResource implements ModelMapperTarget<RepositoryCheckout, RepositoryCheckoutRecord_>
{
    /**
     * The Git repository this checkout is cloned from.
     */
    @Optio3ControlNotifications(reason = "Only notify repo of checkout's changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getRepository")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "repository", nullable = false, foreignKey = @ForeignKey(name = "REPOCHECKOUT__REPOSITORY__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private RepositoryRecord repository;

    //--//

    /**
     * Which branch is currently checked out.
     */
    @Column(name = "current_branch")
    private String currentBranch;

    /**
     * Which commit is currently checked out.
     */
    @Column(name = "current_commit")
    private String currentCommit;

    //--//

    /**
     * The local directory holding the Git metadata.
     */
    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getDirectoryForDb")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "directory_for_db", foreignKey = @ForeignKey(name = "DIRECTORY_FOR_DB__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private ManagedDirectoryRecord directoryForDb;

    /**
     * The local directory holding the Git working tree.
     */
    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getDirectoryForWork")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "directory_for_work", foreignKey = @ForeignKey(name = "DIRECTORY_FOR_WORK__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private ManagedDirectoryRecord directoryForWork;

    //--//

    public RepositoryCheckoutRecord()
    {
    }

    public static RepositoryCheckoutRecord newInstance(HostRecord host,
                                                       RepositoryRecord repo)
    {
        requireNonNull(host);
        requireNonNull(repo);

        RepositoryCheckoutRecord res = new RepositoryCheckoutRecord();
        res.setOwningHost(host);
        res.repository = repo;
        return res;
    }

    //--//

    public RepositoryRecord getRepository()
    {
        return repository;
    }

    public String getCurrentBranch()
    {
        return currentBranch;
    }

    public void setCurrentBranch(String currentBranch)
    {
        this.currentBranch = currentBranch;
    }

    public String getCurrentCommit()
    {
        return currentCommit;
    }

    public void setCurrentCommit(String currentCommit)
    {
        this.currentCommit = currentCommit;
    }

    public ManagedDirectoryRecord getDirectoryForDb()
    {
        return directoryForDb;
    }

    public void setDirectoryForDb(ManagedDirectoryRecord directoryForDb)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        ManagedDirectoryRecord rec_oldValue = this.directoryForDb;

        this.directoryForDb = directoryForDb;

        //
        // We have invariants to maintain, make sure we update them.
        //
        if (rec_oldValue != null && rec_oldValue != directoryForDb)
        {
            rec_oldValue.getCheckoutsForDb()
                        .remove(this);
        }
    }

    public ManagedDirectoryRecord getDirectoryForWork()
    {
        return directoryForWork;
    }

    public void setDirectoryForWork(ManagedDirectoryRecord directoryForWork)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        ManagedDirectoryRecord rec_oldValue = this.directoryForWork;

        this.directoryForWork = directoryForWork;

        //
        // We have invariants to maintain, make sure we update them.
        //
        if (rec_oldValue != null && rec_oldValue != directoryForWork)
        {
            rec_oldValue.getCheckoutsForWork()
                        .remove(this);
        }
    }

    //--//

    public static List<RepositoryCheckoutRecord> getBatch(RecordHelper<RepositoryCheckoutRecord> helper,
                                                          List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public boolean tryToAcquire(RecordHelper<RepositoryCheckoutRecord> helper,
                                JobRecord job)
    {
        if (this.getAcquiredBy() == null)
        {
            this.acquire(job);
            return true;
        }

        return false;
    }

    //--//

    @Override
    protected void checkConditionsForFreeingResourcesInner(ValidationResultsHolder validation)
    {
        ManagedDirectoryRecord rec_dirForDb = directoryForDb;
        if (rec_dirForDb != null && rec_dirForDb.isMappedInAnyContainer())
        {
            validation.addFailure("directoryForDb", "Directory %s still bound to containers, can't delete checkout %s", rec_dirForDb.getSysId(), getSysId());
        }

        ManagedDirectoryRecord rec_dirForWork = directoryForWork;
        if (rec_dirForWork != null && rec_dirForWork.isMappedInAnyContainer())
        {
            validation.addFailure("directoryForWork", "Directory %s still bound to containers, can't delete checkout %s", rec_dirForDb.getSysId(), getSysId());
        }
    }

    @Override
    protected void freeResourcesInner(HostRemoter remoter,
                                      ValidationResultsHolder validation) throws
                                                                          Exception
    {
        ManagedDirectoryRecord rec_dirForDb = directoryForDb;
        if (rec_dirForDb != null)
        {
            setDirectoryForDb(null);
            rec_dirForDb.deleteRecursively(remoter, validation);
        }

        ManagedDirectoryRecord rec_dirForWork = directoryForWork;
        if (rec_dirForWork != null)
        {
            setDirectoryForWork(null);
            rec_dirForWork.deleteRecursively(remoter, validation);
        }
    }

    @Override
    protected List<? extends HostBoundResource> deleteRecursivelyInner(HostRemoter remoter,
                                                                       ValidationResultsHolder validation)
    {
        if (repository != null)
        {
            repository.getCheckouts()
                      .remove(this);
        }

        return getOwningHost().getResources();
    }

    //--//

    @Override
    public void acquire(JobRecord job)
    {
        acquireChild(job, directoryForDb);
        acquireChild(job, directoryForWork);

        super.acquire(job);
    }

    @Override
    public void release(HostRemoter remoter,
                        SessionHolder sessionHolder) throws
                                                     Exception
    {
        releaseChild(remoter, sessionHolder, directoryForDb);
        releaseChild(remoter, sessionHolder, directoryForWork);

        super.release(remoter, sessionHolder);
    }
}
