/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import com.optio3.cloud.client.hub.api.SearchApi;
import com.optio3.cloud.client.hub.api.UsersApi;
import com.optio3.cloud.client.hub.model.SearchRequest;
import com.optio3.cloud.client.hub.model.SearchResultSet;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.ObjectMappers;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import com.optio3.util.FileSystem;
import com.optio3.util.TimeUtils;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

public class SearchTest extends Optio3Test
{
    private static FileSystem.TmpDirHolder indexDir;

    private static SearchApi searchProxy;
    private static int       hibernateVersion;

    private static String id_loc;
    private static String id_root;
    private static String id_child1;
    private static String id_child2;
    private static String name_loc    = "location";
    private static String name_root   = "root";
    private static String name_child1 = "child1";
    private static String name_child2 = "child2";

    @ClassRule
    public static final TestApplicationWithDbRule<HubApplication, HubConfiguration> applicationRule = new TestApplicationWithDbRule<>(HubApplication.class, "hub-test.yml", (configuration) ->
    {
        indexDir = FileSystem.createTempDirectory("SearchTest");

        Optio3DataSourceFactory dataSourceFactory = configuration.getDataSourceFactory();

        dataSourceFactory.enableEvents                       = true;
        dataSourceFactory.hibernateSearchIndexLocation       = indexDir.getAbsolutePath();
        dataSourceFactory.hibernateSearchIndexingDelay       = 0;
        dataSourceFactory.hibernateSearchIdleRebuildingDelay = 1;
        configuration.data                                   = new HubDataDefinition[] { new HubDataDefinition("demodata/defaultUsers.json", false, false) };

        // Disabled background processing, because some handlers like RecurringRestCounters could write to the Result Staging table and delay the indexer.
        configuration.startBackgroundProcessing = false;
    }, null);

    @AfterClass
    public static void cleanUp()
    {
        indexDir.delete();
    }

    @Test(timeout = 10000)
    @TestOrder(0)
    public void testHibernateInitialize() throws
                                          Exception
    {
        waitForHibernateSearch();
        createTestData();
        searchProxy = applicationRule.createProxy("api/v1", SearchApi.class);
        UsersApi userProxy = applicationRule.createProxy("api/v1", UsersApi.class);
        userProxy.login("admin@demo.optio3.com", "adminPwd");
    }

    @Test
    @TestOrder(10)
    public void testBasicQuery() throws
                                 Exception
    {
        waitForHibernateSearch();

        assertQueryResultsEqual(name_loc, 1, 2);
        assertQueryResultsEqual(name_root, 1, 2);
        assertQueryResultsEqual(name_child1, 0, 1);
        assertQueryResultsEqual(name_child2, 0, 1);
    }

    @Test
    @TestOrder(20)
    public void testSimpleUpdate() throws
                                   Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<DeviceElementRecord> helper = holder.createHelper(DeviceElementRecord.class);

            DeviceElementRecord rec_child1 = helper.get(id_child1);
            name_child1 += " updated";
            rec_child1.setDisplayName(name_child1);

            holder.commit();
        }

        waitForHibernateSearch();

        assertQueryResultsEqual(name_child1, 0, 1);
    }

    @Test
    @TestOrder(30)
    public void testCascadeUpdate() throws
                                    Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<LocationRecord> helper = holder.createHelper(LocationRecord.class);

            LocationRecord rec_loc = helper.get(id_loc);
            name_loc = "totally different name";
            rec_loc.setDisplayName(name_loc);

            holder.commit();
        }

        waitForHibernateSearch();

        assertQueryResultsEqual(name_loc, 1, 2);
    }

    @Test
    @TestOrder(40)
    public void testCascadeDelete() throws
                                    Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<AssetRecord> helper = holder.createHelper(AssetRecord.class);

            AssetRecord rec_root = helper.get(id_root);

            try (ValidationResultsHolder validation = new ValidationResultsHolder(holder, null, true))
            {
                rec_root.remove(validation, helper);
            }

            holder.commit();
        }

        waitForHibernateSearch();

        assertQueryResultsEqual(name_root, 0, 0);
    }

    private void assertQueryResultsEqual(String query,
                                         int expectedDeviceCount,
                                         int expectedDeviceElementCount)
    {
        SearchRequest request = new SearchRequest();
        request.query = query;
        SearchResultSet results = searchProxy.search(request, 0, 10);

        assertEquals(expectedDeviceCount, results.totalDevices.intValue());
        assertEquals(expectedDeviceElementCount, results.totalDeviceElements.intValue());
    }

    private void waitForHibernateSearch() throws
                                          Exception
    {
        Optio3DataSourceFactory dataSourceFactory = applicationRule.getApplication()
                                                                   .getDataSourceFactory(null);

        waitForVersionToChange(5000);

        getAndUnwrapException(dataSourceFactory.getHibernateSearch(), 5, TimeUnit.SECONDS);

        hibernateVersion = dataSourceFactory.getHibernateSearchVersion();
    }

    private void waitForVersionToChange(int timeout) throws
                                                     Exception
    {
        Optio3DataSourceFactory dataSourceFactory = applicationRule.getApplication()
                                                                   .getDataSourceFactory(null);
        long currentTime = System.currentTimeMillis();
        while (dataSourceFactory.getHibernateSearchVersion() == hibernateVersion)
        {
            Thread.sleep(100);
            long elapsedTime = System.currentTimeMillis() - currentTime;
            if (elapsedTime > timeout)
            {
                throw new Exception("Version did not change within timeout.");
            }
        }
    }

    private void createTestData() throws
                                  Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<AssetRecord> helper_asset = holder.createHelper(AssetRecord.class);

            LocationRecord rec_loc = new LocationRecord();
            rec_loc.setDisplayName(name_loc);
            rec_loc.setType(LocationType.GARAGE);
            holder.persistEntity(rec_loc);
            id_loc = rec_loc.getSysId();

            DeviceRecord rec_root = new DeviceRecord();
            rec_root.setDisplayName(name_root);
            rec_root.setLocation(rec_loc);
            holder.persistEntity(rec_root);
            id_root = rec_root.getSysId();
            System.out.printf("id_root: %s%n", id_root);

            DeviceElementRecord rec_child1 = new DeviceElementRecord();
            rec_child1.setIdentifier(name_child1);
            rec_child1.setDisplayName(name_child1);
            rec_child1.setContents(ObjectMappers.SkipNulls, TimeUtils.now());
            holder.persistEntity(rec_child1);
            id_child1 = rec_child1.getSysId();
            System.out.printf("id_child1: %s%n", id_child1);
            rec_child1.linkToParent(helper_asset, rec_root);

            DeviceElementRecord rec_child2 = new DeviceElementRecord();
            rec_child2.setIdentifier(name_child2);
            rec_child2.setDisplayName(name_child2);
            rec_child2.setContents(ObjectMappers.SkipNulls, TimeUtils.now());
            holder.persistEntity(rec_child2);
            id_child2 = rec_child2.getSysId();
            System.out.printf("id_child2: %s%n", id_child2);
            rec_child2.linkToParent(helper_asset, rec_root);

            holder.commit();
        }
    }
}
