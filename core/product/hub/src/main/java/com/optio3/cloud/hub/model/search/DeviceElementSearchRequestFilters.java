/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.search;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.SearchResultSet;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.search.HibernateSearch;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;

@JsonTypeName("DeviceElementSearchRequestFilters")
public class DeviceElementSearchRequestFilters extends SearchRequestFilters
{
    public boolean hasAnySampling;
    public boolean hasNoSampling;

    public boolean isClassified;
    public boolean isUnclassified;

    public boolean isHidden;
    public boolean isNotHidden;

    @Override
    public Class<DeviceElementRecord> getRecordClass()
    {
        return DeviceElementRecord.class;
    }

    @Override
    public Query buildQuery(QueryBuilder qb)
    {

        BooleanJunction bool = qb.bool();
        if (hasAnySampling || hasNoSampling)
        {
            bool = bool.must(qb.keyword()
                               .onField("sampled")
                               .matching(hasAnySampling ? "true" : "false")
                               .createQuery());
        }

        if (isClassified || isUnclassified)
        {
            bool = bool.must(qb.keyword()
                               .onField("classified")
                               .matching(isClassified ? "true" : "false")
                               .createQuery());
        }

        if (isHidden || isNotHidden)
        {
            bool = bool.must(qb.keyword()
                               .onField("hidden")
                               .matching(isHidden ? "true" : "false")
                               .createQuery());
        }

        if (!bool.isEmpty())
        {
            return bool.createQuery();
        }

        return null;
    }

    @Override
    public void updateResultSet(SearchResultSet results,
                                HibernateSearch.ResultSet queryResults)
    {
        final HibernateSearch.Results<DeviceElementRecord> deviceElements = queryResults.getResults(DeviceElementRecord.class);
        results.deviceElements      = deviceElements;
        results.totalDeviceElements = deviceElements.totalResults;
    }
}
