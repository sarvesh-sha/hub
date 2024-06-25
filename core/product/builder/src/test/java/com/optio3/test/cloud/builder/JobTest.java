/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.builder;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.BuilderUserLogic;
import com.optio3.cloud.builder.persistence.config.RoleRecord;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.builder.persistence.jobs.JobDefinitionRecord;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.client.builder.api.JobStepsApi;
import com.optio3.cloud.client.builder.api.JobsApi;
import com.optio3.cloud.client.builder.api.UsersApi;
import com.optio3.cloud.client.builder.model.JobStatus;
import com.optio3.cloud.client.builder.model.JobStep;
import com.optio3.cloud.client.builder.model.LogLine;
import com.optio3.cloud.persistence.LogBlock;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import org.junit.ClassRule;
import org.junit.Test;

public class JobTest extends Optio3Test
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
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            JobDefinitionRecord recDef = new JobDefinitionRecord();
            recDef.setIdPrefix("test");
            recDef.setName("test job");
            RecordHelper<JobDefinitionRecord> helperDef = holder.createHelper(JobDefinitionRecord.class);
            helperDef.persist(recDef);

            JobRecord rec = JobRecord.newInstance(recDef, null, null, null);
            rec.setStatus(com.optio3.cloud.builder.model.jobs.JobStatus.EXECUTING);
            rec.setIdPrefix("test");
            rec.setName("test job");

            RecordHelper<JobRecord> helper = holder.createHelper(JobRecord.class);
            helper.persist(rec);

            holder.commit();
            sysIdOfJob = rec.getSysId();
        }
    }

    @Test
    @TestOrder(20)
    public void testCreateStep()
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<JobRecord> helperJob = holder.createHelper(JobRecord.class);
            JobRecord               rec       = helperJob.get(sysIdOfJob);

            JobStepRecord recStep = JobStepRecord.newInstance(rec);
            recStep.setStatus(com.optio3.cloud.builder.model.jobs.JobStatus.EXECUTING);
            recStep.setName("test step");

            RecordHelper<JobStepRecord> helperStep = holder.createHelper(JobStepRecord.class);
            helperStep.persist(recStep);

            holder.commit();
            sysIdOfStep1 = recStep.getSysId();
        }
    }

    @Test
    @TestOrder(21)
    public void testGetStep()
    {
        JobStepsApi proxy = applicationRule.createProxy("api/v1", JobStepsApi.class);

        JobStep body = proxy.get(sysIdOfStep1);
        assertEquals("test step", body.name);
        assertEquals(JobStatus.EXECUTING, body.status);
    }

    @Test
    @TestOrder(30)
    public void testAddLog() throws
                             IOException
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordLocked<JobStepRecord> lock_step = holder.getEntityWithLock(JobStepRecord.class, sysIdOfStep1, 30, TimeUnit.SECONDS);

            try (var logHandler = JobStepRecord.allocateLogHandler(lock_step))
            {
                for (int blocks = 0; blocks < 10; blocks++)
                {
                    LogBlock block = new LogBlock();
                    for (int i = 0; i < 20; i++)
                         block.addLine("test " + i);

                    logHandler.saveBlock(block.items);
                }
            }

            holder.commit();
        }
    }

    @Test
    @TestOrder(31)
    public void testGetLog()
    {
        JobStepsApi proxy = applicationRule.createProxy("api/v1", JobStepsApi.class);

        JobStep body = proxy.get(sysIdOfStep1);
        assertEquals(20 * 10, (int) body.lastOffset);

        List<LogLine> lines = proxy.getLog(sysIdOfStep1, 0, 19, null);
        assertEquals(20, lines.size());
        for (int i = 0; i < 20; i++)
             assertEquals("test " + i, lines.get(i).line);

        List<LogLine> lines2 = proxy.getLog(sysIdOfStep1, 40, 40 + 19, null);
        assertEquals(20, lines2.size());
        for (int i = 0; i < 20; i++)
             assertEquals("test " + i, lines2.get(i).line);
    }

    @Test
    @TestOrder(40)
    public void testDeleteJob()
    {
        CookiePrincipal principal = applicationRule.getApplication()
                                                   .buildPrincipal("synthetic@local");

        principal.setEmbeddedRolesEx(WellKnownRole.Administrator);

        principal.disableAutoRefresh();
        principal.setExpirationDate(Duration.ofSeconds(60));
        JobsApi proxy = applicationRule.createProxyWithPrincipal("api/v1", JobsApi.class, principal);

        proxy.remove(sysIdOfJob, null);
    }
}
