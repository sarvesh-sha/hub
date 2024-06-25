/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.report;

import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Tuple;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.model.report.ReportDefinition;
import com.optio3.cloud.hub.model.report.ReportDefinitionDetails;
import com.optio3.cloud.hub.model.report.ReportDefinitionFilterRequest;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordForRecurringActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.CollectionUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "REPORT_DEFINITION")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "ReportDefinition", model = ReportDefinition.class, metamodel = ReportDefinitionRecord_.class)
public class ReportDefinitionRecord extends RecordForRecurringActivity<ReportDefinitionRecord> implements ModelMapperTarget<ReportDefinition, ReportDefinitionRecord_>
{
    @Optio3ControlNotifications(reason = "No change notification", direct = Notify.NEVER, reverse = Notify.NEVER)
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getUser", setter = "setUser")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "user", foreignKey = @ForeignKey(name = "REPORT_USER__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private UserRecord user;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private boolean active;

    @Column(name = "auto_delete")
    private ZonedDateTime autoDelete;

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getHeadVersion")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "head_version", foreignKey = @ForeignKey(name = "REPORT_DEFINITION__HEAD_VERSION__FK"))
    private ReportDefinitionVersionRecord headVersion;

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getReleaseVersion")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "release_version", foreignKey = @ForeignKey(name = "REPORT_DEFINITION__RELEASE_VERSION__FK"))
    private ReportDefinitionVersionRecord releaseVersion;

    @OneToMany(mappedBy = "definition", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<ReportDefinitionVersionRecord> versions;

    @OneToMany(mappedBy = "reportDefinition", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<ReportRecord> reports;

    public ReportDefinitionRecord()
    {
    }

    //--//

    public UserRecord getUser()
    {
        return user;
    }

    public boolean setUser(UserRecord user)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (SessionHolder.sameEntity(this.user, user))
        {
            return false; // Nothing changed.
        }

        this.user = user;
        return true;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public boolean getActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public ZonedDateTime getAutoDelete()
    {
        return autoDelete;
    }

    public void setAutoDelete(ZonedDateTime autoDelete)
    {
        this.autoDelete = autoDelete;
    }

    public ReportDefinitionVersionRecord getHeadVersion()
    {
        return headVersion;
    }

    public boolean setHeadVersion(ReportDefinitionVersionRecord headVersion)
    {
        if (SessionHolder.sameEntity(this.headVersion, headVersion))
        {
            return false; // Nothing changed.
        }

        this.headVersion = headVersion;
        return true;
    }

    public ReportDefinitionVersionRecord getReleaseVersion()
    {
        return releaseVersion;
    }

    public boolean setReleaseVersion(ReportDefinitionVersionRecord releaseVersion)
    {
        if (SessionHolder.sameEntity(this.releaseVersion, releaseVersion))
        {
            return false; // Nothing changed.
        }

        this.releaseVersion = releaseVersion;
        return true;
    }

    public void refreshNextActivation()
    {
        ReportDefinitionVersionRecord release = this.getReleaseVersion();
        ReportDefinitionDetails       details = release != null ? release.getDetails() : null;
        if (active && details != null)
        {
            setNextActivation(details.getNextActivation());
        }
        else
        {
            setNextActivation(null);
        }
    }

    public List<ReportRecord> getReports()
    {
        return CollectionUtils.asEmptyCollectionIfNull(reports);
    }

    public List<ReportDefinitionVersionRecord> getVersions()
    {
        return CollectionUtils.asEmptyCollectionIfNull(versions);
    }

    //--//

    public static List<ReportDefinitionRecord> getBatch(RecordHelper<ReportDefinitionRecord> helper,
                                                        List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static TypedRecordIdentityList<ReportDefinitionRecord> filter(RecordHelper<ReportDefinitionRecord> helper,
                                                                         UserRecord rec_user,
                                                                         ReportDefinitionFilterRequest filters)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, ReportDefinitionRecord_.user, rec_user);

            if (filters != null)
            {
                applyFilters(jh, filters);
            }
        });
    }

    private static void applyFilters(QueryHelperWithCommonFields<Tuple, ReportDefinitionRecord> jh,
                                     ReportDefinitionFilterRequest filters)
    {
        if (!filters.includeAutoDelete)
        {
            jh.addWhereClauseIsNull(jh.root, ReportDefinitionRecord_.autoDelete);
        }

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
                        jh.addOrderBy(jh.root, ReportDefinitionRecord_.title, sort.ascending);
                        jh.addOrderBy(jh.root, ReportDefinitionRecord_.description, sort.ascending);
                        break;
                    }

                    case "active":
                    {
                        jh.addOrderBy(jh.root, ReportDefinitionRecord_.active, sort.ascending);
                    }
                }
            }
        }
    }

    //--//

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<ReportDefinitionRecord> helper) throws
                                                                    Exception
    {
        if (validation.canProceed())
        {
            removeInner(validation, helper);
        }
    }

    private void removeInner(ValidationResultsHolder validation,
                             RecordHelper<ReportDefinitionRecord> helper)
    {
        setReleaseVersion(null);
        setHeadVersion(null);

        RecordHelper<ReportRecord> reportHelper = helper.wrapFor(ReportRecord.class);

        for (ReportRecord rec_child : Lists.newArrayList(getReports()))
        {
            reportHelper.delete(rec_child);
        }

        for (ReportDefinitionVersionRecord rec_child : Lists.newArrayList(getVersions()))
        {
            rec_child.remove(validation, helper.wrapFor(ReportDefinitionVersionRecord.class));
        }

        helper.delete(this);
    }
}
