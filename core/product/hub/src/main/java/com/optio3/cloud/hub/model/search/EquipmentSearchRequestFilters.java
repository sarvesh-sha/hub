/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.search;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.SearchResultSet;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.search.HibernateSearch;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;

@JsonTypeName("EquipmentSearchRequestFilters")
public class EquipmentSearchRequestFilters extends SearchRequestFilters
{
    public boolean isClassified;
    public boolean isUnclassified;

    @Override
    public Class<LogicalAssetRecord> getRecordClass()
    {
        return LogicalAssetRecord.class;
    }

    @Override
    public Query buildQuery(QueryBuilder qb)
    {

        BooleanJunction bool = qb.bool();

        if (isClassified || isUnclassified)
        {
            bool = bool.must(qb.keyword()
                               .onField("classified")
                               .matching(isClassified ? "true" : "false")
                               .createQuery());

            return bool.createQuery();
        }

        return null;
    }

    @Override
    public void updateResultSet(SearchResultSet results,
                                HibernateSearch.ResultSet queryResults)
    {
        final HibernateSearch.Results<LogicalAssetRecord> groups = queryResults.getResults(LogicalAssetRecord.class);
        results.logicalGroups      = groups;
        results.totalLogicalGroups = groups.totalResults;
    }
}
