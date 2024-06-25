/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.report;

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
import com.optio3.cloud.hub.model.report.ReportDefinitionDetails;
import com.optio3.cloud.hub.model.report.ReportDefinitionVersion;
import com.optio3.cloud.hub.persistence.FixupProcessingRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordForFixupProcessing;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithSequenceNumber;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Logger;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "REPORT_DEFINITION_VERSION")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "ReportDefinitionVersion", model = ReportDefinitionVersion.class, metamodel = ReportDefinitionVersionRecord_.class)
public class ReportDefinitionVersionRecord extends RecordWithSequenceNumber<ReportDefinitionVersionRecord> implements ModelMapperTarget<ReportDefinitionVersion, ReportDefinitionVersionRecord_>
{
    public static class FixupForReportSchedules extends FixupProcessingRecord.Handler
    {
        @Override
        public RecordForFixupProcessing.Handler.Result process(Logger logger,
                                                               SessionHolder sessionHolder)
        {
            RecordHelper<ReportDefinitionVersionRecord> helper = sessionHolder.createHelper(ReportDefinitionVersionRecord.class);

            for (ReportDefinitionVersionRecord rec_version : helper.listAll())
            {
                ReportDefinitionDetails details = rec_version.getDetails();
                if (details != null)
                {
                    details.cleanUp(sessionHolder);
                    rec_version.setDetails(details);
                }
            }

            return Result.Done;
        }
    }

    //--//

    @Column(name = "version", nullable = false)
    private int version;

    @Lob
    @Column(name = "details")
    private String details;

    @Transient
    private final PersistAsJsonHelper<String, ReportDefinitionDetails> m_detailsHelper = new PersistAsJsonHelper<>(() -> details,
                                                                                                                   (val) -> details = val,
                                                                                                                   String.class,
                                                                                                                   ReportDefinitionDetails.class,
                                                                                                                   ObjectMappers.SkipNulls,
                                                                                                                   true);

    //--//

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getDefinition")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getDefinition")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "definition", nullable = false, foreignKey = @ForeignKey(name = "REPORT_DEFINITION_VERSION__DEFINITION__FK"))
    private ReportDefinitionRecord definition;

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getPredecessor")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getPredecessor", setter = "setPredecessor")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "predecessor", nullable = true, foreignKey = @ForeignKey(name = "REPORT_DEFINITION_VERSION__PREDECESSOR__FK"))
    private ReportDefinitionVersionRecord predecessor;

    @OneToMany(mappedBy = "predecessor", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("version DESC")
    private List<ReportDefinitionVersionRecord> successors;

    //--//

    public ReportDefinitionVersionRecord()
    {
    }

    //--//

    public static ReportDefinitionVersionRecord newInstance(RecordHelper<ReportDefinitionVersionRecord> helper,
                                                            ReportDefinitionRecord rec_reportDefinition,
                                                            ReportDefinitionVersionRecord rec_predecessor,
                                                            ReportDefinitionVersion model,
                                                            Integer version,
                                                            boolean setSysId)
    {
        ReportDefinitionVersionRecord rec_new = new ReportDefinitionVersionRecord();
        rec_new.definition  = rec_reportDefinition;
        rec_new.predecessor = rec_predecessor;
        rec_new.setDetails(model.details);

        if (setSysId)
        {
            rec_new.setSysId(model.sysId);
        }

        rec_new.version = rec_new.assignUniqueNumber(helper, version, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, ReportDefinitionVersionRecord_.definition, rec_reportDefinition);
        });

        helper.persist(rec_new);

        rec_reportDefinition.setHeadVersion(rec_new);

        if (rec_reportDefinition.getReleaseVersion() == null)
        {
            rec_reportDefinition.setReleaseVersion(rec_new);
        }

        return rec_new;
    }

    public static int checkUsages(SessionProvider sessionProvider,
                                  UsageFilterRequest filters,
                                  TypedRecordIdentityList<ReportDefinitionVersionRecord> lst)
    {
        return filters.analyzeRecords(sessionProvider, lst, ReportDefinitionVersionRecord.class, (qh) ->
        {
            qh.addString(ReportDefinitionVersionRecord_.details, (obj, val) -> obj.value1 = val);
        });
    }

    @Override
    protected SingularAttribute<ReportDefinitionVersionRecord, Integer> fetchSequenceNumberField()
    {
        return ReportDefinitionVersionRecord_.version;
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

    public ReportDefinitionDetails getDetails()
    {
        return m_detailsHelper.get();
    }

    public boolean setDetails(ReportDefinitionDetails details)
    {
        return m_detailsHelper.set(details);
    }

    public ReportDefinitionVersionRecord getPredecessor()
    {
        return predecessor;
    }

    public boolean setPredecessor(ReportDefinitionVersionRecord predecessor)
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

    public List<ReportDefinitionVersionRecord> getSuccessors()
    {
        return CollectionUtils.asEmptyCollectionIfNull(successors);
    }

    public ReportDefinitionRecord getDefinition()
    {
        return definition;
    }

    public void cleanUp(SessionHolder sessionHolder)
    {
        ReportDefinitionDetails details = getDetails();
        details.cleanUp(sessionHolder);
        setDetails(details);
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

    public static List<ReportDefinitionVersionRecord> getBatch(RecordHelper<ReportDefinitionVersionRecord> helper,
                                                               List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<ReportDefinitionVersionRecord> helper)
    {
        if (validation.canProceed())
        {
            helper.delete(this);
        }
    }
}
