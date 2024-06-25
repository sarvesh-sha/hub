/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.asyncawait;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.optio3.asyncawait.AsyncQueue;
import com.optio3.test.common.Optio3Test;
import org.junit.Test;

public class AsyncQueueTest extends Optio3Test
{
    @Test
    public void testQueueingWithNoConsumerWaiting() throws
                                                    InterruptedException,
                                                    ExecutionException
    {
        AsyncQueue<String> queue = new AsyncQueue<>();

        assertEquals(0, queue.size());

        queue.push("test1");
        assertEquals(1, queue.size());

        queue.push("test2");
        assertEquals(2, queue.size());

        queue.push("test3");
        assertEquals(3, queue.size());

        assertQueueReady(queue, "test1");
        assertQueueReady(queue, "test2");
        assertQueueReady(queue, "test3");
    }

    @Test
    public void testQueueingWithConsumerWaiting() throws
                                                  InterruptedException,
                                                  ExecutionException
    {
        AsyncQueue<String> queue = new AsyncQueue<>();

        assertEquals(0, queue.size());

        CompletableFuture<String> v = queue.pull();
        assertTrue(!v.isDone());

        queue.push("test1");
        assertEquals(0, queue.size());

        assertTrue(v.isDone());
        assertEquals("test1", v.get());

        queue.push("test2");
        assertEquals(1, queue.size());

        queue.push("test3");
        assertEquals(2, queue.size());

        assertQueueReady(queue, "test2");
        assertQueueReady(queue, "test3");
    }

    //--//

    private void assertQueueReady(AsyncQueue<String> queue,
                                  String expected) throws
                                                   InterruptedException,
                                                   ExecutionException
    {
        CompletableFuture<String> v = queue.pull();
        assertTrue(v.isDone());
        assertEquals(expected, v.get());
    }
}
