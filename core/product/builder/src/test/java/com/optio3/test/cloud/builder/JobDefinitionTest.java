/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.builder;

import static org.junit.Assert.assertEquals;

import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.BuilderUserLogic;
import com.optio3.cloud.builder.persistence.config.RoleRecord;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.client.builder.api.JobDefinitionStepsApi;
import com.optio3.cloud.client.builder.api.JobDefinitionsApi;
import com.optio3.cloud.client.builder.api.UsersApi;
import com.optio3.cloud.client.builder.model.JobDefinition;
import com.optio3.cloud.client.builder.model.JobDefinitionStep;
import com.optio3.cloud.client.builder.model.JobDefinitionStepForDockerRun;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import org.junit.ClassRule;
import org.junit.Test;

public class JobDefinitionTest extends Optio3Test
{
    static String sysIdOfJob;
    static String sysIdOfStep1;

    @ClassRule
    public static final TestApplicationWithDbRule<BuilderApplication, BuilderConfiguration> applicationRule = new TestApplicationWithDbRule<>(BuilderApplication.class,
                                                                                                                                              "builder-test.yml",
                                                                                                                                              (configuration) ->
                                                                                                                                              {
                                                                                                                                              },
                                                                                                                                              null);

    @Test
    @TestOrder(1)
    public void testSetup()
    {
        UserRecord user = new UserRecord();
        user.setSysId("test@test.com");
        user.setEmailAddress("test@test.com");

        applicationRule.getSupport()
                       .getConfiguration().userLogic = new BuilderUserLogic(applicationRule.getApplication())
        {
            @Override
            protected UserRecord ensureUserRecord(SessionHolder holder,
                                                  String principal)
            {
                return user;
            }

            @Override
            protected boolean verifyUserPassword(SessionHolder holder,
                                                 UserRecord user,
                                                 String password)
            {
                return true;
            }

            @Override
            protected boolean changeUserPassword(SessionHolder holder,
                                                 UserRecord user,
                                                 String password)
            {
                return false;
            }

            @Override
            public String createUser(SessionHolder holder,
                                     String principal,
                                     String firstName,
                                     String lastName,
                                     String password)
            {
                return null;
            }

            @Override
            public boolean deleteUser(SessionHolder holder,
                                      UserRecord user)
            {
                return false;
            }

            @Override
            public boolean addUserToGroup(SessionHolder holder,
                                          String emailAddress,
                                          RoleRecord role)
            {
                return false;
            }

            @Override
            public boolean removeUserFromGroup(SessionHolder holder,
                                               String emailAddress,
                                               RoleRecord role)
            {
                return false;
            }
        };

        UsersApi proxy = applicationRule.createProxy("api/v1", UsersApi.class);

        proxy.login(user.getEmailAddress(), "any pwd");
    }

    @Test
    @TestOrder(10)
    public void testCreateJob()
    {
        JobDefinitionsApi proxy = applicationRule.createProxy("api/v1", JobDefinitionsApi.class);

        JobDefinition body = new JobDefinition();
        body.idPrefix = "test";
        body.name     = "test job";
        sysIdOfJob    = proxy.create(body).sysId;
    }

    @Test
    @TestOrder(20)
    public void testCreateStep()
    {
        JobDefinitionStepsApi proxy = applicationRule.createProxy("api/v1", JobDefinitionStepsApi.class);

        JobDefinitionStepForDockerRun body = new JobDefinitionStepForDockerRun();
        body.buildId          = "tets1";
        body.name             = "test step";
        body.image            = "bash";
        body.commandLine      = "ls /";
        body.workingDirectory = "/tmp";
        body.environmentVariables.put("key1", "val1");
        body.environmentVariables.put("key2", "val2");

        sysIdOfStep1 = proxy.create(sysIdOfJob, body).sysId;
    }

    @Test
    @TestOrder(30)
    public void testGetStep()
    {
        JobDefinitionStepsApi proxy = applicationRule.createProxy("api/v1", JobDefinitionStepsApi.class);

        JobDefinitionStep             body  = proxy.get(sysIdOfStep1);
        JobDefinitionStepForDockerRun body2 = assertCast(JobDefinitionStepForDockerRun.class, body);
        assertEquals("bash", body2.image);
        assertEquals("val1", body2.environmentVariables.get("key1"));
        assertEquals("val2", body2.environmentVariables.get("key2"));
    }
}
