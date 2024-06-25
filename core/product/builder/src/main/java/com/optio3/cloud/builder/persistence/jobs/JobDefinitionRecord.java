/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.jobs;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.jobs.JobDefinition;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.util.CollectionUtils;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "JOB_DEF")
@Optio3TableInfo(externalId = "JobDefinition", model = JobDefinition.class, metamodel = JobDefinitionRecord_.class)
public class JobDefinitionRecord extends RecordWithCommonFields implements ModelMapperTarget<JobDefinition, JobDefinitionRecord_>
{
    @Column(name = "name", nullable = false)
    private String name;

    @NaturalId
    @Column(name = "id_prefix", nullable = false)
    private String idPrefix;

    @Column(name = "total_timeout", nullable = false)
    private int totalTimeout;

    //--//

    @OneToMany(mappedBy = "owningJob", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("position")
    private List<JobDefinitionStepRecord> steps;

    //--//

    public JobDefinitionRecord()
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

    public String getIdPrefix()
    {
        return idPrefix;
    }

    public void setIdPrefix(String idPrefix)
    {
        this.idPrefix = idPrefix;
    }

    public int getTotalTimeout()
    {
        return totalTimeout;
    }

    public void setTotalTimeout(int totalTimeout)
    {
        this.totalTimeout = totalTimeout;
    }

    //--//

    public List<JobDefinitionStepRecord> getSteps()
    {
        return CollectionUtils.asEmptyCollectionIfNull(steps);
    }

    //--//

    public static JobDefinitionRecord findByName(RecordHelper<JobDefinitionRecord> helper,
                                                 String name)
    {
        return QueryHelperWithCommonFields.getFirstMatch(helper, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, JobDefinitionRecord_.name, name);
        });
    }

    public static TypedRecordIdentityList<JobDefinitionRecord> list(RecordHelper<JobDefinitionRecord> helper)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            jh.addOrderBy(jh.root, JobDefinitionRecord_.name, true);
        });
    }

    public static List<JobDefinitionRecord> getBatch(RecordHelper<JobDefinitionRecord> helper,
                                                     List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static JobDefinitionRecord findByPrefix(RecordHelper<JobDefinitionRecord> helper,
                                                   String prefix)
    {
        return helper.byNaturalId()
                     .using(JobDefinitionRecord_.idPrefix.getName(), prefix)
                     .load();
    }
}
