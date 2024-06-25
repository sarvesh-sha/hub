/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

import static com.optio3.asyncawait.CompileTime.await;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;

public class Main
{
    public static void main(String args[]) throws
                                           Exception
    {
        CompletableFuture<Integer> futureA = new CompletableFuture<>();
        CompletableFuture<Integer> futureB = new CompletableFuture<>();

        System.out.println("Here is the async method we are going to call");
        System.out.println();
        System.out.println("    public static CompletableFuture<Integer> asyncAdd(CompletableFuture<Integer> a, CompletableFuture<Integer> b)");
        System.out.println("    {");
        System.out.println("        return completedFuture(await(a) + await(b));");
        System.out.println("    }");
        System.out.println();

        System.out.println("Calling the instrumented method, without ea async this would block");
        System.out.println("   result = futureA + futureB");
        System.out.println();

        CompletableFuture<Integer> result = asyncAdd(futureA, futureB);

        System.out.println("The method returned a future that is not completed: ");
        System.out.println("   result.isDone = " + result.isDone());
        System.out.println();

        System.out.println("Now we complete the futures that are blocking the async method");

        futureA.complete(1);
        futureB.complete(2);

        System.out.println("   futureA = " + futureA.join());
        System.out.println("   futureB = " + futureB.join());
        System.out.println();

        System.out.println("Result is complete because we have completed futureA and futureB");
        System.out.println("   result.isDone =" + result.isDone());
        System.out.println();

        System.out.println("And here is the result");

        final Integer resultValue = result.join();

        System.out.println("   result = " + resultValue);
        System.out.println();
    }

    public static CompletableFuture<Integer> asyncAdd(CompletableFuture<Integer> a,
                                                      CompletableFuture<Integer> b) throws
                                                                                    Exception
    {
        return completedFuture(await(a) + await(b));
    }
}
