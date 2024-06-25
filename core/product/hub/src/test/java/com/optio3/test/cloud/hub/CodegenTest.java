/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import java.io.IOException;

import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.sdk.codegen.Optio3Coordinator;
import com.optio3.sdk.codegen.Optio3Generator;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import com.optio3.util.FileSystem;
import org.junit.ClassRule;
import org.junit.Test;

public class CodegenTest extends Optio3Test
{
    @ClassRule
    public static final TestApplicationWithDbRule<HubApplication, HubConfiguration> applicationRule = new TestApplicationWithDbRule<>(HubApplication.class, "hub-test.yml", null, null);

    @Test
    @TestOrder(1)
    public void testTypescript() throws
                                 IOException
    {
        String spec = applicationRule.baseUri()
                                     .toString() + "api/v1/swagger.json";

        try (FileSystem.TmpFileHolder holder = FileSystem.createTempFile())
        {
            Optio3Coordinator coord = new Optio3Coordinator(spec,
                                                            holder.get()
                                                                  .getParent());

            Optio3Generator gen = coord.configureForTypescript();

            gen.generate();
        }
    }
}
