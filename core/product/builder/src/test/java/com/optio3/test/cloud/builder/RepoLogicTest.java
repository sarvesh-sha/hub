/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.builder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;

import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.HostRemoter;
import com.optio3.cloud.builder.logic.build.BuildLogicForRepository;
import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.builder.persistence.worker.RepositoryCheckoutRecord;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Manually enable to test, since it performs some destructive operation on Git repos")
public class RepoLogicTest extends Optio3Test
{
    static RecordLocator<HostRecord>               loc_host;
    static RecordLocator<RepositoryRecord>         loc_repo;
    static RecordLocator<JobRecord>                loc_job;
    static RecordLocator<RepositoryCheckoutRecord> loc_checkout;

    @ClassRule
    public static final TestApplicationWithDbRule<BuilderApplication, BuilderConfiguration> applicationRule = new TestApplicationWithDbRule<>(BuilderApplication.class,
                                                                                                                                              "builder-test.yml",
                                                                                                                                              (configuration) ->
                                                                                                                                              {
                                                                                                                                              },
                                                                                                                                              null);

    @Test
    @TestOrder(10)
    public void testSetup()
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            HostRecord rec_host = new HostRecord();
            rec_host.setDomainName("localhost");
            rec_host.setIpAddress("127.0.0.1");
            holder.persistEntity(rec_host);

            RepositoryRecord rec_repo = new RepositoryRecord();
            rec_repo.setGitUrl("https://github.com/optio3/core.git");
            holder.persistEntity(rec_repo);

            //--//

            JobRecord rec_job = new JobRecord();
            rec_job.setStatus(JobStatus.INITIALIZED);
            rec_job.setName("test");
            holder.persistEntity(rec_job);

            loc_host = holder.createLocator(rec_host);
            loc_repo = holder.createLocator(rec_repo);
            loc_job  = holder.createLocator(rec_job);

            holder.flush();

            holder.commit();
        }
    }

    @Test
    @TestOrder(20)
    public void testAcquire() throws
                              Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            HostRecord       rec_host = holder.fromLocator(loc_host);
            RepositoryRecord rec_repo = holder.fromLocator(loc_repo);
            JobRecord        rec_job  = holder.fromLocator(loc_job);

            BuilderConfiguration config = applicationRule.getSupport()
                                                         .getConfiguration();

            BuildLogicForRepository repoLogic = new BuildLogicForRepository(config, holder, rec_host, rec_job);

            RepositoryCheckoutRecord repo_checkout = repoLogic.acquire(newStep(holder, rec_job, "testAcquire"), rec_repo);
            loc_checkout = holder.createLocator(repo_checkout);

            holder.commit();
        }
    }

    @Test
    @TestOrder(21)
    public void testAcquireFailureCleanup() throws
                                            Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            HostRecord       rec_host = holder.fromLocator(loc_host);
            RepositoryRecord rec_repo = holder.fromLocator(loc_repo);
            JobRecord        rec_job  = holder.fromLocator(loc_job);

            BuilderConfiguration config = applicationRule.getSupport()
                                                         .getConfiguration();

            BuildLogicForRepository  repoLogic     = new BuildLogicForRepository(config, holder, rec_host, rec_job);
            RepositoryCheckoutRecord repo_checkout = repoLogic.acquire(newStep(holder, rec_job, "testAcquireFailureCleanup"), rec_repo);
            assertNotEquals(loc_checkout.getId(), repo_checkout.getSysId());

            Path pathDb = repo_checkout.getDirectoryForDb()
                                       .getPath();
            Path pathWork = repo_checkout.getDirectoryForWork()
                                         .getPath();

            assertTrue(pathDb.toFile()
                             .exists());
            assertTrue(pathWork.toFile()
                               .exists());

            holder.rollback();

            assertFalse(pathDb.toFile()
                              .exists());
            assertFalse(pathWork.toFile()
                                .exists());
        }
    }

    @Test
    @TestOrder(30)
    public void testSwitch() throws
                             Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            HostRecord               rec_host      = holder.fromLocator(loc_host);
            JobRecord                rec_job       = holder.fromLocator(loc_job);
            RepositoryCheckoutRecord repo_checkout = holder.fromLocator(loc_checkout);

            BuilderConfiguration config = applicationRule.getSupport()
                                                         .getConfiguration();

            BuildLogicForRepository repoLogic = new BuildLogicForRepository(config, holder, rec_host, rec_job);

            repoLogic.switchToBranch(newStep(holder, rec_job, "testSwitch"), repo_checkout, "snmp", "8a039508616d9f2531713ba55752123d6974e22e");

            holder.commit();
        }
    }

    @Test
    @TestOrder(40)
    public void testRelease() throws
                              Exception
    {

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            HostRecord               rec_host      = holder.fromLocator(loc_host);
            JobRecord                rec_job       = holder.fromLocator(loc_job);
            RepositoryCheckoutRecord repo_checkout = holder.fromLocator(loc_checkout);

            BuilderConfiguration config = applicationRule.getSupport()
                                                         .getConfiguration();

            BuildLogicForRepository repoLogic = new BuildLogicForRepository(config, holder, rec_host, rec_job);

            repoLogic.release(repo_checkout);

            holder.commit();
        }
    }

    @Test
    @TestOrder(50)
    public void testDelete() throws
                             Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            JobRecord rec_job = holder.fromLocator(loc_job);
            holder.deleteEntity(rec_job);

            try (ValidationResultsHolder validation = new ValidationResultsHolder(holder, null, false))
            {
                HostRecord rec_host = holder.fromLocator(loc_host);
                rec_host.deleteRecursively(getHostRemoter(), validation);
            }

            holder.commit();
        }
    }

    //--//

    private HostRemoter getHostRemoter()
    {
        return applicationRule.getSupport()
                              .getConfiguration().hostRemoter;
    }

    private RecordLocked<JobStepRecord> newStep(SessionHolder holder,
                                                JobRecord rec_job,
                                                String name)
    {
        JobStepRecord rec_newStep = JobStepRecord.newInstance(rec_job);
        rec_newStep.setName(name);
        rec_newStep.setStatus(JobStatus.EXECUTING);
        return holder.persistEntity(rec_newStep);
    }
}
