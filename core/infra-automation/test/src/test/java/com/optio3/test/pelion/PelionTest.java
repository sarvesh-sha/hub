/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.pelion;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import com.optio3.concurrency.Executors;
import com.optio3.infra.cellular.PelionHelper;
import com.optio3.infra.pelion.model.DataUsage;
import com.optio3.infra.pelion.model.DataUsageByIpAddress;
import com.optio3.infra.pelion.model.StockOrders;
import com.optio3.infra.pelion.model.SubscriberResponse;
import com.optio3.infra.pelion.model.TariffObject;
import com.optio3.serialization.ObjectMappers;
import com.optio3.test.infra.Optio3InfraTest;
import com.optio3.util.CollectionUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class PelionTest extends Optio3InfraTest
{
    PelionHelper pelion;

    @Before
    public void setup() throws
                        Exception
    {
        ensureCredentials(false, true);

        pelion = PelionHelper.buildIfPossible(credDir, null);
    }

    //--//

    @Ignore("Manually enable to test, since it requires access to Pelion.")
    @Test
    public void listOrders()
    {
        try
        {
            List<StockOrders> orders = pelion.listStockOrders();
            System.out.printf("Total orders: %d\n", orders.size());

            System.out.println();

            for (StockOrders order : orders)
            {
                System.out.printf("Order: %s\n", ObjectMappers.prettyPrintAsJson(order));

                Set<String> sims = pelion.extractSimsFromOrder(order.orderId);
                System.out.printf(" Sims: %d\n", sims.size());

                for (String sim : sims)
                {
                    System.out.printf("   Sim: %s\n", sim);
                    Map<String, List<TariffObject>> tariff = pelion.getTariff(sim);

                    System.out.printf(" %s\n", ObjectMappers.prettyPrintAsJson(tariff));

                    if (tariff != null)
                    {
                        switch (sim)
                        {
                            case "8912230200152772956":
                            case "8912230200152772964":
                            case "8912230200152772972":
                            case "8912230200152772980":
                                for (List<TariffObject> tariffObjects : tariff.values())
                                {
                                    for (TariffObject tariffObject : tariffObjects)
                                    {
                                        System.out.printf("activate %s\n", ObjectMappers.prettyPrintAsJson(pelion.activate(sim, null, tariffObject.productSetID)));
                                        break;
                                    }
                                }
                                break;
                        }
                    }

                    SubscriberResponse sub = pelion.getSim(sim);
                    if (sub != null)
                    {
                        System.out.printf("apn %s\n", ObjectMappers.prettyPrintAsJson(pelion.getAPNLog(sub.physicalId)));
                        System.out.printf("apn details %s\n", ObjectMappers.prettyPrintAsJson(pelion.getAPNDetails(sub.physicalId)));
                    }
                }
            }
        }
        catch (ClientErrorException e)
        {
            Response response = e.getResponse();
            System.out.println(response.getMediaType());
            response.bufferEntity();
            Object o = response.getEntity();
            System.out.println(o);
        }
    }

    @Ignore("Manually enable to test, since it requires access to Pelion.")
    @Test
    public void listSims()
    {
        try
        {
            List<SubscriberResponse> sims = pelion.listSims();
            System.out.printf("Total sims: %d\n", sims.size());

            System.out.println();

            Semaphore GlobalRateLimiter = Executors.allocateSemaphore(1.5);

            CollectionUtils.transformInParallel(sims, GlobalRateLimiter, (sim) ->
            {
                DataUsage usage = pelion.getUsage(sim.physicalId,
                                                  ZonedDateTime.now()
                                                               .minus(30, ChronoUnit.DAYS),
                                                  ZonedDateTime.now());

                if (usage.mobileOriginated.isEmpty() && sim.networkState.isOnline)
                {
                    System.out.printf("Sim %s: %,d (no usage)\n", sim.physicalId, sim.dataUsage);
                }
                else
                {
                    System.out.printf("Sim %s: %,d\n", sim.physicalId, sim.dataUsage);
                }

                return sim;
            });

            CollectionUtils.transformInParallel(sims, GlobalRateLimiter, (sim) ->
            {
                DataUsageByIpAddress usage = pelion.getUsageByIpAddress(sim.physicalId,
                                                                        ZonedDateTime.now()
                                                               .minus(6, ChronoUnit.HOURS),
                                                                        ZonedDateTime.now());
                if (usage != null)
                {
                    System.out.printf("IpUsage %s: %s\n", sim.physicalId, ObjectMappers.prettyPrintAsJson(usage));
                }

                return sim;
            });
        }
        catch (ClientErrorException e)
        {
            Response response = e.getResponse();
            System.out.println(response.getMediaType());
            response.bufferEntity();
            Object o = response.getEntity();
            System.out.println(o);
        }
    }
}
