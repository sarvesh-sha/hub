/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.jobs.input;

import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.jobs.input.RepositoryCommit;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "REPOSITORY_COMMIT")
@Optio3TableInfo(externalId = "RepositoryCommit", model = RepositoryCommit.class, metamodel = RepositoryCommitRecord_.class)
public class RepositoryCommitRecord extends RecordWithCommonFields implements ModelMapperTarget<RepositoryCommit, RepositoryCommitRecord_>
{
    /**
     * The Git repository this commit is from.
     */
    @Optio3ControlNotifications(reason = "Only notify repo of commit's changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getRepository")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "repository", nullable = false, foreignKey = @ForeignKey(name = "REPOCOMMIT__REPOSITORY__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private RepositoryRecord repository;

    @Lob
    @Column(name = "message", nullable = false, length = 64 * 1024)
    private String message;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "author_email_address", nullable = false)
    private String authorEmailAddress;

    @Column(name = "parents", length = 64 * 10)
    private String parents;

    //--//

    public RepositoryCommitRecord()
    {
    }

    public static RepositoryCommitRecord newInstance(RepositoryRecord repo,
                                                     String commitHash,
                                                     ZonedDateTime when)
    {
        RepositoryCommitRecord rec = new RepositoryCommitRecord();

        rec.repository = repo;
        rec.setSysId(repo.fromCommitHashToSysId(commitHash));
        rec.setCreatedOn(when);
        rec.setUpdatedOn(when);

        return rec;
    }

    //--//

    public RepositoryRecord getRepository()
    {
        return repository;
    }

    public String getCommitHash()
    {
        return RepositoryRecord.fromSysIdToCommitHashTo(getSysId());
    }

    //--//

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getAuthorName()
    {
        return authorName;
    }

    public void setAuthorName(String authorName)
    {
        this.authorName = authorName;
    }

    public String getAuthorEmailAddress()
    {
        return authorEmailAddress;
    }

    public void setAuthorEmailAddress(String authorEmailAddress)
    {
        this.authorEmailAddress = authorEmailAddress;
    }

    public String[] getParents()
    {
        return parents != null ? parents.split(";") : null;
    }

    public void setParents(String[] parents)
    {
        this.parents = parents != null && parents.length > 0 ? String.join(";", parents) : null;
    }

    //--//

    public static List<RepositoryCommitRecord> getBatch(RecordHelper<RepositoryCommitRecord> helper,
                                                        List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }
}
