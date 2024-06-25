/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.jobs.input;

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
import com.optio3.cloud.builder.model.jobs.input.RepositoryBranch;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "REPOSITORY_BRANCH")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "RepositoryBranch", model = RepositoryBranch.class, metamodel = RepositoryBranchRecord_.class)
public class RepositoryBranchRecord extends RecordWithCommonFields implements ModelMapperTarget<RepositoryBranch, RepositoryBranchRecord_>
{
    /**
     * The Git repository this branch is from.
     */
    @Optio3ControlNotifications(reason = "Only notify repo of branch's changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getRepository")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "repository", nullable = false, foreignKey = @ForeignKey(name = "REPOBRANCH__REPOSITORY__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private RepositoryRecord repository;

    @Column(name = "name", nullable = false)
    private String name;

    /**
     * The head commit for this branch.
     */
    @Optio3ControlNotifications(reason = "Ignore changes", direct = Notify.NEVER, reverse = Notify.NEVER)
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "head", foreignKey = @ForeignKey(name = "HEAD__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private RepositoryCommitRecord head;

    //--//

    public RepositoryBranchRecord()
    {
    }

    public static RepositoryBranchRecord newInstance(RepositoryRecord repo,
                                                     String name)
    {
        RepositoryBranchRecord rec = new RepositoryBranchRecord();

        rec.repository = repo;
        rec.name       = name;

        return rec;
    }

    //--//

    public RepositoryRecord getRepository()
    {
        return repository;
    }

    public String getName()
    {
        return name;
    }

    public RepositoryCommitRecord getHead()
    {
        return head;
    }

    public void setHead(RepositoryCommitRecord head)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (this.head != head)
        {
            this.head = head;
        }
    }

    //--//

    public static List<RepositoryBranchRecord> getBatch(RecordHelper<RepositoryBranchRecord> helper,
                                                        List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }
}
