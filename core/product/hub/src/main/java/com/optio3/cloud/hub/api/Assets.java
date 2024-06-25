/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.logic.location.LongitudeAndLatitudeRecords;
import com.optio3.cloud.hub.logic.metrics.MetricsBindingSpooler;
import com.optio3.cloud.hub.logic.samples.SamplesCache;
import com.optio3.cloud.hub.logic.spooler.ResultStagingSpooler;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.SummaryFlavor;
import com.optio3.cloud.hub.model.SummaryResult;
import com.optio3.cloud.hub.model.asset.Asset;
import com.optio3.cloud.hub.model.asset.AssetFilterRequest;
import com.optio3.cloud.hub.model.asset.AssetFilterResponse;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.model.asset.AssetTravelLog;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.asset.DeviceElementReportProgress;
import com.optio3.cloud.hub.model.asset.DeviceFilterRequest;
import com.optio3.cloud.hub.model.asset.GatewayFilterRequest;
import com.optio3.cloud.hub.model.asset.HostFilterRequest;
import com.optio3.cloud.hub.model.asset.LocationFilterRequest;
import com.optio3.cloud.hub.model.asset.NetworkFilterRequest;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphRequest;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphResponse;
import com.optio3.cloud.hub.model.metrics.MetricsDefinition;
import com.optio3.cloud.hub.model.tags.TagsJoinQuery;
import com.optio3.cloud.hub.model.tags.TagsSummary;
import com.optio3.cloud.hub.orchestration.tasks.TaskForDeviceElementReport;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.HostAssetRecord;
import com.optio3.cloud.hub.persistence.alert.AlertHistoryRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.MetricsDeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.asset.RelationshipRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowRecord;
import com.optio3.cloud.model.RawImport;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.MetadataTagsMap;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.search.HibernateSearch;
import com.optio3.logging.Severity;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import io.dropwizard.jersey.jsr310.ZonedDateTimeParam;
import io.swagger.annotations.Api;

@Api(tags = { "Assets" }) // For Swagger
@Optio3RestEndpoint(name = "Assets") // For Optio3 Shell
@Path("/v1/assets")
public class Assets
{
    @Inject
    private HubApplication m_app;

    @Inject
    private HubConfiguration m_cfg;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("summary")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<SummaryResult> getSummary(@QueryParam("groupBy") SummaryFlavor groupBy,
                                          AssetFilterRequest filters)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            List<SummaryResult> res = Lists.newArrayList();

            switch (BoxingUtils.get(groupBy, SummaryFlavor.location))
            {
                case location:
                {
                    Map<String, Number> counts;

                    DeviceFilterRequest deviceFilters = Reflection.as(filters, DeviceFilterRequest.class);
                    if (deviceFilters != null)
                    {
                        counts = DeviceRecord.countDevicesByLocation(sessionHolder.createHelper(DeviceRecord.class), deviceFilters);
                    }
                    else
                    {
                        counts = AssetRecord.countAssetsByLocation(sessionHolder.createHelper(AssetRecord.class), filters);
                    }

                    LocationsEngine          locationsEngine   = sessionHolder.getServiceNonNull(LocationsEngine.class);
                    LocationsEngine.Snapshot locationsSnapshot = locationsEngine.acquireSnapshot(false);
                    locationsSnapshot.accumulateByTopLevelLocations(res, counts);
                    break;
                }

                case manufacturer:
                {
                    DeviceFilterRequest deviceFilters = Reflection.as(filters, DeviceFilterRequest.class);
                    if (deviceFilters != null)
                    {
                        Map<String, Number> counts = DeviceRecord.countDevicesByManufacturer(sessionHolder.createHelper(DeviceRecord.class), deviceFilters);

                        for (String cat : counts.keySet())
                        {
                            SummaryResult obj = new SummaryResult();
                            obj.type  = SummaryFlavor.manufacturer;
                            obj.label = cat;
                            obj.count = counts.get(cat)
                                              .intValue();
                            res.add(obj);
                        }
                    }
                    break;
                }

                case relation:
                {
                    Map<AssetRelationship, Integer> map = RelationshipRecord.count(sessionHolder);

                    for (AssetRelationship relationship : map.keySet())
                    {
                        SummaryResult obj = new SummaryResult();
                        obj.id    = relationship.name();
                        obj.type  = SummaryFlavor.relation;
                        obj.label = relationship.getDisplayName();
                        obj.count = map.get(relationship);
                        res.add(obj);
                    }
                    break;
                }
            }

            return res;
        }
    }

    @GET
    @Path("travel-log/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public AssetTravelLog getTravelLog(@PathParam("id") String id,
                                       @QueryParam("maxGapForSegmentInMeters") Integer maxGapForSegmentInMetersParam,
                                       @QueryParam("maxDurationPerSegmentInSeconds") Integer maxDurationPerSegmentInSecondsParam,
                                       @QueryParam("rangeStart") ZonedDateTimeParam rangeStart,
                                       @QueryParam("rangeEnd") ZonedDateTimeParam rangeEnd)
    {
        LongitudeAndLatitudeRecords lookup = extractLookup(id);
        if (lookup == null)
        {
            return null;
        }

        return extractLog(lookup, maxGapForSegmentInMetersParam, maxDurationPerSegmentInSecondsParam, rangeStart != null ? rangeStart.get() : null, rangeEnd != null ? rangeEnd.get() : null);
    }

    private LongitudeAndLatitudeRecords extractLookup(String id)
    {
        LongitudeAndLatitudeRecords lookup = new LongitudeAndLatitudeRecords();

        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            lookup.locate(sessionHolder, sessionHolder.getEntity(AssetRecord.class, id));
        }

        if (!lookup.isValid())
        {
            return null;
        }
        return lookup;
    }

    private AssetTravelLog extractLog(LongitudeAndLatitudeRecords lookup,
                                      Integer maxGapForSegmentParam,
                                      Integer maxDurationPerSegmentParam,
                                      ZonedDateTime rangeStart,
                                      ZonedDateTime rangeEnd)
    {
        AssetTravelLog res = new AssetTravelLog();

        res.collect(m_sessionProvider.getServiceNonNull(SamplesCache.class),
                    lookup.longitude,
                    lookup.latitude,
                    rangeStart,
                    rangeEnd,
                    BoxingUtils.get(maxGapForSegmentParam, 1000),
                    BoxingUtils.get(maxDurationPerSegmentParam, 3600));

        return res;
    }

    //--//

    @POST
    @Path("filter")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public AssetFilterResponse getFiltered(AssetFilterRequest filters)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeviceFilterRequest deviceFilters = Reflection.as(filters, DeviceFilterRequest.class);
            if (deviceFilters != null)
            {
                return filters.handlePagination(DeviceRecord.filterDevices(sessionHolder.createHelper(DeviceRecord.class), deviceFilters));
            }

            DeviceElementFilterRequest deviceElementFilters = Reflection.as(filters, DeviceElementFilterRequest.class);
            if (deviceElementFilters != null)
            {
                return filters.handlePagination(DeviceElementRecord.filterDeviceElements(sessionHolder.createHelper(DeviceElementRecord.class), deviceElementFilters));
            }

            GatewayFilterRequest gatewayFilters = Reflection.as(filters, GatewayFilterRequest.class);
            if (gatewayFilters != null)
            {
                return filters.handlePagination(GatewayAssetRecord.filterGateways(sessionHolder.createHelper(GatewayAssetRecord.class), gatewayFilters));
            }

            NetworkFilterRequest networkFilters = Reflection.as(filters, NetworkFilterRequest.class);
            if (networkFilters != null)
            {
                return filters.handlePagination(NetworkAssetRecord.filterNetworks(sessionHolder.createHelper(NetworkAssetRecord.class), networkFilters));
            }

            LocationFilterRequest locationFilters = Reflection.as(filters, LocationFilterRequest.class);
            if (locationFilters != null)
            {
                return filters.handlePagination(LocationRecord.filterLocations(sessionHolder.createHelper(LocationRecord.class), locationFilters));
            }

            HostFilterRequest hostFilters = Reflection.as(filters, HostFilterRequest.class);
            if (hostFilters != null)
            {
                return filters.handlePagination(HostAssetRecord.filterHosts(sessionHolder.createHelper(HostAssetRecord.class), hostFilters));
            }

            return filters.handlePagination(AssetRecord.filterAssets(sessionHolder.createHelper(AssetRecord.class), filters));
        }
    }

    @POST
    @Path("count")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public long getFilteredCount(AssetFilterRequest filters)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            // Disable any sorting while counting
            filters.sortBy = null;

            DeviceFilterRequest deviceFilters = Reflection.as(filters, DeviceFilterRequest.class);
            if (deviceFilters != null)
            {
                return DeviceRecord.countDevices(sessionHolder.createHelper(DeviceRecord.class), deviceFilters);
            }

            DeviceElementFilterRequest deviceElementFilters = Reflection.as(filters, DeviceElementFilterRequest.class);
            if (deviceElementFilters != null)
            {
                return DeviceElementRecord.countDeviceElements(sessionHolder.createHelper(DeviceElementRecord.class), deviceElementFilters);
            }

            GatewayFilterRequest gatewayFilters = Reflection.as(filters, GatewayFilterRequest.class);
            if (gatewayFilters != null)
            {
                return GatewayAssetRecord.countGateways(sessionHolder.createHelper(GatewayAssetRecord.class), gatewayFilters);
            }

            NetworkFilterRequest networkFilters = Reflection.as(filters, NetworkFilterRequest.class);
            if (networkFilters != null)
            {
                return NetworkAssetRecord.countNetworks(sessionHolder.createHelper(NetworkAssetRecord.class), networkFilters);
            }

            LocationFilterRequest locationFilters = Reflection.as(filters, LocationFilterRequest.class);
            if (locationFilters != null)
            {
                return LocationRecord.countLocations(sessionHolder.createHelper(LocationRecord.class), locationFilters);
            }

            HostFilterRequest hostFilters = Reflection.as(filters, HostFilterRequest.class);
            if (hostFilters != null)
            {
                return HostAssetRecord.countHosts(sessionHolder.createHelper(HostAssetRecord.class), hostFilters);
            }

            return AssetRecord.countAssets(sessionHolder.createHelper(AssetRecord.class), filters);
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<Asset> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            final RecordHelper<AssetRecord> helper = sessionHolder.createHelper(AssetRecord.class);
            List<AssetRecord>               lst    = AssetRecord.getAssetsBatch(helper, ids);

            ResultStagingSpooler spooler = sessionHolder.getServiceNonNull(ResultStagingSpooler.class);
            spooler.flushAssets(lst, Duration.of(100, ChronoUnit.MILLIS));

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, lst);
        }
    }

    @POST
    @Path("create")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    public Asset create(Asset model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<AssetRecord> helper = sessionHolder.createHelper(AssetRecord.class);
            AssetRecord               rec    = model.newRecord();

            ModelMapperPolicy policy = m_cfg.getPolicyWithOverrideForMaintenanceUser(sessionHolder, m_principalAccessor);

            ModelMapper.fromModel(sessionHolder, policy, model, rec);

            helper.persist(rec);

            AssetRecord rec_parent = sessionHolder.fromIdentity(model.parentAsset);
            if (rec_parent != null)
            {
                rec.linkToParent(helper, rec_parent);
            }

            rec.assetPostCreate(sessionHolder);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, policy, rec);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public Asset get(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            AssetRecord rec = sessionHolder.getEntity(AssetRecord.class, id);

            ResultStagingSpooler spooler = sessionHolder.getServiceNonNull(ResultStagingSpooler.class);
            spooler.flushAssets(Lists.newArrayList(rec), Duration.of(100, ChronoUnit.MILLIS));

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    Asset model) throws
                                                 Exception
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkAnyRoles(m_principalAccessor, WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator);

            RecordHelper<AssetRecord> helper = validation.sessionHolder.createHelper(AssetRecord.class);
            AssetRecord               rec    = helper.get(id);

            if (validation.canProceed())
            {
                LocationRecord oldLocation = rec.getLocation();
                ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec);
                LocationRecord newLocation = rec.getLocation();

                if (oldLocation != newLocation)
                {
                    rec.propagateLocationChangeToChildren(HubApplication.LoggerInstance, helper, oldLocation, newLocation);
                }

                rec.assetPostUpdate(validation.sessionHolder);
            }

            return validation.getResults();
        }
    }

    @DELETE
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults remove(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun) throws
                                                                          Exception
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkAnyRoles(m_principalAccessor, WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator);

            RecordHelper<AssetRecord> helper = validation.sessionHolder.createHelper(AssetRecord.class);

            AssetRecord rec = helper.getOrNull(id);
            if (rec != null)
            {
                //
                // Close all the gates, since the delete can cascade to a lot of changes.
                //
                try (var searchGate = m_app.closeGate(HibernateSearch.Gate.class))
                {
                    try (var tagsGate = m_app.closeGate(TagsEngine.Gate.class))
                    {
                        try (var spoolerGate = m_app.closeGate(ResultStagingSpooler.Gate.class))
                        {
                            try (var metricsGate = m_app.closeGate(MetricsBindingSpooler.Gate.class))
                            {
                                rec.remove(validation, helper);
                            }
                        }
                    }
                }
            }

            return validation.getResults();
        }
    }

    @GET
    @Path("item/{id}/reassign-to/{parentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults reassignParent(@PathParam("id") String id,
                                            @PathParam("parentId") String parentId,
                                            @QueryParam("dryRun") Boolean dryRun)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkAnyRoles(m_principalAccessor, WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator);

            RecordHelper<AssetRecord> helper = validation.sessionHolder.createHelper(AssetRecord.class);

            AssetRecord rec        = helper.get(id);
            AssetRecord rec_parent = helper.get(parentId);

            rec.linkToParent(helper, rec_parent, validation);

            return validation.getResults();
        }
    }

    @GET
    @Path("alert-history/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<AlertHistoryRecord> getHistoryByID(@PathParam("id") String id,
                                                                      @QueryParam("rangeStart") ZonedDateTimeParam rangeStartParam,
                                                                      @QueryParam("rangeEnd") ZonedDateTimeParam rangeEndParam)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            AssetRecord   rec        = sessionHolder.getEntity(AssetRecord.class, id);
            ZonedDateTime rangeStart = rangeStartParam != null ? rangeStartParam.get() : null;
            ZonedDateTime rangeEnd   = rangeEndParam != null ? rangeEndParam.get() : null;

            return AlertHistoryRecord.listSorted(sessionHolder.createHelper(AlertHistoryRecord.class), rec, rangeStart, rangeEnd, 0);
        }
    }

    @GET
    @Path("lookup-metrics/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public MetricsDefinition lookupMetrics(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            MetricsDeviceElementRecord rec = sessionHolder.getEntityOrNull(MetricsDeviceElementRecord.class, id);
            if (rec != null)
            {
                MetricsDefinitionRecord rec_def = rec.getMetricsDefinition();
                return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_def);
            }

            return null;
        }
    }

    @GET
    @Path("active-workflows/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<RecordIdentity> getActiveWorkflows(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            AssetRecord rec = sessionHolder.getEntity(AssetRecord.class, id);

            return CollectionUtils.transformToList(rec.getActiveWorkflows(), TypedRecordIdentity::newTypedInstance);
        }
    }

    @POST
    @Path("active-workflows-batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<TypedRecordIdentityList<WorkflowRecord>> getActiveWorkflowsBatch(List<String> ids)
    {
        return CollectionUtils.transformInParallel(ids, HubApplication.GlobalRateLimiter, (id) ->
        {
            try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
            {
                AssetRecord rec = sessionHolder.getEntity(AssetRecord.class, id);

                TypedRecordIdentityList<WorkflowRecord> res = new TypedRecordIdentityList<>();
                for (WorkflowRecord rec_workflow : rec.getActiveWorkflows())
                {
                    res.add(TypedRecordIdentity.newTypedInstance(rec_workflow));
                }
                return res;
            }
        });
    }

    //--//

    @POST
    @Path("device-elements/report/start/{deviceSysId}")
    @Produces(MediaType.TEXT_PLAIN)
    public String startDeviceElementsReport(@PathParam("deviceSysId") String deviceSysId) throws
                                                                                          Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, (sessionHolder) -> TaskForDeviceElementReport.scheduleTask(sessionHolder, deviceSysId));

        return loc_task.getIdRaw();
    }

    @GET
    @Path("device-elements/report/check/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceElementReportProgress checkDeviceElementReport(@PathParam("id") String id,
                                                                @QueryParam("detailed") Boolean detailed)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.getProgress(helper, id, BoxingUtils.get(detailed), TaskForDeviceElementReport.class);
        }
    }

    @GET
    @Path("device-elements/report/excel/{id}/{fileName}")
    @Produces("application/octet-stream")
    @Optio3RequestLogLevel(Severity.Debug)
    public InputStream streamDeviceElementReport(@PathParam("id") String id,
                                                 @PathParam("fileName") String fileName) throws
                                                                                         Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.streamContents(helper, id, TaskForDeviceElementReport.class);
        }
    }

    //--//

    @GET
    @Path("tags-summary")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TagsSummary tagsSummary(@QueryParam("recomputeIfChanged") Integer recomputeIfChanged) throws
                                                                                                 Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            TagsEngine          tagsEngine   = sessionHolder.getServiceNonNull(TagsEngine.class);
            TagsEngine.Snapshot tagsSnapshot = tagsEngine.acquireSnapshot(false);

            return tagsSnapshot.computeSummary(m_sessionProvider, recomputeIfChanged, tagsEngine.getActiveNormalizationRules(sessionHolder));
        }
    }

    @POST
    @Path("tags-query")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<String[]> tagsQuery(TagsJoinQuery query)
    {
        TagsEngine          tagsEngine   = m_sessionProvider.getServiceNonNull(TagsEngine.class);
        TagsEngine.Snapshot tagsSnapshot = tagsEngine.acquireSnapshot(false);

        List<String[]> results = Lists.newArrayList();

        tagsSnapshot.evaluateJoin(query, (tuple) ->
        {
            results.add(tuple.asSysIds());
            return null;
        });

        return results;
    }

    @POST
    @Path("tags-query-distinct")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public Set<RecordIdentity> tagsQueryDistinct(TagsJoinQuery query,
                                                 @QueryParam("level") Optional<Integer> level)
    {
        TagsEngine          tagsEngine   = m_sessionProvider.getServiceNonNull(TagsEngine.class);
        TagsEngine.Snapshot tagsSnapshot = tagsEngine.acquireSnapshot(false);

        Set<RecordIdentity> assets = Sets.newHashSet();

        int desiredIndex = level.orElse(1) - 1;

        tagsSnapshot.evaluateJoin(query, (tuple) ->
        {
            TypedRecordIdentity<? extends AssetRecord>[] res = tuple.asRecordIdentities();
            assets.add(res[desiredIndex]);
            return null;
        });

        return assets;
    }

    @POST
    @Path("asset-graph/evaluate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public AssetGraphResponse evaluateAssetGraph(AssetGraphRequest request)
    {
        TagsEngine          tagsEngine   = m_sessionProvider.getServiceNonNull(TagsEngine.class);
        TagsEngine.Snapshot tagsSnapshot = tagsEngine.acquireSnapshot(false);

        int max = 0;

        // First request, load everything to properly detect errors
        if (request.startOffset == 0 && request.maxResults > 0)
        {
            max                = request.maxResults;
            request.maxResults = -1;
        }

        AssetGraphResponse response = request.evaluate(tagsSnapshot);

        if (max > 0)
        {
            if (response.results.size() > max)
            {
                List<AssetGraphResponse.Resolved> subList = Lists.newArrayList(response.results.subList(0, max));
                response.results.clear();
                response.results.addAll(subList);
                response.nextOffset = max;
            }
        }

        return response;
    }

    //--//

    @GET
    @Path("tags/{id}/{tag}/get")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public Collection<String> getTag(@PathParam("id") String id,
                                     @PathParam("tag") String tag)
    {
        if (AssetRecord.WellKnownTags.isSystemTag(tag))
        {
            return Collections.emptyList();
        }

        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            AssetRecord     rec  = sessionHolder.getEntity(AssetRecord.class, id);
            MetadataTagsMap tags = rec.accessTags();

            return tags.getValuesForTag(tag);
        }
    }

    @POST
    @Path("tags/{id}/{tag}/set")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public Collection<String> setTag(@PathParam("id") String id,
                                     @PathParam("tag") String tag,
                                     Set<String> values)
    {
        if (AssetRecord.WellKnownTags.isSystemTag(tag))
        {
            return Collections.emptyList();
        }

        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            AssetRecord rec = sessionHolder.getEntity(AssetRecord.class, id);

            Collection<String> res = rec.modifyTags((Function<MetadataTagsMap, Collection<String>>) tagMap -> tagMap.setValuesForTag(tag, values));

            sessionHolder.commit();

            return res;
        }
    }

    @GET
    @Path("tags/{id}/{tag}/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public boolean removeTag(@PathParam("id") String id,
                             @PathParam("tag") String tag)
    {
        if (AssetRecord.WellKnownTags.isSystemTag(tag))
        {
            return false;
        }

        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            AssetRecord rec = sessionHolder.getEntity(AssetRecord.class, id);

            Boolean res = rec.modifyTags((Function<MetadataTagsMap, Boolean>) (tags) -> tags.removeTag(tag));

            sessionHolder.commit();

            return res;
        }
    }

    @POST
    @Path("parse-import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Asset parseImport(RawImport rawImport)
    {
        return rawImport.validate(Asset.class);
    }
}