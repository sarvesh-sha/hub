/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.cloud.hub.demo.DataLoader;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.logic.normalizations.TermFrequencyInverseDocumentFrequencyVectorizer;
import com.optio3.cloud.hub.persistence.normalization.NormalizationRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.collection.Memoizer;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import org.junit.ClassRule;
import org.junit.Test;

public class NormalizationTest extends Optio3Test
{
    static RecordLocator<NormalizationRecord> loc_v1;
    static RecordLocator<NormalizationRecord> loc_v2;

    @ClassRule
    public static final TestApplicationWithDbRule<HubApplication, HubConfiguration> applicationRule = new TestApplicationWithDbRule<>(HubApplication.class, "hub-test.yml", (configuration) ->
    {
        configuration.data = new HubDataDefinition[] { new HubDataDefinition("demodata/defaultUsers.json", false, false) };
    }, null);

    private static NormalizationRules getNormalizationRules() throws
                                                              IOException
    {
        try (InputStream stream = ClassLoader.getSystemClassLoader()
                                             .getResourceAsStream("demodata/defaultNormalizationRules.json"))
        {
            ObjectMapper mapper = applicationRule.getApplication()
                                                 .getServiceNonNull(ObjectMapper.class);
            DataLoader dl = mapper.readValue(stream, DataLoader.class);
            return dl.normalizationRules.get(0).rules;
        }
    }

    @Test
    @TestOrder(10)
    public void testCreate1() throws
                              Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<NormalizationRecord> helper = holder.createHelper(NormalizationRecord.class);
            NormalizationRules                rules  = getNormalizationRules();

            NormalizationRecord rec_v1 = NormalizationRecord.newInstance(helper, rules, null);

            loc_v1 = helper.asLocator(rec_v1);
            assertEquals(1, rec_v1.getVersion());

            holder.commit();
        }
    }

    @Test
    @TestOrder(11)
    public void testCreate2() throws
                              Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<NormalizationRecord> helper = holder.createHelper(NormalizationRecord.class);
            NormalizationRules                rules  = getNormalizationRules();

            NormalizationRecord rec_v2 = NormalizationRecord.newInstance(helper, rules, null);

            loc_v2 = helper.asLocator(rec_v2);
            assertEquals(2, rec_v2.getVersion());

            holder.commit();
        }
    }

    @Test
    @TestOrder(20)
    public void testMakeActive()
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<NormalizationRecord> helper = holder.createHelper(NormalizationRecord.class);

            assertNull(NormalizationRecord.findActive(helper));

            final NormalizationRecord rec_v1 = holder.fromLocator(loc_v1);
            final NormalizationRecord rec_v2 = holder.fromLocator(loc_v2);
            assertFalse(rec_v1.isActive());
            assertFalse(rec_v2.isActive());
            rec_v1.makeActive(helper);
            assertTrue(rec_v1.isActive());
            assertFalse(rec_v2.isActive());

            assertNotNull(NormalizationRecord.findActive(helper));

            holder.commit();
        }
    }

    @Test
    @TestOrder(21)
    public void testMakeActive2()
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<NormalizationRecord> helper = holder.createHelper(NormalizationRecord.class);

            final NormalizationRecord rec_v1 = holder.fromLocator(loc_v1);
            final NormalizationRecord rec_v2 = holder.fromLocator(loc_v2);
            assertTrue(rec_v1.isActive());
            assertFalse(rec_v2.isActive());
            rec_v2.makeActive(helper);
            assertFalse(rec_v1.isActive());
            assertTrue(rec_v2.isActive());

            assertNotNull(NormalizationRecord.findActive(helper));

            holder.commit();
        }
    }

    @Test
    @TestOrder(30)
    public void validateWellKnownEquipmentClasses()
    {
        Map<Integer, WellKnownEquipmentClass> seen = Maps.newHashMap();

        for (WellKnownEquipmentClass value : WellKnownEquipmentClass.values())
        {
            WellKnownEquipmentClass valueOld = seen.put(value.getId(), value);
            if (valueOld != null)
            {
                fail(String.format("Found two WellKnownEquipmentClass with same id: %s and %s", value, valueOld));
            }
        }
    }

    @Test
    @TestOrder(31)
    public void validateWellKnownPointClasses()
    {
        Map<Integer, WellKnownPointClass> seen = Maps.newHashMap();

        for (WellKnownPointClass value : WellKnownPointClass.values())
        {
            WellKnownPointClass valueOld = seen.put(value.getId(), value);
            if (valueOld != null)
            {
                fail(String.format("Found two WellKnownPointClass with same id: %s and %s", value, valueOld));
            }
        }
    }

    @Test
    @TestOrder(40)
    public void testTFIDF()
    {
        TermFrequencyInverseDocumentFrequencyVectorizer vectorizer = new TermFrequencyInverseDocumentFrequencyVectorizer(new Memoizer(), 1, 1);

        List<String> documents = List.of("this is a test", "this,; is; Another+ example");
        var          result    = vectorizer.score(documents, "example", 1);
        var          result2   = vectorizer.score(documents, "another example test", 1);

        assertEquals(0.0, result.scores[0], 0);
        assertTrue(result.scores[1] > 0);
        assertTrue(result2.scores[0] < result2.scores[1]);
    }
}
