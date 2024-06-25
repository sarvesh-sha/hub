/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.twilio;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import com.optio3.infra.cellular.TwilioHelper;
import com.optio3.infra.twilio.model.Command;
import com.optio3.infra.twilio.model.DataSession;
import com.optio3.infra.twilio.model.RatePlan;
import com.optio3.infra.twilio.model.Sim;
import com.optio3.infra.twilio.model.UsageRecord;
import com.optio3.serialization.ObjectMappers;
import com.optio3.test.infra.Optio3InfraTest;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TwilioTest extends Optio3InfraTest
{
    TwilioHelper twilio;

    @Before
    public void setup() throws
                        Exception
    {
        ensureCredentials(false, true);

        twilio = TwilioHelper.buildIfPossible(credDir, null);
    }

    //--//

    @Ignore("Manually enable to test, since it requires access to Twilio.")
    @Test
    public void listSims()
    {
        try
        {
            List<Sim> sims = twilio.listSims(null, null, null);
            System.out.printf("Total sims: %d\n", sims.size());
            for (Sim.Status value : Sim.Status.values())
            {
                System.out.printf("%s sims: %d\n",
                                  value,
                                  CollectionUtils.filter(sims, (s) -> s.status == value)
                                                 .size());
            }

            System.out.println();

            for (Sim sim : sims)
            {
                System.out.printf("Sim: %s%n", ObjectMappers.prettyPrintAsJson(sim));
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

    @Ignore("Manually enable to test, since it requires access to Twilio.")
    @Test
    public void listActiveSimsUsage()
    {
        try
        {
            List<Sim> sims = twilio.listSims(null, null, null);

            ZonedDateTime end   = TimeUtils.now();
            ZonedDateTime start = end.minus(7, ChronoUnit.DAYS);

            for (Sim sim : sims)
            {
                if (sim.status == Sim.Status.ACTIVE)
                {
                    List<UsageRecord> usageRecords = twilio.getUsage(sim.sid, start, end, UsageRecord.Granularity.DAILY);
                    System.out.printf("Usage: %s%n", ObjectMappers.prettyPrintAsJson(usageRecords));

                    List<DataSession> sessions = twilio.getSessions(sim.sid, start, end);
                    System.out.printf("Sessions: %s%n", ObjectMappers.prettyPrintAsJson(sessions));
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

    @Ignore("Manually enable to test, since it requires access to Twilio.")
    @Test
    public void listPlans()
    {
        try
        {
            List<RatePlan> ratePlans = twilio.listRatePlans();
            for (RatePlan ratePlan : ratePlans)
            {
                System.out.printf("RatePlan: %s%n", ObjectMappers.prettyPrintAsJson(ratePlan));
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

    @Ignore("Manually enable to test, since it requires access to Twilio.")
    @Test
    public void activateSim()
    {
        try
        {
            String iccid = "8901260862291177763";

            Sim before = CollectionUtils.firstElement(twilio.listSims(null, iccid, null));
            System.out.printf("Before: %s%n", ObjectMappers.prettyPrintAsJson(before));

            twilio.updateSim(before.sid, "Test", Sim.Status.READY, "5CentsPerMB", false);

            Sim after = CollectionUtils.firstElement(twilio.listSims(null, iccid, null));
            System.out.printf("After: %s%n", ObjectMappers.prettyPrintAsJson(after));
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

    @Ignore("Manually enable to test, since it requires access to Twilio.")
    @Test
    public void sendSMS()
    {
        try
        {
            String iccid = "8901260852298281817";

            Sim sim = CollectionUtils.firstElement(twilio.listSims(null, iccid, null));

            Command cmd = twilio.sendTextCommand(sim.sid, "#*,APN,wireless.twilio.com");
            System.out.printf("Result: %s%n", ObjectMappers.prettyPrintAsJson(cmd));
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