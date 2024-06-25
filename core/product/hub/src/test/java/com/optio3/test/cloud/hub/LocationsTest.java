/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.model.location.GeoFenceByPolygon;
import com.optio3.cloud.hub.model.location.GeoFenceByRadius;
import com.optio3.cloud.hub.model.location.LocationPolygon;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.model.location.LongitudeLatitude;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import org.junit.ClassRule;
import org.junit.Test;

public class LocationsTest extends Optio3Test
{
    static String id_root;
    static String id_child1;
    static String id_child2;
    static String id_child2_sub1;

    @ClassRule
    public static final TestApplicationWithDbRule<HubApplication, HubConfiguration> applicationRule = new TestApplicationWithDbRule<>(HubApplication.class, "hub-test.yml", (configuration) ->
    {
        Optio3DataSourceFactory dataSourceFactory = configuration.getDataSourceFactory();
        dataSourceFactory.enableEvents = true;

        configuration.data = new HubDataDefinition[] { new HubDataDefinition("demodata/defaultUsers.json", false, false) };
    }, null);

    @Test
    @TestOrder(10)
    public void testCreate() throws
                             Exception
    {
        try (SessionHolder sessionHolder = applicationRule.openSessionWithTransaction())
        {
            final RecordHelper<LocationRecord> helperLocation = sessionHolder.createHelper(LocationRecord.class);

            LocationRecord rec_root = new LocationRecord();
            rec_root.setDisplayName("root");
            rec_root.setType(LocationType.BUILDING);

            rec_root.setAddress("test facility");

            LongitudeLatitude geo = new LongitudeLatitude();
            geo.latitude = 1.0;
            rec_root.setGeo(geo);

            sessionHolder.persistEntity(rec_root);
            id_root = rec_root.getSysId();
            System.out.printf("id_root: %s%n", id_root);
            ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_root);

            LocationRecord rec_child1 = new LocationRecord();
            rec_child1.setDisplayName("child1");
            rec_child1.setType(LocationType.FLOOR);
            sessionHolder.persistEntity(rec_child1);
            id_child1 = rec_child1.getSysId();
            System.out.printf("id_child1: %s%n", id_child1);
            ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_child1);
            rec_child1.linkToParent(helperLocation, rec_root);

            LocationRecord rec_child2 = new LocationRecord();
            rec_child2.setDisplayName("child2");
            rec_child2.setType(LocationType.FLOOR);
            sessionHolder.persistEntity(rec_child2);
            id_child2 = rec_child2.getSysId();
            System.out.printf("id_child2: %s%n", id_child2);
            ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_child2);
            rec_child2.linkToParent(helperLocation, rec_root);

            LocationRecord rec_child2_sub1 = new LocationRecord();
            rec_child2_sub1.setDisplayName("grandchild1");
            rec_child2_sub1.setType(LocationType.ZONE);
            sessionHolder.persistEntity(rec_child2_sub1);
            id_child2_sub1 = rec_child2_sub1.getSysId();
            System.out.printf("id_child2_sub1: %s%n", id_child2_sub1);
            ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_child2_sub1);
            rec_child2_sub1.linkToParent(helperLocation, rec_child2);

            sessionHolder.commit();
        }
    }

    @Test
    @TestOrder(20)
    public void testList1()
    {
        try (SessionHolder sessionHolder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<LocationRecord> locWrapper = sessionHolder.createHelper(LocationRecord.class);

            List<LocationRecord> results = locWrapper.listAll();
            assertEquals(4, results.size());
            for (LocationRecord rec_loc : results)
            {
                final AssetRecord rec_parent = rec_loc.getParentAsset();

                ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_loc);

                System.out.printf("ID: %s%n", rec_loc.getSysId());
                System.out.printf("  parent: %s%n", rec_parent != null ? rec_parent.getSysId() : "<null>");

                for (LocationRecord rec_child : rec_loc.getChildren(locWrapper))
                {
                    System.out.printf("  child: %s%n", rec_child.getSysId());
                }
            }
        }
    }

    @Test
    @TestOrder(21)
    public void testUpdate()
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<LocationRecord> locWrapper = holder.createHelper(LocationRecord.class);

            LocationRecord rec_child2 = locWrapper.get(id_child2);
            assertNotNull(rec_child2);

            rec_child2.setDisplayName("new name");

            holder.commit();
        }

        applicationRule.drainDatabaseEvents();
    }

    @Test
    @TestOrder(30)
    public void testDelete() throws
                             Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<AssetRecord>    helper_asset    = holder.createHelper(AssetRecord.class);
            RecordHelper<LocationRecord> helper_location = holder.createHelper(LocationRecord.class);

            LocationRecord rec_root = helper_location.get(id_root);
            assertNotNull(rec_root);
            LocationRecord rec_child2 = helper_location.get(id_child2);
            assertNotNull(rec_child2);

            List<LocationRecord> sub1 = rec_root.getChildren(helper_location);
            assertEquals(2, sub1.size());
            for (LocationRecord rec_child : sub1)
                System.out.printf("  BEFORE child: %s%n", rec_child.getSysId());

            try (ValidationResultsHolder validation = new ValidationResultsHolder(holder, false, true))
            {
                rec_child2.remove(validation, helper_asset);
            }

            helper_location.flush();

            List<LocationRecord> sub2 = rec_root.getChildren(helper_location);
            assertEquals(1, sub2.size());
            for (LocationRecord rec_child : sub2)
                System.out.printf("  AFTER  child: %s%n", rec_child.getSysId());

            holder.commit();
        }

        applicationRule.drainDatabaseEvents();
    }

    @Test
    @TestOrder(40)
    public void testList2()
    {
        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<LocationRecord> locWrapper = holder.createHelper(LocationRecord.class);

            List<LocationRecord> results = locWrapper.listAll();
            assertEquals(2, results.size());
            for (LocationRecord rec_loc : results)
            {
                final AssetRecord rec_parent = rec_loc.getParentAsset();

                System.out.printf("ID: %s%n", rec_loc.getSysId());
                System.out.printf("  parent: %s%n", rec_parent != null ? rec_parent.getSysId() : "<null>");

                for (LocationRecord rec_child : rec_loc.getChildren(locWrapper))
                {
                    System.out.printf("  child: %s%n", rec_child.getSysId());
                }
            }
        }
    }

    @Test
    @TestOrder(50)
    public void testBadFences()
    {
        assertFailure(InvalidArgumentException.class, () ->
        {
            GeoFenceByPolygon g1 = new GeoFenceByPolygon();
            g1.uniqueId = "Self-intersects";
            g1.boundary = buildPolygon(0, 0, 10, 0, 0, 10, 10, 10);

            g1.validate();
        });

        assertFailure(InvalidArgumentException.class, () ->
        {
            GeoFenceByPolygon g1 = new GeoFenceByPolygon();
            g1.uniqueId        = "Exclusion not contained";
            g1.boundary        = buildPolygon(0, 0, 10, 0, 10, 10, 0, 10);
            g1.innerExclusions = Lists.newArrayList(buildPolygon(12, 2, 12 + 10, 2, 12 + 10, 2 + 2, 12, 2 + 2));

            g1.validate();
        });

        assertFailure(InvalidArgumentException.class, () ->
        {
            GeoFenceByPolygon g1 = new GeoFenceByPolygon();
            g1.uniqueId        = "Exclusions overlap";
            g1.boundary        = buildPolygon(0, 0, 10, 0, 10, 10, 0, 10);
            g1.innerExclusions = Lists.newArrayList(buildPolygon(2, 2, 4, 2, 4, 4, 2, 4), buildPolygon(1, 1, 3, 1, 3, 3, 1, 3));

            g1.validate();
        });

        {
            GeoFenceByPolygon g1 = new GeoFenceByPolygon();
            g1.uniqueId        = "Proper holes";
            g1.boundary        = buildPolygon(0, 0, 10, 0, 10, 10, 0, 10);
            g1.innerExclusions = Lists.newArrayList(buildPolygon(2, 2, 4, 2, 4, 4, 2, 4), buildPolygon(5, 1, 7, 1, 7, 3, 5, 3));

            g1.validate();
        }
    }

    @Test
    @TestOrder(51)
    public void testFences()
    {
        RecordLocator<LocationRecord> loc_g1;
        RecordLocator<LocationRecord> loc_g2;
        RecordLocator<LocationRecord> loc_g3;

        final LongitudeLatitude centerOfCircle = LongitudeLatitude.fromLngLat(-120, 30);

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            final RecordHelper<LocationRecord> helperLocation = holder.createHelper(LocationRecord.class);

            {
                LocationRecord rec = new LocationRecord();
                rec.setDisplayName("Fence With Radius");
                rec.setType(LocationType.BUILDING);

                GeoFenceByRadius g1 = new GeoFenceByRadius();
                g1.center = centerOfCircle;
                g1.radius = 1000;

                rec.setFences(Lists.newArrayList(g1));

                holder.persistEntity(rec);

                loc_g1 = helperLocation.asLocator(rec);
            }

            {
                LocationRecord rec = new LocationRecord();
                rec.setDisplayName("Fence With Triangle");
                rec.setType(LocationType.BUILDING);

                GeoFenceByPolygon g1 = new GeoFenceByPolygon();
                g1.boundary = buildPolygon(0, 10, 10, -10, -10, -10);

                rec.setFences(Lists.newArrayList(g1));

                holder.persistEntity(rec);

                loc_g2 = helperLocation.asLocator(rec);
            }

            {
                LocationRecord rec = new LocationRecord();
                rec.setDisplayName("Fence With Hole");
                rec.setType(LocationType.BUILDING);

                GeoFenceByPolygon g1 = new GeoFenceByPolygon();
                g1.boundary = buildPolygon(-100, 10, -90, 10, -90, -10, -100, -10);

                g1.innerExclusions = Lists.newArrayList(buildPolygon(-94, 2, -96, 2, -96, -2, -94, -2));

                rec.setFences(Lists.newArrayList(g1));

                holder.persistEntity(rec);

                loc_g3 = helperLocation.asLocator(rec);
            }

            holder.commit();
        }

        applicationRule.drainDatabaseEvents();

        LocationsEngine engine = applicationRule.getApplication()
                                                .getService(LocationsEngine.class);
        LocationsEngine.Snapshot snapshot = engine.acquireSnapshot(true);

        {
            List<TypedRecordIdentity<LocationRecord>> intersections = snapshot.findIntersections(LongitudeLatitude.fromLngLat(0, 0));
            assertEquals(1, intersections.size());
            assertEquals(loc_g2.getIdRaw(), intersections.get(0).sysId);
        }

        {
            // On the boundary, but not inside.
            List<TypedRecordIdentity<LocationRecord>> intersections = snapshot.findIntersections(LongitudeLatitude.fromLngLat(5, 0));
            assertEquals(0, intersections.size());
        }

        {
            List<TypedRecordIdentity<LocationRecord>> intersections = snapshot.findIntersections(LongitudeLatitude.fromLngLat(-95, 8));
            assertEquals(1, intersections.size());
            assertEquals(loc_g3.getIdRaw(), intersections.get(0).sysId);
        }

        {
            // In the hole.
            List<TypedRecordIdentity<LocationRecord>> intersections = snapshot.findIntersections(LongitudeLatitude.fromLngLat(-95, 0));
            assertEquals(0, intersections.size());
        }

        {
            List<TypedRecordIdentity<LocationRecord>> intersections = snapshot.findIntersections(centerOfCircle);
            assertEquals(1, intersections.size());
            assertEquals(loc_g1.getIdRaw(), intersections.get(0).sysId);
        }

        {
            List<TypedRecordIdentity<LocationRecord>> intersections = snapshot.findIntersections(centerOfCircle.computeDestination(990, 90));
            assertEquals(1, intersections.size());
            assertEquals(loc_g1.getIdRaw(), intersections.get(0).sysId);
        }

        {
            List<TypedRecordIdentity<LocationRecord>> intersections = snapshot.findIntersections(centerOfCircle.computeDestination(1050, 30));
            assertEquals(0, intersections.size());
        }
    }

    private static LocationPolygon buildPolygon(double... args)
    {
        LongitudeLatitude[] pt = new LongitudeLatitude[args.length / 2];

        for (int i = 0; i < pt.length; i++)
        {
            pt[i] = LongitudeLatitude.fromLngLat(args[i * 2], args[i * 2 + 1]);
        }

        return LocationPolygon.from(pt);
    }
}
