/*
 * Copyright (C) 2017-2018, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import java.util.Set;

import ch.qos.logback.classic.Logger;
import com.azure.digitaltwins.core.BasicDigitalTwin;
import com.azure.digitaltwins.core.BasicDigitalTwinMetadata;
import com.azure.digitaltwins.core.BasicRelationship;
import com.google.common.collect.Sets;
import com.optio3.concurrency.Executors;
import com.optio3.infra.integrations.azuredigitaltwins.AzureDigitalTwinsHelper;
import com.optio3.infra.integrations.azuredigitaltwins.ExtendModel;
import com.optio3.infra.integrations.azuredigitaltwins.InterfaceModel;
import com.optio3.serialization.ObjectMappers;
import io.dropwizard.logging.LoggingUtil;
import org.junit.Ignore;
import org.junit.Test;

public class AzureDigitalTwinsTest
{
    @Ignore("Manually enable to test, since it requires access to AZURE")
    @Test
    public void listADT() throws
                          Exception
    {
        disableSlf4jLogger();

        var cred = new AzureDigitalTwinsHelper.Credentials();
        cred.tenantId     = "<tenantId>";
        cred.clientId     = "<clientId>";
        cred.clientSecret = "<clientSecret>";
        cred.endpoint     = "<URL>";

        AzureDigitalTwinsHelper adtHelper = new AzureDigitalTwinsHelper(cred);

        adtHelper.loadModels();

        Set<InterfaceModel> seen = Sets.newHashSet();

        for (InterfaceModel itf : adtHelper.interfaces.values())
        {
            displayItf(seen, itf);
        }
    }

    //--//

    @Ignore("Manually enable to test, since it requires access to AZURE")
    @Test
    public void testADT() throws
                          Exception
    {
        disableSlf4jLogger();

        var cred = new AzureDigitalTwinsHelper.Credentials();
        cred.tenantId     = "<tenantId>";
        cred.clientId     = "<clientId>";
        cred.clientSecret = "<clientSecret>";
        cred.endpoint     = "<URL>";

        AzureDigitalTwinsHelper adtHelper = new AzureDigitalTwinsHelper(cred);

        //--//

        var lst1 = adtHelper.queryTwins("SELECT * FROM DIGITALTWINS T WHERE IS_DEFINED(T.externalIds.Optio3)");
        System.out.printf("Query: %d\n", lst1.size());

        BasicDigitalTwin createdTwinA = adtHelper.getTwin("twin1");

        BasicDigitalTwin basicTwin1 = new BasicDigitalTwin("twin1").setMetadata(new BasicDigitalTwinMetadata().setModelId("dtmi:digitaltwins:rec_3_3:core:Asset;1"));
        adtHelper.setTwinProperty(basicTwin1, "1234", "name");
        adtHelper.setTwinProperty(basicTwin1, "test1", "externalIds", "Optio3");
        BasicDigitalTwin createdTwinB = adtHelper.createOrReplaceTwin(basicTwin1);

        BasicDigitalTwin createdTwinC = adtHelper.getTwin("twin1");
        System.out.printf("createdTwinA:\n%s\n", ObjectMappers.prettyPrintAsJson(createdTwinA));
        System.out.printf("createdTwinB:\n%s\n", ObjectMappers.prettyPrintAsJson(createdTwinB));
        System.out.printf("createdTwinC:\n%s\n", ObjectMappers.prettyPrintAsJson(createdTwinC));

        BasicDigitalTwin basicTwin2 = new BasicDigitalTwin("twin2").setMetadata(new BasicDigitalTwinMetadata().setModelId("dtmi:digitaltwins:rec_3_3:core:Asset;1"));
        adtHelper.setTwinProperty(basicTwin1, "test2", "name");
        BasicDigitalTwin createdTwinD = adtHelper.createOrReplaceTwin(basicTwin2);

        BasicDigitalTwin basicTwin3 = new BasicDigitalTwin("twin3").setMetadata(new BasicDigitalTwinMetadata().setModelId("dtmi:digitaltwins:rec_3_3:core:Asset;1"));
        adtHelper.setTwinProperty(basicTwin1, "test3", "name");
        BasicDigitalTwin createdTwinE = adtHelper.createOrReplaceTwin(basicTwin3);

        BasicRelationship rel1 = new BasicRelationship("rel1", createdTwinB.getId(), createdTwinD.getId(), "hasPart");
        rel1 = adtHelper.createOrReplaceRelationship(rel1);
        System.out.printf("rel1:\n%s\n", ObjectMappers.prettyPrintAsJson(rel1));

        BasicRelationship rel2 = new BasicRelationship("rel2", createdTwinB.getId(), createdTwinE.getId(), "hasPart");
        rel2 = adtHelper.createOrReplaceRelationship(rel2);
        System.out.printf("rel2:\n%s\n", ObjectMappers.prettyPrintAsJson(rel2));

        BasicRelationship rel3 = new BasicRelationship("rel1", createdTwinD.getId(), createdTwinB.getId(), "hasPart");
        rel3 = adtHelper.createOrReplaceRelationship(rel3);
        System.out.printf("rel3:\n%s\n", ObjectMappers.prettyPrintAsJson(rel3));

        System.out.printf("%s\n",
                          adtHelper.listRelationships(createdTwinB.getId())
                                   .size());

        System.out.printf("B rel: %s\n%s\n",
                          ObjectMappers.prettyPrintAsJson(adtHelper.listRelationships(createdTwinB.getId())),
                          ObjectMappers.prettyPrintAsJson(adtHelper.listReverseRelationships(createdTwinB.getId())));
        System.out.printf("D rel: %s\n%s\n",
                          ObjectMappers.prettyPrintAsJson(adtHelper.listRelationships(createdTwinD.getId())),
                          ObjectMappers.prettyPrintAsJson(adtHelper.listReverseRelationships(createdTwinD.getId())));
        System.out.printf("E rel: %s\n%s\n",
                          ObjectMappers.prettyPrintAsJson(adtHelper.listRelationships(createdTwinE.getId())),
                          ObjectMappers.prettyPrintAsJson(adtHelper.listReverseRelationships(createdTwinE.getId())));

        //--//

        for (int i = 0; i < 20; i++)
        {
            var lst2  = adtHelper.queryTwins("SELECT * FROM DIGITALTWINS T WHERE IS_DEFINED(T.externalIds.Optio3)");
            var lst2b = adtHelper.queryRelationships("SELECT * FROM RELATIONSHIPS");
            System.out.printf("Query2: %d %d %d\n", i, lst2.size(), lst2b.size());
            Executors.safeSleep(1000);
        }

        //--//

        adtHelper.deleteRelationship(createdTwinB.getId(), rel1.getId());
        adtHelper.deleteRelationship(createdTwinB.getId(), rel2.getId());
        adtHelper.deleteRelationship(createdTwinD.getId(), rel3.getId());

        adtHelper.deleteTwin("twin3");
        adtHelper.deleteTwin("twin1");
        adtHelper.deleteTwin("twin2");

        for (int i = 0; i < 20; i++)
        {
            var lst2 = adtHelper.queryTwins("SELECT * FROM DIGITALTWINS T WHERE IS_DEFINED(T.externalIds.Optio3)");
            System.out.printf("Query3: %d %d\n", i, lst2.size());
            Executors.safeSleep(1000);
        }
    }

    private void displayItf(Set<InterfaceModel> seen,
                            InterfaceModel itf)
    {
        if (seen.add(itf))
        {
            if (itf.superClasses != null)
            {
                for (ExtendModel value : itf.superClasses.values())
                {
                    displayItf(seen, value.target);
                }
            }

            System.out.println("#############################################################");
            System.out.println();
            System.out.println(ObjectMappers.prettyPrintAsJson(itf));
            System.out.println();
        }
    }

    private void disableSlf4jLogger()
    {
        final Logger root = LoggingUtil.getLoggerContext()
                                       .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.detachAndStopAllAppenders();
    }
}
