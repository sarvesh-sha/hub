/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub.digineous;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.optio3.cloud.client.hub.api.DataConnectionApi;
import com.optio3.cloud.client.hub.util.DataConnectionHelper;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.cloud.hub.model.customization.digineous.InstanceConfigurationForDigineous;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousBlackBoxPayload;
import com.optio3.protocol.model.FieldModel;
import com.optio3.serialization.ObjectMappers;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

public class DigineousSamplesTest extends Optio3Test
{
    static URI baseUri = URI.create("http://localhost:8080/");

    @ClassRule
    public static final TestApplicationWithDbRule<HubApplication, HubConfiguration> applicationRule = new TestApplicationWithDbRule<>(HubApplication.class, "hub-test.yml", (configuration) ->
    {
        configuration.data = new HubDataDefinition[] { new HubDataDefinition("demodata/defaultUsers.json", false, false) };

        configuration.instanceConfigurationForUnitTest = new InstanceConfigurationForDigineous();
    }, null);

    //--//

    @Ignore("Manually enable to test, it requires a local file")
    @Test
    @TestOrder(10)
    public void test507_single()
    {
        var proxy = applicationRule.createProxy(baseUri, "api/v1", DataConnectionApi.class);

        DigineousBlackBoxPayload bb = new DigineousBlackBoxPayload();
        bb.deviceID         = 507;
        bb.ReceivedDateTime = LocalDateTime.now();
        bb.AI1              = 12f;
        bb.AI2              = 13f;

        DataConnectionHelper.callEndpoint(proxy, InstanceConfigurationForDigineous.endpoint__DATA, null, bb, DigineousBlackBoxPayload.class);
    }

    //--//

    public static class Payload
    {
        public List<DigineousBlackBoxPayload> Sheet1;
    }

    @Ignore("Manually enable to test, it requires a local file")
    @Test
    @TestOrder(20)
    public void test507() throws
                          Exception
    {
        try (InputStream stream = new FileInputStream(System.getenv("HOME") + "/Downloads/json/Device_507_oct.txt"))
        {
            ObjectMapper mapper  = DigineousBlackBoxPayload.getFixupObjectMapper(ObjectMappers.SkipNulls);
            Payload      payload = mapper.readValue(stream, Payload.class);
            System.out.printf("%d\n", payload.Sheet1.size());

            for (FieldModel fieldModel : DigineousBlackBoxPayload.getDescriptors())
            {
                Field field = DigineousBlackBoxPayload.class.getField(fieldModel.name);

                Object firstVal = null;

                for (DigineousBlackBoxPayload bb : payload.Sheet1)
                {
                    Object val = field.get(bb);
                    if (val != null)
                    {
                        if (val instanceof Float)
                        {
                            if (firstVal == null)
                            {
                                firstVal = val;
                            }
                            else if ((float) val != (float) firstVal)
                            {
                                System.out.printf(" %s is set\n", fieldModel.name);
                                break;
                            }
                        }

                        if (val instanceof Boolean)
                        {
                            if (firstVal == null)
                            {
                                firstVal = val;
                            }
                            else if ((boolean) val != (boolean) firstVal)
                            {
                                System.out.printf(" %s is set\n", fieldModel.name);
                                break;
                            }
                        }
                    }
                }
            }

            var proxy = applicationRule.createProxy(baseUri, "api/v1", DataConnectionApi.class);

            List<DigineousBlackBoxPayload> batch = Lists.newArrayList();
            AtomicInteger                  count = new AtomicInteger();

            for (DigineousBlackBoxPayload bb : payload.Sheet1)
            {
                batch.add(bb);

                if (batch.size() >= 1000)
                {
                    flushBatch(proxy, batch, count);
                }
            }

            flushBatch(proxy, batch, count);
        }
    }

    private void flushBatch(DataConnectionApi proxy,
                            List<DigineousBlackBoxPayload> batch,
                            AtomicInteger count)
    {
        if (!batch.isEmpty())
        {
            System.out.printf(" Sending %d samples...\n", batch.size());
            JsonNode node = DataConnectionHelper.callEndpoint(proxy, InstanceConfigurationForDigineous.endpoint__DATA, null, batch, JsonNode.class);
            System.out.printf(" Sent %d total samples\n", count.addAndGet(batch.size()));
            batch.clear();
        }
    }
}
