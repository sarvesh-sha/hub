/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.metrics;

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

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.hub.engine.EngineExecutionProgram;
import com.optio3.cloud.hub.engine.metrics.MetricsDefinitionDetails;
import com.optio3.cloud.hub.model.config.UsageFilterRequest;
import com.optio3.cloud.hub.model.metrics.MetricsDefinitionVersion;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
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
@Table(name = "METRICS_DEFINITION_VERSION")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "MetricsDefinitionVersion", model = MetricsDefinitionVersion.class, metamodel = MetricsDefinitionVersionRecord_.class)
public class MetricsDefinitionVersionRecord extends RecordWithSequenceNumber<MetricsDefinitionVersionRecord> implements ModelMapperTarget<MetricsDefinitionVersion, MetricsDefinitionVersionRecord_>
{
    @Column(name = "version", nullable = false)
    private int version;

    @Lob
    @Column(name = "details")
    private String details;

    @Transient
    private final PersistAsJsonHelper<String, MetricsDefinitionDetails> m_detailsHelper = new PersistAsJsonHelper<>(() -> details,
                                                                                                                    (val) -> details = val,
                                                                                                                    String.class,
                                                                                                                    MetricsDefinitionDetails.class,
                                                                                                                    ObjectMappers.SkipNulls,
                                                                                                                    true);

    //--//

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getDefinition")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getDefinition")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "definition", nullable = false, foreignKey = @ForeignKey(name = "METRICS_DEFINITION_VERSION_DEFINITION__FK"))
    private MetricsDefinitionRecord definition;

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getPredecessor")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getPredecessor", setter = "setPredecessor")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "predecessor", nullable = true, foreignKey = @ForeignKey(name = "METRICS_DEFINITION_VERSION_PREDECESSOR__FK"))
    private MetricsDefinitionVersionRecord predecessor;

    @OneToMany(mappedBy = "predecessor", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("version DESC")
    private List<MetricsDefinitionVersionRecord> successors;

    //--//

    public static MetricsDefinitionVersionRecord newInstance(RecordHelper<MetricsDefinitionVersionRecord> helper,
                                                             MetricsDefinitionVersion model,
                                                             MetricsDefinitionRecord rec_metricsDefinition,
                                                             MetricsDefinitionVersionRecord rec_predecessor,
                                                             Integer version)
    {
        MetricsDefinitionVersionRecord rec_new = new MetricsDefinitionVersionRecord();
        rec_new.definition  = rec_metricsDefinition;
        rec_new.predecessor = rec_predecessor;
        rec_new.setDetails(model.details);

        rec_new.version = rec_new.assignUniqueNumber(helper, version, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, MetricsDefinitionVersionRecord_.definition, rec_metricsDefinition);
        });

        helper.persist(rec_new);

        rec_metricsDefinition.setHeadVersion(rec_new);

        if (rec_metricsDefinition.getReleaseVersion() == null)
        {
            rec_metricsDefinition.setReleaseVersion(rec_new);
        }

        return rec_new;
    }

    public static int checkUsages(SessionProvider sessionProvider,
                                  UsageFilterRequest filters,
                                  TypedRecordIdentityList<MetricsDefinitionVersionRecord> lst)
    {
        return filters.analyzeRecords(sessionProvider, lst, MetricsDefinitionVersionRecord.class, (qh) ->
        {
            qh.addString(MetricsDefinitionVersionRecord_.details, (obj, val) -> obj.value1 = val);
        });
    }

    @Override
    protected SingularAttribute<MetricsDefinitionVersionRecord, Integer> fetchSequenceNumberField()
    {
        return MetricsDefinitionVersionRecord_.version;
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

    public MetricsDefinitionDetails getDetails()
    {
        return m_detailsHelper.get();
    }

    public MetricsDefinitionDetails getDetailsForCopy()
    {
        return m_detailsHelper.getNoCaching();
    }

    public boolean setDetails(MetricsDefinitionDetails details)
    {
        return m_detailsHelper.set(details);
    }

    public boolean isTemporary()
    {
        return getDetails().temporary;
    }

    public MetricsDefinitionRecord getDefinition()
    {
        return definition;
    }

    public MetricsDefinitionVersionRecord getPredecessor()
    {
        return predecessor;
    }

    public boolean setPredecessor(MetricsDefinitionVersionRecord predecessor)
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

    public List<MetricsDefinitionVersionRecord> getSuccessors()
    {
        return CollectionUtils.asEmptyCollectionIfNull(successors);
    }

    //--//

    public void makeHead()
    {
        definition.setHeadVersion(this);
    }

    public void makeRelease()
    {
        definition.setReleaseVersion(this);
    }

    public static MetricsDefinitionVersionRecord squash(RecordHelper<MetricsDefinitionVersionRecord> versionHelper,
                                                        String baseId,
                                                        String finalId)
    {
        MetricsDefinitionVersionRecord rec_base       = versionHelper.get(baseId);
        MetricsDefinitionVersionRecord rec_final      = versionHelper.get(finalId);
        MetricsDefinitionRecord        rec_definition = rec_base.getDefinition();

        //
        // Find first temporary entry.
        //
        while (true)
        {
            MetricsDefinitionVersionRecord rec = rec_base.getPredecessor();
            if (rec == null || !rec.isTemporary())
            {
                break;
            }

            rec_base = rec;
        }

        //
        // Convert first temporary entry to permanent and store final details.
        //
        MetricsDefinitionDetails finalDetails = rec_final.getDetailsForCopy();
        finalDetails.temporary = false;
        rec_base.setDetails(finalDetails);
        rec_base.makeHead();

        List<MetricsDefinitionVersionRecord> allVersions  = rec_definition.getVersions();
        List<MetricsDefinitionVersionRecord> tempVersions = Lists.newArrayList();

        for (MetricsDefinitionVersionRecord rec : allVersions)
        {
            if (rec.isTemporary())
            {
                tempVersions.add(rec);
            }
            else
            {
                MetricsDefinitionVersionRecord rec_pred = rec.getPredecessor();
                if (rec_pred != null && rec_pred.isTemporary())
                {
                    rec.setPredecessor(rec_base);
                }
            }
        }

        for (MetricsDefinitionVersionRecord rec_temp : tempVersions)
        {
            versionHelper.delete(rec_temp);
        }

        return rec_base;
    }

    public static List<MetricsDefinitionVersionRecord> getBatch(RecordHelper<MetricsDefinitionVersionRecord> helper,
                                                                List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<MetricsDefinitionVersionRecord> helper)
    {
        if (validation.canProceed())
        {
            helper.delete(this);
        }
    }

    //--//

    public EngineExecutionProgram<MetricsDefinitionDetails> prepareProgram(SessionHolder sessionHolder)
    {
        // If there are issues, the constructor will throw.
        return new EngineExecutionProgram<>(sessionHolder, getDetails());
    }
}
