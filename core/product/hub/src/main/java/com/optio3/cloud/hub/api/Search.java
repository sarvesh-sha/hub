/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

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
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.SearchResultSet;
import com.optio3.cloud.hub.model.search.SearchRequest;
import com.optio3.cloud.hub.model.search.SearchRequestFilters;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
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
    private HubApplication m_app;

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
            HibernateSearch search  = ensureSearch();
            SearchResultSet results = new SearchResultSet();

            if (!request.scopeToFilters)
            {
                searchAll(search, sessionHolder, results, request, offset, limit);
            }

            if (request.filters != null)
            {
                for (SearchRequestFilters filters : request.filters)
                {
                    HibernateSearch.ResultSet queryResults = search.query(sessionHolder, filters.getRecordClass(), request.query, offset, limit, filters::buildQuery);
                    filters.updateResultSet(results, queryResults);
                }
            }

            return results;
        }
    }

    private void searchAll(HibernateSearch search,
                           SessionHolder sessionHolder,
                           SearchResultSet result,
                           SearchRequest request,
                           int offset,
                           int limit)
    {
        HibernateSearch.ResultSet resultSet = search.queryAll(sessionHolder, request.query, offset, limit);

        final HibernateSearch.Results<UserRecord> users = resultSet.getResults(UserRecord.class);
        result.users      = users;
        result.totalUsers = users.totalResults;

        final HibernateSearch.Results<DeviceRecord> devices = resultSet.getResults(DeviceRecord.class);
        result.devices      = devices;
        result.totalDevices = devices.totalResults;

        final HibernateSearch.Results<AlertRecord> alerts = resultSet.getResults(AlertRecord.class);
        result.alerts      = alerts;
        result.totalAlerts = alerts.totalResults;

        final HibernateSearch.Results<LocationRecord> locations = resultSet.getResults(LocationRecord.class);
        result.locations      = locations;
        result.totalLocations = locations.totalResults;

        final HibernateSearch.Results<NetworkAssetRecord> networks = resultSet.getResults(NetworkAssetRecord.class);
        result.networks      = networks;
        result.totalNetworks = networks.totalResults;

        final HibernateSearch.Results<GatewayAssetRecord> gateways = resultSet.getResults(GatewayAssetRecord.class);
        result.gateways      = gateways;
        result.totalGateways = gateways.totalResults;

        final HibernateSearch.Results<LogicalAssetRecord> logicalGroups = resultSet.getResults(LogicalAssetRecord.class);
        result.logicalGroups      = logicalGroups;
        result.totalLogicalGroups = logicalGroups.totalResults;

        final HibernateSearch.Results<DeviceElementRecord> deviceElements = resultSet.getResults(DeviceElementRecord.class);
        result.deviceElements      = deviceElements;
        result.totalDeviceElements = deviceElements.totalResults;
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
