/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.asset.RelationshipRecord;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.MetadataTagsMap;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.serialization.ObjectMappers;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import com.optio3.util.TimeUtils;
import org.junit.ClassRule;
import org.junit.Test;

public class AssetsTest extends Optio3Test
{
    private static String id_root;
    private static String id_child1;
    private static String id_child2;
    private static String id_child2_sub1;

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
            RecordHelper<AssetRecord> helper_asset = sessionHolder.createHelper(AssetRecord.class);

            DeviceRecord rec_root = new DeviceRecord();
            rec_root.setFirmwareVersion("root1");

            sessionHolder.persistEntity(rec_root);
            id_root = rec_root.getSysId();
            System.out.printf("id_root: %s%n", id_root);
            ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_root);

            //--//

            DeviceElementRecord rec_child1 = new DeviceElementRecord();
            rec_child1.setIdentifier("child1");
            rec_child1.setContents(ObjectMappers.SkipNulls, TimeUtils.now());
            sessionHolder.persistEntity(rec_child1);
            rec_child1.linkToParent(helper_asset, rec_root);

            id_child1 = rec_child1.getSysId();
            System.out.printf("id_child1: %s%n", id_child1);
            ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_child1);

            //--//

            DeviceElementRecord rec_child2 = new DeviceElementRecord();
            rec_child2.setIdentifier("child2");
            rec_child2.setContents(ObjectMappers.SkipNulls, TimeUtils.now());
            sessionHolder.persistEntity(rec_child2);
            rec_child2.linkToParent(helper_asset, rec_root);

            id_child2 = rec_child2.getSysId();
            System.out.printf("id_child2: %s%n", id_child2);
            ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_child2);

            //--//

            DeviceElementRecord rec_child2_sub1 = new DeviceElementRecord();
            rec_child2_sub1.setIdentifier("child2_sbu1");
            rec_child2_sub1.setContents(ObjectMappers.SkipNulls, TimeUtils.now());
            sessionHolder.persistEntity(rec_child2_sub1);
            rec_child2_sub1.linkToParent(helper_asset, rec_child2);

            id_child2_sub1 = rec_child2_sub1.getSysId();
            System.out.printf("id_child2_sub1: %s%n", id_child2_sub1);
            ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_child2_sub1);

            sessionHolder.commit();
        }
    }

    @Test
    @TestOrder(20)
    public void testList1() throws
                            Exception
    {
        try (SessionHolder sessionHolder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<AssetRecord> assetHelper = sessionHolder.createHelper(AssetRecord.class);

            List<AssetRecord> results = assetHelper.listAll();
            assertEquals(4 + 1, results.size()); // +1 for the Host asset.
            for (AssetRecord rec_asset : results)
            {
                final AssetRecord rec_parent = rec_asset.getParentAsset();

                ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_asset);

                System.out.printf("ID: %s%n", rec_asset.getSysId());
                System.out.printf("  parent: %s%n", rec_parent != null ? rec_parent.getSysId() : "<null>");

                rec_asset.enumerateChildren(sessionHolder.createHelper(AssetRecord.class), true, -1, null, (rec_child) ->
                {
                    System.out.printf("  child: %s%n", rec_child.getSysId());
                    return StreamHelperNextAction.Continue;
                });
            }
        }
    }

    @Test
    @TestOrder(21)
    public void testUpdate() throws
                             Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<DeviceElementRecord> assetHelper = holder.createHelper(DeviceElementRecord.class);

            DeviceElementRecord rec_child2 = assetHelper.get(id_child2);
            assertNotNull(rec_child2);

            rec_child2.setContents(ObjectMappers.SkipNulls, TimeUtils.now());

            holder.commit();
        }
    }

    @Test
    @TestOrder(22)
    public void testTags()
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<AssetRecord> assetHelper = holder.createHelper(AssetRecord.class);

            AssetRecord rec_root = assetHelper.get(id_root);

            rec_root.modifyTags((tags) ->
                                {
                                    assertTrue(tags.addTag("Tag1", false));
                                    assertFalse(tags.addTag("Tag1", false));
                                    assertTrue(tags.addValueToTag("Tag2", "V2"));
                                    assertTrue(tags.addValueToTag("Tag2", "V1"));
                                    assertFalse(tags.addValueToTag("Tag2", "V2"));
                                });

            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<AssetRecord> assetHelper = holder.createHelper(AssetRecord.class);

            AssetRecord rec_root = assetHelper.get(id_root);

            MetadataTagsMap tags = rec_root.accessTags();

            assertTrue(tags.hasTag("Tag1"));
            assertTrue(tags.hasTag("Tag2"));

            assertEquals(0,
                         tags.getValuesForTag("Tag1")
                             .size());
            assertEquals(2,
                         tags.getValuesForTag("Tag2")
                             .size());

            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<AssetRecord> assetHelper = holder.createHelper(AssetRecord.class);

            AssetRecord rec_root = assetHelper.get(id_root);

            rec_root.modifyTags((tags) ->
                                {

                                    assertTrue(tags.removeTag("Tag1"));
                                    assertFalse(tags.removeTag("Tag1"));

                                    assertTrue(tags.removeValueFromTag("Tag2", "V1"));
                                    assertEquals(1,
                                                 tags.getValuesForTag("Tag2")
                                                     .size());
                                    assertTrue(tags.removeValueFromTag("Tag2", "V2"));
                                    assertEquals(0,
                                                 tags.getValuesForTag("Tag2")
                                                     .size());
                                    assertTrue(tags.hasTag("Tag2"));
                                });

            holder.commit();
        }
    }

    @Test
    @TestOrder(30)
    public void testDelete() throws
                             Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<AssetRecord> assetHelper = holder.createHelper(AssetRecord.class);

            AssetRecord rec_root = assetHelper.get(id_root);
            assertNotNull(rec_root);
            AssetRecord rec_child2 = assetHelper.get(id_child2);
            assertNotNull(rec_child2);

            List<AssetRecord> sub1 = Lists.newArrayList();
            rec_root.enumerateChildren(holder.createHelper(AssetRecord.class), true, -1, null, (rec_child) ->
            {
                sub1.add(rec_child);
                return StreamHelperNextAction.Continue;
            });
            assertEquals(2, sub1.size());
            for (AssetRecord rec_child : sub1)
                System.out.printf("  BEFORE rec_child: %s%n", rec_child.getSysId());

            try (ValidationResultsHolder validation = new ValidationResultsHolder(holder, false, true))
            {
                rec_child2.remove(validation, assetHelper);
            }

            assetHelper.flush();

            List<AssetRecord> sub2 = Lists.newArrayList();
            rec_root.enumerateChildren(holder.createHelper(AssetRecord.class), true, -1, null, (rec_child) ->
            {
                sub2.add(rec_child);
                return StreamHelperNextAction.Continue;
            });
            assertEquals(1, sub2.size());
            for (AssetRecord rec_child : sub2)
                System.out.printf("  AFTER  rec_child: %s%n", rec_child.getSysId());

            holder.commit();
        }
    }

    @Test
    @TestOrder(40)
    public void testList2() throws
                            Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<AssetRecord> assetHelper = holder.createHelper(AssetRecord.class);

            List<AssetRecord> results = assetHelper.listAll();
            assertEquals(2 + 1, results.size()); // +1 for the Host asset.
            for (AssetRecord rec_loc : results)
            {
                System.out.printf("ID: %s%n", rec_loc.getSysId());

                final AssetRecord rec_parent = rec_loc.getParentAsset();
                System.out.printf("  parent: %s%n", rec_parent != null ? rec_parent.getSysId() : "<null>");

                rec_loc.enumerateChildren(assetHelper, true, -1, null, (rec_child) ->
                {
                    System.out.printf("  child: %s%n", rec_child.getSysId());
                    return StreamHelperNextAction.Continue;
                });
            }
        }
    }

    @Test
    @TestOrder(60)
    public void testNetworkBindAndDelete()
    {
        String id_gateway;
        String id_network;

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<GatewayAssetRecord> helper_gateway = holder.createHelper(GatewayAssetRecord.class);
            RecordHelper<NetworkAssetRecord> helper_network = holder.createHelper(NetworkAssetRecord.class);

            GatewayAssetRecord rec_gateway = new GatewayAssetRecord();
            rec_gateway.setInstanceId("test1");

            helper_gateway.persist(rec_gateway);
            id_gateway = rec_gateway.getSysId();
            System.out.printf("id_gateway: %s%n", id_gateway);

            NetworkAssetRecord rec_network = new NetworkAssetRecord();
            rec_network.setCidr("192.168.1.0/24");

            helper_network.persist(rec_network);
            id_network = rec_network.getSysId();
            System.out.printf("id_network: %s%n", id_network);

            System.out.println("testNetworkBindAndDelete: create");
            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<GatewayAssetRecord> helper_gateway = holder.createHelper(GatewayAssetRecord.class);
            RecordHelper<NetworkAssetRecord> helper_network = holder.createHelper(NetworkAssetRecord.class);

            GatewayAssetRecord rec_gateway = helper_gateway.get(id_gateway);

            NetworkAssetRecord rec_network = helper_network.get(id_network);

            rec_gateway.getBoundNetworks()
                       .add(rec_network);

            System.out.println("testNetworkBindAndDelete: bind");
            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<GatewayAssetRecord> helper_gateway = holder.createHelper(GatewayAssetRecord.class);

            GatewayAssetRecord rec_gateway = helper_gateway.get(id_gateway);

            assertEquals(1,
                         rec_gateway.getBoundNetworks()
                                    .size());
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<GatewayAssetRecord> helper_gateway = holder.createHelper(GatewayAssetRecord.class);

            GatewayAssetRecord rec_gateway = helper_gateway.get(id_gateway);

            rec_gateway.setDisplayName("foo2");

            System.out.println("testNetworkBindAndDelete: set name");
            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<GatewayAssetRecord> helper_gateway = holder.createHelper(GatewayAssetRecord.class);

            GatewayAssetRecord rec_gateway = helper_gateway.get(id_gateway);

            assertEquals(1,
                         rec_gateway.getBoundNetworks()
                                    .size());
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<GatewayAssetRecord> helper_gateway = holder.createHelper(GatewayAssetRecord.class);
            RecordHelper<NetworkAssetRecord> helper_network = holder.createHelper(NetworkAssetRecord.class);

            GatewayAssetRecord rec_gateway = helper_gateway.get(id_gateway);

            NetworkAssetRecord rec_network = helper_network.get(id_network);

            rec_gateway.getBoundNetworks()
                       .remove(rec_network);

            System.out.println("testNetworkBindAndDelete: unbind");
            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<GatewayAssetRecord> helper_gateway = holder.createHelper(GatewayAssetRecord.class);
            RecordHelper<NetworkAssetRecord> helper_network = holder.createHelper(NetworkAssetRecord.class);

            GatewayAssetRecord rec_gateway = helper_gateway.get(id_gateway);

            NetworkAssetRecord rec_network = helper_network.get(id_network);

            rec_gateway.getBoundNetworks()
                       .add(rec_network);

            System.out.println("testNetworkBindAndDelete: rebind");
            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<GatewayAssetRecord> helper_gateway = holder.createHelper(GatewayAssetRecord.class);
            RecordHelper<NetworkAssetRecord> helper_network = holder.createHelper(NetworkAssetRecord.class);

            GatewayAssetRecord rec_gateway = helper_gateway.get(id_gateway);
            NetworkAssetRecord rec_network = helper_network.get(id_network);

            assertTrue(rec_gateway.getBoundNetworks()
                                  .contains(rec_network));

            helper_network.delete(rec_network);

            System.out.println("testNetworkBindAndDelete: delete network");
            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<GatewayAssetRecord> helper_gateway = holder.createHelper(GatewayAssetRecord.class);

            GatewayAssetRecord rec_gateway = helper_gateway.get(id_gateway);

            assertEquals(0,
                         rec_gateway.getBoundNetworks()
                                    .size());

            helper_gateway.delete(rec_gateway);

            System.out.println("testNetworkBindAndDelete: delete gateway");
            holder.commit();
        }
    }

    @Test
    @TestOrder(70)
    public void testLogicalGroups() throws
                                    Exception
    {
        RecordLocator<LogicalAssetRecord>  id_group;
        RecordLocator<DeviceRecord>        id_root;
        RecordLocator<DeviceElementRecord> id_asset1;
        RecordLocator<DeviceElementRecord> id_asset2;

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<LogicalAssetRecord>  helper_group  = holder.createHelper(LogicalAssetRecord.class);
            RecordHelper<AssetRecord>         helper_asset  = holder.createHelper(AssetRecord.class);
            RecordHelper<DeviceRecord>        helper_device = holder.createHelper(DeviceRecord.class);
            RecordHelper<DeviceElementRecord> helper_de     = holder.createHelper(DeviceElementRecord.class);

            LogicalAssetRecord recGroup = new LogicalAssetRecord();
            helper_group.persist(recGroup);
            id_group = helper_group.asLocator(recGroup);

            DeviceRecord recRoot = new DeviceRecord();
            recRoot.setDisplayName("Test");
            helper_device.persist(recRoot);
            id_root = helper_device.asLocator(recRoot);

            DeviceElementRecord recAsset1 = new DeviceElementRecord();
            recAsset1.setIdentifier("Id1");
            helper_de.persist(recAsset1);
            id_asset1 = helper_de.asLocator(recAsset1);
            recAsset1.linkToParent(helper_asset, recRoot);

            DeviceElementRecord recAsset2 = new DeviceElementRecord();
            recAsset2.setIdentifier("Id2");
            helper_de.persist(recAsset2);
            id_asset2 = helper_de.asLocator(recAsset2);
            recAsset2.linkToParent(helper_asset, recRoot);

            System.out.println("testLogicalGroups: create");
            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            LogicalAssetRecord  recGroup  = holder.fromLocator(id_group);
            DeviceElementRecord recAsset1 = holder.fromLocator(id_asset1);

            RelationshipRecord.addRelation(holder, recGroup, recAsset1, AssetRelationship.controls);

            System.out.println("testLogicalGroups: add1");
            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<DeviceElementRecord> helper_asset = holder.createHelper(DeviceElementRecord.class);

            LogicalAssetRecord  recGroup  = holder.fromLocator(id_group);
            DeviceElementRecord recAsset1 = holder.fromLocator(id_asset1);
            DeviceElementRecord recAsset2 = holder.fromLocator(id_asset2);

            List<String> logicalNestedAssets = RelationshipRecord.getChildren(holder, id_group.getIdRaw(), AssetRelationship.controls);
            assertEquals(1, logicalNestedAssets.size());
            assertTrue(logicalNestedAssets.contains(id_asset1.getIdRaw()));
            assertFalse(logicalNestedAssets.contains(id_asset2.getIdRaw()));

            final List<String> parents1 = RelationshipRecord.getParents(holder, recAsset1.getSysId(), AssetRelationship.controls);
            assertTrue(parents1.contains(recGroup.getSysId()));

            final List<String> parents2 = RelationshipRecord.getParents(holder, recAsset2.getSysId(), AssetRelationship.controls);
            assertFalse(parents2.contains(recGroup.getSysId()));

            helper_asset.delete(recAsset1);

            List<String> logicalNestedAssets2 = RelationshipRecord.getChildren(holder, id_group.getIdRaw(), AssetRelationship.controls);
            assertEquals(0, logicalNestedAssets2.size());
            RelationshipRecord.addRelation(helper_asset, recGroup, recAsset2, AssetRelationship.controls);

            System.out.println("testLogicalGroups: add2");
            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            LogicalAssetRecord  recGroup  = holder.fromLocator(id_group);
            DeviceElementRecord recAsset2 = holder.fromLocator(id_asset2);

            final List<String> parents2 = RelationshipRecord.getParents(holder, recAsset2.getSysId(), AssetRelationship.controls);
            assertTrue(parents2.contains(recGroup.getSysId()));

            List<String> logicalNestedAssets = RelationshipRecord.getChildren(holder, id_group.getIdRaw(), AssetRelationship.controls);
            assertEquals(1, logicalNestedAssets.size());
            assertTrue(logicalNestedAssets.contains(id_asset2.getIdRaw()));

            recAsset2.setDisplayName("Foo");

            System.out.println("testLogicalGroups: update");
            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            DeviceElementRecord recAsset2 = holder.fromLocator(id_asset2);
            holder.deleteEntity(recAsset2);

            System.out.println("testLogicalGroups: delete");

            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            List<String> logicalNestedAssets = RelationshipRecord.getChildren(holder, id_group.getIdRaw(), AssetRelationship.controls);
            assertEquals(0, logicalNestedAssets.size());

            holder.commit();
        }
    }
}
