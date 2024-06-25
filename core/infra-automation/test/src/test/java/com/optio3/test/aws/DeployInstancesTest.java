/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.aws;

import java.io.IOException;

import com.amazonaws.regions.Regions;
import com.optio3.infra.AwsHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.deploy.BuilderDeployer;
import com.optio3.infra.deploy.LdapDeployer;
import com.optio3.infra.deploy.NexusDeployer;
import com.optio3.test.infra.Optio3InfraTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class DeployInstancesTest extends Optio3InfraTest
{
    AwsHelper aws;

    @Before
    public void setup() throws
                        Exception
    {
        ensureCredentials(true, true);

        aws = AwsHelper.buildWithDirectoryLookup(credDir, WellKnownSites.optio3DomainName(), Regions.US_WEST_2);
    }

    //--//

    @Ignore("Manually enable to test, since it requires access to AWS")
    @Test
    public void testNexusInstance() throws
                                    IOException
    {
        NexusDeployer deployer = new NexusDeployer(credDir);
        deployer.deploy(true, false, false);
    }

    @Ignore("Manually enable to test, since it requires access to AWS")
    @Test
    public void testLdapInstance() throws
                                   IOException
    {
        LdapDeployer deployer = new LdapDeployer(credDir);
        deployer.deploy(true, false, false);
    }

    //--//

    @Ignore("Manually enable to test, since it requires access to AWS")
    @Test
    public void testBuilderInstance() throws
                                      IOException
    {
        BuilderDeployer deployer = new BuilderDeployer(credDir);
        deployer.deploy(true, false, false);
    }
}
