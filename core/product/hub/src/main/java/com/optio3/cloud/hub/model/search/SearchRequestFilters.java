/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.search;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.cloud.hub.model.SearchResultSet;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.search.HibernateSearch;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = DeviceElementSearchRequestFilters.class), @JsonSubTypes.Type(value = EquipmentSearchRequestFilters.class) })
public abstract class SearchRequestFilters
{
    public abstract Class<? extends RecordWithCommonFields> getRecordClass();

    public abstract Query buildQuery(QueryBuilder qb);

    public abstract void updateResultSet(SearchResultSet results,
                                         HibernateSearch.ResultSet queryResults);
}
