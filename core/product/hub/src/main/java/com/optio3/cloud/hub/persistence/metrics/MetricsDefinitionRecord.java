/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.metrics;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Tuple;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.model.metrics.MetricsDefinition;
import com.optio3.cloud.hub.model.metrics.MetricsDefinitionFilterRequest;
import com.optio3.cloud.hub.persistence.asset.MetricsDeviceElementRecord;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.CollectionUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "METRICS_DEFINITION")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "MetricsDefinition", model = MetricsDefinition.class, metamodel = MetricsDefinitionRecord_.class)
public class MetricsDefinitionRecord extends RecordWithCommonFields implements ModelMapperTarget<MetricsDefinition, MetricsDefinitionRecord_>
{
    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getHeadVersion")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "head_version", foreignKey = @ForeignKey(name = "METRICS_DEFINITION__HEAD_VERSION__FK"))
    private MetricsDefinitionVersionRecord headVersion;

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getReleaseVersion")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "release_version", foreignKey = @ForeignKey(name = "METRICS_DEFINITION__RELEASE_VERSION__FK"))
    private MetricsDefinitionVersionRecord releaseVersion;

    @OneToMany(mappedBy = "definition", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<MetricsDefinitionVersionRecord> versions;

    //--//

    @OneToMany(mappedBy = "metricsDefinition", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private Set<MetricsDeviceElementRecord> syntheticAssets;

    //--//

    public static MetricsDefinitionRecord newInstance(String sysId)
    {
        MetricsDefinitionRecord record = new MetricsDefinitionRecord();
        if (sysId != null)
        {
            record.setSysId(sysId);
        }
        return record;
    }

    //--//

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public List<MetricsDefinitionVersionRecord> getVersions()
    {
        return CollectionUtils.asEmptyCollectionIfNull(versions);
    }

    public MetricsDefinitionVersionRecord getHeadVersion()
    {
        return headVersion;
    }

    public boolean setHeadVersion(MetricsDefinitionVersionRecord headVersion)
    {
        if (SessionHolder.sameEntity(this.headVersion, headVersion))
        {
            return false; // Nothing changed.
        }

        this.headVersion = headVersion;
        return true;
    }

    public MetricsDefinitionVersionRecord getReleaseVersion()
    {
        return releaseVersion;
    }

    public boolean setReleaseVersion(MetricsDefinitionVersionRecord releaseVersion)
    {
        if (SessionHolder.sameEntity(this.releaseVersion, releaseVersion))
        {
            return false; // Nothing changed.
        }

        this.releaseVersion = releaseVersion;
        return true;
    }

    //--//

    public Set<MetricsDeviceElementRecord> getSyntheticAssets()
    {
        return CollectionUtils.asEmptyCollectionIfNull(syntheticAssets);
    }

    //--//

    public static List<MetricsDefinitionRecord> getBatch(RecordHelper<MetricsDefinitionRecord> helper,
                                                         List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static TypedRecordIdentityList<MetricsDefinitionRecord> filter(RecordHelper<MetricsDefinitionRecord> helper,
                                                                          MetricsDefinitionFilterRequest filters)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            if (filters != null)
            {
                applyFilters(jh, filters);
            }
        });
    }

    private static void applyFilters(QueryHelperWithCommonFields<Tuple, MetricsDefinitionRecord> jh,
                                     MetricsDefinitionFilterRequest filters)
    {
        if (filters.sortBy != null)
        {
            for (SortCriteria sort : filters.sortBy)
            {
                switch (sort.column)
                {
                    case "createdOn":
                    {
                        jh.addOrderBy(jh.root, RecordWithCommonFields_.createdOn, sort.ascending);
                        break;
                    }

                    case "updatedOn":
                    {
                        jh.addOrderBy(jh.root, RecordWithCommonFields_.updatedOn, sort.ascending);
                        break;
                    }

                    case "title":
                    {
                        jh.addOrderBy(jh.root, MetricsDefinitionRecord_.title, sort.ascending);
                        break;
                    }
                }
            }
        }
    }

    //--//

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<MetricsDefinitionRecord> helper) throws
                                                                     Exception
    {
        if (validation.canProceed())
        {
            removeInner(validation, helper);
        }
    }

    private void removeInner(ValidationResultsHolder validation,
                             RecordHelper<MetricsDefinitionRecord> helper)
    {
        setReleaseVersion(null);
        setHeadVersion(null);

        final RecordHelper<MetricsDefinitionVersionRecord> helper_version = helper.wrapFor(MetricsDefinitionVersionRecord.class);
        for (MetricsDefinitionVersionRecord rec_child : Lists.newArrayList(getVersions()))
        {
            rec_child.remove(validation, helper_version);
        }

        helper.delete(this);
    }
}
