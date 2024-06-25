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
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.jobs.input.Repository;
import com.optio3.cloud.builder.persistence.worker.RepositoryCheckoutRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "REPOSITORY")
@Optio3TableInfo(externalId = "Repository", model = Repository.class, metamodel = RepositoryRecord_.class)
public class RepositoryRecord extends RecordWithCommonFields implements ModelMapperTarget<Repository, RepositoryRecord_>
{
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * The remote origin of this Git repository.
     */
    @Column(name = "git_url", nullable = false, length = 1024)
    private String gitUrl;

    /**
     * List of all the various checkouts of this Git repository.
     */
    @OneToMany(mappedBy = "repository", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<RepositoryBranchRecord> branches;

    /**
     * List of all the various checkouts of this Git repository.
     */
    @OneToMany(mappedBy = "repository", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<RepositoryCheckoutRecord> checkouts;

    //--//

    public RepositoryRecord()
    {
    }

    //--//

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getGitUrl()
    {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl)
    {
        this.gitUrl = gitUrl;
    }

    public List<RepositoryBranchRecord> getBranches()
    {
        return CollectionUtils.asEmptyCollectionIfNull(branches);
    }

    public List<RepositoryCheckoutRecord> getCheckouts()
    {
        return CollectionUtils.asEmptyCollectionIfNull(checkouts);
    }

    //--//

    public static RepositoryRecord findByName(RecordHelper<RepositoryRecord> helper,
                                              String name)
    {
        return QueryHelperWithCommonFields.getFirstMatch(helper, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, RepositoryRecord_.name, name);
        });
    }

    public static TypedRecordIdentityList<RepositoryRecord> list(RecordHelper<RepositoryRecord> helper)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            jh.addOrderBy(jh.root, RepositoryRecord_.gitUrl, true);
        });
    }

    public static List<RepositoryRecord> getBatch(RecordHelper<RepositoryRecord> helper,
                                                  List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public List<String> getCommits(RecordHelper<RepositoryCommitRecord> helper)
    {
        return QueryHelperWithCommonFields.listRaw(helper, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, RepositoryCommitRecord_.repository, this);
        });
    }

    //--//

    public RepositoryCommitRecord findCommitByHash(RecordHelper<RepositoryCommitRecord> helper,
                                                   String commit)
    {
        return helper.getOrNull(fromCommitHashToSysId(commit));
    }

    public RepositoryBranchRecord findBranch(String name)
    {
        for (RepositoryBranchRecord rec_branch : getBranches())
        {
            if (StringUtils.equals(rec_branch.getName(), name))
            {
                return rec_branch;
            }
        }

        return null;
    }

    //--//

    public String fromCommitHashToSysId(String commit)
    {
        return getSysId() + "/" + commit;
    }

    public static String fromSysIdToCommitHashTo(String sysId)
    {
        int pos = sysId.indexOf('/');
        if (pos < 0)
        {
            throw Exceptions.newRuntimeException("Invalid SysId for commit: %s", sysId);
        }

        return sysId.substring(pos + 1);
    }
}
