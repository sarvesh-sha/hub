/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.normalization;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.metamodel.SingularAttribute;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.model.config.UsageFilterRequest;
import com.optio3.cloud.hub.model.normalization.DeviceElementBaseRun;
import com.optio3.cloud.hub.model.normalization.Normalization;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithSequenceNumber;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.serialization.ObjectMappers;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "NORMALIZATION")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "Normalization", model = Normalization.class, metamodel = NormalizationRecord_.class)
public class NormalizationRecord extends RecordWithSequenceNumber<NormalizationRecord> implements ModelMapperTarget<Normalization, NormalizationRecord_>
{
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Lob
    @Column(name = "rules", nullable = false)
    @Basic(fetch = FetchType.LAZY)
    private String rules;

    @Transient
    private final PersistAsJsonHelper<String, NormalizationRules> m_rulesHelper = new PersistAsJsonHelper<>(() -> rules,
                                                                                                            (val) -> rules = val,
                                                                                                            String.class,
                                                                                                            NormalizationRules.class,
                                                                                                            ObjectMappers.SkipNulls);

    //--//

    public static NormalizationRecord newInstance(RecordHelper<NormalizationRecord> helper,
                                                  NormalizationRules rules,
                                                  Integer version)
    {
        helper.lockTableUntilEndOfTransaction(10, TimeUnit.SECONDS);

        NormalizationRecord rec_new = new NormalizationRecord();
        rec_new.setRules(rules);

        rec_new.version = rec_new.assignUniqueNumber(helper, version, null);

        helper.persist(rec_new);

        return rec_new;
    }

    public static int checkUsages(SessionProvider sessionProvider,
                                  UsageFilterRequest filters,
                                  TypedRecordIdentityList<NormalizationRecord> lst)
    {
        return filters.analyzeRecords(sessionProvider, lst, NormalizationRecord.class, (qh) ->
        {
            qh.addString(NormalizationRecord_.rules, (obj, val) -> obj.value1 = val);
        });
    }

    @Override
    protected SingularAttribute<NormalizationRecord, Integer> fetchSequenceNumberField()
    {
        return NormalizationRecord_.version;
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

    public boolean isActive()
    {
        return active;
    }

    public NormalizationRules getRules() throws
                                         Exception
    {
        return m_rulesHelper.get();
    }

    public boolean setRules(NormalizationRules rules)
    {
        return m_rulesHelper.set(rules);
    }

    //--//

    public static TypedRecordIdentityList<NormalizationRecord> list(RecordHelper<NormalizationRecord> helper)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            jh.addOrderBy(jh.root, NormalizationRecord_.version, false);
        });
    }

    public static List<NormalizationRecord> getBatch(RecordHelper<NormalizationRecord> helper,
                                                     List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public void makeActive(RecordHelper<NormalizationRecord> helper)
    {
        helper.lockTableUntilEndOfTransaction(10, TimeUnit.SECONDS);

        NormalizationRecord rec_active = findActive(helper);
        if (rec_active != this)
        {
            if (rec_active != null)
            {
                rec_active.active = false;
            }

            this.active = true;
        }
    }

    public static NormalizationRecord findActive(RecordHelper<NormalizationRecord> helper)
    {
        return QueryHelperWithCommonFields.getFirstMatch(helper, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, NormalizationRecord_.active, true);
        });
    }

    public static NormalizationRecord findVersion(RecordHelper<NormalizationRecord> helper,
                                                  int version)
    {
        return QueryHelperWithCommonFields.getFirstMatch(helper, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, NormalizationRecord_.version, version);
        });
    }

    //--//

    public static List<RecordLocator<DeviceRecord>> extractDevices(RecordHelper<DeviceRecord> helper,
                                                                   DeviceElementBaseRun run) throws
                                                                                             Exception
    {
        List<RecordLocator<DeviceRecord>> locators = Lists.newArrayList();

        if (run.devices == null)
        {
            RecordHelper<NetworkAssetRecord> helperNetwork = helper.wrapFor(NetworkAssetRecord.class);
            RecordHelper<DeviceRecord>       helperDevice  = helper.wrapFor(DeviceRecord.class);

            for (NetworkAssetRecord rec_network : helperNetwork.listAll())
            {
                rec_network.enumerateChildrenNoNesting(helperDevice, -1, (filters) -> filters.addState(AssetState.operational), (rec_device) ->
                {
                    locators.add(helper.asLocator(rec_device));

                    return StreamHelperNextAction.Continue_Evict;
                });
            }
        }
        else
        {
            for (TypedRecordIdentity<DeviceRecord> ri : run.devices)
            {
                locators.add(RecordLocator.create(helper, ri));
            }
        }

        return locators;
    }
}

