/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.logic.tags.TagsStreamNextAction;
import com.optio3.cloud.hub.model.asset.AssetFilterRequest;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.model.tags.TagsCondition;
import com.optio3.cloud.hub.model.tags.TagsConditionBinaryLogic;
import com.optio3.cloud.hub.model.tags.TagsConditionIsAsset;
import com.optio3.cloud.hub.model.tags.TagsConditionLocation;
import com.optio3.cloud.hub.model.tags.TagsConditionOperator;
import com.optio3.cloud.hub.model.tags.TagsConditionTerm;
import com.optio3.cloud.hub.model.tags.TagsConditionTermWithValue;
import com.optio3.cloud.hub.model.tags.TagsJoin;
import com.optio3.cloud.hub.model.tags.TagsJoinQuery;
import com.optio3.cloud.hub.model.tags.TagsJoinTerm;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.RelationshipRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import com.optio3.util.CollectionUtils;
import org.junit.ClassRule;
import org.junit.Test;

public class TagsEngineTest extends Optio3Test
{
    private static final Multimap<String, String> lookupFromTagToSysId = HashMultimap.create();
    private static final Multimap<String, String> lookupFromSysIdToTag = HashMultimap.create();
    private static final List<String>             sysIds               = Lists.newArrayList();
    private static       String                   sysId_locTop;
    private static       String                   sysId_locChild1;
    private static       String                   sysId_locChild2;

    @ClassRule
    public static final TestApplicationWithDbRule<HubApplication, HubConfiguration> applicationRule = new TestApplicationWithDbRule<>(HubApplication.class, "hub-test.yml", (configuration) ->
    {
        Optio3DataSourceFactory dataSourceFactory = configuration.getDataSourceFactory();
        dataSourceFactory.enableEvents = true;

        configuration.data = new HubDataDefinition[] { new HubDataDefinition("demodata/defaultUsers.json", false, false) };
    }, null);

    @Test
    @TestOrder(10)
    public void testCreate()
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<AssetRecord>    helper_asset    = holder.createHelper(AssetRecord.class);
            RecordHelper<LocationRecord> helper_location = holder.createHelper(LocationRecord.class);

            LocationRecord rec_locTop = new LocationRecord();
            rec_locTop.setDisplayName("Top");
            rec_locTop.setType(LocationType.BUILDING);
            helper_location.persist(rec_locTop);
            sysId_locTop = rec_locTop.getSysId();

            LocationRecord rec_locChild1 = new LocationRecord();
            rec_locChild1.setDisplayName("Child1");
            rec_locChild1.setType(LocationType.FLOOR);
            helper_location.persist(rec_locChild1);
            sysId_locChild1 = rec_locChild1.getSysId();
            rec_locChild1.linkToParent(helper_asset, rec_locTop);

            LocationRecord rec_locChild2 = new LocationRecord();
            rec_locChild2.setDisplayName("Child2");
            rec_locChild2.setType(LocationType.FLOOR);
            helper_location.persist(rec_locChild2);
            sysId_locChild2 = rec_locChild2.getSysId();
            rec_locChild2.linkToParent(helper_asset, rec_locTop);

            Random rnd = new Random(123587);

            for (int i = 0; i < 1000; i++)
            {
                DeviceElementRecord rec = new DeviceElementRecord();
                rec.setIdentifier(String.format("child%d", i));

                rec.setLocation(i % 2 == 0 ? rec_locChild1 : rec_locChild2);
                helper_asset.persist(rec);
                String sysId = rec.getSysId();
                sysIds.add(sysId);

                rec.modifyTags((tags) ->
                               {
                                   int count = 1 + rnd.nextInt(10);
                                   for (int j = 0; j < count; j++)
                                   {
                                       String tag = String.format("Tag%d", rnd.nextInt(10));
                                       tags.addValueToTag(tag, Integer.toString(j));
                                       lookupFromTagToSysId.put(tag, sysId);
                                       lookupFromSysIdToTag.put(sysId, tag);
                                   }
                               });
            }

            holder.commit();
        }

        applicationRule.drainDatabaseEvents();
    }

    @Test
    @TestOrder(20)
    public void testQuery()
    {
        HubApplication app    = applicationRule.getApplication();
        TagsEngine     engine = app.getServiceNonNull(TagsEngine.class);

        for (int i = 0; i < 10; i++)
        {
            // Force a GC to test lazy heap loading.
            System.gc();

            String tag = String.format("Tag%d", i);

            TagsCondition query = TagsConditionTerm.build(tag);
            Set<String> results = extractSysId(engine.acquireSnapshot(true)
                                                     .evaluateCondition(query));

            Set<String> expectedResults = Sets.newHashSet(lookupFromTagToSysId.get(tag));
            assertEquals(expectedResults, results);

            Set<String> resultsByValue = Sets.newHashSet();
            for (int j = 0; j < 10; j++)
            {
                TagsCondition queryByValue = TagsConditionTermWithValue.build(tag, Integer.toString(j));

                Set<String> results2 = extractSysId(engine.acquireSnapshot(true)
                                                          .evaluateCondition(queryByValue));
                resultsByValue.addAll(results2);
            }

            for (String sysId : expectedResults)
            {
                TagsCondition queryByValue = TagsConditionIsAsset.build(sysId);

                Set<String> results2 = extractSysId(engine.acquireSnapshot(true)
                                                          .evaluateCondition(queryByValue));

                assertEquals(1, results2.size());
                assertTrue(results2.contains(sysId));
            }

            assertEquals(expectedResults, resultsByValue);
        }
    }

    @Test
    @TestOrder(21)
    public void testQueryAnd()
    {
        HubApplication app    = applicationRule.getApplication();
        TagsEngine     engine = app.getServiceNonNull(TagsEngine.class);

        for (int i = 0; i < 10; i++)
        {
            // Force a GC to test lazy heap loading.
            System.gc();

            TagsConditionTerm queryA = TagsConditionTerm.build("Tag0");
            TagsConditionTerm queryB = TagsConditionTerm.build(String.format("Tag%d", i));

            Set<String> results = extractSysId(engine.acquireSnapshot(true)
                                                     .evaluateCondition(TagsConditionBinaryLogic.build(queryA, queryB, TagsConditionOperator.And)));

            Set<String> sysIdsA         = Sets.newHashSet(lookupFromTagToSysId.get(queryA.tag));
            Set<String> sysIdsB         = Sets.newHashSet(lookupFromTagToSysId.get(queryB.tag));
            Set<String> expectedResults = Sets.newHashSet(Sets.intersection(sysIdsA, sysIdsB));
            assertEquals(expectedResults, results);
        }
    }

    @Test
    @TestOrder(22)
    public void testQueryOr()
    {
        HubApplication app    = applicationRule.getApplication();
        TagsEngine     engine = app.getServiceNonNull(TagsEngine.class);

        for (int i = 0; i < 10; i++)
        {
            // Force a GC to test lazy heap loading.
            System.gc();

            TagsConditionTerm queryA = TagsConditionTerm.build("Tag0");
            TagsConditionTerm queryB = TagsConditionTerm.build(String.format("Tag%d", i));

            Set<String> results = extractSysId(engine.acquireSnapshot(true)
                                                     .evaluateCondition(TagsConditionBinaryLogic.build(queryA, queryB, TagsConditionOperator.Or)));

            Set<String> sysIdsA         = Sets.newHashSet(lookupFromTagToSysId.get(queryA.tag));
            Set<String> sysIdsB         = Sets.newHashSet(lookupFromTagToSysId.get(queryB.tag));
            Set<String> expectedResults = Sets.newHashSet(Sets.union(sysIdsA, sysIdsB));
            assertEquals(expectedResults, results);
        }
    }

    @Test
    @TestOrder(23)
    public void testQueryLocation()
    {
        HubApplication app    = applicationRule.getApplication();
        TagsEngine     engine = app.getServiceNonNull(TagsEngine.class);

        TagsCondition query = TagsConditionLocation.build(sysId_locTop);
        Set<String> results = extractSysId(engine.acquireSnapshot(true)
                                                 .evaluateCondition(query));
        assertEquals(results, Sets.newHashSet(sysIds));

        query   = TagsConditionLocation.build(sysId_locChild1);
        results = extractSysId(engine.acquireSnapshot(true)
                                     .evaluateCondition(query));
        for (int i = 0; i < sysIds.size(); i++)
        {
            assertEquals(i % 2 == 0, results.contains(sysIds.get(i)));
        }

        query   = TagsConditionLocation.build(sysId_locChild2);
        results = extractSysId(engine.acquireSnapshot(true)
                                     .evaluateCondition(query));
        for (int i = 0; i < sysIds.size(); i++)
        {
            assertEquals(i % 2 == 1, results.contains(sysIds.get(i)));
        }
    }

    @Test
    @TestOrder(24)
    public void testFilterWithQuery()
    {
        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<AssetRecord> helper_asset = holder.createHelper(AssetRecord.class);

            for (int i = 0; i < 10; i++)
            {
                // Force a GC to test lazy heap loading.
                System.gc();

                TagsConditionTerm queryA = TagsConditionTerm.build("Tag0");
                TagsConditionTerm queryB = TagsConditionTerm.build(String.format("Tag%d", i));

                AssetFilterRequest filters = new AssetFilterRequest();
                filters.tagsQuery = TagsConditionBinaryLogic.build(queryA, queryB, TagsConditionOperator.And);

                Set<String> results = Sets.newHashSet(CollectionUtils.transformToList(AssetRecord.filterAssets(helper_asset, filters), (item) -> item.sysId));

                Set<String> sysIdsA         = Sets.newHashSet(lookupFromTagToSysId.get(queryA.tag));
                Set<String> sysIdsB         = Sets.newHashSet(lookupFromTagToSysId.get(queryB.tag));
                Set<String> expectedResults = Sets.newHashSet(Sets.intersection(sysIdsA, sysIdsB));
                assertEquals(expectedResults, results);
            }
        }
    }

    @Test
    @TestOrder(25)
    public void testQueryAfterDelete()
    {
        HubApplication app    = applicationRule.getApplication();
        TagsEngine     engine = app.getServiceNonNull(TagsEngine.class);

        for (int i = 0; i < 10; i++)
        {
            // Force a GC to test lazy heap loading.
            System.gc();

            String tag = String.format("Tag%d", i);

            try (SessionHolder holder = applicationRule.openSessionWithTransaction())
            {
                RecordHelper<AssetRecord> helper_asset = holder.createHelper(AssetRecord.class);

                String sysId = CollectionUtils.firstElement(lookupFromTagToSysId.get(tag));

                helper_asset.delete(helper_asset.get(sysId));

                holder.commit();

                List<String> removeTags = Lists.newArrayList(lookupFromSysIdToTag.get(sysId));
                for (String removeTag : removeTags)
                {
                    lookupFromTagToSysId.remove(removeTag, sysId);
                    lookupFromSysIdToTag.remove(sysId, removeTag);
                }
            }

            applicationRule.drainDatabaseEvents();

            TagsCondition query = TagsConditionTerm.build(tag);
            Set<String> results = extractSysId(engine.acquireSnapshot(true)
                                                     .evaluateCondition(query));

            Set<String> expectedResults = Sets.newHashSet(lookupFromTagToSysId.get(tag));
            assertEquals(expectedResults, results);
        }
    }

    @Test
    @TestOrder(30)
    public void testGroups()
    {
        RecordLocator<AssetRecord>         id_assetRoot;
        RecordLocator<DeviceElementRecord> id_asset1;
        RecordLocator<DeviceElementRecord> id_asset2;
        RecordLocator<DeviceElementRecord> id_asset3;
        RecordLocator<DeviceElementRecord> id_asset4;

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<AssetRecord>         helper_asset         = holder.createHelper(AssetRecord.class);
            RecordHelper<DeviceElementRecord> helper_deviceElement = holder.createHelper(DeviceElementRecord.class);

            DeviceRecord recAssetRoot = new DeviceRecord();
            recAssetRoot.setDisplayName("IdRoot");
            helper_asset.persist(recAssetRoot);
            id_assetRoot = helper_asset.asLocator(recAssetRoot);

            DeviceElementRecord recAsset1 = new DeviceElementRecord();
            recAsset1.setIdentifier("Id1");
            addTag(recAsset1, "TagA");
            helper_deviceElement.persist(recAsset1);
            id_asset1 = helper_deviceElement.asLocator(recAsset1);

            DeviceElementRecord recAsset2 = new DeviceElementRecord();
            recAsset2.setIdentifier("Id2");
            addTag(recAsset2, "TagA");
            addTag(recAsset2, "TagB");
            helper_deviceElement.persist(recAsset2);
            id_asset2 = helper_deviceElement.asLocator(recAsset2);

            DeviceElementRecord recAsset3 = new DeviceElementRecord();
            recAsset3.setIdentifier("Id3");
            addTag(recAsset3, "TagC");
            helper_deviceElement.persist(recAsset3);
            id_asset3 = helper_deviceElement.asLocator(recAsset3);

            DeviceElementRecord recAsset4 = new DeviceElementRecord();
            recAsset4.setIdentifier("Id4");
            addTag(recAsset4, "TagC");
            addTag(recAsset4, "TagD");
            helper_deviceElement.persist(recAsset4);
            id_asset4 = helper_deviceElement.asLocator(recAsset4);

            recAsset1.linkToParent(helper_asset, recAssetRoot);
            recAsset2.linkToParent(helper_asset, recAssetRoot);

            RelationshipRecord.addRelation(holder, recAssetRoot, recAsset3, AssetRelationship.controls);
            RelationshipRecord.addRelation(holder, recAssetRoot, recAsset4, AssetRelationship.controls);

            holder.commit();
        }

        applicationRule.drainDatabaseEvents();

        HubApplication app    = applicationRule.getApplication();
        TagsEngine     engine = app.getServiceNonNull(TagsEngine.class);

        Set<String> set1 = extractSysId(engine.acquireSnapshot(true)
                                              .resolveRelations(id_assetRoot.getIdRaw(), AssetRelationship.structural, false));
        assertTrue(set1.contains(id_asset1.getIdRaw()));
        assertTrue(set1.contains(id_asset2.getIdRaw()));
        assertFalse(set1.contains(id_asset3.getIdRaw()));
        assertFalse(set1.contains(id_asset4.getIdRaw()));

        Set<String> set2 = extractSysId(engine.acquireSnapshot(true)
                                              .resolveRelations(id_assetRoot.getIdRaw(), AssetRelationship.controls, false));
        assertFalse(set2.contains(id_asset1.getIdRaw()));
        assertFalse(set2.contains(id_asset2.getIdRaw()));
        assertTrue(set2.contains(id_asset3.getIdRaw()));
        assertTrue(set2.contains(id_asset4.getIdRaw()));

        //--//

        TagsJoinQuery joinQuery = new TagsJoinQuery();

        TagsJoinTerm joinTermA = new TagsJoinTerm();
        joinTermA.id = "a";
        joinQuery.terms.add(joinTermA);

        TagsJoinTerm joinTermB = new TagsJoinTerm();
        joinTermB.id         = "b";
        joinTermB.conditions = TagsConditionTerm.build("TagA");
        joinQuery.terms.add(joinTermB);

        TagsJoin join1 = new TagsJoin();
        join1.leftSide  = "a";
        join1.relation  = AssetRelationship.structural;
        join1.rightSide = "b";
        joinQuery.joins.add(join1);

        Multimap<String, String> res1 = HashMultimap.create();

        engine.acquireSnapshot(true)
              .evaluateJoin(joinQuery, (tuple) ->
              {
                  String[] res = tuple.asSysIds();
                  assertEquals(2, res.length);
                  res1.put(res[0], res[1]);
                  return null;
              });

        assertTrue(res1.keySet()
                       .contains(id_assetRoot.getIdRaw()));

        final Collection<String> relations1 = res1.get(id_assetRoot.getIdRaw());
        assertTrue(relations1.contains(id_asset1.getIdRaw()));
        assertTrue(relations1.contains(id_asset2.getIdRaw()));
        assertFalse(relations1.contains(id_asset3.getIdRaw()));
        assertFalse(relations1.contains(id_asset4.getIdRaw()));

        //--//

        joinTermB.conditions = TagsConditionTerm.build("TagB");

        res1.clear();

        engine.acquireSnapshot(true)
              .evaluateJoin(joinQuery, (tuple) ->
              {
                  String[] res = tuple.asSysIds();
                  assertEquals(2, res.length);
                  res1.put(res[0], res[1]);
                  return null;
              });

        assertTrue(res1.keySet()
                       .contains(id_assetRoot.getIdRaw()));

        final Collection<String> relations2 = res1.get(id_assetRoot.getIdRaw());
        assertFalse(relations2.contains(id_asset1.getIdRaw()));
        assertTrue(relations2.contains(id_asset2.getIdRaw()));
        assertFalse(relations2.contains(id_asset3.getIdRaw()));
        assertFalse(relations2.contains(id_asset4.getIdRaw()));

        //--//

        join1.leftSide  = "b";
        join1.rightSide = "a";

        res1.clear();

        engine.acquireSnapshot(true)
              .evaluateJoin(joinQuery, (tuple) ->
              {
                  String[] res = tuple.asSysIds();
                  assertEquals(2, res.length);
                  res1.put(res[0], res[1]);
                  return null;
              });

        assertEquals(0, res1.size());
    }

    private void addTag(DeviceElementRecord rec,
                        String tag)
    {
        rec.modifyTags((tagMap ->
        {
            tagMap.addTag(tag, false);
        }));
    }

    private Set<String> extractSysId(TagsEngine.Snapshot.AssetSet inputs)
    {
        Set<String> outputs = Sets.newHashSet();

        inputs.streamResolved((ri) ->
                              {
                                  outputs.add(ri.sysId);
                                  return TagsStreamNextAction.Continue;
                              });

        return outputs;
    }
}