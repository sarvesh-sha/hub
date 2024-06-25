/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.alert;

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
import com.optio3.cloud.hub.engine.alerts.AlertDefinitionDetails;
import com.optio3.cloud.hub.model.alert.AlertDefinitionVersion;
import com.optio3.cloud.hub.model.config.UsageFilterRequest;
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
@Table(name = "ALERT_DEFINITION_VERSION")
@DynamicUpdate
@Optio3TableInfo(externalId = "AlertDefinitionVersion", model = AlertDefinitionVersion.class, metamodel = AlertDefinitionVersionRecord_.class)
public class AlertDefinitionVersionRecord extends RecordWithSequenceNumber<AlertDefinitionVersionRecord> implements ModelMapperTarget<AlertDefinitionVersion, AlertDefinitionVersionRecord_>
{
    @Column(name = "version", nullable = false)
    private int version;

    @Lob
    @Column(name = "details")
    private String details;

    @Transient
    private final PersistAsJsonHelper<String, AlertDefinitionDetails> m_detailsHelper = new PersistAsJsonHelper<>(() -> details,
                                                                                                                  (val) -> details = val,
                                                                                                                  String.class,
                                                                                                                  AlertDefinitionDetails.class,
                                                                                                                  ObjectMappers.SkipNulls,
                                                                                                                  true);

    //--//

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getDefinition")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getDefinition")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "definition", nullable = false, foreignKey = @ForeignKey(name = "ALERT_DEFINITION_VERSION_DEFINITION__FK"))
    private AlertDefinitionRecord definition;

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getPredecessor")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getPredecessor", setter = "setPredecessor")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "predecessor", nullable = true, foreignKey = @ForeignKey(name = "ALERT_DEFINITION_VERSION_PREDECESSOR__FK"))
    private AlertDefinitionVersionRecord predecessor;

    @OneToMany(mappedBy = "predecessor", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("version DESC")
    private List<AlertDefinitionVersionRecord> successors;

    //--//

    public static AlertDefinitionVersionRecord newInstance(RecordHelper<AlertDefinitionVersionRecord> helper,
                                                           AlertDefinitionVersion model,
                                                           AlertDefinitionRecord rec_alertDefinition,
                                                           AlertDefinitionVersionRecord rec_predecessor,
                                                           Integer version)
    {
        AlertDefinitionVersionRecord rec_new = new AlertDefinitionVersionRecord();
        rec_new.definition  = rec_alertDefinition;
        rec_new.predecessor = rec_predecessor;
        rec_new.setDetails(model.details);

        rec_new.version = rec_new.assignUniqueNumber(helper, version, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, AlertDefinitionVersionRecord_.definition, rec_alertDefinition);
        });

        helper.persist(rec_new);

        rec_alertDefinition.setHeadVersion(rec_new);

        if (rec_alertDefinition.getReleaseVersion() == null)
        {
            rec_alertDefinition.setReleaseVersion(rec_new);
        }

        return rec_new;
    }

    public static int checkUsages(SessionProvider sessionProvider,
                                  UsageFilterRequest filters,
                                  TypedRecordIdentityList<AlertDefinitionVersionRecord> lst)
    {
        return filters.analyzeRecords(sessionProvider, lst, AlertDefinitionVersionRecord.class, (qh) ->
        {
            qh.addString(AlertDefinitionVersionRecord_.details, (obj, val) -> obj.value1 = val);
        });
    }

    @Override
    protected SingularAttribute<AlertDefinitionVersionRecord, Integer> fetchSequenceNumberField()
    {
        return AlertDefinitionVersionRecord_.version;
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

    public AlertDefinitionDetails getDetails()
    {
        return m_detailsHelper.get();
    }

    public AlertDefinitionDetails getDetailsForCopy()
    {
        return m_detailsHelper.getNoCaching();
    }

    public boolean setDetails(AlertDefinitionDetails details)
    {
        return m_detailsHelper.set(details);
    }

    public boolean isTemporary()
    {
        return getDetails().temporary;
    }

    public AlertDefinitionRecord getDefinition()
    {
        return definition;
    }

    public AlertDefinitionVersionRecord getPredecessor()
    {
        return predecessor;
    }

    public boolean setPredecessor(AlertDefinitionVersionRecord predecessor)
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

    public List<AlertDefinitionVersionRecord> getSuccessors()
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

    public static AlertDefinitionVersionRecord squash(RecordHelper<AlertDefinitionVersionRecord> versionHelper,
                                                      String baseId,
                                                      String finalId)
    {
        AlertDefinitionVersionRecord rec_base       = versionHelper.get(baseId);
        AlertDefinitionVersionRecord rec_final      = versionHelper.get(finalId);
        AlertDefinitionRecord        rec_definition = rec_base.getDefinition();

        //
        // Find first temporary entry.
        //
        while (true)
        {
            AlertDefinitionVersionRecord rec = rec_base.getPredecessor();
            if (rec == null || !rec.isTemporary())
            {
                break;
            }

            rec_base = rec;
        }

        //
        // Convert first temporary entry to permanent and store final details.
        //
        AlertDefinitionDetails finalDetails = rec_final.getDetailsForCopy();
        finalDetails.temporary = false;
        rec_base.setDetails(finalDetails);
        rec_base.makeHead();

        List<AlertDefinitionVersionRecord> allVersions  = rec_definition.getVersions();
        List<AlertDefinitionVersionRecord> tempVersions = Lists.newArrayList();

        for (AlertDefinitionVersionRecord rec : allVersions)
        {
            if (rec.isTemporary())
            {
                tempVersions.add(rec);
            }
            else
            {
                AlertDefinitionVersionRecord rec_pred = rec.getPredecessor();
                if (rec_pred != null && rec_pred.isTemporary())
                {
                    rec.setPredecessor(rec_base);
                }
            }
        }

        for (AlertDefinitionVersionRecord rec_temp : tempVersions)
        {
            versionHelper.delete(rec_temp);
        }

        return rec_base;
    }

    public static List<AlertDefinitionVersionRecord> getBatch(RecordHelper<AlertDefinitionVersionRecord> helper,
                                                              List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<AlertDefinitionVersionRecord> helper)
    {
        if (validation.canProceed())
        {
            helper.delete(this);
        }
    }

    //--//

    public EngineExecutionProgram<AlertDefinitionDetails> prepareProgram(SessionHolder sessionHolder)
    {
        // If there are issues, the constructor will throw.
        return new EngineExecutionProgram<>(sessionHolder, getDetails());
    }
}
