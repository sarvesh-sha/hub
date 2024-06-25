/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.ses;

import java.util.List;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.model.IdentityDkimAttributes;
import com.amazonaws.services.simpleemail.model.IdentityVerificationAttributes;
import com.optio3.infra.AwsHelper;
import com.optio3.infra.GoDaddyHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.test.infra.Optio3InfraTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SimpleEmailServiceTest extends Optio3InfraTest
{
    static final String c_targetDomain = WellKnownSites.optio3DomainName();
//    static final String c_targetDomain = WellKnownSites.digineousDomainName();

    AwsHelper     aws;
    GoDaddyHelper godaddy;

    @Before
    public void setup() throws
                        Exception
    {
        ensureCredentials(false, true);

        aws     = AwsHelper.buildWithDirectoryLookup(credDir, WellKnownSites.optio3DomainName(), Regions.US_WEST_2);
        godaddy = new GoDaddyHelper(credDir, c_targetDomain);
    }

    //--//

    @Ignore("Manually enable to test, since it requires access to AWS and GoDaddy.")
    @Test
    public void verifyOptio3Io()
    {
        final String domain = c_targetDomain;

        IdentityVerificationAttributes status = aws.checkDomainVerification(domain);
        if (status != null && StringUtils.equals(status.getVerificationStatus(), "Success"))
        {
            System.out.printf("Domain %s already verified%n", domain);
            return;
        }

        String token = aws.getDomainVerificationToken(domain);

        godaddy.updateOrCreateTxtRecord(domain, "_amazonses." + domain, token, 86400);
        godaddy.updateOrCreateTxtRecord(domain, "_amazonses", token, 86400);
    }

    @Ignore("Manually enable to test, since it requires access to AWS and GoDaddy.")
    @Test
    public void verifyOptio3IoDKIM()
    {
        final String domain = c_targetDomain;

        IdentityDkimAttributes status = aws.checkDkimVerification(domain);
        if (status != null && StringUtils.equals(status.getDkimVerificationStatus(), "Success"))
        {
            System.out.printf("Domain %s already DKIM verified%n", domain);
            return;
        }

        List<String> tokens = aws.getDkimTokens(domain);

        for (String token : tokens)
        {
            godaddy.updateOrCreateCNameRecord(domain, token + "._domainkey", token + ".dkim.amazonses.com", 86400);
        }
    }

    @Ignore("Manually enable to test, since it requires access to AWS.")
    @Test
    public void sendEmail()
    {
        String id = aws.sendTextEmail("builder@optio3.io", "Hello World", "This is a simple test.", "davide.m@optio3.com");
        System.out.println(id);
    }

    @Ignore("Manually enable to test, since it requires access to AWS.")
    @Test
    public void sendEmailHtml()
    {
        String id = aws.sendHtmlEmail("builder@optio3.io", "Hello World", "<html><h1>This is a <i>simple</i> test.</html>", "davide.m@optio3.com");
        System.out.println(id);
    }

    @Ignore("Manually enable to test, since it requires access to AWS.")
    @Test
    public void sendSms()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Notification from Optio3\n");
        for (int i = 0; i < 20; i++)
        {
            sb.append(String.format("This is a simple test %d\n", i));
        }
        String id = aws.sendTextMessage(null, sb.toString(), "+1-425-xxx-xxxx");
        System.out.println(id);
    }
}
