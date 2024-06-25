/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.orchestration.AbstractHubActivityHandler;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.BackgroundActivityStatus;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.scheduler.BackgroundActivityFilterPair;
import com.optio3.cloud.model.scheduler.BackgroundActivityFilterRequest;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.logging.Severity;
import com.optio3.serialization.Reflection;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import com.optio3.util.IdGenerator;
import com.optio3.util.TimeUtils;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

public class HubBackgroundActivityTest extends Optio3Test
{
    private static RecordLocator<BackgroundActivityRecord> loc_activity1;
    private static RecordLocator<BackgroundActivityRecord> loc_activity2;
    private static RecordLocator<BackgroundActivityRecord> loc_activity3;
    private static RecordLocator<BackgroundActivityRecord> loc_activity4;

    private static final List<Integer> activityDone = Lists.newArrayList();

    public static class TestActivityChunk
    {
        public int    valueInt;
        public String valueString;
    }

    public static class TestActivity extends AbstractHubActivityHandler implements BackgroundActivityHandler.ICleanupOnFailure
    {
        public int     state;
        public boolean cancelled;

        public TestActivity()
        {
        }

        @Override
        public String getTitle()
        {
            return "Test";
        }

        @Override
        public RecordLocator<? extends RecordWithCommonFields> getContext()
        {
            return null;
        }

        @BackgroundActivityMethod(needsSession = true)
        public void process(SessionHolder sessionHolder) throws
                                                         Exception
        {
            System.out.printf("Done TestActivity %d%n", state);

            TestActivityChunk chunk = new TestActivityChunk();
            chunk.valueInt    = (int) (1 + 100 * Math.random());
            chunk.valueString = IdGenerator.newGuid();
            putChunk("Test1", chunk);

            chunk          = new TestActivityChunk();
            chunk.valueInt = 1;
            addChunkToSequence("Seq", chunk);

            chunk          = new TestActivityChunk();
            chunk.valueInt = 2;
            addChunkToSequence("Seq", chunk);

            synchronized (activityDone)
            {
                activityDone.add(state);
            }

            markAsCompleted();
        }

        @Override
        public void cleanupOnFailure(Throwable t)
        {
            if (t instanceof CancellationException)
            {
                cancelled = true;
            }
        }
    }

    @ClassRule
    public static final TestApplicationWithDbRule<HubApplication, HubConfiguration> applicationRule = new TestApplicationWithDbRule<>(HubApplication.class, "hub-test.yml", (configuration) ->
    {
        if (false)
        {
            configuration.getDataSourceFactory().showSql = true;
        }

        configuration.startBackgroundProcessing             = false;
        configuration.noBackgroundProcessingDefragmentation = true;
    }, null);

    @Test
    @TestOrder(5)
    @SuppressWarnings("rawtypes")
    public void validateClasses() throws
                                  Exception
    {
        Reflections                                     reflections = new Reflections("com.optio3", new SubTypesScanner(false));
        Set<Class<? extends BackgroundActivityHandler>> candidates  = reflections.getSubTypesOf(BackgroundActivityHandler.class);

        for (Class<? extends BackgroundActivityHandler> candidate : candidates)
        {
            if (Reflection.isAbstractClass(candidate))
            {
                continue;
            }

            for (Constructor<?> constructor : candidate.getConstructors())
            {
                if (constructor.getParameterCount() == 0)
                {
                    System.out.printf("Checking %s...\n", candidate.getSimpleName());
                    BackgroundActivityHandler handler = Reflection.newInstance(candidate);
                    handler.validateStates();
                }
            }
        }
    }

    @Test
    @TestOrder(10)
    public void testCreate() throws
                             Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            ZonedDateTime now = TimeUtils.now();

            {
                TestActivity activity = BackgroundActivityHandler.allocate(TestActivity.class);
                activity.state = 1;

                ZonedDateTime            when = now.plus(500, ChronoUnit.MILLIS);
                BackgroundActivityRecord rec  = BackgroundActivityRecord.schedule(holder, activity, when);
                loc_activity1 = holder.createLocator(rec);
            }

            {
                TestActivity activity = BackgroundActivityHandler.allocate(TestActivity.class);
                activity.state = 2;

                ZonedDateTime            when = now.plus(10, ChronoUnit.HOURS);
                BackgroundActivityRecord rec  = BackgroundActivityRecord.schedule(holder, activity, when);
                rec.transitionToPaused();
                loc_activity2 = holder.createLocator(rec);
            }

            {
                TestActivity activity = BackgroundActivityHandler.allocate(TestActivity.class);
                activity.state = 3;

                ZonedDateTime            when = now.plus(2, ChronoUnit.SECONDS);
                BackgroundActivityRecord rec  = BackgroundActivityRecord.schedule(holder, activity, when);
                loc_activity3 = holder.createLocator(rec);
            }

            {
                TestActivity activity = BackgroundActivityHandler.allocate(TestActivity.class);
                activity.state = 4;

                ZonedDateTime            when = now.minus(2, ChronoUnit.SECONDS);
                BackgroundActivityRecord rec  = BackgroundActivityRecord.schedule(holder, activity, when);
                loc_activity4 = holder.createLocator(rec);
            }

            holder.commit();
        }
    }

    @Test
    @TestOrder(20)
    public void testList()
    {
        try (SessionHolder sessionHolder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            TypedRecordIdentityList<BackgroundActivityRecord> list;
            BackgroundActivityFilterRequest                   filters = new BackgroundActivityFilterRequest();

            filters.onlyReadyToGo = false;
            filters.statusFilter  = BackgroundActivityFilterPair.build(Lists.newArrayList(BackgroundActivityStatus.ACTIVE));
            filters.sortBy        = null;

            list = BackgroundActivityRecord.list(helper, null, filters);
            assertEquals(3, list.size());
            assertTrue(contains(list, loc_activity1));
            assertFalse(contains(list, loc_activity2));
            assertTrue(contains(list, loc_activity3));
            assertTrue(contains(list, loc_activity4));

            //--//

            filters.onlyReadyToGo = false;
            filters.statusFilter  = null;
            filters.sortBy        = RecordForBackgroundActivity.SortByNextActivation;

            list = BackgroundActivityRecord.list(helper, null, filters);
            assertEquals(4, list.size());
            assertTrue(contains(list, loc_activity1));
            assertTrue(contains(list, loc_activity2));
            assertTrue(contains(list, loc_activity3));
            assertTrue(contains(list, loc_activity4));

            //--//

            filters.onlyReadyToGo = true;
            filters.statusFilter  = null;
            filters.sortBy        = null;

            list = BackgroundActivityRecord.list(helper, null, filters);
            assertEquals(1, list.size());
            assertFalse(contains(list, loc_activity1));
            assertFalse(contains(list, loc_activity2));
            assertFalse(contains(list, loc_activity3));
            assertTrue(contains(list, loc_activity4));

            //--//

            filters.onlyReadyToGo = false;
            filters.statusFilter  = null;
            filters.sortBy        = RecordForBackgroundActivity.SortByNextActivation;

            list = BackgroundActivityRecord.list(helper, null, filters);
            assertEquals(4, list.size());

            int count = 0;

            for (var ri : BackgroundActivityRecord.findHandlers(sessionHolder, false, false, TestActivity.class, null))
            {
                BackgroundActivityRecord rec_task = sessionHolder.fromIdentity(ri);
                TestActivity             h        = (TestActivity) rec_task.getHandler(sessionHolder);

                switch (h.state)
                {
                    case 1:
                        assertEquals(loc_activity1.getId(), rec_task.getSysId());
                        break;

                    case 2:
                        assertEquals(loc_activity2.getId(), rec_task.getSysId());
                        break;

                    case 3:
                        assertEquals(loc_activity3.getId(), rec_task.getSysId());
                        break;

                    case 4:
                        assertEquals(loc_activity4.getId(), rec_task.getSysId());
                        break;

                    default:
                        fail();
                }

                count++;
            }

            assertEquals(4, count);
        }
    }

    @Test
    @TestOrder(21)
    public void testScheduler() throws
                                Exception
    {
        BackgroundActivityScheduler.LoggerInstance.enable(Severity.Debug);
        applicationRule.getApplication()
                       .startScheduler(null);

        Thread.sleep(2200);

        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<BackgroundActivityRecord> helper = holder.createHelper(BackgroundActivityRecord.class);

            TypedRecordIdentityList<BackgroundActivityRecord> list;
            BackgroundActivityFilterRequest                   filters = new BackgroundActivityFilterRequest();

            filters.onlyReadyToGo = false;
            filters.statusFilter  = BackgroundActivityFilterPair.build(Lists.newArrayList(BackgroundActivityStatus.ACTIVE));
            filters.sortBy        = null;

            list = BackgroundActivityRecord.list(helper, null, filters);
            assertEquals(0, list.size());

            for (RecordLocator<BackgroundActivityRecord> loc : Lists.newArrayList(loc_activity1, loc_activity2, loc_activity3, loc_activity4))
            {
                BackgroundActivityRecord rec     = holder.fromLocator(loc);
                TestActivity             handler = (TestActivity) rec.getHandler(holder);
                TestActivityChunk        chunk   = handler.getChunk("Test1", TestActivityChunk.class);
                if (rec.getStatus() == BackgroundActivityStatus.COMPLETED)
                {
                    assertNotNull(chunk);
                    assertNotNull(chunk.valueString);
                    assertTrue(chunk.valueInt >= 1);

                    handler.forEachChunkInSequence("Seq", TestActivityChunk.class, (seq, chunk2) ->
                    {
                        assertEquals((int) seq + 1, chunk2.valueInt);
                    });
                }
                else
                {
                    assertNull(chunk);
                }
            }
        }

        assertEquals(3, activityDone.size());
        assertEquals(1, activityDone.indexOf(1));
        assertEquals(-1, activityDone.indexOf(2));
        assertEquals(2, activityDone.indexOf(3));
        assertEquals(0, activityDone.indexOf(4));
    }

    @Ignore("Manually enable to test, since it is timing-sensitive")
    @Test
    @TestOrder(22)
    public void testLock() throws
                           Exception
    {
        Thread        thread;
        AtomicInteger state = new AtomicInteger(0);

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordLocked<BackgroundActivityRecord> lock = holder.fromLocatorWithLock(loc_activity1, 10, TimeUnit.SECONDS);
            BackgroundActivityRecord               rec  = lock.get();

            thread = new Thread(() -> subTestLock(rec, state));
            thread.start();

            System.out.println("testLock1");
            assertEquals(0, state.get());
            System.out.println("testLock2");
            Thread.sleep(5000);
            System.out.println("testLock3");
            assertEquals(0, state.get());
            System.out.println("testLock4");
        }

        Thread.sleep(100);
        System.out.println("testLock5");
        assertEquals(1, state.get());
        System.out.println("testLock6");

        thread.join(1000);
        System.out.println("testLock8");
    }

    private Object subTestLock(BackgroundActivityRecord rec,
                               AtomicInteger state)
    {
        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            System.out.println("subTestLock1");
            holder.beginTransaction();

            System.out.println("subTestLock2");
            RecordHelper<BackgroundActivityRecord> helper = holder.createHelper(BackgroundActivityRecord.class);

            rec = helper.get(rec.getSysId());
            System.out.println("subTestLock3");
            helper.optimisticallyUpgradeToLocked(rec, 10, TimeUnit.SECONDS);
            System.out.println("subTestLock4");

            state.set(1);
            System.out.println("subTestLock5");
        }

        return null;
    }

    @Test
    @TestOrder(25)
    public void testCancel() throws
                             InterruptedException
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<BackgroundActivityRecord> helper = holder.createHelper(BackgroundActivityRecord.class);

            BackgroundActivityRecord rec = RecordForBackgroundActivity.cancelActivity(helper, (String) loc_activity2.getId());
            holder.commit();
            assertTrue(rec.getStatus()
                          .isCancelling());
        }

        // Force a database update.
        applicationRule.getApplication()
                       .triggerScheduler();

        Thread.sleep(100);

        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            BackgroundActivityRecord rec = holder.fromLocator(loc_activity2);
            assertEquals(BackgroundActivityStatus.CANCELLED, rec.getStatus());
            TestActivity handler = (TestActivity) rec.getHandler(holder);
            assertTrue(handler.cancelled);
        }
    }

    @Test
    @TestOrder(30)
    public void testDelete()
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<BackgroundActivityRecord> helper = holder.createHelper(BackgroundActivityRecord.class);

            BackgroundActivityRecord rec;
            rec = helper.fromLocator(loc_activity1);
            helper.delete(rec);
            rec = helper.fromLocator(loc_activity2);
            helper.delete(rec);
            rec = helper.fromLocator(loc_activity3);
            helper.delete(rec);
            rec = helper.fromLocator(loc_activity4);
            helper.delete(rec);

            holder.commit();
        }
    }

    @Test
    @TestOrder(40)
    public void testChunks() throws
                             Exception
    {
        RecordLocator<BackgroundActivityRecord> loc;

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            TestActivity activity = BackgroundActivityHandler.allocate(TestActivity.class);

            BackgroundActivityRecord rec = BackgroundActivityRecord.schedule(holder, activity, TimeUtils.future(1, TimeUnit.HOURS));
            loc = holder.createLocator(rec);

            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<BackgroundActivityRecord> helper = holder.createHelper(BackgroundActivityRecord.class);

            BackgroundActivityRecord rec = helper.fromLocator(loc);

            TestActivity activity = (TestActivity) rec.getHandler(holder);
            try (OutputStream stream = activity.writeAsStream("testStream", 1024))
            {
                for (int i = 0; i < 10000; i++)
                {
                    stream.write(i);
                }
            }

            rec.setHandler(activity);

            holder.commit();
        }

        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<BackgroundActivityRecord> helper = holder.createHelper(BackgroundActivityRecord.class);

            BackgroundActivityRecord rec = helper.fromLocator(loc);

            TestActivity activity = (TestActivity) rec.getHandler(holder);
            try (InputStream stream = activity.readAsStream("testStream"))
            {
                for (int i = 0; i < 10000; i++)
                {
                    int read = stream.read();
                    assertFalse(read < 0);
                    assertEquals((byte) i, (byte) read);
                }
                assertTrue(stream.read() < 0);
            }
        }
    }

    private static boolean contains(TypedRecordIdentityList<BackgroundActivityRecord> list,
                                    RecordLocator<BackgroundActivityRecord> loc)
    {
        for (RecordIdentity ri : list)
        {
            if (ri.sysId.equals(loc.getId()))
            {
                return true;
            }
        }

        return false;
    }
}
