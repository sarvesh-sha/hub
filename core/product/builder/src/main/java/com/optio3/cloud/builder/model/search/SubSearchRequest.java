/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.search;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;

@JsonTypeName("SubSearchRequest")
public class SubSearchRequest extends SearchRequest
{
    public Query buildQuery(QueryBuilder qb)
    {
        return null;
    }
}
