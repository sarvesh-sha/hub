/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.asyncawait;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Stopwatch;
import com.optio3.asyncawait.CompileTime;
import com.optio3.asyncawait.bootstrap.AsyncClassFileTransformer;
import com.optio3.asyncawait.converter.AsyncTransformer;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger;
import com.optio3.test.common.AutoRetryOnFailure;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.objectweb.asm.tree.analysis.AnalyzerException;

@Ignore("Disable because of the JDK 11 Module System messes up with Agent and Instrumentation")
public class InstrumentTest extends Optio3Test
{
    @BeforeClass
    public static void init()
    {
        CompileTime.bootstrap();
    }

    @Test
    @TestOrder(10)
    @AutoRetryOnFailure(retries = 1, reason = "Keep test output quiet on success")
    public void directTransform() throws
                                  AnalyzerException,
                                  IOException
    {
        String internalName = AsyncTransformer.getInternalName(AsyncSamples.class);

        CodeAnalysisLogger logger = null;
        if (failedOnFirstRun())
        {
            logger = CodeAnalysisLogger.createCallbackLogger(CodeAnalysisLogger.Level.TRACE, (s) -> System.err.println(s));
        }

        AsyncTransformer worker = new AsyncTransformer(InstrumentTest.class.getClassLoader(), internalName, logger);
        assertNotNull(worker.transform());
    }

    @Test
    @TestOrder(11)
    public void directTransformNegativeOutcome() throws
                                                 AnalyzerException,
                                                 IOException
    {
        CodeAnalysisLogger.Level reset = AsyncClassFileTransformer.VerbosityLevelOnFailure;
        try
        {
            AsyncClassFileTransformer.VerbosityLevelOnFailure = CodeAnalysisLogger.Level.OFF;

            transformShouldFail(SampleWithMonitorAroundBlockingCall.class, "Detected a blocking call in method");
            transformShouldFail(SampleWithBadReturnType.class, "does not return a CompletableFuture<T>");
        }
        finally
        {
            AsyncClassFileTransformer.VerbosityLevelOnFailure = reset;
        }
    }

    private void transformShouldFail(Class<?> clz,
                                     String expectedMessage)
    {
        try
        {
            AsyncClassFileTransformer.ignore(clz);

            String internalName = AsyncTransformer.getInternalName(clz);

            AsyncTransformer worker = new AsyncTransformer(InstrumentTest.class.getClassLoader(), internalName, null);
            assertNotNull(worker.transform());
            fail("This should have failed");
        }
        catch (AnalyzerException ex)
        {
            if (ex.getMessage()
                  .contains(expectedMessage))
            {
                return;
            }

            ex.printStackTrace();
            fail(String.format("Expecting exception with '%s' in the message, instead got %s", expectedMessage, ex.getMessage()));
            return;
        }
        catch (IOException e)
        {
            fail("This should not have happened");
        }
    }

    @Test
    @TestOrder(15)
    public void loadAgent() throws
                            InterruptedException,
                            ExecutionException
    {
        if (!AsyncClassFileTransformer.isRunning())
        {
            CompileTime.bootstrap();
            assertTrue(AsyncClassFileTransformer.isRunning());
        }
    }

    @Test
    @TestOrder(20)
    public void testSumAsync() throws
                               InterruptedException,
                               ExecutionException
    {
        CompletableFuture<Integer> arg1 = CompletableFuture.completedFuture(100);
        CompletableFuture<Integer> arg2 = new CompletableFuture<Integer>();
        CompletableFuture<Integer> arg3 = new CompletableFuture<Integer>();

        System.out.println("Calling async method...");
        CompletableFuture<Integer> t = AsyncSamples.sumAsync(null, 20, 1000, arg1, arg2, arg3);
        System.out.println("sumAsync returned");
        assertTrue(!t.isDone());

        System.out.println("Resolving arg2");
        arg2.complete(200);
        assertTrue(!t.isDone());
        System.out.println("Resolved");

        System.out.println("Resolving arg3");
        arg3.complete(400);
        assertTrue(t.isDone());
        System.out.println("Resolved");

        assertEquals(720, (int) t.get());
    }

    @Test
    @TestOrder(21)
    public void testSumAsyncWithLimit() throws
                                        InterruptedException,
                                        ExecutionException
    {
        CompletableFuture<Integer> arg1 = CompletableFuture.completedFuture(100);
        CompletableFuture<Integer> arg2 = new CompletableFuture<Integer>();
        CompletableFuture<Integer> arg3 = new CompletableFuture<Integer>();

        System.out.println("Calling async method...");
        CompletableFuture<Integer> t = AsyncSamples.sumAsync(null, 20, 200, arg1, arg2, arg3);
        System.out.println("sumAsync returned");
        assertTrue(!t.isDone());

        System.out.println("Resolving arg2");
        arg2.complete(500);
        assertTrue(t.isDone());
        System.out.println("Resolved");
        assertTrue(t.isCompletedExceptionally());
    }

    @Test
    @TestOrder(22)
    public void testSumAsyncWithCancellation() throws
                                               InterruptedException,
                                               ExecutionException,
                                               TimeoutException
    {
        CompletableFuture<Integer> arg1 = CompletableFuture.completedFuture(100);
        CompletableFuture<Integer> arg2 = new CompletableFuture<Integer>();
        CompletableFuture<Integer> arg3 = new CompletableFuture<Integer>();

        CompletableFuture<Integer> result = new CompletableFuture<Integer>();

        System.out.println("Calling async method...");
        CompletableFuture<Integer> t = AsyncSamples.sumAsync(result, 20, 200, arg1, arg2, arg3);
        System.out.println("sumAsync returned");
        assertTrue(!t.isDone());

        System.out.println("Resolving arg2");
        t.cancel(false);
        assertTrue(t.isDone());

        result.get(10, TimeUnit.SECONDS);
        assertTrue(result.isDone());

        assertEquals(120, (int) result.get());
    }

    @Test(timeout = 5_000L)
    @TestOrder(23)
    public void testSumAsyncWithTimeout() throws
                                          InterruptedException,
                                          ExecutionException,
                                          TimeoutException
    {
        CompletableFuture<Integer> arg1 = CompletableFuture.completedFuture(100);
        CompletableFuture<Integer> arg2 = new CompletableFuture<Integer>();
        CompletableFuture<Integer> arg3 = new CompletableFuture<Integer>();

        System.out.println("Calling async method...");
        CompletableFuture<Integer> t = AsyncSamples.sumAsyncWithTimeout(100, arg1, arg2, arg3);
        System.out.println("sumAsync returned");
        assertTrue(!t.isDone());

        try
        {
            t.get();
            fail("This should have failed");
        }
        catch (Exception e)
        {
            ExecutionException ee = assertCast(ExecutionException.class, e);
            assertCast(TimeoutException.class, ee.getCause());
        }
    }

    @Test(timeout = 5_000L)
    @TestOrder(24)
    public void testSumAsyncWithMonitor() throws
                                          InterruptedException,
                                          ExecutionException,
                                          TimeoutException
    {
        Object                     lock = new Object();
        CompletableFuture<Integer> arg1 = CompletableFuture.completedFuture(100);
        CompletableFuture<Integer> arg2 = new CompletableFuture<Integer>();
        CompletableFuture<Integer> arg3 = new CompletableFuture<Integer>();

        System.out.println("Calling async method...");
        CompletableFuture<Integer> t = AsyncSamples.sumAsyncWithMonitor(lock, 100, arg1, arg2, arg3);
        System.out.println("sumAsync returned");
        assertTrue(!t.isDone());

        try
        {
            t.get();
            fail("This should have failed");
        }
        catch (Exception e)
        {
            ExecutionException ee = assertCast(ExecutionException.class, e);
            assertCast(TimeoutException.class, ee.getCause());
        }
    }

    @Test(timeout = 5_000L)
    @TestOrder(25)
    public void sumAsyncWithForegroundProcessing() throws
                                                   Exception
    {
        System.out.println("Calling async method...");
        Stopwatch               sw = Stopwatch.createStarted();
        CompletableFuture<Void> t  = AsyncSamples.nopForegroundProcessing();
        System.out.println("sumAsync returned");
        sw.stop();
        assertTrue(sw.elapsed(TimeUnit.MILLISECONDS) < 50);
        assertTrue(!t.isDone());

        t.get(200, TimeUnit.MILLISECONDS);
    }

    @Test(timeout = 5_000L)
    @TestOrder(25)
    public void sumAsyncWithBackgroundProcessing() throws
                                                   InterruptedException,
                                                   ExecutionException,
                                                   TimeoutException
    {
        System.out.println("Calling async method...");
        Stopwatch               sw = Stopwatch.createStarted();
        CompletableFuture<Void> t  = AsyncSamples.nopBackgroundProcessing();
        System.out.println("sumAsync returned");
        sw.stop();
        assertTrue(sw.elapsed(TimeUnit.MILLISECONDS) < 50);
        assertTrue(!t.isDone());

        t.get(200, TimeUnit.MILLISECONDS);
    }

    @Test(timeout = 5_000L)
    @TestOrder(26)
    public void sumAsyncWithBackgroundProcessingAndDelay() throws
                                                           Exception
    {
        CompletableFuture<Integer> arg1 = CompletableFuture.completedFuture(100);
        CompletableFuture<Integer> arg2 = new CompletableFuture<Integer>();
        CompletableFuture<Integer> arg3 = new CompletableFuture<Integer>();

        System.out.println("Calling async method...");
        Stopwatch                  sw = Stopwatch.createStarted();
        CompletableFuture<Integer> t  = AsyncSamples.sumAsyncWithBackgroundProcessing(100, TimeUnit.MILLISECONDS, arg1, arg2, arg3);
        System.out.println("sumAsync returned");
        sw.stop();
        assertTrue(sw.elapsed(TimeUnit.MILLISECONDS) < 50);
        assertTrue(!t.isDone());

        arg2.complete(200);
        arg3.complete(400);

        assertEquals(700, (int) t.get());
    }

    @Test(timeout = 5_000L)
    @TestOrder(27)
    public void backgroundProcessingThatShouldNotExecute() throws
                                                           InterruptedException,
                                                           ExecutionException,
                                                           TimeoutException
    {
        Stopwatch sw;

        //
        // First pass, we want to cancel the processing before it even starts.
        //
        CompletableFuture<Void> done = new CompletableFuture<Void>();
        sw = Stopwatch.createStarted();
        CompletableFuture<Void> res = AsyncSamples.backgroundProcessingThatShouldNotExecute(10, TimeUnit.MILLISECONDS, done);
        sw.stop();
        assertTrue(sw.elapsed(TimeUnit.MILLISECONDS) < 5);
        assertTrue(!res.isDone());
        assertTrue(!done.isDone());

        res.cancel(false);
        assertTrue(res.isDone());

        Thread.sleep(30);
        assertTrue(!done.isDone());

        //
        // Second pass, we want to verify the processing would actually run if not cancelled.
        //
        CompletableFuture<Void> done2 = new CompletableFuture<Void>();
        sw = Stopwatch.createStarted();
        CompletableFuture<Void> res2 = AsyncSamples.backgroundProcessingThatShouldNotExecute(10, TimeUnit.MILLISECONDS, done2);
        sw.stop();
        assertTrue(sw.elapsed(TimeUnit.MILLISECONDS) < 5);
        assertTrue(!res2.isDone());
        assertTrue(!done2.isDone());

        Thread.sleep(20);
        assertTrue(done2.isDone());
    }

    @Test(timeout = 5_000L)
    @TestOrder(30)
    public void testThenCompose()
    {
        CompletableFuture<Integer> futureA = new CompletableFuture<>();
        CompletableFuture<Integer> futureB = new CompletableFuture<>();

        // calling the instrumented function
        // without async this would block
        CompletableFuture<Integer> result = AsyncSamples.asyncAdd2(futureA, futureB);

        // it is not done because futureA and futureB are not completed
        assertFalse(result.isDone());

        System.out.println("A");
        futureA.complete(1);
        System.out.println("B");
        futureB.completeExceptionally(new RuntimeException());
        System.out.println("C");

        // now the result is complete because we have completed futureA and futureB
        assertTrue(result.isDone());

        // and here is the result
        assertEquals((Integer) 1, result.join());
    }

    @Test(timeout = 5_000L)
    @TestOrder(31)
    public void testThenComposeWithException()
    {
        CompletableFuture<Integer> futureA = new CompletableFuture<>();
        CompletableFuture<Integer> futureB = new CompletableFuture<>();

        // calling the instrumented function
        // without async this would block
        CompletableFuture<Integer> result = AsyncSamples.asyncAdd2(futureA, futureB);

        // it is not done because futureA and futureB are not completed
        assertFalse(result.isDone());

        System.out.println("A");
        futureA.complete(1);
        System.out.println("B");
        futureB.complete(2);
        System.out.println("C");

        // now the result is complete because we have completed futureA and futureB
        assertTrue(result.isDone());

        // and here is the result
        assertEquals((Integer) 3, result.join());
    }

    @Test(timeout = 5_000L)
    @TestOrder(32)
    @AutoRetryOnFailure(retries = 1, reason = "Keep test output quiet on success")
    public void testThenComposeLong() throws
                                      Exception
    {
        CompletableFuture<Long> futureA = new CompletableFuture<>();
        CompletableFuture<Long> futureB = new CompletableFuture<>();

        // calling the instrumented function
        // without async this would block
        CompletableFuture<Long> result = AsyncSamples.asyncAddLong(futureA, futureB);

        // it is not done because futureA and futureB are not completed
        assertFalse(result.isDone());

        System.out.println("A");
        futureA.complete(1L << 38);
        System.out.println("B");
        futureB.complete(4L << 38);
        System.out.println("C");

        // now the result is complete because we have completed futureA and futureB
        assertTrue(result.isDone());

        // and here is the result
        assertEquals((Long) (5L << 38), result.join());
    }
    @Test
    @TestOrder(40)
    public void testAwaitInConstructorCall() throws
                                             InterruptedException,
                                             ExecutionException
    {
        CompileTime.bootstrap();
        CompletableFuture<Integer> futureA = new CompletableFuture<>();
        CompletableFuture<Integer> futureB = new CompletableFuture<>();

        CompletableFuture<Integer> result = AsyncSamples.awaitInConstructorCall(futureA, futureB);
        assertFalse(result.isDone());

        System.out.println("A");
        futureA.complete(1);
        System.out.println("B");
        futureB.complete(2);
        System.out.println("C");

        // now the result is complete because we have completed futureA and futureB
        assertTrue(result.isDone());

        // and here is the result
        assertEquals((Integer) 3, result.join());
    }
}
