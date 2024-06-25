/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.client.hub.api.ExportsApi;
import com.optio3.cloud.client.hub.api.UsersApi;
import com.optio3.cloud.client.hub.model.ExportCell;
import com.optio3.cloud.client.hub.model.ExportColumn;
import com.optio3.cloud.client.hub.model.ExportHeader;
import com.optio3.cloud.client.hub.model.User;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import com.optio3.util.TimeUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.ClassRule;
import org.junit.Test;

public class ExportsTest extends Optio3Test
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
    public void testExport() throws
                             IOException
    {
        ExportsApi proxy = applicationRule.createProxy("api/v1", ExportsApi.class);

        ExportHeader rh = new ExportHeader();
        rh.sheetName = "Sheet Test";

        ExportColumn c1 = new ExportColumn();
        c1.title = "t1";
        rh.columns.add(c1);

        ExportColumn c2 = new ExportColumn();
        c2.title         = "t2";
        c2.dateFormatter = "m/d/yy h:mm:ss";
        rh.columns.add(c2);

        String id = proxy.start(rh);

        for (int k = 0; k < 30; k++)
        {
            List<List<ExportCell>> rows = Lists.newArrayList();
            for (int i = 0; i < 100; i++)
            {
                List<ExportCell> row = Lists.newArrayList();
                ExportCell       rc1 = new ExportCell();
                rc1.text = "test";
                row.add(rc1);
                ExportCell rc2 = new ExportCell();
                rc2.dateTime = TimeUtils.now();
                row.add(rc2);
                rows.add(row);
                rows.add(row);
            }
            proxy.add(id, rows);
        }

        proxy.generateExcel(id);

        InputStream stream = proxy.streamExcel(id, "test");

        XSSFWorkbook workbook = new XSSFWorkbook(stream);
        XSSFSheet    sheet    = workbook.getSheetAt(0);
        assertEquals("Sheet Test", sheet.getSheetName());
    }
}
