/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package test;

import static com.optio3.asyncawait.CompileTime.await;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;

public class AsyncTest
{
    @Test(timeout = 5_000L)
    public void testThenCompose()
    {
        CompletableFuture<Integer> futureA = new CompletableFuture<>();
        CompletableFuture<Integer> futureB = new CompletableFuture<>();
        CompletableFuture<Integer> futureC = new CompletableFuture<>();

        // calling the instrumented function
        // without async this would block
        CompletableFuture<Integer> result = asyncAdd(futureA, futureB, futureC);

        // it is not done because futureA and futureB are not completed
        assertFalse(result.isDone());

        System.out.println("A");
        futureA.complete(1);
        System.out.println("B");
        futureB.complete(2);
        System.out.println("C");
        futureC.completeExceptionally(new RuntimeException());

        // now the result is complete because we have completed futureA and futureB
        assertTrue(result.isDone());

        // and here is the result
        assertEquals((Integer) 3, result.join());

    }

    public CompletableFuture<Integer> asyncAdd(CompletableFuture<Integer>... args)
    {
        int a = 0;
        for (CompletableFuture<Integer> v : args)
        {
            System.out.println(String.format("Before: %d", a));
            try
            {
                a += await(v);
            }
            catch (Exception e)
            {
                System.out.println(String.format("Got: %s", e));
            }
            System.out.println(String.format("After: %d", a));
        }
        return completedFuture(a);
    }
}
