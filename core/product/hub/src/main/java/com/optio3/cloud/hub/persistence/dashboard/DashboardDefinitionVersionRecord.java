/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.dashboard;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.metamodel.SingularAttribute;

import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.hub.model.config.UsageFilterRequest;
import com.optio3.cloud.hub.model.dashboard.DashboardConfiguration;
import com.optio3.cloud.hub.model.dashboard.DashboardDefinitionVersion;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.ModelSanitizerContext;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithSequenceNumber;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "DASHBOARD_DEFINITION_VERSION")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "DashboardDefinitionVersion", model = DashboardDefinitionVersion.class, metamodel = DashboardDefinitionVersionRecord_.class)
public class DashboardDefinitionVersionRecord extends RecordWithSequenceNumber<DashboardDefinitionVersionRecord> implements ModelMapperTarget<DashboardDefinitionVersion, DashboardDefinitionVersionRecord_>
{
    @Column(name = "version", nullable = false)
    private int version;

    @Lob
    @Column(name = "details")
    private String details;

    @Transient
    private final PersistAsJsonHelper<String, DashboardConfiguration> m_detailsHelper = new PersistAsJsonHelper<>(() -> details,
                                                                                                                  (val) -> details = val,
                                                                                                                  String.class,
                                                                                                                  DashboardConfiguration.class,
                                                                                                                  ObjectMappers.SkipNulls,
                                                                                                                  true);

    //--//

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getDefinition")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getDefinition")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "definition", nullable = false, foreignKey = @ForeignKey(name = "DASHBOARD_DEFINITION_VERSION__DEFINITION__FK"))
    private DashboardDefinitionRecord definition;

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getPredecessor")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getPredecessor", setter = "setPredecessor")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "predecessor", nullable = true, foreignKey = @ForeignKey(name = "DASHBOARD_DEFINITION_VERSION__PREDECESSOR__FK"))
    private DashboardDefinitionVersionRecord predecessor;

    @OneToMany(mappedBy = "predecessor", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("version DESC")
    private List<DashboardDefinitionVersionRecord> successors;

    //--//

    public DashboardDefinitionVersionRecord()
    {
    }

    //--//

    public static DashboardDefinitionVersionRecord newInstance(RecordHelper<DashboardDefinitionVersionRecord> helper,
                                                               DashboardDefinitionRecord rec_dashboardDefinition,
                                                               DashboardDefinitionVersionRecord rec_predecessor,
                                                               DashboardDefinitionVersion model,
                                                               Integer version,
                                                               boolean setSysId)
    {
        DashboardDefinitionVersionRecord rec_new = new DashboardDefinitionVersionRecord();
        rec_new.definition  = rec_dashboardDefinition;
        rec_new.predecessor = rec_predecessor;
        rec_new.setDetails(model.details);

        if (setSysId)
        {
            rec_new.setSysId(model.sysId);
        }

        rec_new.version = rec_new.assignUniqueNumber(helper, version, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, DashboardDefinitionVersionRecord_.definition, rec_dashboardDefinition);
        });

        helper.persist(rec_new);

        rec_dashboardDefinition.setHeadVersion(rec_new);

        if (rec_dashboardDefinition.getReleaseVersion() == null)
        {
            rec_dashboardDefinition.setReleaseVersion(rec_new);
        }

        return rec_new;
    }

    public static int checkUsages(SessionProvider sessionProvider,
                                  UsageFilterRequest filters,
                                  TypedRecordIdentityList<DashboardDefinitionVersionRecord> lst)
    {
        return filters.analyzeRecords(sessionProvider, lst, DashboardDefinitionVersionRecord.class, (qh) ->
        {
            qh.addString(DashboardDefinitionVersionRecord_.details, (obj, val) -> obj.value1 = val);
        });
    }

    @Override
    protected SingularAttribute<DashboardDefinitionVersionRecord, Integer> fetchSequenceNumberField()
    {
        return DashboardDefinitionVersionRecord_.version;
    }

    @Override
    protected int fetchSequenceNumberValue()
    {
        return getVersion();
    }

    //--//

    public int getVersion()
    {
        return version;
    }

    public void setDetails(String details)
    {
        this.details = details;
    }

    public DashboardConfiguration getDetails()
    {
        return m_detailsHelper.get();
    }

    public boolean setDetails(DashboardConfiguration details)
    {
        return m_detailsHelper.set(details);
    }

    public DashboardDefinitionVersionRecord getPredecessor()
    {
        return predecessor;
    }

    public boolean setPredecessor(DashboardDefinitionVersionRecord predecessor)
    {
        if (SessionHolder.sameEntity(this.predecessor, predecessor))
        {
            return false; // Nothing changed.
        }

        for (var ptr = predecessor; ptr != null; ptr = ptr.getPredecessor())
        {
            if (ptr == this)
            {
                throw Exceptions.newGenericException(InvalidArgumentException.class, "Invalid link: %s and %s would form a loop", predecessor.getSysId(), getSysId());
            }
        }

        this.predecessor = predecessor;
        return true;
    }

    public List<DashboardDefinitionVersionRecord> getSuccessors()
    {
        return CollectionUtils.asEmptyCollectionIfNull(successors);
    }

    public DashboardDefinitionRecord getDefinition()
    {
        return definition;
    }

    public void makeHead()
    {
        definition.setHeadVersion(this);
    }

    public void makeRelease()
    {
        definition.setReleaseVersion(this);
    }

    //--//

    public static List<DashboardDefinitionVersionRecord> getBatch(RecordHelper<DashboardDefinitionVersionRecord> helper,
                                                                  List<String> ids)
    {
        var results = QueryHelperWithCommonFields.getBatch(helper, ids);

        for (DashboardDefinitionVersionRecord result : results)
        {
            var details = result.getDetails();

            var ctx = new ModelSanitizerContext.Simple(helper.currentSessionHolder());

            details = ctx.processTyped(details);

            if (ctx.wasModified())
            {
                result.setDetails(details);
            }
        }

        return results;
    }

    //--//

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<DashboardDefinitionVersionRecord> helper)
    {
        if (validation.canProceed())
        {
            helper.delete(this);
        }
    }
}
