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

import com.optio3.cloud.client.hub.api.UserPreferencesApi;
import com.optio3.cloud.client.hub.api.UsersApi;
import com.optio3.cloud.client.hub.model.User;
import com.optio3.cloud.client.hub.model.UserPreference;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.cloud.hub.persistence.config.UserPreferenceRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import org.junit.ClassRule;
import org.junit.Test;

public class UserPrefsTest extends Optio3Test
{
    private static final String KEY1   = "test1";
    private static final String VALUE1 = "value1";

    private static final String KEY2   = "test2";
    private static final String VALUE2 = "test2_foo";

    static User user;

    @ClassRule
    public static final TestApplicationWithDbRule<HubApplication, HubConfiguration> applicationRule = new TestApplicationWithDbRule<>(HubApplication.class, "hub-test.yml", (configuration) ->
    {
        configuration.data = new HubDataDefinition[] { new HubDataDefinition("demodata/defaultUsers.json", false, false) };
    }, null);

    @Test
    @TestOrder(1)
    public void testSetup()
    {
        UsersApi proxy = applicationRule.createProxy("api/v1", UsersApi.class);

        user = proxy.login("admin@demo.optio3.com", "adminPwd");
        assertNotNull(user);
    }

    @Test
    @TestOrder(2)
    public void testSet()
    {
        UserPreferencesApi proxy = applicationRule.createProxy("api/v1", UserPreferencesApi.class);

        assertNotNull(proxy.setValue(user.sysId, null, KEY1, VALUE1));
        assertNotNull(proxy.setValue(user.sysId, "path1", KEY1, VALUE1));
    }

    @Test
    @TestOrder(3)
    public void testSet2()
    {
        UserPreferencesApi proxy = applicationRule.createProxy("api/v1", UserPreferencesApi.class);

        assertNotNull(proxy.setValue(user.sysId, null, KEY2, VALUE2));
        assertNotNull(proxy.setValue(user.sysId, "path2", KEY2, VALUE2));
    }

    @Test
    @TestOrder(4)
    public void testList()
    {
        UserPreferencesApi proxy = applicationRule.createProxy("api/v1", UserPreferencesApi.class);

        List<String> list = proxy.listValues(user.sysId, null);
        assertEquals(2, list.size());
        assertTrue(list.contains(KEY1));
        assertTrue(list.contains(KEY2));

        List<String> list1 = proxy.listValues(user.sysId, "path1");
        assertEquals(1, list1.size());
        assertTrue(list1.contains(KEY1));
        assertTrue(!list1.contains(KEY2));

        List<String> list2 = proxy.listValues(user.sysId, "path2");
        assertEquals(1, list2.size());
        assertTrue(!list2.contains(KEY1));
        assertTrue(list2.contains(KEY2));

        //--//

        List<String> subkeys1 = proxy.listSubKeys(user.sysId, null);
        assertEquals(2, subkeys1.size());

        proxy.removeSubKeys(user.sysId, null);

        List<String> subkeys2 = proxy.listSubKeys(user.sysId, null);
        assertEquals(0, subkeys2.size());
    }

    @Test
    @TestOrder(5)
    public void testGet()
    {
        UserPreferencesApi proxy = applicationRule.createProxy("api/v1", UserPreferencesApi.class);

        UserPreference pref;
        pref = proxy.getValue(user.sysId, null, KEY1);
        assertEquals(VALUE1, pref.value);
        pref = proxy.getValue(user.sysId, null, KEY2);
        assertEquals(VALUE2, pref.value);
    }

    @Test
    @TestOrder(6)
    public void testDelete()
    {
        UserPreferencesApi proxy = applicationRule.createProxy("api/v1", UserPreferencesApi.class);

        proxy.removeValue(user.sysId, null, KEY1);
    }

    @Test
    @TestOrder(7)
    public void testList2()
    {
        UserPreferencesApi proxy = applicationRule.createProxy("api/v1", UserPreferencesApi.class);

        List<String> list = proxy.listValues(user.sysId, null);
        assertEquals(1, list.size());
        assertFalse(list.contains(KEY1));
        assertTrue(list.contains(KEY2));
    }

    @Test
    @TestOrder(20)
    public void testCascade()
    {
        UserPreferencesApi proxy = applicationRule.createProxy("api/v1", UserPreferencesApi.class);

        String sysId = proxy.setValue(user.sysId, null, KEY2, VALUE2);
        assertNotNull(sysId);

        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<UserPreferenceRecord> helper = holder.createHelper(UserPreferenceRecord.class);
            helper.get(sysId);
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<UserRecord> helper   = holder.createHelper(UserRecord.class);
            UserRecord               rec_user = helper.get(user.sysId);
            helper.delete(rec_user);

            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<UserPreferenceRecord> helper = holder.createHelper(UserPreferenceRecord.class);
            assertEquals(null, helper.getOrNull(sysId));
        }
    }
}
