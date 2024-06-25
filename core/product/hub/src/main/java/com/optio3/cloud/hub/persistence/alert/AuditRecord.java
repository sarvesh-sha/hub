/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.alert;

import static java.util.Objects.requireNonNull;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Tuple;

import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.model.audit.Audit;
import com.optio3.cloud.hub.model.audit.AuditFilterRequest;
import com.optio3.cloud.hub.model.audit.AuditType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.event.EventRecord;
import com.optio3.cloud.model.PaginatedRecordIdentityList;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.search.Optio3QueryAnalyzerOverride;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "AUDIT")
@DynamicUpdate // Due to HHH-11506
@Indexed
@Optio3QueryAnalyzerOverride("fuzzy_query")
@Optio3TableInfo(externalId = "Audit", model = Audit.class, metamodel = AuditRecord_.class)
public class AuditRecord extends EventRecord
{
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AuditType type;

    //--//

    public static AuditRecord newInstance(RecordHelper<AuditRecord> helper,
                                          Integer sequenceNumber,
                                          AssetRecord rec_asset,
                                          AuditType type)
    {
        requireNonNull(rec_asset);

        AuditRecord res = EventRecord.newInstance(helper, sequenceNumber, rec_asset);
        res.type = type;

        return res;
    }

    //--//

    public AuditType getType()
    {
        return type;
    }

    //--//

    private static class JoinHelper<T> extends EventRecord.JoinHelper<T, AuditRecord>
    {
        JoinHelper(RecordHelper<AuditRecord> helper,
                   Class<T> clz)
        {
            super(helper, clz);
        }

        //--//

        void applyFilters(AuditFilterRequest filters)
        {
            super.applyFilters(filters);

            if (filters.hasTypes())
            {
                filterByTypes(filters.auditTypeIDs);
            }
        }

        @Override
        protected void handleSortCriteria(SortCriteria sort)
        {
            switch (sort.column)
            {
                case "type":
                {
                    addOrderBy(root, AuditRecord_.type, sort.ascending);
                    break;
                }

                default:
                    super.handleSortCriteria(sort);
            }
        }

        //--//

        void filterByTypes(List<AuditType> lst)
        {
            addWhereClauseIn(root, AuditRecord_.type, lst);
        }
    }

    //--//

    public static PaginatedRecordIdentityList filter(RecordHelper<AuditRecord> helper,
                                                     AuditFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        //--//

        return JoinHelper.returnFilterTuples(helper, jh);
    }

    public static long count(RecordHelper<AuditRecord> helper,
                             AuditFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return jh.count();
    }
}
