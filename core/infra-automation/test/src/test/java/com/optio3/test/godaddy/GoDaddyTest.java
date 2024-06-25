/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.godaddy;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import com.optio3.infra.GoDaddyHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.godaddy.model.DNSRecord;
import com.optio3.infra.godaddy.model.RecordType;
import com.optio3.serialization.ObjectMappers;
import com.optio3.test.infra.Optio3InfraTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class GoDaddyTest extends Optio3InfraTest
{
    static final String c_targetDomain = WellKnownSites.optio3DomainName();
//    static final String c_targetDomain = WellKnownSites.digineousDomainName();

    GoDaddyHelper godaddy;

    @Before
    public void setup() throws
                        Exception
    {
        ensureCredentials(false, true);

        godaddy = new GoDaddyHelper(credDir, c_targetDomain);
    }

    //--//

    @Ignore("Manually enable to test, since it requires access to GoDaddy.")
    @Test
    public void listDomains()
    {
        try
        {
            final String domainName = c_targetDomain;

            List<DNSRecord> input = godaddy.listDomain(domainName, RecordType.A);
            for (DNSRecord dns : input)
            {
                System.out.printf("%s %s %s%n", dns.type, dns.name, dns.data);
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

    @Ignore("Manually enable to test, since it requires access to GoDaddy.")
    @Test
    public void dumpAllRecords() throws
                                 IOException
    {
        try
        {
            final String domainName = "optio3.com";

            List<DNSRecord> input = godaddy.listDomain(domainName);
            System.out.println(ObjectMappers.prettyPrintAsJson(input));
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

    @Ignore("Manually enable to test, since it requires access to GoDaddy.")
    @Test
    public void addTestRecord()
    {
        try
        {
            final String domainName = c_targetDomain;

            for (DNSRecord dns : godaddy.listDomain(domainName, RecordType.A))
            {
                System.out.printf("BEFORE: %s %s %s%n", dns.type, dns.name, dns.data);
            }

            godaddy.refreshRecord(domainName, "test.dev", null, "1.2.3.4", 600);

            for (DNSRecord dns : godaddy.listDomain(domainName, RecordType.A))
            {
                System.out.printf("AFTER 1: %s %s %s%n", dns.type, dns.name, dns.data);
            }

            godaddy.refreshRecord(domainName, "test.dev", null, "1.2.3.6", 600);

            for (DNSRecord dns : godaddy.listDomain(domainName, RecordType.A))
            {
                System.out.printf("AFTER 2: %s %s %s%n", dns.type, dns.name, dns.data);
            }

            godaddy.refreshRecord(domainName, "test.dev", "1.2.3.4", "1.2.3.5", 600);

            for (DNSRecord dns : godaddy.listDomain(domainName, RecordType.A))
            {
                System.out.printf("AFTER 3: %s %s %s%n", dns.type, dns.name, dns.data);
            }

            godaddy.refreshRecord(domainName, "test.dev", "1.2.3.5", null, 600);
            godaddy.refreshRecord(domainName, "test.dev", "1.2.3.6", null, 600);

            for (DNSRecord dns : godaddy.listDomain(domainName, RecordType.A))
            {
                System.out.printf("AFTER 4: %s %s %s%n", dns.type, dns.name, dns.data);
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
}
