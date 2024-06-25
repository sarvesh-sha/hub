/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub.digineous;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import ch.qos.logback.classic.Logger;
import com.optio3.concurrency.Executors;
import com.optio3.infra.integrations.infiniteimpulse.InfiniteImpulseHelper;
import com.optio3.infra.integrations.infiniteimpulse.model.TrendEntry;
import com.optio3.infra.integrations.infiniteimpulse.model.TrendHistory;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.TimeUtils;
import io.dropwizard.logging.LoggingUtil;
import org.junit.Ignore;
import org.junit.Test;

public class DigineousVibrationTest
{
    @Ignore("Manually enable to test, it requires credentials")
    @Test
    public void testInfiniteImpulse()
    {
        final Logger root = LoggingUtil.getLoggerContext()
                                       .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.detachAndStopAllAppenders();

        InfiniteImpulseHelper helper = new InfiniteImpulseHelper(null, "<email>", "<password>");

        for (Integer plantId : helper.getPlants()
                                     .keySet())
        {
            System.out.printf("############\n");
            System.out.printf("Plant %d:\n", plantId);
            System.out.printf("%s\n", ObjectMappers.prettyPrintAsJson(helper.getMonitorsByPlantId(plantId, true)));
        }

        for (int i = 0; i < 10; i++)
        {
            ZonedDateTime to   = TimeUtils.nowUtc();
            ZonedDateTime from = to.minus(2, ChronoUnit.MINUTES);

            TrendHistory res = helper.getTrend(from, to, true, 8293);
            for (TrendEntry basic_feature : res.basic_features)
            {
                System.out.printf("%s %s: %s\n", to, basic_feature.time, basic_feature.avg.temperature);
            }
//            System.out.println(ObjectMappers.prettyPrintAsJson(res));

            System.out.printf("############\n");

            Executors.safeSleep(5_000);
        }
    }
}
