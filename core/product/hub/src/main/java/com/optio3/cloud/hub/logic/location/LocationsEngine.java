/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.location;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.turf.TurfJoins;
import com.mapbox.turf.TurfMeasurement;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.DeliveryOptions;
import com.optio3.cloud.hub.model.SummaryFlavor;
import com.optio3.cloud.hub.model.SummaryResult;
import com.optio3.cloud.hub.model.location.GeoFence;
import com.optio3.cloud.hub.model.location.GeoFenceByPolygon;
import com.optio3.cloud.hub.model.location.GeoFenceByRadius;
import com.optio3.cloud.hub.model.location.LocationHierarchy;
import com.optio3.cloud.hub.model.location.LocationPolygon;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.model.location.LongitudeLatitude;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord_;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord_;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.DatabaseActivity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.DbEvent;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.RecordWithMetadata_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.logging.LoggerPeriodic;
import com.optio3.logging.Severity;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;

public class LocationsEngine
{
    public static class Snapshot
    {
        private final State m_state;

        Snapshot(State state)
        {
            m_state = state;
        }

        public int getVersion()
        {
            return m_state.m_version;
        }

        //--//

        public String getNameLazy(LocationRecord rec)
        {
            if (rec == null)
            {
                return null;
            }

            // If it's a proxy, using "getSysId()" would not trigger a DB access.
            String name = getName(rec.getSysId());
            if (name != null)
            {
                return name;
            }

            // Not in the cache, access record directly.
            return rec.getName();
        }

        public String getName(String sysId)
        {
            LocationDetails details = getDetails(sysId);
            return details != null ? details.name : null;
        }

        public String getHierarchicalName(String sysId)
        {
            LocationDetails details = getDetails(sysId);
            if (details == null)
            {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            details.getHierarchicalName(sb);
            return sb.toString();
        }

        public LocationType getType(String sysId)
        {
            LocationDetails details = getDetails(sysId);
            return details != null ? details.type : null;
        }

        public String getAdtModel(String sysId)
        {
            LocationDetails details = getDetails(sysId);
            return details != null ? details.adtModel : null;
        }

        public Set<String> getTags(String sysId)
        {
            LocationDetails details = getDetails(sysId);
            return details != null ? details.tags : null;
        }

        public String getTimeZone(String sysId)
        {
            LocationDetails details = getDetails(sysId);
            while (details != null && details.timeZone == null)
            {
                details = details.parent;
            }

            return details != null ? details.timeZone : null;
        }

        public DeliveryOptions getEmailSettings(String sysId,
                                                boolean includeParents)
        {
            DeliveryOptions res = null;

            for (LocationDetails details = getDetails(sysId); details != null; details = details.parent)
            {
                DeliveryOptions d = details.emailSettings;
                if (d != null)
                {
                    if (res == null)
                    {
                        res = new DeliveryOptions();
                    }

                    res.users.addAll(d.users);
                    res.groups.addAll(d.groups);
                    res.roles.addAll(d.roles);
                }

                if (!includeParents)
                {
                    break;
                }
            }

            return res;
        }

        public DeliveryOptions getSmsSettings(String sysId,
                                              boolean includeParents)
        {
            DeliveryOptions res = null;

            for (LocationDetails details = getDetails(sysId); details != null; details = details.parent)
            {
                DeliveryOptions d = details.smsSettings;
                if (d != null)
                {
                    if (res == null)
                    {
                        res = new DeliveryOptions();
                    }

                    res.users.addAll(d.users);
                    res.groups.addAll(d.groups);
                    res.roles.addAll(d.roles);
                }

                if (!includeParents)
                {
                    break;
                }
            }

            return res;
        }

        public LongitudeLatitude getPosition(String sysId)
        {
            LocationDetails details = getDetails(sysId);
            return details != null ? details.position : null;
        }

        public List<LocationHierarchy> extractHierarchy()
        {
            return m_state.extractHierarchy();
        }

        public Map<String, String> extractReverseHierarchy()
        {
            return m_state.extractReverseHierarchy();
        }

        public List<String> withGeoFences()
        {
            return m_state.withGeoFences();
        }

        private LocationDetails getDetails(String sysId)
        {
            return m_state.m_lookup.get(sysId);
        }

        public void recursivelyCollectStructure(Set<String> locations,
                                                boolean recursive,
                                                String locationId)
        {
            m_state.recursivelyCollectStructure(locations, recursive, locationId);
        }

        public void accumulateByTopLevelLocations(List<SummaryResult> res,
                                                  Map<String, Number> counts)
        {
            for (LocationDetails ld : m_state.m_topLevel)
            {
                SummaryResult obj = new SummaryResult();
                obj.id    = ld.ri.sysId;
                obj.type  = SummaryFlavor.location;
                obj.label = ld.name;
                obj.count = ld.recursivelyAccumulate(counts);
                res.add(obj);
            }
        }

        public void accumulateByRollupType(List<SummaryResult> res,
                                           Map<String, Number> counts,
                                           LocationType rollupType)
        {
            accumulateByRollupType(m_state.m_topLevel, res, counts, rollupType);
        }

        private void accumulateByRollupType(List<LocationDetails> locations,
                                            List<SummaryResult> res,
                                            Map<String, Number> counts,
                                            LocationType rollupType)
        {
            if (CollectionUtils.isEmpty(locations))
            {
                return;
            }

            for (LocationDetails ld : locations)
            {
                if (rollupType == null || ld.type == rollupType)
                {
                    SummaryResult obj = new SummaryResult();
                    obj.id    = ld.ri.sysId;
                    obj.type  = SummaryFlavor.location;
                    obj.label = ld.name;
                    obj.count = ld.recursivelyAccumulate(counts);
                    res.add(obj);
                }

                if (rollupType != null)
                {
                    accumulateByRollupType(ld.subLocations, res, counts, rollupType);
                }
            }
        }

        public List<TypedRecordIdentity<LocationRecord>> findIntersections(LongitudeLatitude point)
        {
            List<TypedRecordIdentity<LocationRecord>> intersections = Lists.newArrayList();
            Point                                     point2        = toPoint(point);

            for (LocationDetails ld : m_state.m_lookup.values())
            {
                for (FenceDetails fence : ld.fences)
                {
                    if (fence.isInside(point2))
                    {
                        intersections.add(ld.ri);
                    }
                }
            }

            return intersections;
        }
    }

    private static abstract class FenceDetails
    {
        final GeoFence fence;

        FenceDetails(GeoFence fence)
        {
            this.fence = fence;
        }

        public abstract boolean isInside(Point point);
    }

    private static class FenceDetailsByPolygon extends FenceDetails
    {
        final Polygon polygon;

        FenceDetailsByPolygon(GeoFenceByPolygon fence)
        {
            super(fence);

            LineString outer = toGeometry(fence.boundary);

            if (fence.innerExclusions != null)
            {
                List<LineString> inner = CollectionUtils.transformToList(fence.innerExclusions, LocationsEngine::toGeometry);

                polygon = Polygon.fromOuterInner(outer, inner);
            }
            else
            {
                polygon = Polygon.fromOuterInner(outer);
            }
        }

        @Override
        public boolean isInside(Point point)
        {
            return TurfJoins.inside(point, polygon);
        }
    }

    private static class FenceDetailsByRadius extends FenceDetails
    {
        final Point  center;
        final double radius;

        FenceDetailsByRadius(GeoFenceByRadius fence)
        {
            super(fence);

            this.center = toPoint(fence.center);
            this.radius = fence.radius;
        }

        @Override
        public boolean isInside(Point point)
        {
            return TurfMeasurement.distance(center, point, "meters") < radius;
        }
    }

    private static class LocationDetails
    {
        final TypedRecordIdentity<LocationRecord> ri;
        final String                              name;
        final List<LocationDetails>               subLocations = Lists.newArrayList();
        final List<FenceDetails>                  fences       = Lists.newArrayList();
        final LongitudeLatitude                   position;
        final String                              adtModel;
        final Set<String>                         tags;
        final LocationType                        type;
        final String                              timeZone;
        final DeliveryOptions                     emailSettings;
        final DeliveryOptions                     smsSettings;

        LocationDetails parent;

        LocationDetails(RawModel model)
        {
            ri   = TypedRecordIdentity.newTypedInstance(LocationRecord.class, model.sysId);
            name = model.name;
            type = model.type;

            MetadataMap metadata = MetadataMap.decodeMetadata(model.metadataCompressed);

            position = LocationRecord.getGeo(metadata);

            adtModel      = AssetRecord.WellKnownMetadata.azureDigitalTwinModel.get(metadata);
            tags          = AssetRecord.WellKnownTags.getTags(AssetRecord.accessTags(metadata), false, true, true);
            timeZone      = LocationRecord.WellKnownMetadata.locationTimeZone.get(metadata);
            emailSettings = LocationRecord.WellKnownMetadata.locationEmailSettings.get(metadata);
            smsSettings   = LocationRecord.WellKnownMetadata.locationSmsSettings.get(metadata);

            List<GeoFence> fences = LocationRecord.getFences(metadata);
            if (fences != null)
            {
                for (GeoFence fence : fences)
                {
                    if (fence instanceof GeoFenceByPolygon)
                    {
                        this.fences.add(new FenceDetailsByPolygon((GeoFenceByPolygon) fence));
                    }
                    else if (fence instanceof GeoFenceByRadius)
                    {
                        this.fences.add(new FenceDetailsByRadius((GeoFenceByRadius) fence));
                    }
                }
            }
        }

        void recursivelyCollectStructure(Set<String> locations,
                                         boolean recursive)
        {
            locations.add(ri.sysId);

            if (recursive)
            {
                for (LocationDetails lhSub : subLocations)
                {
                    lhSub.recursivelyCollectStructure(locations, true);
                }
            }
        }

        int recursivelyAccumulate(Map<String, Number> counts)
        {
            int count = 0;

            Number n = counts.get(ri.sysId);
            if (n != null)
            {
                count += n.intValue();
            }

            for (LocationDetails ldSub : subLocations)
            {
                count += ldSub.recursivelyAccumulate(counts);
            }

            return count;
        }

        void getHierarchicalName(StringBuilder sb)
        {
            if (parent != null)
            {
                parent.getHierarchicalName(sb);
                sb.append(" / ");
            }

            sb.append(name);
        }
    }

    private static class RawModel
    {
        public String       sysId;
        public String       parentAsset;
        public String       name;
        public LocationType type;
        public byte[]       metadataCompressed;
    }

    private static class State
    {
        final int                          m_version;
        final Map<String, LocationDetails> m_lookup   = Maps.newHashMap();
        final List<LocationDetails>        m_topLevel = Lists.newArrayList();

        State(int version)
        {
            m_version = version;
        }

        void processLocation(Multimap<String, String> hierarchy,
                             RawModel model)
        {
            LocationDetails ld = m_lookup.get(model.sysId);
            if (ld == null)
            {
                ld = new LocationDetails(model);

                m_lookup.put(model.sysId, ld);

                if (model.parentAsset != null)
                {
                    hierarchy.put(model.parentAsset, model.sysId);
                }
                else
                {
                    m_topLevel.add(ld);
                }
            }
        }

        //--//

        void postProcessLocations(Multimap<String, String> hierarchy)
        {
            for (String parentSysId : hierarchy.keySet())
            {
                LocationDetails ldParent = m_lookup.get(parentSysId);

                for (String childSysId : hierarchy.get(parentSysId))
                {
                    LocationDetails ldChild = m_lookup.get(childSysId);

                    ldChild.parent = ldParent;
                    ldParent.subLocations.add(ldChild);
                }
            }

            sort(m_topLevel);
        }

        private void sort(List<LocationDetails> lst)
        {
            lst.sort((a, b) -> StringUtils.compareIgnoreCase(a.name, b.name));

            for (LocationDetails lh : lst)
            {
                sort(lh.subLocations);
            }
        }

        Map<String, String> extractReverseHierarchy()
        {
            Map<String, String> reverseHierarchy = Maps.newHashMap();

            for (LocationDetails ld : m_lookup.values())
            {
                for (LocationDetails ldChild : ld.subLocations)
                {
                    reverseHierarchy.put(ldChild.ri.sysId, ld.ri.sysId);
                }
            }

            return reverseHierarchy;
        }

        List<LocationHierarchy> extractHierarchy()
        {
            return extractHierarchy(m_topLevel);
        }

        public List<String> withGeoFences()
        {
            List<String> entries = Lists.newArrayList();
            for (LocationDetails details : m_lookup.values())
            {
                if (CollectionUtils.isNotEmpty(details.fences))
                {
                    entries.add(details.ri.sysId);
                }
            }

            return entries;
        }

        private List<LocationHierarchy> extractHierarchy(List<LocationDetails> lst)
        {
            return CollectionUtils.transformToList(lst, (loc) ->
            {
                LocationHierarchy lh = new LocationHierarchy();
                lh.ri           = loc.ri;
                lh.name         = loc.name;
                lh.type         = loc.type;
                lh.subLocations = extractHierarchy(loc.subLocations);
                return lh;
            });
        }

        void recursivelyCollectStructure(Set<String> locations,
                                         boolean recursive,
                                         String locationId)
        {
            LocationDetails lh = m_lookup.get(locationId);
            if (lh != null)
            {
                lh.recursivelyCollectStructure(locations, recursive);
            }
        }
    }

    //--//

    public static final Logger LoggerInstance = new Logger(LocationsEngine.class);

    private static final CompletableFuture<Void> s_done = AsyncRuntime.NullResult;

    private final HubApplication m_app;
    private final Object         m_lock          = new Object();
    private final AtomicBoolean  m_keepRunning   = new AtomicBoolean(true);
    private final AtomicBoolean  m_rebuildNeeded = new AtomicBoolean(true);

    private DatabaseActivity.LocalSubscriber         m_regDbActivity;
    private State                                    m_state                  = new State(0); // m_state is always non-null.
    private int                                      m_version                = 0;
    private int                                      m_pendingWorkerLazyDelay = 10;
    private ScheduledFuture<CompletableFuture<Void>> m_pendingWorkerLazy;
    private CompletableFuture<Void>                  m_pendingWorker;

    private int  m_runs;
    private long m_runs_time;
    private long m_runs_count;

    private final LoggerPeriodic m_periodicDump = new LoggerPeriodic(LoggerInstance, Severity.Info, 1, TimeUnit.HOURS)
    {
        @Override
        protected void onActivation()
        {
            LoggerInstance.info("%,d runs in %,d millisec (%,d per run): found avg. %,d locations", m_runs, m_runs_time, m_runs_time / m_runs, m_runs_count / m_runs);

            m_runs       = 0;
            m_runs_time  = 0;
            m_runs_count = 0;
        }
    };

    //--//

    public LocationsEngine(HubApplication app)
    {
        m_app = app;

        app.registerService(LocationsEngine.class, () -> this);
    }

    public void initialize()
    {
        m_regDbActivity = DatabaseActivity.LocalSubscriber.create(m_app.getServiceNonNull(MessageBusBroker.class));

        m_regDbActivity.subscribeToTable(LocationRecord.class, (dbEvent) ->
        {
            switch (dbEvent.action)
            {
                case INSERT:
                case DELETE:
                case UPDATE_DIRECT: // We don't invalidate for indirect updates.
                    if (!m_rebuildNeeded.getAndSet(true))
                    {
                        queueLazySynchronization(dbEvent);
                    }
                    break;
            }
        });

        // If a fixup runs at startup, make sure we reprocess.
        m_rebuildNeeded.set(true);

        startLocationSynchronization(null);
    }

    public void close()
    {
        m_keepRunning.set(false);

        // Wait for worker to finish.
        synchronizeLocations(true);

        if (m_regDbActivity != null)
        {
            m_regDbActivity.close();
            m_regDbActivity = null;
        }
    }

    //--//

    public int getVersion()
    {
        return m_state.m_version;
    }

    public Snapshot acquireSnapshot(boolean waitForSynchronization)
    {
        State state = synchronizeLocations(waitForSynchronization);

        return new Snapshot(state);
    }

    private State synchronizeLocations(boolean wait)
    {
        try
        {
            AtomicBoolean           shouldWait      = new AtomicBoolean(wait);
            CompletableFuture<Void> synchronization = startLocationSynchronization(shouldWait);

            if (shouldWait.get())
            {
                synchronization.get();
            }
        }
        catch (Exception e)
        {
            // Never going to happen...
        }

        return m_state;
    }

    private void queueLazySynchronization(DbEvent dbEvent)
    {
        synchronized (m_lock)
        {
            if (m_keepRunning.get())
            {
                if (m_pendingWorkerLazy != null && m_pendingWorkerLazy.isDone())
                {
                    m_pendingWorkerLazy = null;
                }

                if (m_pendingWorkerLazy == null)
                {
                    if (dbEvent == null)
                    {
                        LoggerInstance.debug("Detected DB activity, scheduling analysis in %d seconds...", m_pendingWorkerLazyDelay);
                    }
                    else
                    {
                        LoggerInstance.debug("Detected DB activity (%s:%s %s), scheduling analysis in %d seconds...",
                                             dbEvent.context.getTable(),
                                             dbEvent.context.sysId,
                                             dbEvent.action,
                                             m_pendingWorkerLazyDelay);
                    }

                    m_pendingWorkerLazy = Executors.scheduleOnDefaultPool(() -> startLocationSynchronization(null), m_pendingWorkerLazyDelay, TimeUnit.SECONDS);
                }
            }
        }
    }

    private CompletableFuture<Void> startLocationSynchronization(AtomicBoolean shouldWait)
    {
        synchronized (m_lock)
        {
            if (shouldWait != null && m_state.m_version == 0)
            {
                shouldWait.set(true);
            }

            if (!m_keepRunning.get())
            {
                return s_done;
            }

            if (m_pendingWorker == null)
            {
                m_pendingWorker = Executors.getDefaultLongRunningThreadPool()
                                           .queue(this::synchronizeLocations);
            }

            return m_pendingWorker;
        }
    }

    private void synchronizeLocations()
    {
        boolean shouldReschedule = false;

        try
        {
            if (m_rebuildNeeded.getAndSet(false))
            {
                try (SessionHolder sessionHolder = SessionHolder.createWithNewReadOnlySession(m_app, null, Optio3DbRateLimiter.System))
                {
                    Stopwatch st = Stopwatch.createStarted();

                    LoggerInstance.debug("Starting analysis...");

                    State                                    state     = new State(++m_version);
                    Multimap<String, String>                 hierarchy = HashMultimap.create();
                    RawQueryHelper<LocationRecord, RawModel> qh        = new RawQueryHelper<>(sessionHolder, LocationRecord.class);

                    qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
                    qh.addReferenceRaw(AssetRecord_.parentAsset, (obj, val) -> obj.parentAsset = val);
                    qh.addObject(RecordWithMetadata_.metadataCompressed, byte[].class, (obj, val) -> obj.metadataCompressed = val);
                    qh.addString(AssetRecord_.name, (obj, val) -> obj.name = val);
                    qh.addEnum(LocationRecord_.type, LocationType.class, (obj, val) -> obj.type = val);

                    // Reuse the same instance, since we don't store the individual models.
                    final var singletonModel = new RawModel();

                    qh.stream(() -> singletonModel, (model) ->
                    {
                        state.processLocation(hierarchy, model);
                    });

                    state.postProcessLocations(hierarchy);

                    LoggerInstance.debug("Completed analysis: found %d locations", state.m_lookup.size());
                    m_runs++;
                    m_runs_time += st.elapsed(TimeUnit.MILLISECONDS);
                    m_runs_count += state.m_lookup.size();
                    m_periodicDump.process();

                    synchronized (m_lock)
                    {
                        m_state = state;

                        if (m_rebuildNeeded.get())
                        {
                            m_pendingWorkerLazyDelay = Math.min(10 * 60, Math.min(60, m_pendingWorkerLazyDelay * 2));
                            shouldReschedule         = true;
                        }
                        else
                        {
                            m_pendingWorkerLazyDelay = 10;
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            LoggerInstance.error("Failed to update LocationsEngine state, due to %s", e);
            shouldReschedule = true;
        }
        finally
        {
            synchronized (m_lock)
            {
                m_pendingWorker.complete(null);
                m_pendingWorker = null;
            }
        }

        if (shouldReschedule)
        {
            queueLazySynchronization(null);
        }
    }

    //--//

    public static void validate(GeoFenceByPolygon fence)
    {
        if (fence.boundary == null)
        {
            throw Exceptions.newGenericException(InvalidArgumentException.class, "GeoFenceByPolygon '%s' doesn't have any boundary", fence.uniqueId);
        }

        final LineString outer        = validate(fence.boundary);
        final Polygon    outerPolygon = Polygon.fromOuterInner(outer);

        if (fence.innerExclusions != null)
        {
            List<LineString> inner = CollectionUtils.transformToList(fence.innerExclusions, LocationsEngine::validate);

            for (int i = 0; i < inner.size(); i++)
            {
                final LineString inner1        = inner.get(i);
                final Polygon    inner1Polygon = Polygon.fromOuterInner(inner1);

                if (!isFullyInside(outerPolygon, inner1))
                {
                    throw Exceptions.newGenericException(InvalidArgumentException.class,
                                                         "GeoFenceByPolygon '%s' doesn't have a proper inside: boundary '%s' doesn't cover '%s'",
                                                         fence.uniqueId,
                                                         outer.toJson(),
                                                         inner1.toJson());
                }

                for (int j = i + 1; j < inner.size(); j++)
                {
                    final LineString inner2        = inner.get(j);
                    final Polygon    inner2Polygon = Polygon.fromOuterInner(inner2);

                    if (isAnyInside(inner1Polygon, inner2) || isAnyInside(inner2Polygon, inner1))
                    {
                        throw Exceptions.newGenericException(InvalidArgumentException.class,
                                                             "GeoFenceByPolygon '%s' doesn't have a proper inside: exclusions '%s' and '%s' are not disjoint",
                                                             fence.uniqueId,
                                                             inner1.toJson(),
                                                             inner2.toJson());
                    }
                }
            }
        }
    }

    private static boolean isFullyInside(Polygon outer,
                                         LineString inner)
    {
        for (Point coordinate : inner.coordinates())
        {
            if (!TurfJoins.inside(coordinate, outer))
            {
                return false;
            }
        }

        return true;
    }

    private static boolean isAnyInside(Polygon outer,
                                       LineString inner)
    {
        for (Point coordinate : inner.coordinates())
        {
            if (TurfJoins.inside(coordinate, outer))
            {
                return true;
            }
        }

        return false;
    }

    public static void validate(GeoFenceByRadius fence)
    {
        if (fence.center == null)
        {
            throw Exceptions.newGenericException(InvalidArgumentException.class, "GeoFenceByRadius '%s' doesn't have a center", fence.uniqueId);
        }

        if (fence.radius < 0.0)
        {
            throw Exceptions.newGenericException(InvalidArgumentException.class, "GeoFenceByRadius '%s' doesn't have a positive radius", fence.uniqueId);
        }
    }

    public static LineString validate(LocationPolygon polygon)
    {
        if (polygon.points == null)
        {
            throw Exceptions.newGenericException(InvalidArgumentException.class, "Polygon doesn't have any points");
        }

        return toGeometry(polygon);
    }

    public static Point toPoint(LongitudeLatitude pt)
    {
        return Point.fromLngLat(pt.longitude, pt.latitude);
    }

    public static LineString toGeometry(LocationPolygon polygon)
    {
        checkForSimpleRing(polygon);

        List<Point> pointsLst = CollectionUtils.transformToList(polygon.points, LocationsEngine::toPoint);

        // Duplicate first element to close the ring.
        pointsLst.add(pointsLst.get(0));

        return LineString.fromLngLats(pointsLst);
    }

    private static void checkForSimpleRing(LocationPolygon polygon)
    {
        List<Coordinate> pointsLst = CollectionUtils.transformToList(polygon.points, (pt) -> new Coordinate(pt.longitude, pt.latitude));

        // Duplicate first element to close the ring.
        pointsLst.add(pointsLst.get(0));

        LinearRing ring = safeWrapper("Polygon is invalid: %s", () -> new GeometryFactory().createLinearRing(pointsLst.toArray(new Coordinate[0])));
        if (!ring.isSimple())
        {
            throw Exceptions.newGenericException(InvalidArgumentException.class, "Polygon is not a simple ring: %s", ring.toText());
        }
    }

    private static <T> T safeWrapper(String errorFormat,
                                     Callable<T> callback)
    {
        try
        {
            return callback.call();
        }
        catch (Throwable t)
        {
            throw Exceptions.newGenericException(InvalidArgumentException.class, errorFormat, t.getMessage());
        }
    }
}