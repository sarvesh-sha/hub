/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
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
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import org.junit.ClassRule;
import org.junit.Test;

public class TagsEngineWithEquipmentLookupTest extends Optio3Test
{
    private static final Multimap<String, String> lookupParent  = HashMultimap.create();
    private static final Multimap<String, String> lookupChild   = HashMultimap.create();
    private static final Multimap<String, String> lookupClasses = HashMultimap.create();

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
            RecordHelper<LogicalAssetRecord> helper_assetLogical = holder.createHelper(LogicalAssetRecord.class);

            List<List<LogicalAssetRecord>> classes = Lists.newArrayList();

            for (int i = 0; i < 10; i++)
            {
                List<LogicalAssetRecord> lst = createEquipments(helper_assetLogical, "class" + i, 10);
                classes.add(lst);
            }

            for (int i = 0; i < 9; i++)
            {
                int j = i + 1;

                {
                    List<LogicalAssetRecord> parents  = classes.get(i);
                    List<LogicalAssetRecord> children = classes.get(j);

                    for (int k = 0; k < parents.size(); k++)
                    {
                        LogicalAssetRecord child  = children.get(k);
                        LogicalAssetRecord parent = parents.get(k);

                        child.linkToParent(helper_assetLogical, parent);
                        lookupChild.put(parent.getSysId(), child.getSysId());
                        lookupParent.put(child.getSysId(), parent.getSysId());
                    }
                }
            }

            holder.commit();
        }

        applicationRule.drainDatabaseEvents();
    }

    private List<LogicalAssetRecord> createEquipments(RecordHelper<LogicalAssetRecord> helper,
                                                      String classId,
                                                      int count)
    {
        List<LogicalAssetRecord> res = Lists.newArrayList();

        for (int i = 0; i < count; i++)
        {
            res.add(createEquipment(helper, classId, i));
        }

        return res;
    }

    private LogicalAssetRecord createEquipment(RecordHelper<LogicalAssetRecord> helper,
                                               String classId,
                                               int index)
    {
        LogicalAssetRecord rec = new LogicalAssetRecord();

        rec.modifyTags((tags) ->
                       {
                           tags.addTag(AssetRecord.WellKnownTags.isEquipment, false);
                           tags.setValuesForTag(AssetRecord.WellKnownTags.equipmentClassId, Sets.newHashSet(classId));
                       });

        rec.setSysId(classId + '#' + index);

        helper.persist(rec);

        lookupClasses.put(classId, rec.getSysId());

        return rec;
    }

    @Test
    @TestOrder(20)
    public void testQuery()
    {
        HubApplication app    = applicationRule.getApplication();
        TagsEngine     engine = app.getServiceNonNull(TagsEngine.class);

        AssetRecord.EquipmentLookup lookup = new AssetRecord.EquipmentLookup(engine.acquireSnapshot(true));
        assertEquals(100, lookup.equipments.size());
        assertEquals(10,
                     lookup.classIds.keySet()
                                    .size());

        for (String classId : lookup.classIds.keySet())
        {
            assertEquals(lookupClasses.get(classId), lookup.classIds.get(classId));
        }

        for (int i = 0; i < 9; i++)
        {
            int j = i + 1;

            {
                Collection<String> parents  = lookupClasses.get("class" + i);
                Collection<String> children = lookupClasses.get("class" + j);

                for (String child : children)
                {
                    TypedRecordIdentity<? extends AssetRecord> ri = lookup.findParent(child, AssetRelationship.structural);
                    assertNotNull(ri);
                    assertTrue(parents.contains(ri.sysId));
                }

                for (String parent : parents)
                {
                    Set<TypedRecordIdentity<? extends AssetRecord>> lst = lookup.getChildren(parent, AssetRelationship.structural);
                    assertEquals(1, lst.size());

                    for (TypedRecordIdentity<? extends AssetRecord> child : lst)
                    {
                        assertTrue(children.contains(child.sysId));
                    }
                }
            }
        }
    }
}