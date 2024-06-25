/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.infra.various;

import static org.junit.Assert.assertNotNull;

import com.optio3.infra.NgrokHelper;
import com.optio3.test.common.Optio3Test;
import org.junit.Ignore;
import org.junit.Test;

public class NgrokTest extends Optio3Test
{
    @Ignore("Manually enable to test, since it requires access to Ngrok")
    @Test
    public void testExtractUrl() throws
                                 Exception
    {
        try (NgrokHelper helper = new NgrokHelper("8180"))
        {
            String url = helper.start();
            assertNotNull(url);
            System.out.printf("Ngrok URL is: %s%n", url);
        }
    }
}
