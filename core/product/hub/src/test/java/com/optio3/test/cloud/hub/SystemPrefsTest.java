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

import com.optio3.cloud.client.hub.api.SystemPreferencesApi;
import com.optio3.cloud.client.hub.api.UsersApi;
import com.optio3.cloud.client.hub.model.SystemPreference;
import com.optio3.cloud.client.hub.model.User;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import org.junit.ClassRule;
import org.junit.Test;

public class SystemPrefsTest extends Optio3Test
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
        SystemPreferencesApi proxy = applicationRule.createProxy("api/v1", SystemPreferencesApi.class);

        String sysId = proxy.setValue(null, KEY1, VALUE1);
        assertNotNull(sysId);
    }

    @Test
    @TestOrder(3)
    public void testSet2()
    {
        SystemPreferencesApi proxy = applicationRule.createProxy("api/v1", SystemPreferencesApi.class);

        String sysId = proxy.setValue(null, KEY2, VALUE2);
        assertNotNull(sysId);
    }

    @Test
    @TestOrder(4)
    public void testList()
    {
        SystemPreferencesApi proxy = applicationRule.createProxy("api/v1", SystemPreferencesApi.class);

        List<String> list = proxy.listValues(null);
        assertEquals(2, list.size());
        assertTrue(list.contains(KEY1));
        assertTrue(list.contains(KEY2));
    }

    @Test
    @TestOrder(5)
    public void testGet()
    {
        SystemPreferencesApi proxy = applicationRule.createProxy("api/v1", SystemPreferencesApi.class);

        SystemPreference pref;
        pref = proxy.getValue(null, KEY1);
        assertEquals(VALUE1, pref.value);
        pref = proxy.getValue(null, KEY2);
        assertEquals(VALUE2, pref.value);
    }

    @Test
    @TestOrder(6)
    public void testDelete()
    {
        SystemPreferencesApi proxy = applicationRule.createProxy("api/v1", SystemPreferencesApi.class);

        proxy.removeValue(null, KEY1);
    }

    @Test
    @TestOrder(7)
    public void testList2()
    {
        SystemPreferencesApi proxy = applicationRule.createProxy("api/v1", SystemPreferencesApi.class);

        List<String> list = proxy.listValues(null);
        assertEquals(1, list.size());
        assertFalse(list.contains(KEY1));
        assertTrue(list.contains(KEY2));
    }
}
