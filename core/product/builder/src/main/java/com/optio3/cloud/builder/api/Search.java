/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.model.SearchResultSet;
import com.optio3.cloud.builder.model.search.SearchRequest;
import com.optio3.cloud.builder.model.search.SubSearchRequest;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.search.HibernateSearch;
import io.swagger.annotations.Api;

@Api(tags = { "Search" }) // For Swagger
@Optio3RestEndpoint(name = "Search") // For Optio3 Shell
@Path("/v1/search")
public class Search
{
    @Inject
    private BuilderApplication m_app;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("query")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SearchResultSet search(SearchRequest request,
                                  @QueryParam("offset") int offset,
                                  @QueryParam("limit") int limit) throws
                                                                  Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            HibernateSearch search = ensureSearch();

            if (request instanceof SubSearchRequest)
            {
                return searchAll(search, sessionHolder, (SubSearchRequest) request, offset, limit);
            }
            else
            {
                return searchAll(search, sessionHolder, request, offset, limit);
            }
        }
    }

    private SearchResultSet searchAll(HibernateSearch search,
                                      SessionHolder sessionHolder,
                                      SearchRequest request,
                                      int offset,
                                      int limit)
    {
        HibernateSearch.ResultSet resultSet = search.queryAll(sessionHolder, request.query, offset, limit);

        SearchResultSet result = new SearchResultSet();

        final HibernateSearch.Results<UserRecord> users = resultSet.getResults(UserRecord.class);
        result.users      = users;
        result.totalUsers = users.totalResults;

        final HibernateSearch.Results<CustomerRecord> customers = resultSet.getResults(CustomerRecord.class);
        result.customers      = customers;
        result.totalCustomers = customers.totalResults;

        final HibernateSearch.Results<CustomerServiceRecord> customerServices = resultSet.getResults(CustomerServiceRecord.class);
        result.customerServices      = customerServices;
        result.totalCustomerServices = customerServices.totalResults;

        final HibernateSearch.Results<DeploymentHostRecord> deploymentHosts = resultSet.getResults(DeploymentHostRecord.class);
        result.deploymentHosts      = deploymentHosts;
        result.totalDeploymentHosts = deploymentHosts.totalResults;

        return result;
    }

    private HibernateSearch ensureSearch() throws
                                           Exception
    {
        Optio3DataSourceFactory            dataSourceFactory = m_app.getDataSourceFactory(null);
        CompletableFuture<HibernateSearch> search            = dataSourceFactory.getHibernateSearch();
        if (search == null || !search.isDone())
        {
            throw new InvalidStateException("Search index is currently not available.");
        }

        return search.get();
    }
}
